package ic2.neoforge.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Bridge for the client-side baked-model scan; the dedicated server keeps the pass-through default
 * so common code never references client classes.
 */
public final class ObscuratorSampling {
    public interface Scanner {
        boolean scan(ItemStack stack, Player player, Level level, BlockPos pos, Direction side);
    }

    private static volatile Scanner scanner = (stack, player, level, pos, side) -> false;

    public static void register(Scanner implementation) {
        scanner = java.util.Objects.requireNonNull(implementation);
    }

    public static boolean scan(
            ItemStack stack, Player player, Level level, BlockPos pos, Direction side) {
        return scanner.scan(stack, player, level, pos, side);
    }

    private ObscuratorSampling() {}
}
