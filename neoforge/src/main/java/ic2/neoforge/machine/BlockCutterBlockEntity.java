package ic2.neoforge.machine;

import ic2.core.recipe.ProcessingMethod;
import ic2.neoforge.item.CuttingBladeItem;
import ic2.neoforge.recipe.ProcessingRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block cutting machine: 4 EU/t over 450 ticks with a permanent blade in its own slot. A recipe
 * only runs while the blade's hardness reaches the recipe requirement, matching the legacy
 * bladeTooWeak gate reported to the GUI.
 */
public final class BlockCutterBlockEntity extends SingleInputBlockEntity {
    public static final int BLADE = 3;
    private boolean bladeTooWeak;

    public BlockCutterBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    protected ProcessingMethod method() {
        return ProcessingMethod.BLOCK_CUTTER;
    }

    @Override
    protected Job findJob(ServerLevel level) {
        Job job = super.findJob(level);
        if (job == null) {
            bladeTooWeak = false;
            return null;
        }
        var blade = inventory.stack(BLADE);
        int bladeHardness = blade.getItem() instanceof CuttingBladeItem item ? item.hardness() : -1;
        int required =
                recipes()
                        .getRecipeFor(new SingleRecipeInput(inventory.stack(INPUT)), level)
                        .map(
                                found ->
                                        found.value() instanceof ProcessingRecipe processing
                                                ? processing.hardness()
                                                : 0)
                        .orElse(0);
        bladeTooWeak = bladeHardness < required;
        return bladeTooWeak ? null : job;
    }

    public boolean bladeTooWeak() {
        return bladeTooWeak;
    }

    @Override
    public int menuValue(int index) {
        return index == 0 && bladeTooWeak ? 1 : 0;
    }
}
