package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyMode;
import ic2.core.energy.grid.EnergyNode;
import ic2.core.machine.MachineProcess;
import ic2.core.machine.UpgradeProfile;
import ic2.neoforge.energy.EnergyConfig;
import ic2.neoforge.energy.WorldEnergyNetworks;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.transfer.UpgradeTransfers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Upgrade policy is shared; recipe lookup and machine transactions remain in each family. */
public abstract class UpgradeableBlockEntity extends PoweredBlockEntity {
    protected final MachineProcess process = new MachineProcess();
    protected final MachineJournal<MachineProcess.State> journal =
            new MachineJournal<>(energy, process::state, process::restore, this::setChanged);
    private UpgradeProfile upgrades;
    private int overclockers = -1, transformers = -1, storageUpgrades = -1;

    protected UpgradeableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(
                type,
                pos,
                state,
                ((MachineBlock) state.getBlock()).kind().capacity(),
                ((MachineBlock) state.getBlock()).kind().slots());
        refreshUpgrades();
    }

    public final UpgradeProfile upgradeProfile() {
        refreshUpgrades();
        return upgrades;
    }

    protected final void refreshUpgrades() {
        int speed = 0, tier = 0, storage = 0;
        for (int slot = kind().upgradeStart(); slot < inventory.size(); slot++) {
            var resource = inventory.getResource(slot);
            int count = Math.min(64, inventory.getAmountAsInt(slot));
            if (resource.getItem() instanceof UpgradeItem item && item.kind().suitable(kind())) {
                switch (item.kind()) {
                    case OVERCLOCKER -> speed += count;
                    case TRANSFORMER -> tier += count;
                    case ENERGY_STORAGE -> storage += count;
                    default -> {}
                }
            }
        }
        if (overclockers == speed && transformers == tier && storageUpgrades == storage) return;
        var next =
                UpgradeProfile.calculate(
                        kind().ticks(),
                        kind().euPerTick(),
                        kind().capacity(),
                        speed,
                        tier,
                        storage);
        if (upgrades != null)
            process.restore(
                    new MachineProcess.State(
                            process.state().recipe(),
                            next.rescaleProgress(process.state().progress(), upgrades.ticks())));
        upgrades = next;
        overclockers = speed;
        transformers = tier;
        storageUpgrades = storage;
        energy.resize(next.capacity());
        if (level instanceof ServerLevel server) WorldEnergyNetworks.invalidate(server);
        setChanged();
    }

    @Override
    protected void prepareEnergyLoad() {
        refreshUpgrades();
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        var profile = upgradeProfile();
        boolean gt = EnergyConfig.MODE.get() == EnergyMode.GT;
        return EnergyNode.Terminal.sink(
                energy,
                gt ? profile.workingVoltage() : profile.classicVoltage(),
                gt ? profile.amperage() : 1);
    }

    @Override
    public int progress() {
        return process.state().progress();
    }

    @Override
    public int progressMaximum() {
        return upgradeProfile().ticks();
    }

    protected final void beginProcessingTick() {
        refreshUpgrades();
        var battery = inventory.stack(2);
        double charge =
                ElectricItemEnergy.discharge(
                        battery, energy.free(), upgrades.itemTier(), false, true, false);
        if (charge > 0) {
            inventory.set(2, ItemResource.of(battery), battery.getCount());
            energy.insert(charge);
            setChanged();
        }
    }

    protected final void finishProcessingTick(ServerLevel level) {
        UpgradeTransfers.tick(level, this);
    }
}
