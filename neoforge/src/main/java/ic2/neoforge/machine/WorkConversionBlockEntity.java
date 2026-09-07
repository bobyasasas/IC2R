package ic2.neoforge.machine;

import ic2.core.energy.VoltageTier;
import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.api.WorkCapabilities;
import ic2.neoforge.energy.WorldEnergyNetworks;
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
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Native capability consumer: atomically converts the facing neighbor's HU/KU into buffered EU. */
public final class WorkConversionBlockEntity extends PoweredBlockEntity {
    private int voltage = 8;
    private double production;
    private final MachineJournal<Integer> journal =
            new MachineJournal<>(energy, () -> 0, ignored -> {}, this::setChanged);

    public WorkConversionBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                16384,
                0);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(inventory, slot -> false, slot -> false);
    }

    @Override
    public boolean emitsTo(Direction side) {
        return side != getBlockState().getValue(MachineBlock.FACING);
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        return EnergyNode.Terminal.source(energy, voltage, 1);
    }

    @Override
    public void serverTick(ServerLevel level) {
        production = 0;
        var facing = getBlockState().getValue(MachineBlock.FACING);
        var neighbor = worldPosition.relative(facing);
        boolean kinetic = kind() == MachineKind.KINETIC_GENERATOR;
        double ratio =
                kinetic
                        ? .25 * GenerationConfig.KINETIC_CONVERSION.get()
                        : .5 * GenerationConfig.STIRLING_CONVERSION.get();
        if (ratio <= 0
                || !level.getChunkSource().hasChunk(neighbor.getX() >> 4, neighbor.getZ() >> 4)) {
            setActive(false);
            return;
        }
        var source =
                level.getCapability(
                        kinetic ? WorkCapabilities.KINETIC : WorkCapabilities.HEAT,
                        neighbor,
                        facing.getOpposite());
        if (source != null) {
            double maximum = Math.min(kinetic ? 512 : 8192, source.bandwidth() * ratio);
            if (maximum > 0) {
                int nextVoltage = VoltageTier.fromPower(maximum).getVoltage();
                if (voltage != nextVoltage) {
                    voltage = nextVoltage;
                    WorldEnergyNetworks.invalidate(level);
                    setChanged();
                }
            }
            double capacity =
                    Math.max(
                            voltage + Math.ceil(Math.min(ratio, kinetic ? 512 : 8192)),
                            energy.stored());
            if (energy.capacity() != capacity) energy.resize(capacity);
            double desired = Math.min(energy.free(), Math.min(maximum, source.available() * ratio));
            if (desired > 0) {
                try (var transaction = Transaction.openRoot()) {
                    journal.updateSnapshots(transaction);
                    int consumed = source.extract((int) Math.ceil(desired / ratio), transaction);
                    production = energy.insert(Math.min(desired, consumed * ratio));
                    transaction.commit();
                }
            }
        }
        setActive(production > 0);
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
    public int menuValue(int index) {
        return index == 0 ? (int) Math.round(production * 100) : index == 1 ? voltage : 0;
    }

    @Override
    protected void prepareEnergyLoad() {
        energy.resize(16384);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        voltage =
                VoltageTier.fromPower(
                                Math.clamp(
                                        input.getIntOr("outputVoltage", 8),
                                        8,
                                        kind() == MachineKind.KINETIC_GENERATOR ? 512 : 8192))
                        .getVoltage();
        energy.resize(Math.max(voltage, energy.stored()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("outputVoltage", voltage);
    }
}
