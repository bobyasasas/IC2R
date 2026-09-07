package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyNode;
import ic2.core.machine.MachineProcess;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class ElectricFurnaceBlockEntity extends PoweredBlockEntity {
    public static final int INPUT = 0, OUTPUT = 1, BATTERY = 2;
    private final MachineProcess process = new MachineProcess();
    private final MachineJournal<MachineProcess.State> journal =
            new MachineJournal<>(energy, process::state, process::restore, this::setChanged);
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> recipes =
            RecipeManager.createCheck(RecipeType.SMELTING);
    private double experience;

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.ELECTRIC_FURNACE_ENTITY.get(), pos, state, 300, 3);
    }

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
        return 100;
    }

    @Override
    public void serverTick(ServerLevel level) {
        var battery = inventory.stack(BATTERY);
        double extracted =
                ElectricItemEnergy.discharge(battery, energy.free(), 1, false, true, false);
        if (extracted > 0) {
            inventory.set(BATTERY, ItemResource.of(battery), battery.getCount());
            energy.insert(extracted);
            setChanged();
        }
        var inputStack = inventory.stack(INPUT);
        var input = new SingleRecipeInput(inputStack);
        var recipe = recipes.getRecipeFor(input, level).orElse(null);
        var order =
                recipe == null
                        ? null
                        : new MachineProcess.WorkOrder(recipe.id().identifier().toString(), 100, 3);
        var output =
                recipe == null
                        ? net.minecraft.world.item.ItemStack.EMPTY
                        : recipe.value().assemble(input);
        boolean fits = false;
        if (!output.isEmpty()) {
            try (var simulation = Transaction.openRoot()) {
                fits =
                        inventory.insert(
                                        OUTPUT,
                                        ItemResource.of(output),
                                        output.getCount(),
                                        simulation)
                                == output.getCount();
            }
        }
        MachineProcess.Outcome outcome;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            outcome = process.tick(order, fits, energy);
            if (outcome == MachineProcess.Outcome.COMPLETED) {
                if (inventory.extract(INPUT, ItemResource.of(inputStack), 1, transaction) != 1
                        || inventory.insert(
                                        OUTPUT,
                                        ItemResource.of(output),
                                        output.getCount(),
                                        transaction)
                                != output.getCount()) return;
            }
            transaction.commit();
        }
        if (outcome == MachineProcess.Outcome.COMPLETED) {
            experience += recipe.value().experience();
            setChanged();
        }
        setActive(
                outcome == MachineProcess.Outcome.RUNNING
                        || outcome == MachineProcess.Outcome.COMPLETED);
    }

    public void awardExperience(net.minecraft.world.entity.player.Player player) {
        if (level instanceof ServerLevel server && experience >= 1) {
            int whole = (int) Math.min(Integer.MAX_VALUE, Math.floor(experience));
            experience -= whole;
            net.minecraft.world.entity.ExperienceOrb.award(server, player.position(), whole);
            setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        process.restore(
                new MachineProcess.State(
                        input.getStringOr("recipe", ""),
                        Math.clamp(input.getIntOr("progress", 0), 0, 99)));
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
