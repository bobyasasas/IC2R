package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.component.RemoteLinks;
import ic2.neoforge.world.DynamiteBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Remote detonator (legacy ItemRemote): right-clicking linked dynamite toggles its pairing; using
 * it in the air detonates every paired stick in the current dimension. Links live in the {@code
 * ic2:remote_links} component.
 */
public class RemoteItem extends Item {
    public RemoteItem(Properties properties) {
        super(properties);
    }

    private static RemoteLinks links(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.REMOTE_LINKS, RemoteLinks.EMPTY);
    }

    private static void setLinks(ItemStack stack, RemoteLinks links) {
        stack.set(ModDataComponents.REMOTE_LINKS, links);
    }

    private static GlobalPos key(Level level, BlockPos pos) {
        return GlobalPos.of(level.dimension(), pos.immutable());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        if (level.isClientSide() || player == null) return InteractionResult.SUCCESS;
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DynamiteBlock)) return InteractionResult.PASS;

        var stack = context.getItemInHand();
        var links = links(stack);
        var key = GlobalPos.of(level.dimension(), pos.immutable());
        if (state.getValue(DynamiteBlock.LINKED)) {
            // Legacy ItemRemote.useOn: an already linked stick only unlinks through the
            // remote holding its link; any other remote is refused with a message.
            if (!links.targets().contains(key)) {
                player.sendSystemMessage(Component.translatable("ic2.remote.cannot_unlink"));
                return InteractionResult.SUCCESS;
            }
            setLinks(stack, links.remove(key));
            level.setBlock(pos, state.setValue(DynamiteBlock.LINKED, false), 3);
            return InteractionResult.SUCCESS;
        }
        setLinks(stack, links.add(key));
        level.setBlock(pos, state.setValue(DynamiteBlock.LINKED, true), 3);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(
            Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    net.minecraft.sounds.SoundEvents.TNT_PRIMED,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
            // Legacy launchRemotes(): a launch is one-shot — every loaded target is
            // consumed after its attempt, while links in other dimensions or unloaded
            // chunks stay for a later trigger.
            var remaining = new ArrayList<GlobalPos>();
            for (var target : links(stack).targets()) {
                if (!target.dimension().equals(level.dimension()) || !level.isLoaded(target.pos())) {
                    remaining.add(target);
                    continue;
                }
                var state = level.getBlockState(target.pos());
                if (state.getBlock() instanceof DynamiteBlock
                        && state.getValue(DynamiteBlock.LINKED)) {
                    DynamiteBlock.detonate(level, target.pos(), player);
                }
            }
            setLinks(stack, new RemoteLinks(remaining));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        int linked = links(stack).targets().size();
        if (linked > 0) {
            tooltip.accept(Component.translatable("ic2.remote.tooltip.linked", linked));
        }
    }
}
