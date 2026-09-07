package ic2.neoforge.machine;

import ic2.core.machine.MachineProcess;
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
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Objects;

/**
 * Shared atomic processing supports multiple outputs and additional resource costs. Recipe
 * selection and machine-specific environment updates remain in each family.
 */
public abstract class ProcessingBlockEntity extends UpgradeableBlockEntity {
    public static final int INPUT = 0, OUTPUT = 1, BATTERY = 2;
    private double experience;

    protected record Job(
            String recipe, int inputCount, List<ItemStackTemplate> outputs, double experience) {
        protected Job {
            if (recipe.isBlank()
                    || inputCount < 1
                    || !Double.isFinite(experience)
                    || experience < 0) throw new IllegalArgumentException("Invalid processing job");
            outputs = List.copyOf(outputs);
            if (outputs.size() > 3)
                throw new IllegalArgumentException("Too many processing outputs");
        }

        protected Job(String recipe, int inputCount, ItemStackTemplate output, double experience) {
            this(recipe, inputCount, List.of(Objects.requireNonNull(output)), experience);
        }
    }

    protected ProcessingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> slot == INPUT && side != Direction.DOWN,
                this::outputSlot,
                (slot, resource) ->
                        slot != INPUT
                                || level instanceof ServerLevel server
                                        && acceptsInput(resource, server));
    }

    protected abstract boolean acceptsInput(ItemResource resource, ServerLevel level);

    protected abstract Job findJob(ServerLevel level);

    protected boolean outputSlot(int slot) {
        return slot == OUTPUT;
    }

    protected void afterProcessing(ServerLevel level) {}

    protected boolean consumeInputs(Job job, ItemResource input, Transaction transaction) {
        return inventory.extract(INPUT, input, job.inputCount(), transaction) == job.inputCount();
    }

    protected boolean readyToProcess(Job job) {
        return true;
    }

    protected final boolean canFitOutputs(Job job) {
        try (var simulation = Transaction.openRoot()) {
            return insertOutputs(job, simulation);
        }
    }

    private boolean insertOutputs(Job job, Transaction transaction) {
        var port = new ResourcePort<>(inventory, this::outputSlot, slot -> false);
        for (var output : job.outputs()) {
            if (ResourceHandlerUtil.insertStacking(
                            port, ItemResource.of(output), output.count(), transaction)
                    != output.count()) return false;
        }
        return true;
    }

    protected void completed() {}

    @Override
    public final void serverTick(ServerLevel level) {
        beginProcessingTick();
        Job job = findJob(level);
        var inputStack = inventory.stack(INPUT);
        var order =
                job == null
                        ? null
                        : new MachineProcess.WorkOrder(
                                job.recipe(),
                                upgradeProfile().ticks(),
                                upgradeProfile().euPerTick());
        boolean fits = job != null && readyToProcess(job) && canFitOutputs(job);
        MachineProcess.Outcome outcome;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            outcome = process.tick(order, fits, energy);
            if (outcome == MachineProcess.Outcome.COMPLETED) {
                if (!consumeInputs(job, ItemResource.of(inputStack), transaction)
                        || !insertOutputs(job, transaction)) return;
            }
            transaction.commit();
        }
        if (outcome == MachineProcess.Outcome.COMPLETED) {
            experience += job.experience();
            completed();
            setChanged();
            for (int operation = 1;
                    operation < Math.min(64, upgradeProfile().operations());
                    operation++) {
                var extra = findJob(level);
                if (extra == null || !readyToProcess(extra)) break;
                var ingredient = inventory.stack(INPUT);
                try (var transaction = Transaction.openRoot()) {
                    if (!consumeInputs(extra, ItemResource.of(ingredient), transaction)
                            || !insertOutputs(extra, transaction)) break;
                    transaction.commit();
                }
                experience += extra.experience();
                completed();
            }
        }
        afterProcessing(level);
        finishProcessingTick(level);
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
                        Math.clamp(input.getIntOr("progress", 0), 0, progressMaximum() - 1)));
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
