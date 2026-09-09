package ic2.neoforge.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/** Blocks wrench-and-player breaking of personal chests by non-owners or with contents. */
public final class PersonalChestGuard {
    public static void guard(BreakBlockEvent event) {
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof MachineBlock block)
                || block.kind() != MachineKind.PERSONAL_CHEST) return;
        if (!(level.getBlockEntity(pos) instanceof PersonalChestBlockEntity chest)) return;
        Player player = event.getPlayer();
        boolean holdingContents = false;
        for (int slot = 0; slot < chest.inventory().size(); slot++) {
            if (!chest.inventory().stack(slot).isEmpty()) {
                holdingContents = true;
                break;
            }
        }
        if (!chest.owned()) return;
        if (!chest.permits(player)) {
            player.sendSystemMessage(
                    Component.literal("This safe is owned by " + chest.ownerName()));
            event.setCanceled(true);
        } else if (holdingContents) {
            player.sendSystemMessage(
                    Component.literal("Can't break a safe that still holds items"));
            event.setCanceled(true);
        }
    }

    private PersonalChestGuard() {}
}
