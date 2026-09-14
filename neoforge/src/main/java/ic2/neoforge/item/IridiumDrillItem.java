package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Legacy ItemDrillIridium: a fresh drill always arms fortune III, and a sneak use swaps it for
 * silk touch I (announced with the legacy laser mode message) and back.
 */
public class IridiumDrillItem extends DrillItem {
    public IridiumDrillItem(Properties properties, ElectricItemSpec specification) {
        super(
                properties,
                specification,
                BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
                24.0F,
                800,
                800,
                200,
                20,
                3,
                7.0F);
    }

    /** Legacy getItemStack: the creative page hands out fortune III like a fresh legacy drill. */
    public static ItemStack fortuneStack(HolderLookup.Provider registries) {
        ItemStack stack = ModTools.IRIDIUM_DRILL.toStack();
        stack.set(DataComponents.ENCHANTMENTS, fortuneEnchantments(registries));
        return stack;
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, owner, slot);
        // 26.1 has no default-component data packs, so a fresh stack arms fortune III lazily;
        // the empty enchantments component ships by default, so check both mode levels instead
        // of component presence — a user-chosen silk touch or fortune must never be overridden.
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments current =
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (current.getLevel(enchantments.getOrThrow(Enchantments.SILK_TOUCH)) == 0
                && current.getLevel(enchantments.getOrThrow(Enchantments.FORTUNE)) == 0) {
            stack.set(DataComponents.ENCHANTMENTS, fortuneEnchantments(level.registryAccess()));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player.isSecondaryUseActive()) {
            toggleMode(level, player.getItemInHand(hand), player);
            return InteractionResult.SUCCESS;
        }
        return super.use(level, player, hand);
    }

    /** Legacy mode-switch: silk touch I <-> fortune III, announced like the mining laser. */
    private void toggleMode(Level level, ItemStack stack, Player player) {
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> silk = enchantments.getOrThrow(Enchantments.SILK_TOUCH);
        Holder<Enchantment> fortune = enchantments.getOrThrow(Enchantments.FORTUNE);
        boolean silkActive =
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                        .getLevel(silk)
                        > 0;
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        mutable.set(silk, 0);
        mutable.set(fortune, 0);
        if (silkActive) {
            mutable.set(fortune, 3);
            player.sendSystemMessage(modeMessage("item.ic2.mining_laser.tooltip.mode.normal"));
        } else {
            mutable.set(silk, 1);
            player.sendSystemMessage(modeMessage("item.ic2.mining_laser.tooltip.mode.silkTouch"));
        }
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
    }

    private static ItemEnchantments fortuneEnchantments(HolderLookup.Provider registries) {
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(
                registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),
                3);
        return enchantments.toImmutable();
    }

    private static Component modeMessage(String key) {
        return Component.translatable(
                "item.ic2.mining_laser.tooltip.mode", Component.translatable(key));
    }

    /** Test hook: the current level of a legacy mode enchantment on a drill stack. */
    public static int modeLevel(
            ItemStack stack, HolderLookup.Provider registries, ResourceKey<Enchantment> key) {
        Holder<Enchantment> holder =
                registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                .getLevel(holder);
    }
}
