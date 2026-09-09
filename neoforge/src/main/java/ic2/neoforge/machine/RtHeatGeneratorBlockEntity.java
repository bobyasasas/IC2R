package ic2.neoforge.machine;

import ic2.core.machine.RadioisotopeOutput;
import ic2.core.machine.WorkBuffer;
import ic2.neoforge.api.WorkSource;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModReactorItems;
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

import org.jspecify.annotations.Nullable;

/**
 * Radioisotope heat: every installed pellet doubles the emission of the whole machine and the fuel
 * is never consumed. Heat accumulates in a buffer that only drains through the front face, matching
 * the recovered single-sided heat source.
 */
public final class RtHeatGeneratorBlockEntity extends MachineBlockEntity {
    private final WorkBuffer work = new WorkBuffer(Integer.MAX_VALUE);
    private final StateJournal<WorkBuffer.State> journal =
            new StateJournal<>(work::state, work::restore, this::setChanged);

    public RtHeatGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.RT_HEAT_GENERATOR), pos, state, 6);
    }

    public int installed() {
        int count = 0;
        for (int slot = 0; slot < MachineKind.RT_HEAT_GENERATOR.slots(); slot++)
            if (isPellet(inventory.getResource(slot))) count++;
        return count;
    }

    public int maxOutput() {
        return RadioisotopeOutput.output(
                installed(), 2.0 * GenerationConfig.RADIOISOTOPE_HEAT.get());
    }

    private static boolean isPellet(ItemResource resource) {
        return resource.getItem() == ModReactorItems.RTG_PELLET.get();
    }

    public WorkSource output(@Nullable Direction side) {
        return new WorkOutput(this, side, work, this::maxOutput, journal::updateSnapshots);
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        return slot < MachineKind.RT_HEAT_GENERATOR.slots()
                ? isPellet(resource)
                : super.acceptsInventorySlot(slot, resource);
    }

    @Override
    protected int inventorySlotLimit(int slot) {
        return 1;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory, slot -> true, slot -> false, (slot, resource) -> isPellet(resource));
    }

    @Override
    public void serverTick(ServerLevel level) {
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            int target = maxOutput();
            if (target > 0) work.insert(target, target);
            transaction.commit();
        }
        setActive(work.state().stored() > 0);
    }

    @Override
    public int progress() {
        return (int) Math.min(Integer.MAX_VALUE, work.state().stored());
    }

    @Override
    public int progressMaximum() {
        return Math.max(1, maxOutput());
    }

    @Override
    public int fuelRemaining() {
        return (int) Math.min(Integer.MAX_VALUE, work.state().stored());
    }

    @Override
    public int fuelMaximum() {
        return Math.max(1, maxOutput());
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> installed();
            case 1 -> maxOutput();
            case 2 -> (int) Math.min(Integer.MAX_VALUE, work.state().stored());
            default -> 0;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        double stored = input.getDoubleOr("work", 0);
        work.restore(
                new WorkBuffer.State(
                        Double.isFinite(stored) ? Math.max(0, stored) : 0,
                        input.getLongOr("workTick", Long.MIN_VALUE),
                        input.getIntOr("workExtracted", 0)));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        var state = work.state();
        output.putDouble("work", state.stored());
        output.putLong("workTick", state.tick());
        output.putInt("workExtracted", state.extracted());
    }
}
