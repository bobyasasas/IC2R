package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.registration.ModArmor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.text.DecimalFormat;
import java.util.WeakHashMap;

/**
 * Legacy JetpackHandler: a chest armor marked {@code jetpack_attached} carries a virtual electric
 * jetpack. Flight drains the armor's own battery when it is an electric item and the component
 * battery otherwise; when the armor breaks under damage the jetpack pops back with its charge.
 * Keyboard downgrade: legacy armed flight with the jump key and toggled hover with jump + mode
 * switch; the port uses the same {@code jetpack_active} flag as the jetpack items, toggled with
 * sneak + use on the held armor (which also suppresses the vanilla equip swap).
 */
public final class JetpackAttachmentHelper {
    private static final WeakHashMap<Player, ItemStack> ARMOR_BUFFER = new WeakHashMap<>();

    public static boolean hasAttached(ItemStack stack) {
        return !stack.isEmpty() && stack.getOrDefault(ModDataComponents.JETPACK_ATTACHED.get(), false);
    }

    /** The charge the virtual jetpack can draw: the armor's battery, or the component battery. */
    public static double charge(ItemStack armor) {
        if (armor.getItem() instanceof ElectricItem) return ElectricItemEnergy.charge(armor);
        return armor.getOrDefault(ModDataComponents.JETPACK_CHARGE.get(), 0.0);
    }

    /** A JetpackLike view with the electric jetpack's parameters over any attached armor. */
    public static JetpackLike jetpackView() {
        return View.INSTANCE;
    }

    private enum View implements JetpackLike {
        INSTANCE;

        private static double capacity() {
            return ((JetpackElectricItem) ModArmor.JETPACK_ELECTRIC.get()).specification().capacity();
        }

        @Override
        public float jetpackPower(ItemStack stack) {
            return 0.7F;
        }

        @Override
        public float dropPercentage() {
            return 0.05F;
        }

        @Override
        public float hoverMultiplier() {
            return 0.1F;
        }

        @Override
        public float worldHeightDivisor() {
            return 1.28F;
        }

        @Override
        public double chargeLevel(ItemStack stack) {
            return charge(stack) / capacity();
        }

        @Override
        public void drainFlightEnergy(ItemStack stack, int amount) {
            if (stack.getItem() instanceof ElectricItem) {
                ElectricItemEnergy.discharge(stack, amount + 6, Integer.MAX_VALUE, true, false, false);
            } else {
                double remaining = charge(stack) - amount - 6;
                if (remaining <= 0) {
                    stack.remove(ModDataComponents.JETPACK_CHARGE.get());
                } else {
                    stack.set(ModDataComponents.JETPACK_CHARGE.get(), remaining);
                }
            }
        }

        @Override
        public boolean isJetpackActive(ItemStack stack) {
            return stack.getOrDefault(ModDataComponents.JETPACK_ACTIVE.get(), false);
        }
    }

    /**
     * Legacy armor-break protection: while an attack lands on a player wearing an attached
     * jetpack, remember the piece; if the chest is empty on the next tick the armor broke and the
     * jetpack returns with its charge.
     */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (hasAttached(chest)) ARMOR_BUFFER.put(player, chest);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        ItemStack remembered = ARMOR_BUFFER.remove(player);
        if (remembered == null || remembered.isEmpty() || !hasAttached(remembered)) return;
        if (!player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) return;
        double oldCharge = charge(remembered);
        ItemStack fresh = new ItemStack(ModArmor.JETPACK_ELECTRIC.get());
        ElectricItemEnergy.charge(fresh, oldCharge, Integer.MAX_VALUE, true, false);
        player.setItemSlot(EquipmentSlot.CHEST, fresh);
    }

    /** Sneak + use on a held attached armor toggles flight instead of the vanilla equip swap. */
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!event.getEntity().isShiftKeyDown()) return;
        ItemStack held = event.getItemStack();
        if (!hasAttached(held)) return;
        event.setCanceled(true);
        if (event.getLevel().isClientSide()) return;
        boolean enabled = !held.getOrDefault(ModDataComponents.JETPACK_ACTIVE.get(), false);
        held.set(ModDataComponents.JETPACK_ACTIVE.get(), enabled);
        event.getEntity().sendSystemMessage(
                Component.translatable(enabled ? "ic2.hover_mode.enabled" : "ic2.hover_mode.disabled"));
    }

    /** Legacy client tooltip: the yellow attached line plus the virtual charge when applicable. */
    public static void addTooltip(ItemTooltipEvent event) {
        if (!hasAttached(event.getItemStack())) return;
        event.getToolTip()
                .add(Component.translatable("ic2.jetpackAttached").withStyle(ChatFormatting.YELLOW));
        if (!(event.getItemStack().getItem() instanceof ElectricItem)) {
            double charge = charge(event.getItemStack());
            event.getToolTip()
                    .add(Component.literal(
                            new DecimalFormat("0.##").format(charge) + " EU"));
        }
    }

    private JetpackAttachmentHelper() {}
}
