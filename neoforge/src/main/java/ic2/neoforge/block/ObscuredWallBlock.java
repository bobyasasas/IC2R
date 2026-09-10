package ic2.neoforge.block;

import ic2.neoforge.machine.ObscuredWallBlockEntity;
import ic2.neoforge.registration.ModFoam;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

/**
 * A construction foam wall whose faces show sampled block textures. Drops depend on the stored wall
 * color, mirroring the legacy adjustDrop returning the pick block.
 */
public class ObscuredWallBlock extends Block implements EntityBlock {
    public ObscuredWallBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ObscuredWallBlockEntity(pos, state);
    }

    @Override
    public void playerDestroy(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            ItemStack destroyedWith) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        if (level instanceof ServerLevel server) {
            Block wall =
                    blockEntity instanceof ObscuredWallBlockEntity obscured
                            ? ModFoam.WALLS.get(obscured.color().getName()).get()
                            : this;
            popResource(server, pos, new ItemStack(wall));
        }
    }
}
