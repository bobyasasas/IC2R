package ic2.neoforge.machine;

import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;

public final class ElectricFurnaceBlockEntity extends ProcessingBlockEntity {
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> recipes =
            RecipeManager.createCheck(RecipeType.SMELTING);

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.ELECTRIC_FURNACE), pos, state);
    }

    @Override
    protected boolean acceptsInput(ItemResource resource, ServerLevel level) {
        return recipes.getRecipeFor(new SingleRecipeInput(resource.toStack(64)), level).isPresent();
    }

    @Override
    protected Job findJob(ServerLevel level) {
        var input = new SingleRecipeInput(inventory.stack(INPUT));
        return recipes.getRecipeFor(input, level)
                .map(
                        recipe ->
                                new Job(
                                        recipe.id().identifier().toString(),
                                        1,
                                        ItemStackTemplate.fromNonEmptyStack(
                                                recipe.value().assemble(input)),
                                        recipe.value().experience()))
                .orElse(null);
    }
}
