package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.function.Consumer;

/**
 * Shared legacy night vision logic (ItemArmorNanoSuit.getNightVisionOrNot/affectPlayer): while
 * the helmet's flag is set, one EU per tick keeps the effect up — bright skylight blinds the
 * player instead, matching the goggles. Toggling is sneak + use (legacy Alt+ModeSwitch).
 */
public final class NightVisionHelper {
    private static final String ENABLED_KEY = "ic2.night_vision.mode.enabled";
    private static final String DISABLED_KEY = "ic2.night_vision.mode.disabled";

    public static void toggle(ItemStack stack, Player player, Level level) {
        if (level.isClientSide()) {
            return;
        }

        boolean enabled =
                !stack.getOrDefault(ModDataComponents.NIGHT_VISION_ACTIVE.get(), false);
        stack.set(ModDataComponents.NIGHT_VISION_ACTIVE.get(), enabled);
        player.sendSystemMessage(Component.translatable(enabled ? ENABLED_KEY : DISABLED_KEY));
    }

    /** Legacy per-tick effect maintenance for any worn helmet with the flag set. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof ElectricItem)) return;
        if (!helmet.getOrDefault(ModDataComponents.NIGHT_VISION_ACTIVE.get(), false)) return;

        if (ElectricItemEnergy.use(helmet, 1.0, player)) {
            int skylight =
                    level.getMaxLocalRawBrightness(BlockPos.containing(player.position()));
            affectPlayer(player, skylight);
        }
    }

    /** Legacy ItemArmorNanoSuit.affectPlayer. */
    public static void affectPlayer(Player player, int skylight) {
        if (skylight > 8) {
            player.removeEffect(MobEffects.NIGHT_VISION);
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, true, true));
        } else {
            player.removeEffect(MobEffects.BLINDNESS);
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, true));
        }
    }

    public static void addToggleTooltip(Consumer<Component> tooltip) {
        tooltip.accept(
                Component.translatable(
                        "item.ic2.tooltip.night_vision.toggle",
                        Component.literal("Sneak"),
                        Component.literal("Use")));
    }

    private NightVisionHelper() {}
}
