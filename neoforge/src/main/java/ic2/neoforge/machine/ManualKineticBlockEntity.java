package ic2.neoforge.machine;

import ic2.core.machine.ManualDrive;
import ic2.core.machine.WorkBuffer;
import ic2.neoforge.api.WorkSource;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jspecify.annotations.Nullable;

public final class ManualKineticBlockEntity extends MachineBlockEntity {
    private record State(ManualDrive.State drive, WorkBuffer.State work) {}

    private final ManualDrive drive = new ManualDrive();
    private final WorkBuffer work = new WorkBuffer(1000);
    private final StateJournal<State> journal =
            new StateJournal<>(
                    () -> new State(drive.state(), work.state()),
                    state -> {
                        drive.restore(state.drive());
                        work.restore(state.work());
                    },
                    this::setChanged);

    public ManualKineticBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.MANUAL_KINETIC_GENERATOR), pos, state, 0);
    }

    public WorkSource output(@Nullable Direction side) {
        return new WorkOutput(
                this, side, work, () -> 1000, journal::updateSnapshots, WorkOutput.Face.ANY);
    }

    public boolean turn(ServerPlayer player) {
        if (level == null
                || player.level() != level
                || player.distanceToSqr(
                                worldPosition.getX() + .5,
                                worldPosition.getY() + .5,
                                worldPosition.getZ() + .5)
                        > 64) return false;
        boolean simulated = player instanceof FakePlayer;
        ManualDrive.Result result;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            result =
                    drive.click(
                            level.getGameTime(),
                            player.getFoodData().getFoodLevel(),
                            simulated,
                            GenerationConfig.MANUAL_KINETIC.get(),
                            work);
            transaction.commit();
        }
        if (result.accepted()) {
            player.causeFoodExhaustion(.25f);
            if (!simulated)
                player.sendOverlayMessage(
                        Component.translatable("ic2.manual.added", result.added(), progress()));
        } else if (!simulated && player.getFoodData().getFoodLevel() <= 6)
            player.sendOverlayMessage(Component.translatable("ic2.manual.hungry"));
        return result.accepted();
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(inventory, slot -> false, slot -> false);
    }

    @Override
    public void serverTick(ServerLevel level) {
        /* Budgets reset lazily against world time. */
    }

    @Override
    public int progress() {
        return (int) work.state().stored();
    }

    @Override
    public int progressMaximum() {
        return 1000;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        double stored = input.getDoubleOr("kinetic", 0);
        work.restore(
                new WorkBuffer.State(
                        Double.isFinite(stored) ? Math.clamp(stored, 0, 1000) : 0,
                        input.getLongOr("workTick", Long.MIN_VALUE),
                        Math.clamp(input.getIntOr("workExtracted", 0), 0, 1000)));
        drive.restore(
                new ManualDrive.State(
                        input.getLongOr("clickTick", Long.MIN_VALUE),
                        Math.clamp(input.getIntOr("clicks", 0), 0, 10)));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("kinetic", work.state().stored());
        output.putLong("workTick", work.state().tick());
        output.putInt("workExtracted", work.state().extracted());
        output.putLong("clickTick", drive.state().tick());
        output.putInt("clicks", drive.state().clicks());
    }
}
