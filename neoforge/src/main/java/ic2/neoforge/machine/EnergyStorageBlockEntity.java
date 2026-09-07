package ic2.neoforge.machine;

import ic2.core.energy.StorageRedstoneMode;
import ic2.core.energy.VoltageTier;
import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.energy.WorldEnergyNetworks;
import ic2.neoforge.item.ElectricItem;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.Objects;
import java.util.Optional;

/** Four storage tiers share rules; the marked face is the sole output. */
public final class EnergyStorageBlockEntity extends PoweredBlockEntity {
    public static final int CHARGE = 0, DISCHARGE = 1;
    private StorageRedstoneMode mode = StorageRedstoneMode.IGNORE;
    private boolean sending = true;
    private int signal, comparator;

    public EnergyStorageBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                ((MachineBlock) state.getBlock()).kind().capacity(),
                2);
    }

    public int voltage() {
        return VoltageTier.fromIcTier(kind().electricalTier()).getVoltage();
    }

    public StorageRedstoneMode mode() {
        return mode;
    }

    public int signal() {
        return signal;
    }

    public int comparator() {
        return (int) Math.floor(15 * energy.stored() / energy.capacity());
    }

    @Override
    public int progress() {
        return 0;
    }

    @Override
    public int progressMaximum() {
        return 0;
    }

    @Override
    public boolean emitsTo(Direction side) {
        return side == getBlockState().getValue(MachineBlock.FACING);
    }

    @Override
    public boolean acceptsFrom(Direction side) {
        return !emitsTo(side);
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        return new EnergyNode.Terminal(
                energy,
                sending ? Optional.of(new EnergyNode.Output(voltage(), 1)) : Optional.empty(),
                Optional.of(new EnergyNode.Input(voltage(), 2)));
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        int slot = side == Direction.DOWN ? DISCHARGE : CHARGE;
        return new ResourcePort<>(
                inventory,
                index -> index == slot,
                index -> index == slot,
                (index, item) -> item.getItem() instanceof ElectricItem);
    }

    public void setMode(StorageRedstoneMode mode) {
        this.mode = Objects.requireNonNull(mode);
        setChanged();
        if (level instanceof ServerLevel server) refreshSignals(server);
    }

    private void refreshSignals(ServerLevel level) {
        boolean nextSending =
                mode.emitsEnergy(
                        level.hasNeighborSignal(worldPosition),
                        energy.stored(),
                        energy.capacity(),
                        voltage());
        if (nextSending != sending) {
            sending = nextSending;
            WorldEnergyNetworks.invalidate(level);
        }
        int nextSignal = mode.emitsSignal(energy.stored(), energy.capacity(), voltage()) ? 15 : 0;
        if (nextSignal != signal) {
            signal = nextSignal;
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        int nextComparator = comparator();
        if (nextComparator != comparator) {
            comparator = nextComparator;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    public void serverTick(ServerLevel level) {
        var charged = inventory.stack(CHARGE);
        double used =
                ElectricItemEnergy.charge(
                        charged, energy.stored(), kind().electricalTier(), false, false);
        if (used > 0) {
            energy.extract(used);
            inventory.set(CHARGE, ItemResource.of(charged), charged.getCount());
            setChanged();
        }
        var discharged = inventory.stack(DISCHARGE);
        double gained =
                ElectricItemEnergy.discharge(
                        discharged, energy.free(), kind().electricalTier(), false, true, false);
        if (gained > 0) {
            energy.insert(gained);
            inventory.set(DISCHARGE, ItemResource.of(discharged), discharged.getCount());
            setChanged();
        }
        refreshSignals(level);
    }

    @Override
    public int menuValue(int index) {
        return index == 0 ? mode.ordinal() : 0;
    }

    @Override
    public boolean menuAction(int id) {
        if (id < 0 || id >= StorageRedstoneMode.values().length) return false;
        setMode(StorageRedstoneMode.values()[id]);
        return true;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mode = StorageRedstoneMode.fromSavedId(input.getIntOr("redstoneMode", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("redstoneMode", mode.ordinal());
    }
}
