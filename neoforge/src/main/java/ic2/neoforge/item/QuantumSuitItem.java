package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.component.ModDataComponents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Legacy ItemArmorQuantumSuit: ten million EU at tier 4, 20000 EU per absorbed damage point
 * (chest ratio 1.2, others 1.0), uncapped fall absorption from ten blocks, hazmat-grade set
 * protection while charged, plus the helmet/legs/feet life-support and motion ticks handled in
 * {@link QuantumArmorHelper}. The movement features are toggled per piece with sneak + use
 * (legacy followed the client keyboard, which a server-side port cannot read).
 */
public class QuantumSuitItem extends ElectricArmorItem implements HazmatLike {
    public static final int ENERGY_PER_DAMAGE = 20000;

    private final ArmorType type;

    public QuantumSuitItem(
            Properties properties,
            ElectricItemSpec specification,
            ArmorType type,
            ArmorMaterial material,
            int chargedProtection) {
        super(properties, specification, type, material, chargedProtection, ENERGY_PER_DAMAGE, 1.0);
        this.type = type;
    }

    /** Legacy getDamageAbsorptionRatio: the chest protects above its own damage share. */
    @Override
    public double damageAbsorptionRatio() {
        return this.type == ArmorType.CHESTPLATE ? 1.2 : 1.0;
    }

    /** Legacy ItemArmorQuantumSuit.absorbFall: no seven-point cap, offset ten blocks. */
    @Override
    public boolean absorbFall(ItemStack stack, float distance) {
        int fallDamage = Math.max((int) distance - 10, 0);
        double energyCost = this.energyPerDamage() * fallDamage;
        if (energyCost > ElectricItemEnergy.charge(stack)) {
            return false;
        }

        ElectricItemEnergy.discharge(stack, energyCost, Integer.MAX_VALUE, true, false, false);
        return true;
    }

    /** Legacy IHazmatLike.addsProtection: a charged piece guards its slot. */
    @Override
    public boolean addsProtection(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        return ElectricItemEnergy.charge(stack) > 0;
    }

    /** Legacy getColor: the vanilla dyed-colour component (data-driven dye recipes). */
    public boolean hasCustomColor(ItemStack stack) {
        return stack.get(DataComponents.DYED_COLOR) != null;
    }

    public int getColor(ItemStack stack) {
        return DyedItemColor.getOrDefault(stack, -1);
    }

    public void setColor(ItemStack stack, int color) {
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                this.togglePiece(stack, player);
            }
            return InteractionResult.SUCCESS;
        }
        return super.use(level, player, hand);
    }

    private void togglePiece(ItemStack stack, Player player) {
        switch (this.type) {
            case HELMET -> toggle(
                    stack, ModDataComponents.NIGHT_VISION_ACTIVE.get(), false,
                    player, "ic2.night_vision.mode.enabled", "ic2.night_vision.mode.disabled");
            case CHESTPLATE -> toggle(
                    stack, ModDataComponents.JETPACK_ACTIVE.get(), false,
                    player, "ic2.hover_mode.enabled", "ic2.hover_mode.disabled");
            case LEGGINGS -> toggle(
                    stack, ModDataComponents.SPEED_ENABLED.get(), true,
                    player, "ic2.speed.mode.enabled", "ic2.speed.mode.disabled");
            case BOOTS -> toggle(
                    stack, ModDataComponents.JUMP_ENABLED.get(), true,
                    player, "ic2.jump.mode.enabled", "ic2.jump.mode.disabled");
            default -> {}
        }
    }

    /** Flips a piece flag (default matching the legacy absent-NBT behaviour) and messages it. */
    private static void toggle(
            ItemStack stack,
            net.minecraft.core.component.DataComponentType<Boolean> component,
            boolean defaultWhenAbsent,
            Player player,
            String enabledKey,
            String disabledKey) {
        boolean enabled = !stack.getOrDefault(component, defaultWhenAbsent);
        stack.set(component, enabled);
        player.sendSystemMessage(
                Component.translatable(enabled ? enabledKey : disabledKey));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        switch (this.type) {
            case HELMET -> NightVisionHelper.addToggleTooltip(tooltip);
            case CHESTPLATE -> addToggleTooltip(tooltip, "item.ic2.tooltip.jetpack.toggle");
            case LEGGINGS -> addToggleTooltip(tooltip, "item.ic2.tooltip.speed.toggle");
            case BOOTS -> addToggleTooltip(tooltip, "item.ic2.tooltip.jump.toggle");
            default -> {}
        }
    }

    private static void addToggleTooltip(Consumer<Component> tooltip, String key) {
        tooltip.accept(
                Component.translatable(
                        key, Component.literal("Sneak"), Component.literal("Use")));
    }
}
