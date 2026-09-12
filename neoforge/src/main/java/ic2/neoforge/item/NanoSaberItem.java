package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.registration.ModSounds;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Legacy AbstractItemNanoSaber/ItemNanoSaber: a right-click toggled energy blade. While active
 * it drains 64 EU per 16 ticks held in a hand (16 EU per 64 ticks stored away), hits for 20
 * damage at full attack speed and bills 400 EU per strike, mines at speed 4 (cobwebs at 50) and
 * shuts itself off the moment its energy runs out. Striking an IC2 armor piece bills 2000 EU and
 * dumps 48000 EU (nano suit) or 300000 EU (quantum suit) straight out of the worn piece, removing
 * it once fully drained.
 */
public class NanoSaberItem extends ElectricItem {
    public static final double ACTIVATION_MINIMUM = 16.0;
    public static final double ACTIVE_ATTACK_COST = 400.0;
    public static final double ARMOR_DISABLE_COST = 2000.0;
    public static final double NANO_SUIT_DRAIN = 48000.0;
    public static final double QUANTUM_SUIT_DRAIN = 300000.0;
    public static final double HELD_BILL = 64.0;
    public static final double STORED_BILL = 16.0;
    private static final int HELD_BILL_PERIOD = 16;
    private static final int STORED_BILL_PERIOD = 64;

    private final ItemAttributeModifiers activeModifiers;
    private final ItemAttributeModifiers idleModifiers;

