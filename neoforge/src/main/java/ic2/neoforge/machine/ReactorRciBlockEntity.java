package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.item.CondensatorItem;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Reactor coolant injector (legacy TileEntityAbstractRCI): consumes coolant blocks (redstone for
 * the RSH variant, lapis for LZH) at 1,000 EU per operation to fully recharge a grid condensator
 * whose stored heat passes 85%.
 */
public final class ReactorRciBlockEntity extends PoweredBlockEntity {
    private final boolean lzh;

    public ReactorRciBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                48000,
                13);
        lzh = ((MachineBlock) state.getBlock()).kind() == MachineKind.RCI_LZH;
    }

    private ItemStack coolantBlock() {
        return new ItemStack(
                lzh
                        ? net.minecraft.world.level.block.Blocks.LAPIS_BLOCK
                        : net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
    }

    private ItemStack targetCondensator() {
        var condensator =
                lzh
                        ? ic2.neoforge.registration.ModReactorItems.LZH_CONDENSATOR
                        : ic2.neoforge.registration.ModReactorItems.RSH_CONDENSATOR;
        return condensator.get().getDefaultInstance();
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        return resource.is(coolantBlock().getItem());
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ic2.neoforge.transfer.ResourcePort<>(inventory, slot -> slot < 9, slot -> false);
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        return EnergyNode.Terminal.sink(energy, 32, 1);
    }

    public NuclearReactorBlockEntity findReactor(ServerLevel level) {
        for (Direction direction : Direction.values()) {
            var neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor instanceof NuclearReactorBlockEntity reactor) return reactor;
            // Legacy RCIs also reach the grid through a chamber wall.
            if (neighbor instanceof ReactorChamberBlockEntity chamber) {
                var throughChamber = chamber.findReactor();
                if (throughChamber != null) return throughChamber;
            }
        }
        return null;
    }

    @Override
    public void serverTick(ServerLevel level) {
        var reactor = findReactor(level);
        if (reactor == null) {
            setActive(false);
            return;
        }
        boolean worked = false;
        for (int slot = 0; slot < 9; slot++) {
            var stack = reactor.inventory().stack(slot);
            if (!stack.is(targetCondensator().getItem())) continue;
            var condensator = (CondensatorItem) stack.getItem();
            if (condensator.storedHeat(stack) * 100.0 / condensator.maxUse() <= 85.0) continue;
            if (energy.stored() < 1000) break;
            try (var transaction = Transaction.openRoot()) {
                if (inventory.extract(slot, ItemResource.of(coolantBlock()), 1, transaction) != 1)
                    break;
                var charged = stack.copy();
                charged.set(ic2.neoforge.component.ModDataComponents.REACTOR_HEAT, 0);
                reactor.inventory().set(slot, ItemResource.of(charged), 1);
                energy.extract(1000);
                transaction.commit();
                worked = true;
                break;
            }
        }
        setActive(worked);
    }

    @Override
    public int progress() {
        return 0;
    }

    @Override
    public int progressMaximum() {
        return 0;
    }
}
