package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.item.ElectricItem;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * A storage pad that pushes energy into whatever a player standing on it carries: main hand, off
 * hand, armor, hot bar, then the rest of the inventory, one item at a time. The pad keeps its
 * storage tier, accepts network power from every face but the marked output face, and can still
 * feed that face like the storage blocks it is derived from.
 */
public final class ChargepadBlockEntity extends PoweredBlockEntity {
    private static final AABB PAD = new AABB(0, 0, 0, 1, 0.9375, 1);
    private int cycle;

    public ChargepadBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                ((MachineBlock) state.getBlock()).kind().capacity(),
                0);
    }

    public int padOutput() {
        return kind().padOutput();
    }

    public Direction outputFace() {
        return getBlockState().getValue(MachineBlock.FACING);
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        int voltage = ic2.core.energy.VoltageTier.fromIcTier(kind().electricalTier()).getVoltage();
        return new EnergyNode.Terminal(
                energy,
                java.util.Optional.of(new EnergyNode.Output(voltage, 1)),
                java.util.Optional.of(new EnergyNode.Input(voltage, 2)));
    }

    @Override
    public boolean emitsTo(Direction side) {
        return side == outputFace();
    }

    @Override
    public boolean acceptsFrom(Direction side) {
        return !emitsTo(side);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(inventory, slot -> false, slot -> false);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (cycle++ % 2 != 0) return;
        var players =
                level.getEntitiesOfClass(
                        Player.class,
                        PAD.move(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()));
        boolean active = false;
        for (var player : players) {
            if (energy.stored() >= 1 && chargeInventory(player)) active = true;
        }
        setActive(active);
    }

    /** Charges the player's carried items in the recovered priority order. */
    public boolean chargeInventory(Player player) {
        int factor = padOutput();
        var order = new java.util.ArrayList<ItemStack>();
        order.add(player.getMainHandItem());
        order.add(player.getOffhandItem());
        var inventory = player.getInventory();
        for (int slot = 36; slot < 40; slot++) order.add(inventory.getItem(slot));
        for (int slot = 0; slot < 36; slot++) order.add(inventory.getItem(slot));
        for (var stack : order) {
            if (!stack.isEmpty() && chargeItem(stack, factor)) return true;
        }
        return false;
    }

    @Override
    public int progress() {
        return 0;
    }

    @Override
    public int progressMaximum() {
        return 0;
    }

    private boolean chargeItem(ItemStack stack, int factor) {
        if (stack.getItem() instanceof ElectricItem item) {
            double free = item.specification().capacity() - ElectricItemEnergy.charge(stack);
            double budget = Math.min((double) factor * 2, energy.stored());
            double amount = Math.min(free, budget);
            if (amount <= 0) return false;
            double accepted =
                    ElectricItemEnergy.charge(stack, amount, Integer.MAX_VALUE, true, false);
            if (accepted > 0) {
                energy.extract(accepted);
                return ElectricItemEnergy.charge(stack) < item.specification().capacity();
            }
            return false;
        }
        return false;
    }
}
