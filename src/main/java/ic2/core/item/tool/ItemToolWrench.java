package ic2.core.item.tool;

import ic2.api.item.BlockBreakableItem;
import ic2.api.item.IBoxable;
import ic2.api.item.IEnhancedOverlayProvider;
import ic2.api.tile.IWrenchAble;
import ic2.core.IC2;
import ic2.core.IHitSoundOverride;
import ic2.core.init.IC2Config;
import ic2.core.item.PriorityUsableItem;
import ic2.core.ref.Ic2BlockTags;
import ic2.core.ref.Ic2ItemTags;
import ic2.core.ref.Ic2SoundEvents;
import ic2.core.util.Ic2Tooltip;
import ic2.core.util.LogCategory;
import ic2.core.util.RotationUtil;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemToolWrench extends Item
        implements PriorityUsableItem,
                IBoxable,
                BlockBreakableItem,
                IEnhancedOverlayProvider,
                IHitSoundOverride {
    private static final int MINE_DAMAGE = 1;
    public static final float WRENCH_DESTROY_SPEED = 6.0F;

    public ItemToolWrench(Properties settings) {
        super(settings);
    }

    public static boolean isWrenchTarget(BlockState state) {
        return state.is(Ic2BlockTags.MINEABLE_WITH_WRENCH)
                || state.getBlock() instanceof IWrenchAble;
    }

    public static Direction facingFromHit(Direction side, BlockPos pos, Vec3 hitLocation) {
        float hitX = (float) (hitLocation.x - pos.getX());
        float hitY = (float) (hitLocation.y - pos.getY());
        float hitZ = (float) (hitLocation.z - pos.getZ());
        return RotationUtil.rotateByHit(side, hitX, hitY, hitZ);
    }

    public static InteractionResult trySetFacingFromHit(UseOnContext context, Player player) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return InteractionResult.FAIL;
        }

        if (state.getBlock() instanceof IWrenchAble wrenchAble) {
            Direction targetFacing =
                    facingFromHit(context.getClickedFace(), pos, context.getClickLocation());
            wrenchAble.setFacing(world, pos, targetFacing, player);
            if (world.isClientSide) {
                player.playSound(Ic2SoundEvents.ITEM_WRENCH_USE, 1.0F, 1.0F);
                return InteractionResult.PASS;
            } else {
                return InteractionResult.SUCCESS;
            }
        } else {
            return InteractionResult.FAIL;
        }
    }

    public static boolean tryRemoveWithWrench(
            Level world, Player player, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof IWrenchAble wrenchAble
                && wrenchAble.wrenchCanRemove(world, pos, player)) {
            removeBlockWithWrench(world, pos, state, player, wrenchAble);
            return true;
        } else {
            return false;
        }
    }

    static void removeBlockWithWrench(
            Level world, BlockPos pos, BlockState state, Player player, IWrenchAble wrenchAble) {
        if (!world.isClientSide) {
            if (!player.blockActionRestricted(
                    world, pos, ((ServerPlayer) player).gameMode.getGameModeForPlayer())) {
                Block block = state.getBlock();
                BlockEntity te = world.getBlockEntity(pos);
                if (IC2Config.protection.wrenchLogging.get()) {
                    String playerName =
                            player.getGameProfile().getName()
                                    + "/"
                                    + player.getGameProfile().getId();
                    IC2.log.info(
                            LogCategory.PlayerActivity,
                            "Player %s used a wrench to remove the block %s (te %s) at %s.",
                            playerName,
                            state,
                            getTeName(te),
                            Util.formatPosition(world, pos));
                }

                block.playerWillDestroy(world, pos, state, player);
                if (world.removeBlock(pos, false)) {
                    block.destroy(world, pos, state);
                }

                List<ItemStack> drops = wrenchAble.getWrenchDrops(world, pos, state, te, player, 0);
                if (drops != null && !drops.isEmpty()) {
                    for (ItemStack drop : drops) {
                        StackUtil.dropAsEntity(world, pos, drop);
                    }
                } else if (IC2Config.debug.logEmptyWrenchDrops.get()) {
                    IC2.log.warn(
                            LogCategory.General,
                            "The block %s (te %s) at %s didn't yield any wrench drops.",
                            state,
                            getTeName(te),
                            Util.formatPosition(world, pos));
                }

                if (!player.getAbilities().instabuild) {
                    state.spawnAfterBreak((ServerLevel) world, pos, player.getUseItem(), false);
                }
            }
        }
    }

    private static String getTeName(BlockEntity te) {
        return te != null
                ? ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(te.getType()).toString()
                : "none";
    }

    @Override
    public InteractionResult onBlockStartBreak(
            Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean beforeBlockBreak(
            Level world,
            Player player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity) {
        if (tryRemoveWithWrench(world, player, pos, state)) {
            player.getMainHandItem()
                    .hurtAndBreak(1, player, p -> p.broadcastBreakEvent(p.getUsedItemHand()));
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void afterBlockBreak(
            Level world,
            Player player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity) {}

    @Override
    public boolean isCorrectToolForDrops(BlockState state) {
        return isWrenchTarget(state);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return this.isCorrectToolForDrops(state) ? 6.0F : super.getDestroySpeed(stack, state);
    }

    public boolean canTakeDamage() {
        return true;
    }

    public boolean canTakeDamage(ItemStack stack, int amount) {
        return true;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (!this.canTakeDamage(stack, 1)) {
            return InteractionResult.FAIL;
        }

        Player player = context.getPlayer();
        return player == null ? InteractionResult.PASS : trySetFacingFromHit(context, player);
    }

    public void damage(ItemStack is, int damage, Player player, InteractionHand hand) {
        is.hurtAndBreak(damage, player, p -> p.broadcastBreakEvent(hand));
    }

    @Override
    public boolean canBeStoredInToolbox(ItemStack itemstack) {
        return true;
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, ItemStack repair) {
        return repair.is(Ic2ItemTags.BRONZE_INGOTS);
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            Level world,
            List<Component> info,
            @NotNull TooltipFlag flag) {
        Component attackKey = Minecraft.getInstance().options.keyAttack.getTranslatedKeyMessage();
        Component useKey = Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage();
        Ic2Tooltip.add(info, Component.translatable("item.ic2.wrench.tooltip.mine", attackKey));
        Ic2Tooltip.add(info, Component.translatable("item.ic2.wrench.tooltip.rotate", useKey));
    }

    @Override
    public boolean providesEnhancedOverlay(
            Level world, BlockPos pos, Direction side, Player player, ItemStack stack) {
        return world.getBlockState(pos).getBlock() instanceof IWrenchAble;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public SoundEvent getHitSoundForBlock(
            LocalPlayer player, Level world, BlockPos pos, ItemStack stack) {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public SoundEvent getBreakSoundForBlock(
            LocalPlayer player, Level world, BlockPos pos, ItemStack stack) {
        if (player.getAbilities().instabuild) {
            return null;
        } else {
            return world.getBlockState(pos).getBlock() instanceof IWrenchAble
                    ? Ic2SoundEvents.ITEM_WRENCH_USE
                    : null;
        }
    }
}
