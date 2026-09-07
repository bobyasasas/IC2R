package ic2.neoforge.machine;

import ic2.core.machine.CannerMode;
import ic2.core.machine.MachineProcess;
import ic2.neoforge.recipe.CannerInput;
import ic2.neoforge.recipe.EnrichingRecipe;
import ic2.neoforge.recipe.SolidCanningRecipe;
import ic2.neoforge.registration.ModCannerRecipes;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.MachineFluidTank;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Objects;

/** Four typed operations share one atomic boundary for fluids, items, EU and progress. */
public final class CannerBlockEntity extends UpgradeableBlockEntity implements FluidMachine {
    public static final int ADDITIVE = 0, OUTPUT = 1, BATTERY = 2, CONTAINER = 3;
    private final MachineFluidTank inputTank =
            new MachineFluidTank(8000, this::setChanged, fluid -> true);
    private final MachineFluidTank outputTank =
            new MachineFluidTank(8000, this::setChanged, fluid -> true);
    private final RecipeManager.CachedCheck<CannerInput, SolidCanningRecipe> solids =
            RecipeManager.createCheck(ModCannerRecipes.SOLID.get());
    private final RecipeManager.CachedCheck<CannerInput, EnrichingRecipe> enrichments =
            RecipeManager.createCheck(ModCannerRecipes.ENRICH.get());
    private CannerMode mode = CannerMode.BOTTLE_SOLID;

    @FunctionalInterface
    private interface Operation {
        boolean apply(TransactionContext transaction);
    }

    private record Job(String key, Operation operation) {}

