package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyNode;
import ic2.core.machine.MachineProcess;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Objects;

/**
 * Shared atomic processing; recipe lookup and outcome selection are the only machine-specific
 * parts.
 */
public abstract class ProcessingBlockEntity extends PoweredBlockEntity {
    public static final int INPUT = 0, OUTPUT = 1, BATTERY = 2;
    protected final MachineProcess process = new MachineProcess();
    private final MachineJournal<MachineProcess.State> journal =
            new MachineJournal<>(energy, process::state, process::restore, this::setChanged);
    private double experience;

    protected record Job(
            String recipe, int inputCount, ItemStackTemplate output, double experience) {
        protected Job {
            if (recipe.isBlank()
                    || inputCount < 1
                    || !Double.isFinite(experience)
                    || experience < 0) throw new IllegalArgumentException("Invalid processing job");
            Objects.requireNonNull(output);
        }
    }

    protected ProcessingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(
                type,
                pos,
                state,
                ((MachineBlock) state.getBlock()).kind().capacity(),
                ((MachineBlock) state.getBlock()).kind().slots());
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory, slot -> slot == INPUT && side != Direction.DOWN, slot -> slot == OUTPUT);
    }

    protected abstract Job findJob(ServerLevel level);

    protected void completed() {}

    @Override
    public EnergyNode.Terminal energyNode() {
        return EnergyNode.Terminal.sink(energy, 32, 1);
    }

    @Override
    public int progress() {
        return process.state().progress();
    }

    @Override
    public int progressMaximum() {
        return kind().ticks();
    }

    @Override
    public final void serverTick(ServerLevel level) {
        var battery = inventory.stack(BATTERY);
        double extracted =
                ElectricItemEnergy.discharge(battery, energy.free(), 1, false, true, false);
        if (extracted > 0) {
            inventory.set(BATTERY, ItemResource.of(battery), battery.getCount());
            energy.insert(extracted);
            setChanged();
        }
        Job job = findJob(level);
        var inputStack = inventory.stack(INPUT);
        var order =
                job == null
                        ? null
                        : new MachineProcess.WorkOrder(
                                job.recipe(), kind().ticks(), kind().euPerTick());
        boolean fits = false;
        if (job != null) {
            try (var simulation = Transaction.openRoot()) {
                fits =
                        inventory.insert(
                                        OUTPUT,
                                        ItemResource.of(job.output()),
                                        job.output().count(),
                                        simulation)
                                == job.output().count();
            }
        }
        MachineProcess.Outcome outcome;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            outcome = process.tick(order, fits, energy);
            if (outcome == MachineProcess.Outcome.COMPLETED) {
                if (inventory.extract(
                                        INPUT,
                                        ItemResource.of(inputStack),
                                        job.inputCount(),
                                        transaction)
                                != job.inputCount()
                        || inventory.insert(
                                        OUTPUT,
                                        ItemResource.of(job.output()),
                                        job.output().count(),
                                        transaction)
                                != job.output().count()) return;
            }
            transaction.commit();
        }
        if (outcome == MachineProcess.Outcome.COMPLETED) {
            experience += job.experience();
            completed();
            setChanged();
        }
        setActive(
                outcome == MachineProcess.Outcome.RUNNING
                        || outcome == MachineProcess.Outcome.COMPLETED);
    }

    public void awardExperience(Player player) {
        if (level instanceof ServerLevel server && experience >= 1) {
            int whole = (int) Math.min(Integer.MAX_VALUE, Math.floor(experience));
            experience -= whole;
            ExperienceOrb.award(server, player.position(), whole);
            setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        process.restore(
                new MachineProcess.State(
                        input.getStringOr("recipe", ""),
                        Math.clamp(input.getIntOr("progress", 0), 0, kind().ticks() - 1)));
        double savedExperience = input.getDoubleOr("xp", 0);
        experience = Double.isFinite(savedExperience) ? Math.max(0, savedExperience) : 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("recipe", process.state().recipe());
        output.putInt("progress", process.state().progress());
        output.putDouble("xp", experience);
    }
}
