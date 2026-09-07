package ic2.neoforge.item;

import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModGameEvents;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModSounds;
import ic2.neoforge.world.ResinState;
import ic2.neoforge.world.RubberLogBlock;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;

/** One server-side extraction, shared by manual and electric taps. */
final class TreetapActions {
    static boolean extract(UseOnContext context, boolean electric) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        var state = level.getBlockState(pos);
        if (player == null
                || !(state.getBlock() instanceof RubberLogBlock)
                || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))
            return false;
        var resin = state.getValue(RubberLogBlock.RESIN);
        if (resin.plain() || resin.facing() != context.getClickedFace()) return false;
        if (level.isClientSide()) return resin.wet();
        var random = level.getRandom();
        int quantity;
        boolean changed;
        if (resin.wet()) {
            changed =
                    level.setBlockAndUpdate(
                            pos, state.setValue(RubberLogBlock.RESIN, resin.withWet(false)));
            if (!changed) return false;
            quantity = random.nextInt(3) + 1;
        } else {
            changed = random.nextInt(5) == 0;
            if (changed
                    && !level.setBlockAndUpdate(
                            pos, state.setValue(RubberLogBlock.RESIN, ResinState.PLAIN)))
                return false;
            // Recovered dry tapping has two resin drops on its independent 20% yield roll.
            quantity = random.nextInt(5) == 0 ? 2 : 0;
        }
        if (!changed && quantity == 0) return false;
        var side = context.getClickedFace();
        for (int i = 0; i < quantity; i++) {
            var entity =
                    new ItemEntity(
                            level,
                            pos.getX() + .5 + side.getStepX() * .3,
                            pos.getY() + .5 + side.getStepY() * .3,
                            pos.getZ() + .5 + side.getStepZ() * .3,
                            new ItemStack(ModItems.MATERIALS.get(MaterialDefinition.RESIN).get()));
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
        level.playSound(
                null,
                pos,
                (electric ? ModSounds.ITEM_TREETAP_ELECTRIC_USE : ModSounds.ITEM_TREETAP_USE).get(),
                SoundSource.PLAYERS,
                1,
                1);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
        level.gameEvent(ModGameEvents.TOOL_USE, pos, GameEvent.Context.of(player));
        return true;
    }

    private TreetapActions() {}
}
