package ic2.neoforge.crop;

import net.minecraft.world.level.block.Block;

/** Legacy CropSoilType: blocks a crop stick may be placed on. */
public enum CropSoilType {
    FARMLAND(net.minecraft.world.level.block.Blocks.FARMLAND),
    MYCELIUM(net.minecraft.world.level.block.Blocks.MYCELIUM),
    SAND(net.minecraft.world.level.block.Blocks.SAND),
    SOUL_SAND(net.minecraft.world.level.block.Blocks.SOUL_SAND);

    private final Block block;

    CropSoilType(Block block) {
        this.block = block;
    }

    public static boolean contains(Block block) {
        for (CropSoilType type : values()) {
            if (type.block == block) return true;
        }
        return false;
    }

    public Block block() {
        return block;
    }
}
