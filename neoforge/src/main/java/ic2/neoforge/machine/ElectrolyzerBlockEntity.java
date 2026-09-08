package ic2.neoforge.machine;

import com.mojang.serialization.JsonOps;

import ic2.core.energy.VoltageTier;
import ic2.core.energy.grid.EnergyMode;
import ic2.core.energy.grid.EnergyNode;
import ic2.core.machine.MachineProcess;
import ic2.neoforge.energy.EnergyConfig;
import ic2.neoforge.energy.WorldEnergyNetworks;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.recipe.ElectrolyzingRecipe;
import ic2.neoforge.recipe.FluidRecipeInput;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModThermalRecipes;
import ic2.neoforge.transfer.MachineFluidTank;
import ic2.neoforge.transfer.ResourcePort;
import ic2.neoforge.transfer.UpgradeTransfers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import org.jspecify.annotations.Nullable;

/** Directed fluid outputs and final input consumption commit together with the last EU payment. */
public final class ElectrolyzerBlockEntity extends PoweredBlockEntity implements FluidMachine {
    private final MachineProcess process = new MachineProcess();
    private final MachineJournal<MachineProcess.State> journal =
            new MachineJournal<>(energy, process::state, process::restore, this::setChanged);
    private final MachineFluidTank input =
            new MachineFluidTank(8000, this::setChanged, this::acceptsFluid);
    private final RecipeManager.CachedCheck<FluidRecipeInput, ElectrolyzingRecipe> recipes =
            RecipeManager.createCheck(ModThermalRecipes.ELECTROLYZING.get());
    private @Nullable RecipeHolder<ElectrolyzingRecipe> current;
    private String signature = "";
    private int voltage = 128, amps = 1;

    public ElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(MachineKind.ELECTROLYZER),
                pos,
                state,
                32000,
                MachineKind.ELECTROLYZER.slots());
    }

    public MachineFluidTank inputTank() {
        return input;
    }

    private boolean acceptsFluid(FluidResource fluid) {
        return level instanceof ServerLevel server
                && server
                        .recipeAccess()
                        .recipeMap()
                        .byType(ModThermalRecipes.ELECTROLYZING.get())
                        .stream()
                        .anyMatch(holder -> fluid.matches(holder.value().input()));
    }

    @Override
    public ResourceHandler<FluidResource> fluidAutomation(Direction side) {
        return new ResourcePort<>(input, slot -> true, slot -> false);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(inventory, slot -> false, slot -> false);
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        return EnergyNode.Terminal.sink(
                energy, voltage, EnergyConfig.MODE.get() == EnergyMode.GT ? amps : 1);
    }

    @Override
    public void serverTick(ServerLevel level) {
        var battery = inventory.stack(0);
        double charge = ElectricItemEnergy.discharge(battery, energy.free(), 2, false, true, false);
        if (charge > 0) {
            inventory.set(0, ItemResource.of(battery), battery.getCount());
            energy.insert(charge);
            setChanged();
        }
        var holder =
                recipes.getRecipeFor(
                                new FluidRecipeInput(input.getResource(0), input.getAmountAsInt(0)),
                                level)
                        .orElse(null);
        if (holder != current) {
            current = holder;
            signature =
                    holder == null
                            ? ""
                            : holder.id().identifier()
                                    + "|"
                                    + ElectrolyzingRecipe.CODEC
                                            .codec()
                                            .encodeStart(
                                                    level.registryAccess()
                                                            .createSerializationContext(
                                                                    JsonOps.INSTANCE),
                                                    holder.value())
                                            .getOrThrow();
            int power = holder == null ? 0 : holder.value().euPerTick();
            int nextVoltage = Math.max(128, VoltageTier.fromPower(power).getVoltage());
            int nextAmps = (int) (2L * power / nextVoltage + 1);
            if (voltage != nextVoltage || amps != nextAmps) {
                voltage = nextVoltage;
                amps = nextAmps;
                WorldEnergyNetworks.invalidate(level);
            }
        }
        boolean active = false;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            boolean fits = false;
            if (holder != null && energy.stored() >= holder.value().euPerTick())
                try (var probe = Transaction.open(transaction)) {
                    fits = transfer(level, holder.value(), probe);
                }
            if (!fits)
                process.tick(null, false, energy); // Preserve the recovered reset-on-stall rule.
            else {
                var recipe = holder.value();
                var outcome =
                        process.tick(
                                new MachineProcess.WorkOrder(
                                        signature, recipe.ticks(), recipe.euPerTick()),
                                true,
                                energy);
                if (outcome == MachineProcess.Outcome.COMPLETED
                        && !transfer(level, recipe, transaction)) return;
                active =
                        outcome == MachineProcess.Outcome.RUNNING
                                || outcome == MachineProcess.Outcome.COMPLETED;
            }
            transaction.commit();
        }
        setActive(active);
        UpgradeTransfers.tick(level, this);
    }

    private boolean transfer(
            ServerLevel level, ElectrolyzingRecipe recipe, TransactionContext transaction) {
        if (input.extract(0, input.getResource(0), recipe.input().amount(), transaction)
                != recipe.input().amount()) return false;
        for (var output : recipe.outputs()) {
            var target = worldPosition.relative(output.direction());
            if (!level.getChunkSource().hasChunk(target.getX() >> 4, target.getZ() >> 4)
                    || !(level.getBlockEntity(target) instanceof TankBlockEntity)) return false;
            var tank =
                    level.getCapability(
                            Capabilities.Fluid.BLOCK, target, output.direction().getOpposite());
            if (tank == null
                    || tank.insert(
                                    FluidResource.of(output.fluid()),
                                    output.fluid().amount(),
                                    transaction)
                            != output.fluid().amount()) return false;
        }
        return true;
    }

    @Override
    public int progress() {
        return process.state().progress();
    }

    @Override
    public int progressMaximum() {
        return current == null ? 200 : current.value().ticks();
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> current == null ? 0 : current.value().euPerTick();
            case 1 -> input.getAmountAsInt(0);
            case 3 -> BuiltInRegistries.FLUID.getId(input.getResource(0).getFluid());
            default -> 0;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.input.deserialize(input.childOrEmpty("inputTank"));
        process.restore(
                new MachineProcess.State(
                        input.getStringOr("recipe", ""),
                        Math.max(0, input.getIntOr("progress", 0))));
        current = null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        input.serialize(output.child("inputTank"));
        output.putString("recipe", process.state().recipe());
        output.putInt("progress", process.state().progress());
    }
}
