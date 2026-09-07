package ic2.neoforge.machine;

import ic2.core.machine.FuelFurnace;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class IronFurnaceBlockEntity extends MachineBlockEntity {
    private final FuelFurnace furnace = new FuelFurnace();
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> recipes =
            RecipeManager.createCheck(RecipeType.SMELTING);
    private final SnapshotJournal<FuelFurnace.State> journal =
            new SnapshotJournal<>() {
                @Override
                protected FuelFurnace.State createSnapshot() {
                    return furnace.state();
                }

                @Override
                protected void revertToSnapshot(FuelFurnace.State snapshot) {
                    furnace.restore(snapshot);
                }

                @Override
                protected void onRootCommit(FuelFurnace.State snapshot) {
                    if (!snapshot.equals(furnace.state())) setChanged();
                }
            };
    private double experience;

    public IronFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.IRON_FURNACE), pos, state, 3);
    }

    @Override
    public int progress() {
        return furnace.state().progress();
    }

    @Override
    public int progressMaximum() {
        return 160;
    }

    @Override
    public int fuelRemaining() {
        return furnace.state().fuel();
    }

    @Override
    public int fuelMaximum() {
        return furnace.state().totalFuel();
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot ->
                        slot == 0 && side == Direction.UP
                                || slot == 2 && side != Direction.UP && side != Direction.DOWN,
                slot ->
                        slot == 1
                                || slot == 2
                                        && inventory
                                                        .stack(2)
                                                        .getBurnTime(
                                                                RecipeType.SMELTING,
                                                                level.fuelValues())
                                                == 0,
                (slot, resource) ->
                        slot != 2
                                || resource.toStack()
                                                .getBurnTime(
                                                        RecipeType.SMELTING, level.fuelValues())
                                        > 0);
    }

    @Override
    public void serverTick(ServerLevel level) {
        var input = new SingleRecipeInput(inventory.stack(0));
        var recipe = recipes.getRecipeFor(input, level).orElse(null);
        var output = recipe == null ? ItemStack.EMPTY : recipe.value().assemble(input);
        boolean fits = false;
        if (!output.isEmpty())
            try (var simulation = Transaction.openRoot()) {
                fits =
                        inventory.insert(1, ItemResource.of(output), output.getCount(), simulation)
                                == output.getCount();
            }
        FuelFurnace.Tick result;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            if (furnace.needsFuel(fits)) {
                var fuel = inventory.stack(2);
                int burnTime = fuel.getBurnTime(RecipeType.SMELTING, level.fuelValues());
                if (burnTime > 0
                        && inventory.extract(2, ItemResource.of(fuel), 1, transaction) == 1) {
                    var remainder = fuel.getCraftingRemainder();
                    if (remainder != null
                            && inventory.insert(
                                            2,
                                            ItemResource.of(remainder),
                                            remainder.count(),
                                            transaction)
                                    != remainder.count()) return;
                    furnace.acceptFuel(burnTime);
                }
            }
            result = furnace.tick(fits);
            if (result.completed()
                    && (inventory.extract(0, ItemResource.of(input.item()), 1, transaction) != 1
                            || inventory.insert(
                                            1,
                                            ItemResource.of(output),
                                            output.getCount(),
                                            transaction)
                                    != output.getCount())) return;
            transaction.commit();
        }
        if (result.completed()) {
            experience += recipe.value().experience();
            setChanged();
        }
        setActive(result.burning());
    }

    @Override
    public void awardExperience(Player player) {
        if (level instanceof ServerLevel server && experience >= 1) {
            int amount = (int) Math.min(Integer.MAX_VALUE, Math.floor(experience));
            ExperienceOrb.award(server, player.position(), amount);
            experience -= amount;
            setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int total = Math.max(0, input.getIntOr("totalFuel", 0));
        furnace.restore(
                new FuelFurnace.State(
                        Math.clamp(input.getIntOr("fuel", 0), 0, total),
                        total,
                        Math.clamp(input.getIntOr("progress", 0), 0, 159)));
        double saved = input.getDoubleOr("xp", 0);
        experience = Double.isFinite(saved) ? Math.max(0, saved) : 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("fuel", fuelRemaining());
        output.putInt("totalFuel", fuelMaximum());
        output.putInt("progress", progress());
        output.putDouble("xp", experience);
    }
}
