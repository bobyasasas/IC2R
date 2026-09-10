package ic2.neoforge.world;

import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Legacy ExplosionWhitelist (ic2.api.tile): blocks registered here are exempt from the IC2
 * explosion rule that treats ultra resistant blocks (absorption above one thousand) as passable
 * scenery — a whitelisted block keeps its full absorption and can be destroyed by rays like any
 * other block.
 */
public final class ExplosionWhitelist {
    private static Set<Block> whitelist = Collections.newSetFromMap(new IdentityHashMap<>());

    public static void addWhitelistedBlock(Block block) {
        whitelist.add(block);
    }

    public static void removeWhitelistedBlock(Block block) {
        whitelist.remove(block);
    }

    public static boolean isBlockWhitelisted(Block block) {
        return whitelist.contains(block);
    }

    private ExplosionWhitelist() {}
}
