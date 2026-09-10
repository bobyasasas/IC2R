package ic2.neoforge.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Convention block tags scanned by the metal crops' root zone (legacy Ic2BlockTags). Vanilla ore
 * tags cover iron, copper and gold ores; these {@code c:} tags carry the IC2-only metals and the
 * storage blocks.
 */
public final class Ic2BlockTags {
    public static final TagKey<Block> LEAD_ORES = create("lead_ores");
    public static final TagKey<Block> SILVER_ORES = create("silver_ores");
    public static final TagKey<Block> TIN_ORES = create("tin_ores");
    public static final TagKey<Block> IRON_BLOCKS = create("iron_blocks");
    public static final TagKey<Block> COPPER_BLOCKS = create("copper_blocks");
    public static final TagKey<Block> GOLD_BLOCKS = create("gold_blocks");
    public static final TagKey<Block> LEAD_BLOCKS = create("lead_blocks");
    public static final TagKey<Block> SILVER_BLOCKS = create("silver_blocks");
    public static final TagKey<Block> TIN_BLOCKS = create("tin_blocks");

    private Ic2BlockTags() {}

    private static TagKey<Block> create(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", path));
    }
}