    public NanoSaberItem(Properties properties, ElectricItemSpec specification) {
        super(
                properties.component(
                        DataComponents.TOOL,
                        new Tool(
                                List.of(
                                        Tool.Rule.deniesDrops(
                                                blocks().getOrThrow(
                                                        BlockTags.INCORRECT_FOR_DIAMOND_TOOL))),
                                1.0F,
                                0,
                                true)),
                specification);
        this.activeModifiers =
                ItemAttributeModifiers.builder()
                        .add(
                                Attributes.ATTACK_DAMAGE,
                                new AttributeModifier(
                                        Identifier.fromNamespaceAndPath(
                                                "ic2", "nano_saber_attack_damage"),
                                        20.0,
                                        AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .add(
                                Attributes.ATTACK_SPEED,
                                new AttributeModifier(
                                        Identifier.fromNamespaceAndPath(
                                                "ic2", "nano_saber_attack_speed"),
                                        0.0,
                                        AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .build();
        this.idleModifiers =
                ItemAttributeModifiers.builder()
                        .add(
                                Attributes.ATTACK_DAMAGE,
                                new AttributeModifier(
                                        Identifier.fromNamespaceAndPath(
                                                "ic2", "nano_saber_attack_damage"),
                                        4.0,
                                        AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .add(
                                Attributes.ATTACK_SPEED,
                                new AttributeModifier(
                                        Identifier.fromNamespaceAndPath(
                                                "ic2", "nano_saber_attack_speed"),
                                        -3.0,
                                        AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .build();
    }

    private static HolderGetter<Block> blocks() {
        return BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
    }

    public static boolean isActive(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModDataComponents.SABER_ACTIVE));
    }

    /** Legacy NBT only reads back true while set, so deactivation removes the component. */
    private static void setActive(ItemStack stack, boolean active) {
        if (active) {
            stack.set(ModDataComponents.SABER_ACTIVE, true);
        } else {
            stack.remove(ModDataComponents.SABER_ACTIVE);
        }
        stack.remove(ModDataComponents.SABER_TICK);
    }

    /** Legacy manager.canUse: enough stored charge for the requested operation. */
    private static boolean canUse(ItemStack stack, double amount) {
        return ElectricItemEnergy.charge(stack) >= amount;
    }

    public static boolean isNanoSuit(ItemStack stack) {
        return stack.getItem() instanceof NanoSuitItem;
    }

    public static boolean isQuantumSuit(ItemStack stack) {
        return stack.getItem() instanceof QuantumSuitItem;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel
                && (isActive(stack) || canUse(stack, ACTIVATION_MINIMUM))) {
            boolean activating = !isActive(stack);
            setActive(stack, activating);
            this.playSound(
                    serverLevel,
                    player,
                    activating
                            ? ModSounds.ITEM_NANOSABER_POWER_UP.get()
                            : ModSounds.ITEM_NANOSABER_IDLE.get());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, owner, slot);
        if (!isActive(stack) || !(owner instanceof Player player)) {
            return;
        }

        int energyTick = stack.getOrDefault(ModDataComponents.SABER_TICK, 0) + 1;
        stack.set(ModDataComponents.SABER_TICK, energyTick);
        if (energyTick % HELD_BILL_PERIOD != 0) {
            return;
        }

        // Legacy distinguished hotbar (slot < 9) from backpack storage; 26.1.2 tick callbacks
        // only know the equipment slot, so a held blade bills 64 EU per 16 ticks and a stored
        // one 16 EU per 64 ticks.
        boolean held = slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
        if (!held && energyTick % STORED_BILL_PERIOD != 0) {
            return;
        }
        if (!ElectricItemEnergy.use(stack, held ? HELD_BILL : STORED_BILL, player)) {
            // Legacy consumeEnergy override: an empty blade switches itself off.
            setActive(stack, false);
        }
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!isActive(stack) || !(attacker.level() instanceof ServerLevel)) {
            return;
        }
        if (!ElectricItemEnergy.use(stack, ACTIVE_ATTACK_COST, attacker)) {
            setActive(stack, false);
            return;
        }

        boolean mayDisable = !(attacker instanceof ServerPlayer serverPlayer)
                || !(target instanceof Player victim)
                || serverPlayer.canHarmPlayer(victim);
        if (!mayDisable) {
            return;
        }

        for (EquipmentSlot slot :
                new EquipmentSlot[] {
                    EquipmentSlot.HEAD,
                    EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS,
                    EquipmentSlot.FEET
                }) {
            if (!canUse(stack, ARMOR_DISABLE_COST)) {
                break;
            }
            ItemStack armor = target.getItemBySlot(slot);
            double drain = isNanoSuit(armor)
                    ? NANO_SUIT_DRAIN
                    : (isQuantumSuit(armor) ? QUANTUM_SUIT_DRAIN : 0.0);
            if (drain <= 0.0) {
                continue;
            }
            // Legacy drained the worn piece through its own consumeEnergy with the saber's
            // tier and no transfer limit.
            ElectricItemEnergy.discharge(armor, drain, 3, true, false, false);
            if (!canUse(armor, 1.0)) {
                target.setItemSlot(slot, ItemStack.EMPTY);
            }
            ElectricItemEnergy.use(stack, ARMOR_DISABLE_COST, attacker);
        }
    }

    /** Legacy getDestroySpeed: 50 on cobwebs and 4 elsewhere while active, 1 when off. */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!isActive(stack)) {
            return 1.0F;
        }
        return state.getBlock() == Blocks.COBWEB ? 50.0F : 4.0F;
    }

    /**
     * Legacy canAttackBlock: the blade refuses to break blocks in creative. 26.1.2 removed the
     * item hook; NeoForge now fires BreakBlockEvent pre-cancelled for that gate, so the veto
     * lives here instead.
     */
    public static void onBreakBlock(BreakBlockEvent event) {
        if (event.getPlayer().isCreative()
                && event.getPlayer().getMainHandItem().getItem() instanceof NanoSaberItem) {
            event.setCanceled(true);
        }
    }

    /** Legacy getAttributeModifiers: 20 damage at speed 0 active, 4 at speed -3 off. */
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return isActive(stack) && canUse(stack, ACTIVE_ATTACK_COST)
                ? this.activeModifiers
                : this.idleModifiers;
    }

    private void playSound(ServerLevel level, Player player, SoundEvent sound) {
        level.playSound(
                null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS,
                1.0F, 1.0F);
    }
}