    public CannerBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.CANNER), pos, state);
    }

    public CannerMode mode() {
        return mode;
    }

    public MachineFluidTank inputTank() {
        return inputTank;
    }

    public MachineFluidTank outputTank() {
        return outputTank;
    }

    public void setMode(CannerMode mode) {
        Objects.requireNonNull(mode);
        if (this.mode == mode) return;
        this.mode = mode;
        if (level != null && !level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        process.restore(new MachineProcess.State("", 0));
        setActive(false);
        setChanged();
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> mode.id();
            case 1 -> inputTank.getAmountAsInt(0);
            case 2 -> outputTank.getAmountAsInt(0);
            case 3 -> BuiltInRegistries.FLUID.getId(inputTank.getResource(0).getFluid());
            case 4 -> BuiltInRegistries.FLUID.getId(outputTank.getResource(0).getFluid());
            default -> 0;
        };
    }

    @Override
    public boolean menuAction(int id) {
        if (id >= 0 && id < CannerMode.values().length) {
            setMode(CannerMode.byId(id));
            return true;
        }
        return id == 5 && swapTanks();
    }

    public boolean swapTanks() {
        if (progress() != 0) return false;
        try (var transaction = Transaction.openRoot()) {
            FluidResource input = inputTank.getResource(0), output = outputTank.getResource(0);
            int inAmount = inputTank.getAmountAsInt(0), outAmount = outputTank.getAmountAsInt(0);
            if (inAmount > 0 && inputTank.extract(0, input, inAmount, transaction) != inAmount)
                return false;
            if (outAmount > 0 && outputTank.extract(0, output, outAmount, transaction) != outAmount)
                return false;
            if (outAmount > 0 && inputTank.insert(0, output, outAmount, transaction) != outAmount)
                return false;
            if (inAmount > 0 && outputTank.insert(0, input, inAmount, transaction) != inAmount)
                return false;
            transaction.commit();
            return true;
        }
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> slot == CONTAINER || slot == ADDITIVE && mode.acceptsAdditive(),
                slot -> slot == OUTPUT,
                this::acceptsAutomationInput);
    }

    private boolean acceptsAutomationInput(int slot, ItemResource resource) {
        if (!(level instanceof ServerLevel server)) return false;
        var stack = resource.toStack(1);
        var recipes = server.recipeAccess().recipeMap();
        if (slot == CONTAINER) {
            if (mode == CannerMode.BOTTLE_SOLID)
                return recipes.byType(ModCannerRecipes.SOLID.get()).stream()
                        .anyMatch(holder -> holder.value().container().ingredient().test(stack));
            return ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM) != null;
        }
        return slot == ADDITIVE
                && switch (mode) {
                    case BOTTLE_SOLID ->
                            recipes.byType(ModCannerRecipes.SOLID.get()).stream()
                                    .anyMatch(
                                            holder ->
                                                    holder.value()
                                                            .additive()
                                                            .ingredient()
                                                            .test(stack));
                    case ENRICH_LIQUID ->
                            recipes.byType(ModCannerRecipes.ENRICH.get()).stream()
                                    .anyMatch(
                                            holder ->
                                                    holder.value()
                                                            .additive()
                                                            .ingredient()
                                                            .test(stack));
                    default -> false;
                };
    }

    public ResourceHandler<FluidResource> fluidAutomation(Direction side) {
        // Two indexed tanks expose insertion only at input and extraction only at output.
        return new ResourceHandler<>() {
            @Override
            public int size() {
                return 2;
            }

            private MachineFluidTank tank(int index) {
                Objects.checkIndex(index, 2);
                return index == 0 ? inputTank : outputTank;
            }

            @Override
            public FluidResource getResource(int index) {
                return tank(index).getResource(0);
            }

            @Override
            public long getAmountAsLong(int index) {
                return tank(index).getAmountAsLong(0);
            }

            @Override
            public long getCapacityAsLong(int index, FluidResource resource) {
                return tank(index).getCapacityAsLong(0, resource);
            }

            @Override
            public boolean isValid(int index, FluidResource resource) {
                tank(index);
                return index == 0;
            }

            @Override
            public int insert(
                    int index, FluidResource resource, int amount, TransactionContext transaction) {
                tank(index);
                TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
                return index == 0 ? inputTank.insert(0, resource, amount, transaction) : 0;
            }

            @Override
            public int extract(
                    int index, FluidResource resource, int amount, TransactionContext transaction) {
                tank(index);
                TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
                return index == 1 ? outputTank.extract(0, resource, amount, transaction) : 0;
            }
        };
    }

    private ResourceHandler<FluidResource> containerHandler() {
        if (inventory.stack(CONTAINER).isEmpty()) return null;
        var port = new ResourcePort<>(inventory, slot -> slot == OUTPUT, slot -> slot == CONTAINER);
        return ItemAccess.forHandlerIndex(port, CONTAINER)
                .oneByOne()
                .getCapability(Capabilities.Fluid.ITEM);
    }

    private Job findJob(ServerLevel level) {
        var input =
                new CannerInput(
                        inventory.stack(CONTAINER),
                        inventory.stack(ADDITIVE),
                        inputTank.getResource(0),
                        inputTank.getAmountAsInt(0));
        return switch (mode) {
            case BOTTLE_SOLID ->
                    solids.getRecipeFor(input, level)
                            .map(
                                    holder -> {
                                        var recipe = holder.value();
                                        return new Job(
                                                holder.id().identifier().toString(),
                                                transaction ->
                                                        inventory.extract(
                                                                                CONTAINER,
                                                                                ItemResource.of(
                                                                                        input
                                                                                                .container()),
                                                                                recipe.container()
                                                                                        .count(),
                                                                                transaction)
                                                                        == recipe.container()
                                                                                .count()
                                                                && inventory.extract(
                                                                                ADDITIVE,
                                                                                ItemResource.of(
                                                                                        input
                                                                                                .additive()),
                                                                                recipe.additive()
                                                                                        .count(),
                                                                                transaction)
                                                                        == recipe.additive().count()
                                                                && inventory.insert(
                                                                                OUTPUT,
                                                                                ItemResource.of(
                                                                                        recipe
                                                                                                .result()),
                                                                                recipe.result()
                                                                                        .count(),
                                                                                transaction)
                                                                        == recipe.result().count());
                                    })
                            .orElse(null);
            case ENRICH_LIQUID ->
                    enrichments
                            .getRecipeFor(input, level)
                            .map(
                                    holder -> {
                                        var recipe = holder.value();
                                        return new Job(
                                                holder.id().identifier().toString(),
                                                transaction -> {
                                                    if (inventory.extract(
                                                                            ADDITIVE,
                                                                            ItemResource.of(
                                                                                    input
                                                                                            .additive()),
                                                                            recipe.additive()
                                                                                    .count(),
                                                                            transaction)
                                                                    != recipe.additive().count()
                                                            || inputTank.extract(
                                                                            0,
                                                                            input.fluid(),
                                                                            recipe.inputFluid()
                                                                                    .amount(),
                                                                            transaction)
                                                                    != recipe.inputFluid().amount())
                                                        return false;
                                                    int remaining = recipe.result().amount();
                                                    FluidResource result =
                                                            FluidResource.of(recipe.result());
                                                    var containers = containerHandler();
                                                    if (containers != null)
                                                        while (remaining > 0) {
                                                            int filled =
                                                                    containers.insert(
                                                                            result,
                                                                            remaining,
                                                                            transaction);
                                                            if (filled == 0) break;
                                                            remaining -= filled;
                                                        }
                                                    return remaining == 0
                                                            || outputTank.insert(
                                                                            0,
                                                                            result,
                                                                            remaining,
                                                                            transaction)
                                                                    == remaining;
                                                });
                                    })
                            .orElse(null);
            case BOTTLE_LIQUID -> fillJob(input);
            case EMPTY_LIQUID -> emptyJob(input);
        };
    }

    private Job fillJob(CannerInput input) {
        var handler = containerHandler();
        if (handler == null || input.fluid().isEmpty()) return null;
        return new Job(
                containerKey(input, input.fluid()),
                transaction -> {
                    int filled = handler.insert(input.fluid(), input.amount(), transaction);
                    return filled > 0
                            && inputTank.extract(0, input.fluid(), filled, transaction) == filled;
                });
    }

    private Job emptyJob(CannerInput input) {
        var handler = containerHandler();
        if (handler == null) return null;
        for (int index = 0; index < handler.size(); index++) {
            var resource = handler.getResource(index);
            if (resource.isEmpty() || !input.fluid().isEmpty() && !input.fluid().equals(resource))
                continue;
            int amount =
                    Math.min(handler.getAmountAsInt(index), 8000 - outputTank.getAmountAsInt(0));
            if (amount <= 0) return null;
            int tankIndex = index;
            return new Job(
                    containerKey(input, resource),
                    transaction -> {
                        int drained = handler.extract(tankIndex, resource, amount, transaction);
                        return drained > 0
                                && outputTank.insert(0, resource, drained, transaction) == drained;
                    });
        }
        return null;
    }

    private String containerKey(CannerInput input, FluidResource fluid) {
        return BuiltInRegistries.ITEM.getKey(input.container().getItem())
                + "/"
                + BuiltInRegistries.FLUID.getKey(fluid.getFluid());
    }

    @Override
    public void serverTick(ServerLevel level) {
        beginProcessingTick();
        Job job = findJob(level);
        boolean fits = false;
        if (job != null)
            try (var simulation = Transaction.openRoot()) {
                fits = job.operation().apply(simulation);
            }
        var order =
                job == null
                        ? null
                        : new MachineProcess.WorkOrder(
                                mode.serializedName() + "/" + job.key(),
                                upgradeProfile().ticks(),
                                upgradeProfile().euPerTick());
        MachineProcess.Outcome outcome;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            outcome = process.tick(order, fits, energy);
            if (outcome == MachineProcess.Outcome.COMPLETED && !job.operation().apply(transaction))
                return;
            transaction.commit();
        }
        if (outcome == MachineProcess.Outcome.COMPLETED) {
            for (int operation = 1;
                    operation < Math.min(64, upgradeProfile().operations());
                    operation++) {
                var extra = findJob(level);
                if (extra == null) break;
                try (var transaction = Transaction.openRoot()) {
                    if (!extra.operation().apply(transaction)) break;
                    transaction.commit();
                }
            }
        }
        finishProcessingTick(level);
        setActive(
                outcome == MachineProcess.Outcome.RUNNING
                        || outcome == MachineProcess.Outcome.COMPLETED);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        tag.putInt("mode", mode.id());
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int savedMode = input.getIntOr("mode", 0);
        mode =
                savedMode >= 0 && savedMode < CannerMode.values().length
                        ? CannerMode.byId(savedMode)
                        : CannerMode.BOTTLE_SOLID;
        process.restore(
                new MachineProcess.State(
                        input.getStringOr("recipe", ""),
                        Math.clamp(input.getIntOr("progress", 0), 0, progressMaximum() - 1)));
        inputTank.deserialize(input.childOrEmpty("inputTank"));
        outputTank.deserialize(input.childOrEmpty("outputTank"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("mode", mode.id());
        output.putString("recipe", process.state().recipe());
        output.putInt("progress", progress());
        inputTank.serialize(output.child("inputTank"));
        outputTank.serialize(output.child("outputTank"));
    }
}
