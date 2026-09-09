package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.machine.TeleporterBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Links two teleporters: the first use remembers a teleporter, the second use on another teleporter
 * connects the two. Using it in the air right after linking clears the memory; using it in the air
 * later unlinks.
 */
public class FrequencyTransmitterItem extends Item {
    public FrequencyTransmitterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!level.isClientSide() && stack.is(this)) {
            if (stack.has(ModDataComponents.FREQUENCY_JUST_SET)
                    && stack.get(ModDataComponents.FREQUENCY_JUST_SET)) {
                stack.set(ModDataComponents.FREQUENCY_JUST_SET, false);
            } else if (stack.has(ModDataComponents.FREQUENCY_POS)) {
                stack.remove(ModDataComponents.FREQUENCY_POS);
                tell(player, "ic2.frequency_transmitter.unlink_target");
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var player = context.getPlayer();
        if (player == null || level.isClientSide()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(context.getClickedPos())
                instanceof TeleporterBlockEntity teleporter)) {
            return InteractionResult.PASS;
        }
        var stack = context.getItemInHand();
        BlockPos target = stack.get(ModDataComponents.FREQUENCY_POS);
        if (target == null) {
            target = teleporter.getBlockPos();
            stack.set(ModDataComponents.FREQUENCY_POS, target);
            stack.set(ModDataComponents.FREQUENCY_JUST_SET, true);
            tell(
                    player,
                    "ic2.frequency_transmitter.link_target",
                    target.getX(),
                    target.getY(),
                    target.getZ());
        } else if (teleporter.getBlockPos().equals(target)) {
            stack.set(ModDataComponents.FREQUENCY_JUST_SET, true);
            tell(player, "ic2.frequency_transmitter.cannot_link_to_self");
        } else if (teleporter.hasTarget() && teleporter.getTarget().equals(target)) {
            stack.set(ModDataComponents.FREQUENCY_JUST_SET, true);
            tell(player, "ic2.frequency_transmitter.link_already_established");
        } else if (level.getBlockEntity(target) instanceof TeleporterBlockEntity other) {
            teleporter.setTarget(target);
            other.setTarget(context.getClickedPos());
            stack.set(ModDataComponents.FREQUENCY_JUST_SET, true);
            tell(
                    player,
                    "ic2.frequency_transmitter.link_target",
                    target.getX(),
                    target.getY(),
                    target.getZ());
        } else {
            stack.remove(ModDataComponents.FREQUENCY_POS);
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
        var target = stack.get(ModDataComponents.FREQUENCY_POS);
        if (target != null) {
            tooltip.accept(
                    Component.translatable(
                            "ic2.frequency_transmitter.tooltip.target",
                            target.getX(),
                            target.getY(),
                            target.getZ()));
        } else {
            tooltip.accept(Component.translatable("ic2.frequency_transmitter.tooltip.blank"));
        }
    }

    private static void tell(Player player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable(key, args));
    }
}
