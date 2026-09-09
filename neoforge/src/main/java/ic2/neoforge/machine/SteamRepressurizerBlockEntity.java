package ic2.neoforge.machine;

import ic2.core.machine.Repressurization;
import ic2.neoforge.api.WorkCapabilities;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.registration.BalanceConfig;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Trades IC2 steam plus heat for the external steam published under the common steam tag. The
 * candidate resolves every tick in stable registration-id order, so tag reloads take effect without
 * restarts and an already stored output keeps being used until it runs out.
 */
public final class SteamRepressurizerBlockEntity extends MachineBlockEntity
        implements FluidMachine {
    private static final TagKey<Fluid> EXTERNAL_STEAM =
            TagKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath("c", "steam"));

    private Supplier<List<Fluid>> steamSource = SteamRepressurizerBlockEntity::tagSteam;
    private Repressurization state = new Repressurization(0);
    private final StateJournal<Repressurization> journal =
            new StateJournal<>(() -> state, value -> state = value, this::setChanged);
    private final MachineFluidTank inputTank =
            new MachineFluidTank(
                    Repressurization.TANK,
                    this::setChanged,
                    SteamRepressurizerBlockEntity::ic2Steam);
    private final MachineFluidTank outputTank =
            new MachineFluidTank(Repressurization.TANK, this::setChanged, resource -> true);
    private final ResourceHandler<FluidResource> fluids =
            new ResourcePort<>(
                    new CombinedResourceHandler<>(inputTank, outputTank),
                    slot -> slot == 0,
                    slot -> slot == 1);
    private FluidResource candidate = FluidResource.EMPTY;

    public SteamRepressurizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.STEAM_REPRESSURIZER), pos, state, 0);
    }

    public MachineFluidTank inputTank() {
        return inputTank;
    }

    public MachineFluidTank outputTank() {
        return outputTank;
    }

    public Repressurization heatState() {
        return state;
    }

    /**
     * Replaces the common-tag lookup on this machine only, so a GameTest can emulate external steam
     * fluids and tag reloads; null restores the default tag source.
     */
    public void overrideExternalSteam(@Nullable Supplier<List<Fluid>> source) {
        steamSource = source == null ? SteamRepressurizerBlockEntity::tagSteam : source;
    }

    private static boolean ic2Steam(FluidResource resource) {
        return resource.getFluid() == ModFluids.FAMILIES.get(FluidDefinition.STEAM).source().get()
                || resource.getFluid()
                        == ModFluids.FAMILIES.get(FluidDefinition.SUPERHEATED_STEAM).source().get();
    }

    private static List<Fluid> tagSteam() {
        var candidates = new ArrayList<Fluid>();
        for (var holder : BuiltInRegistries.FLUID.getTagOrEmpty(EXTERNAL_STEAM))
            if (isOutputCandidate(holder.value())) candidates.add(holder.value());
        return candidates;
    }

    private static boolean isOutputCandidate(Fluid fluid) {
        return fluid.defaultFluidState().isSource()
                && fluid != ModFluids.FAMILIES.get(FluidDefinition.STEAM).source().get()
                && fluid
                        != ModFluids.FAMILIES.get(FluidDefinition.SUPERHEATED_STEAM).source().get();
    }

    /**
     * A stored compatible fluid stays in use ahead of the tag minimum, so a reload never swaps the
     * output while the previous choice is still inside the tank.
     */
    private FluidResource resolveOutput() {
        var stored = outputTank.getResource(0);
        if (!stored.isEmpty() && isOutputCandidate(stored.getFluid())) return stored;
        return steamSource.get().stream()
                .filter(SteamRepressurizerBlockEntity::isOutputCandidate)
                .min(Comparator.comparing(BuiltInRegistries.FLUID::getKey))
                .map(FluidResource::of)
                .orElse(FluidResource.EMPTY);
    }

    private static int rate(Fluid input) {
        return input == ModFluids.FAMILIES.get(FluidDefinition.SUPERHEATED_STEAM).source().get()
                ? BalanceConfig.STEAM_PER_SUPER_STEAM.get()
                : BalanceConfig.STEAM_PER_STEAM.get();
    }

    @Override
    public ResourceHandler<FluidResource> fluidAutomation(Direction side) {
        return fluids;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> slot == 0 && side == Direction.DOWN,
                slot -> slot == 1,
                (slot, resource) -> FluidContainerPort.accepts(resource));
    }

    @Override
    public void serverTick(ServerLevel level) {
        candidate = resolveOutput();
        boolean running = repressurize(level);
        setActive(running);
    }

    private boolean repressurize(ServerLevel level) {
        var input = inputTank.getResource(0);
        if (!ic2Steam(input) || candidate.isEmpty()) return false;
        var stored = outputTank.getResource(0);
        if (!stored.isEmpty() && !stored.equals(candidate)) return false;
        int inputAmount = inputTank.getAmountAsInt(0);
        int rate = rate(input.getFluid());
        if (rate == 0) return false;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            int request = state.heatRequest(inputAmount);
            int drawn = request > 0 ? drawHeat(level, request, transaction) : 0;
            if (drawn < 0 || drawn > request)
                throw new IllegalStateException("Heat source exceeded request");
            if (drawn > 0) state = new Repressurization(state.absorb(drawn));
            var step =
                    state.process(
                            inputAmount,
                            Repressurization.TANK - outputTank.getAmountAsInt(0),
                            rate);
            if (step.input() > 0
                    && inputTank.extract(0, input, step.input(), transaction) != step.input())
                return false;
            if (step.output() > 0
                    && outputTank.insert(0, candidate, step.output(), transaction) != step.output())
                return false;
            state = step.next();
            transaction.commit();
        }
        return true;
    }

    private int drawHeat(ServerLevel level, int request, Transaction transaction) {
        int heat = 0;
        for (var direction : Direction.values()) {
            var pos = worldPosition.relative(direction);
            if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
            var source = level.getCapability(WorkCapabilities.HEAT, pos, direction.getOpposite());
            if (source == null) continue;
            int extracted = source.extract(request - heat, transaction);
            if (extracted < 0 || extracted > request - heat)
                throw new IllegalStateException("Heat source exceeded request");
            heat += extracted;
            if (heat == request) break;
        }
        return heat;
    }

    @Override
    public int progress() {
        return state.reserve();
    }

    @Override
    public int progressMaximum() {
        return Repressurization.RESERVE_CAP;
    }

    @Override
    public int fuelRemaining() {
        return state.reserve();
    }

    @Override
    public int fuelMaximum() {
        return Repressurization.RESERVE_CAP;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> state.reserve();
            case 1 -> inputTank.getAmountAsInt(0);
            case 2 -> outputTank.getAmountAsInt(0);
            case 3 -> BuiltInRegistries.FLUID.getId(inputTank.getResource(0).getFluid());
            case 4 -> outputValue();
            default -> 0;
        };
    }

    /** Empty output reports -1 while no external steam candidate exists at all. */
    private int outputValue() {
        var stored = outputTank.getResource(0);
        if (!stored.isEmpty()) return BuiltInRegistries.FLUID.getId(stored.getFluid());
        return candidate.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(candidate.getFluid());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inputTank.deserialize(input.childOrEmpty("inputTank"));
        outputTank.deserialize(input.childOrEmpty("outputTank"));
        state =
                new Repressurization(
                        Math.clamp(input.getIntOr("reserve", 0), 0, Repressurization.RESERVE_CAP));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inputTank.serialize(output.child("inputTank"));
        outputTank.serialize(output.child("outputTank"));
        output.putInt("reserve", state.reserve());
    }
}
