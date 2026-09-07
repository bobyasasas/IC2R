package ic2.neoforge.machine;

import ic2.core.energy.TransformerMode;
import ic2.core.energy.VoltageTier;
import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.energy.EnergyConfig;
import ic2.neoforge.energy.WorldEnergyNetworks;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.Objects;
import java.util.Optional;

/** Reversible 4:1 voltage/current conversion with direction changes at the topology boundary. */
public final class TransformerBlockEntity extends PoweredBlockEntity {
    private TransformerMode mode = TransformerMode.REDSTONE;
    private boolean stepUp, initialized;

    public TransformerBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                ((MachineBlock) state.getBlock()).kind().capacity(),
                0);
    }

    public TransformerMode mode() {
        return mode;
    }

    public boolean stepUp() {
        return stepUp;
    }

    private int lowVoltage() {
        return VoltageTier.fromIcTier(kind().electricalTier()).getVoltage();
    }

    public int inputVoltage() {
        return lowVoltage() * (stepUp ? 1 : 4);
    }

    public int outputVoltage() {
        return lowVoltage() * (stepUp ? 4 : 1);
    }

    public int inputAmps() {
        return stepUp ? 4 : 1;
    }

    public int outputAmps() {
        return stepUp ? 1 : 4;
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
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(inventory, slot -> false, slot -> false);
    }

    @Override
    public boolean emitsTo(Direction side) {
        return (side == getBlockState().getValue(MachineBlock.FACING)) == stepUp;
    }

    @Override
    public boolean acceptsFrom(Direction side) {
        return !emitsTo(side);
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        return new EnergyNode.Terminal(
                energy,
                Optional.of(new EnergyNode.Output(outputVoltage(), outputAmps(), 1, outputAmps())),
                Optional.of(new EnergyNode.Input(inputVoltage(), inputAmps())));
    }

    public void setMode(TransformerMode mode) {
        this.mode = Objects.requireNonNull(mode);
        setChanged();
        if (level instanceof ServerLevel server) refreshMode(server);
    }

    private void refreshMode(ServerLevel level) {
        boolean next = mode.stepUp(level.hasNeighborSignal(worldPosition));
        if (initialized && stepUp == next) return;
        if (initialized
                && TransformerMode.unsafeSwitch(
                        EnergyConfig.MODE.get(), stepUp, next, energy.stored())) {
            // Full IC2 blast propagation is tracked in M12, as for other network faults.
            if (EnergyConfig.MACHINE_EXPLOSIONS.get()) {
                level.removeBlock(worldPosition, false);
                level.explode(
                        null,
                        worldPosition.getX() + .5,
                        worldPosition.getY() + .5,
                        worldPosition.getZ() + .5,
                        2.5f,
                        Level.ExplosionInteraction.BLOCK);
            }
            return;
        }
        initialized = true;
        stepUp = next;
        setActive(stepUp);
        WorldEnergyNetworks.invalidate(level);
        setChanged();
    }

    @Override
    public void serverTick(ServerLevel level) {
        refreshMode(level);
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> mode.ordinal();
            case 1 -> inputVoltage();
            case 2 -> inputAmps();
            case 3 -> outputVoltage();
            case 4 -> outputAmps();
            default -> 0;
        };
    }

    @Override
    public boolean menuAction(int id) {
        if (id < 0 || id >= TransformerMode.values().length) return false;
        setMode(TransformerMode.values()[id]);
        return true;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mode = TransformerMode.fromSavedId(input.getIntOr("mode", 0));
        initialized = false;
        stepUp = mode.stepUp(level != null && level.hasNeighborSignal(worldPosition));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("mode", mode.ordinal());
    }
}
