package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.registration.ModSounds;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.GlazedTerracottaBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.function.Consumer;

import javax.annotation.Nullable;

/** Paints dyeable vanilla blocks and sheep; breaks down into the plain painter after 32 uses. */
public class PainterItem extends Item {
    @Nullable private final DyeColor color;

    public PainterItem(Properties properties, @Nullable DyeColor color) {
        super(properties);
        this.color = color;
    }

    private static Identifier coloredBlockId(DyeColor color, String suffix) {
        return Identifier.withDefaultNamespace(color.getName() + "_" + suffix);
    }

    private static Block coloredBlock(DyeColor color, String suffix) {
        return BuiltInRegistries.BLOCK.getValue(coloredBlockId(color, suffix));
    }

    /** A block only accepts paint it does not already carry, matched by the vanilla id. */
    private static boolean canColor(Block block, DyeColor color) {
        return !BuiltInRegistries.BLOCK.getKey(block).getPath().contains(color.getName());
    }

    private static BlockState coloredDefaultState(DyeColor color, String suffix) {
        return coloredBlock(color, suffix).defaultBlockState();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (this.color == null) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (this.colorBlock(level, pos, state.getBlock(), state, this.color)) {
            this.damagePainter(stack, player, context.getHand());
            level.playSound(
                    null, pos, ModSounds.ITEM_PAINTER_USE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** Follows the legacy branch order; the first matching dyeable family wins. */
    private boolean colorBlock(
            Level level, BlockPos pos, Block block, BlockState state, DyeColor newColor) {
        for (Property<?> property : state.getProperties()) {
            if (property.getValueClass() == DyeColor.class) {
                return recolorProperty(level, pos, state, property, newColor);
            }
        }

        if (!canColor(block, newColor)) {
            return false;
        }

        if (state.is(BlockTags.WOOL)) {
            level.setBlockAndUpdate(pos, coloredDefaultState(newColor, "wool"));
            return true;
        }

        if (block instanceof StainedGlassBlock || state.is(Blocks.GLASS)) {
            level.setBlockAndUpdate(pos, coloredDefaultState(newColor, "stained_glass"));
            return true;
        }

        if (block instanceof StainedGlassPaneBlock || state.is(Blocks.GLASS_PANE)) {
            level.setBlockAndUpdate(
                    pos, coloredBlock(newColor, "stained_glass_pane").withPropertiesOf(state));
            return true;
        }

        if (state.is(BlockTags.BEDS)) {
            BlockPos otherHalfPos = pos.relative(BedBlock.getConnectedDirection(state));
            BlockState otherHalf = level.getBlockState(otherHalfPos);
            if (otherHalf.is(block)) {
                level.setBlock(
                        pos,
                        Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
                level.setBlock(
                        otherHalfPos,
                        Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
                level.setBlockAndUpdate(pos, coloredBlock(newColor, "bed").withPropertiesOf(state));
                level.setBlockAndUpdate(
                        otherHalfPos, coloredBlock(newColor, "bed").withPropertiesOf(otherHalf));
            }
            return true;
        }

        if (state.is(BlockTags.CANDLES)) {
            level.setBlockAndUpdate(pos, coloredBlock(newColor, "candle").withPropertiesOf(state));
            return true;
        }

        if (block instanceof BannerBlock) {
            level.setBlockAndUpdate(pos, coloredBlock(newColor, "banner").withPropertiesOf(state));
            return true;
        }

        if (block instanceof WallBannerBlock) {
            level.setBlockAndUpdate(
                    pos, coloredBlock(newColor, "wall_banner").withPropertiesOf(state));
            return true;
        }

        if (state.is(BlockTags.TERRACOTTA)) {
            level.setBlockAndUpdate(pos, coloredDefaultState(newColor, "terracotta"));
            return true;
        }

        if (block instanceof GlazedTerracottaBlock) {
            level.setBlockAndUpdate(
                    pos, coloredBlock(newColor, "glazed_terracotta").withPropertiesOf(state));
            return true;
        }

        if (block instanceof ConcretePowderBlock) {
            level.setBlockAndUpdate(pos, coloredDefaultState(newColor, "concrete_powder"));
            return true;
        }

        if (state.is(BlockTags.WOOL_CARPETS)) {
            level.setBlockAndUpdate(pos, coloredDefaultState(newColor, "carpet"));
            return true;
        }

        if (state.is(BlockTags.SHULKER_BOXES)) {
            return recolorShulkerBox(level, pos, state, newColor);
        }

        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if ("minecraft".equals(id.getNamespace()) && id.getPath().contains("concrete")) {
            level.setBlockAndUpdate(pos, coloredDefaultState(newColor, "concrete"));
            return true;
        }
        return false;
    }

    private static <T extends Comparable<T>> boolean recolorProperty(
            Level level, BlockPos pos, BlockState state, Property<?> property, DyeColor newColor) {
        @SuppressWarnings("unchecked")
        Property<DyeColor> colorProperty = (Property<DyeColor>) property;
        DyeColor oldColor = state.getValue(colorProperty);
        if (oldColor != newColor && colorProperty.getPossibleValues().contains(newColor)) {
            level.setBlockAndUpdate(pos, state.setValue(colorProperty, newColor));
            return true;
        }
        return false;
    }

    private static boolean recolorShulkerBox(
            Level level, BlockPos pos, BlockState state, DyeColor newColor) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return false;
        }
        var tag = blockEntity.saveWithFullMetadata(level.registryAccess());
        BlockState newState = coloredBlock(newColor, "shulker_box").withPropertiesOf(state);
        level.setBlockAndUpdate(pos, newState);
        BlockEntity newBlockEntity =
                BlockEntity.loadStatic(pos, newState, tag, level.registryAccess());
        if (newBlockEntity == null) {
            return false;
        }
        level.setBlockEntity(newBlockEntity);
        return true;
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (this.color == null) {
            return InteractionResult.PASS;
        }
        if (target instanceof Sheep sheep && sheep.getColor() != this.color) {
            sheep.setColor(this.color);
            this.damagePainter(stack, player, hand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * Shift-use in the air toggles automatic refill, mirroring the plain and colored painters. The
     * legacy mode-switch keybind has no ported keybind system yet.
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (this.color == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide()) {
                boolean newValue =
                        !Boolean.TRUE.equals(stack.get(ModDataComponents.PAINTER_AUTO_REFILL));
                stack.set(ModDataComponents.PAINTER_AUTO_REFILL, newValue);
                player.sendSystemMessage(
                        Component.translatable(
                                newValue
                                        ? "ic2.painter.auto_refill.enabled"
                                        : "ic2.painter.auto_refill.disabled"));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * Wears the painter by one use. A spent painter turns back into the plain one; with auto refill
     * enabled a spare painter of the same color from the inventory takes over instead and the plain
     * one is stored away.
     */
    private boolean damagePainter(ItemStack stack, Player player, InteractionHand hand) {
        boolean autoRefill = Boolean.TRUE.equals(stack.get(ModDataComponents.PAINTER_AUTO_REFILL));
        Item item = stack.getItem();
        stack.hurtAndBreak(1, player, hand);
        if (!stack.isEmpty()) {
            return false;
        }

        ItemStack plainPainter = new ItemStack(ModTools.PAINTER.get());
        if (!autoRefill) {
            player.setItemInHand(hand, plainPainter);
            return false;
        }

        ItemStack replacement = consumePainterFromInventory(player, item);
        if (replacement.isEmpty()) {
            player.setItemInHand(hand, plainPainter);
            return true;
        }
        player.setItemInHand(hand, replacement);
        if (!player.getInventory().add(plainPainter)) {
            player.drop(plainPainter, false);
        }
        return true;
    }

    private static ItemStack consumePainterFromInventory(Player player, Item item) {
        var inventory = player.getInventory();
        int selected = inventory.getSelectedSlot();
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack found = inventory.getItem(slot);
            if (!found.is(item)) {
                continue;
            }
            if (player.getAbilities().instabuild) {
                return found.copyWithCount(1);
            }
            if (slot == selected) {
                continue;
            }
            ItemStack taken = found.copyWithCount(1);
            found.shrink(1);
            return taken;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        if (this.color != null) {
            tooltip.accept(
                    Component.translatable(
                            "ic2.tooltip.tool.uses_left",
                            stack.getMaxDamage() - stack.getDamageValue()));
        }
    }
}
