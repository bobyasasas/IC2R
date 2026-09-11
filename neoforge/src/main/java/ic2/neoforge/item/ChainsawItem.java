package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.registration.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.List;
import java.util.function.Consumer;

/**
 * Electric chainsaw: axe-speed logger that also shears vegetation and livestock. Ordinary axe
 * mining and cobweb breaking are free like legacy; shearing a shearable block (shear mode on),
 * shearing a shearable entity or attacking each consume one operation worth of EU.
 */
public class ChainsawItem extends ElectricItem {
    public static final double OPERATION_ENERGY_COST = 100.0;
    public static final float MINING_SPEED = 12.0F;

    public ChainsawItem(Properties properties, ElectricItemSpec specification) {
        super(
                properties.component(DataComponents.TOOL, tool())
                        .component(DataComponents.ATTRIBUTE_MODIFIERS, attributes()),
                specification);
    }

    /** Legacy CHAINSAW material: diamond-level drops, axe-mineable blocks at speed 12. */
    private static Tool tool() {
        HolderGetter<Block> blocks =
                BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        return new Tool(
                List.of(
                        Tool.Rule.deniesDrops(
                                blocks.getOrThrow(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)),
                        Tool.Rule.minesAndDrops(
                                blocks.getOrThrow(BlockTags.MINEABLE_WITH_AXE), MINING_SPEED)),
                1.0F,
                0,
                true);
    }

    /** Legacy DiggerItem attributes: +11 attack damage, -3 attack speed in the main hand. */
    private static ItemAttributeModifiers attributes() {
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                Identifier.fromNamespaceAndPath("ic2", "chainsaw_attack_damage"),
                                11.0,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                Identifier.fromNamespaceAndPath("ic2", "chainsaw_attack_speed"),
                                -3.0,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public boolean canUse(ItemStack stack) {
        return ElectricItemEnergy.charge(stack) >= OPERATION_ENERGY_COST;
    }

    /** Legacy shear break runs unless the disableShear flag was toggled with sneak use. */
    public boolean isShearMode(ItemStack stack) {
        return !Boolean.TRUE.equals(stack.get(ModDataComponents.CHAINSAW_DISABLE_SHEAR));
    }

    /** Legacy Util.canShear: vegetation, tripwire, cobweb and wool. Grass is now short_grass. */
    public static boolean canShear(BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.getBlock() instanceof LeavesBlock
                || state.is(Blocks.COBWEB)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.DEAD_BUSH)
                || state.is(Blocks.HANGING_ROOTS)
                || state.is(Blocks.VINE)
                || state.is(Blocks.TRIPWIRE)
                || state.is(BlockTags.WOOL);
    }

    /** Legacy harvest check has no energy gate: correct for axe blocks, cobweb and shearables. */
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return super.isCorrectToolForDrops(stack, state)
                || state.is(Blocks.COBWEB)
                || canShear(state);
    }

    /** Legacy speed applies whenever charged, shear mode or not. */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!canUse(stack)
                || !state.is(BlockTags.MINEABLE_WITH_AXE)
                        && !state.is(Blocks.COBWEB)
                        && !canShear(state)) {
            return 1.0F;
        }
        return MINING_SPEED;
    }

    /** Ordinary mining and cobweb breaking stay free; only shear paths spend EU. */
    @Override
    public boolean mineBlock(
            ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        return true;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level().isClientSide()) {
            return;
        }
        if (dischargeOperation(stack)) {
            playUsingSound(attacker);
        }
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!this.isShearMode(stack)
                || !(target instanceof Shearable shearable)
                || !shearable.readyForShearing()) {
            return InteractionResult.PASS;
        }
        Level level = target.level();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (dischargeOperation(stack)) {
            shearable.shear((ServerLevel) level, SoundSource.PLAYERS, stack);
            playUsingSound(player);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * Sneak use in the air toggles shear break, mirroring the legacy mode-switch key + use combo.
     * The port has no keybind system yet, so shift+use stands in (same deviation as the painter).
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide()) {
                boolean disableShear = !this.isShearMode(stack);
                stack.set(ModDataComponents.CHAINSAW_DISABLE_SHEAR, disableShear);
                player.sendSystemMessage(
                        Component.translatable(
                                "item.ic2.mining_laser.tooltip.mode",
                                Component.translatable(
                                        disableShear
                                                ? "item.ic2.mining_laser.tooltip.mode.no_shear"
                                                : "item.ic2.mining_laser.tooltip.mode.normal")));
            }
            return InteractionResult.SUCCESS;
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        tooltip.accept(
                Component.translatable(
                        "item.ic2.tooltip.mode.switch",
                        Component.literal("Shift"),
                        Component.translatable("key.use")));
    }

    /** Legacy onBlockStartBreak: shear-mode vegetation drops itself for one operation of EU. */
    public static void onBreakBlock(BreakBlockEvent event) {
        ItemStack stack = event.getPlayer().getMainHandItem();
        if (!(stack.getItem() instanceof ChainsawItem chainsaw)
                || !chainsaw.isShearMode(stack)
                || !canShear(event.getState())) {
            return;
        }
        if (event.getLevel() instanceof Level level
                && chainsaw.dischargeOperation(stack)
                && shearBreak(level, event.getPlayer(), event.getPos(), event.getState(), stack)) {
            event.setCanceled(true);
        }
    }

    /** The legacy shear break: particles, self-drop, air, destroy game event, piglin anger. */
    public static boolean shearBreak(
            Level level, Player player, BlockPos pos, BlockState state, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        serverLevel.levelEvent(player, 2001, pos, Block.getId(state));
        Block block = state.getBlock();
        if (block.asItem() != Items.AIR) {
            Block.popResource(serverLevel, pos, new ItemStack(block));
        }
        serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
        serverLevel.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
        if (state.is(BlockTags.GUARDED_BY_PIGLINS)) {
            PiglinAi.angerNearbyPiglins(serverLevel, player, false);
        }
        return true;
    }

    /** One EU operation spent, mirroring legacy consumeEnergy's canUse + discharge pair. */
    private boolean dischargeOperation(ItemStack stack) {
        return ElectricItemEnergy.discharge(
                        stack,
                        OPERATION_ENERGY_COST,
                        this.specification().tier(),
                        true,
                        false,
                        false)
                >= OPERATION_ENERGY_COST;
    }

    private void playUsingSound(LivingEntity user) {
        SoundEvent sound =
                user.getRandom().nextBoolean()
                        ? ModSounds.ITEM_CHAINSAW_USE1.get()
                        : ModSounds.ITEM_CHAINSAW_USE2.get();
        user.level()
                .playSound(null, user.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
