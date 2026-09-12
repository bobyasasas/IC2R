package ic2.neoforge.machine;

import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Single drop-in slot on top of the kiln: items may only enter through the hatch's own face and
 * never leave except by falling into the firebox below.
 */
public final class CokeKilnHatchBlockEntity extends MachineBlockEntity {
    public CokeKilnHatchBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.COKE_KILN_HATCH), pos, state, 1);
    }

    public ItemStack input() {
        return inventory.stack(0);
    }

    public void consumeInput(int amount) {
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            inventory.extract(0, inventory.getResource(0), amount, transaction);
            transaction.commit();
        }
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> side == getBlockState().getValue(MachineBlock.FACING),
                slot -> false);
    }

    @Override
    public void serverTick(ServerLevel level) {}

    @Override
    public int progress() {
        return 0;
    }

    @Override
    public int progressMaximum() {
        return 0;
    }
}
