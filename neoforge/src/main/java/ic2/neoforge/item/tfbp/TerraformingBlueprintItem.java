package ic2.neoforge.item.tfbp;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

/**
 * Legacy Tfbp/ITerraformingBP: a cartridige for the terraformer carrying one
 * {@link TerraformerProgram}. The blank cartridge has no program — the machine accepts it and
 * keeps running without ever changing the terrain (legacy behaviour).
 */
public class TerraformingBlueprintItem extends Item {
    private final double consume;
    private final int range;
    private final @Nullable TerraformerProgram program;

    public TerraformingBlueprintItem(
            Properties properties, double consume, int range, @Nullable TerraformerProgram program) {
        super(properties);
        this.consume = consume;
        this.range = range;
        this.program = program;
    }

    public double consume() {
        return consume;
    }

    public int range() {
        return range;
    }

    public boolean terraform(Level world, BlockPos pos) {
        return program != null && program.terraform(world, pos);
    }
}
