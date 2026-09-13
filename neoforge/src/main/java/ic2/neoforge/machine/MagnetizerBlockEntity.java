package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Magnetizes adjacent iron fence columns: two EU per boost share, paid from its small store. */
public final class MagnetizerBlockEntity extends PoweredBlockEntity {
    private int sinkAmps = 1;

    public MagnetizerBlockEntity(BlockPos pos, BlockState state) {
        // Legacy ContainerMagnetizer wiring: one discharge slot plus four upgrade slots.
        super(ModMachines.entityType(MachineKind.MAGNETIZER), pos, state, 100, 5);
    }

    public boolean canBoost() {
        return energy.stored() >= 2.0;
    }

    /** Spends the fence climber's share; several magnetizers split the cost. */
    public void boost(double multiplier) {
        energy.extract(2.0 * multiplier);
    }

    @Override
    public ic2.core.energy.grid.EnergyNode.Terminal energyNode() {
        return EnergyNode.Terminal.sink(
                energy,
                ic2.core.energy.VoltageTier.fromIcTier(kind().electricalTier()).getVoltage(),
                sinkAmps);
    }

    /**
     * Legacy TileEntityMagnetizer.setOverclockRates: each storage upgrade widens the buffer by
     * 10,000 EU and each transformer accepts one extra LV packet. Overclockers still fit (the
     * machine declares Augmentable) but do nothing: their legacy {@code distance()} extension has
     * no caller, and the redstone inverter stays inert because the boost path never reads a
     * redstone input in either codebase.
     */
    private void refreshUpgrades() {
        int storage = 0;
        int transformers = 0;
        for (int slot = kind().upgradeStart(); slot < inventory.size(); slot++) {
            int count = Math.min(64, inventory.getAmountAsInt(slot));
            if (count <= 0) continue;
            if (inventory.getResource(slot).getItem() instanceof UpgradeItem item) {
                switch (item.kind()) {
                    case ENERGY_STORAGE -> storage += count;
                    case TRANSFORMER -> transformers += count;
                    default -> {}
                }
            }
        }
        double capacity = 100.0 + 10000.0 * storage;
        int amps = Math.min(5, kind().electricalTier() + transformers);
        if (amps != sinkAmps || energy.capacity() != capacity) {
            sinkAmps = amps;
            energy.resize(capacity);
            setChanged();
        }
    }

    @Override
    public void serverTick(ServerLevel level) {
        refreshUpgrades();
    }

    @Override
    protected void prepareEnergyLoad() {
        // Save data restores upgrades before energy here, so the widened capacity is in place
        // before the stored EU is clamped — the same order legacy uses on load.
        refreshUpgrades();
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory, slot -> false, slot -> false, (slot, resource) -> false);
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
