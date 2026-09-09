package ic2.neoforge.world;

import ic2.neoforge.machine.MagnetizerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Iron fence that carries players upward while a magnetizer feeds the column: the climber rises
 * 0.075 per tick against gravity (faster with metal boots, slower while holding alt) and slides
 * down gently when sneaking. The active magnetizers split the two-EU boost.
 */
public class IronFenceBlock extends FenceBlock {
    private static final int MAX_RANGE = 20;
    private static final Map<UUID, Long> LAST_BOOST = new HashMap<>();

    public IronFenceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            net.minecraft.world.entity.InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        if (level instanceof ServerLevel server && entity instanceof Player player) {
            lift(server, pos, player);
        }
    }

    /**
     * One magnetic tick for a player inside the fence column; returns whether the column was
     * powered and the player rose.
     */
    public static boolean lift(ServerLevel level, BlockPos pos, Player player) {
        long tick = level.getGameTime();
        if (LAST_BOOST.getOrDefault(player.getUUID(), -1L) == tick) return false;
        LAST_BOOST.put(player.getUUID(), tick);
        var magnetizers = findMagnetizers(level, pos);
        if (magnetizers.isEmpty()) return false;
        double share = 1.0 / magnetizers.size();
        for (var magnetizer : magnetizers) {
            if (!magnetizer.canBoost()) return false;
        }
        for (var magnetizer : magnetizers) magnetizer.boost(share);

        Vec3 velocity = player.getDeltaMovement();
        boolean metalShoes = hasMetalShoes(player);
        boolean descending = player.isShiftKeyDown();
        boolean slow = velocity.y >= -0.25 && velocity.y < 1.6;
        if (slow) player.fallDistance = 0.0F;
        if (descending) {
            if (!slow) player.setDeltaMovement(velocity.multiply(1.0, 0.8, 1.0));
            return false;
        }
        player.setDeltaMovement(velocity.add(0.0, 0.075, 0.0));
        Vec3 raised = player.getDeltaMovement();
        if (raised.y > 0.0) player.setDeltaMovement(raised.multiply(1.0, 1.03, 1.0));
        double maxSpeed = 0.5;
        if (metalShoes) maxSpeed = 1.5;
        player.setDeltaMovement(
                player.getDeltaMovement().x,
                Math.min(player.getDeltaMovement().y, maxSpeed),
                player.getDeltaMovement().z);
        player.setOnGround(false);
        return true;
    }

    private static boolean hasMetalShoes(Player player) {
        ItemStack boots = player.getInventory().getItem(36);
        return boots.is(net.minecraft.world.item.Items.IRON_BOOTS)
                || boots.is(net.minecraft.world.item.Items.GOLDEN_BOOTS)
                || boots.is(net.minecraft.world.item.Items.CHAINMAIL_BOOTS);
    }

    /**
     * Walks the connected fence column down and up (at most twenty blocks each way) and collects
     * the magnetizers adjacent to its ends or sides.
     */
    private static List<MagnetizerBlockEntity> findMagnetizers(ServerLevel level, BlockPos start) {
        var found = new ArrayList<MagnetizerBlockEntity>();
        for (var direction : Direction.Plane.HORIZONTAL) {
            var neighbour = start.relative(direction);
            if (level.getBlockState(neighbour).getBlock() instanceof IronFenceBlock) continue;
            var magnetizer = magnetizerAt(level, neighbour, direction.getOpposite());
            if (magnetizer != null) found.add(magnetizer);
        }
        if (!found.isEmpty()) return found;
        BlockPos column = start;
        for (int step = 1; step <= MAX_RANGE; step++) {
            column = column.above();
            if (!(level.getBlockState(column).getBlock() instanceof IronFenceBlock)) break;
            for (var direction : Direction.Plane.HORIZONTAL) {
                var neighbour = column.relative(direction);
                if (level.getBlockState(neighbour).getBlock() instanceof IronFenceBlock) continue;
                var magnetizer = magnetizerAt(level, neighbour, direction.getOpposite());
                if (magnetizer != null && !found.contains(magnetizer)) found.add(magnetizer);
            }
        }
        return found;
    }

    private static MagnetizerBlockEntity magnetizerAt(
            BlockGetter level, BlockPos pos, Direction fromSide) {
        return level.getBlockEntity(pos) instanceof MagnetizerBlockEntity magnetizer
                ? magnetizer
                : null;
    }
}
