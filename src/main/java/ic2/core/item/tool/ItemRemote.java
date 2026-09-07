package ic2.core.item.tool;

import ic2.core.IC2;
import ic2.core.block.BlockDynamite;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2SoundEvents;
import ic2.core.util.Ic2Tooltip;
import ic2.core.util.StackUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemRemote extends Item {
    private static final String COORDS_KEY = "coords";

    public ItemRemote(Properties properties) {
        super(properties.stacksTo(1));
    }

    @NotNull
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Ic2Blocks.DYNAMITE)) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = StackUtil.get(player, context.getHand());
        if (!state.getValue(BlockDynamite.LINKED)) {
            addRemote(pos, stack);
            level.setBlock(pos, state.setValue(BlockDynamite.LINKED, true), 3);
        } else {
            int index = hasRemote(pos, stack);
            if (index > -1) {
                level.setBlock(pos, state.setValue(BlockDynamite.LINKED, false), 3);
                removeRemote(index, stack);
            } else {
                IC2.sideProxy.messagePlayer(player, "ic2.remote.cannot_unlink");
            }
        }

        return InteractionResult.SUCCESS;
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(
            @NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = StackUtil.get(player, hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                Ic2SoundEvents.ITEM_REMOTE_USE,
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        launchRemotes(level, stack, player);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            @Nullable Level level,
            @NotNull List<Component> tooltip,
            @NotNull TooltipFlag flag) {
        int linked = getLinkedCount(stack);
        if (linked > 0) {
            Ic2Tooltip.add(tooltip, Component.translatable("ic2.remote.tooltip.linked", linked));
        }
    }

    public static void addRemote(BlockPos pos, ItemStack remoteStack) {
        CompoundTag compound = StackUtil.getOrCreateNbtData(remoteStack);
        ListTag linkedPositions =
                compound.contains(COORDS_KEY, 9) ? compound.getList(COORDS_KEY, 10) : new ListTag();
        CompoundTag positionTag = new CompoundTag();
        positionTag.putInt("x", pos.getX());
        positionTag.putInt("y", pos.getY());
        positionTag.putInt("z", pos.getZ());
        linkedPositions.add(positionTag);
        compound.put(COORDS_KEY, linkedPositions);
    }

    public static void launchRemotes(Level level, ItemStack remoteStack, Player player) {
        CompoundTag compound = StackUtil.getOrCreateNbtData(remoteStack);
        if (!compound.contains(COORDS_KEY, 9)) {
            return;
        }
        ListTag linkedPositions = compound.getList(COORDS_KEY, 10);
        int index = 0;

        while (index < linkedPositions.size()) {
            CompoundTag positionTag = linkedPositions.getCompound(index);
            BlockPos pos =
                    new BlockPos(
                            positionTag.getInt("x"),
                            positionTag.getInt("y"),
                            positionTag.getInt("z"));
            if (level.isLoaded(pos)) {
                BlockState state = level.getBlockState(pos);
                if (state.is(Ic2Blocks.DYNAMITE) && state.getValue(BlockDynamite.LINKED)) {
                    ((BlockDynamite) state.getBlock()).detonate(level, pos, player);
                }

                // Removal shifts the next entry into the current index.
                linkedPositions.remove(index);
            } else {
                // Keep links in unloaded chunks for a later attempt.
                index++;
            }
        }

        if (linkedPositions.isEmpty()) {
            compound.remove(COORDS_KEY);
        } else {
            compound.put(COORDS_KEY, linkedPositions);
        }
    }

    /** Returns the linked position's index, or -1 when this remote has no matching link. */
    public static int hasRemote(BlockPos pos, ItemStack remoteStack) {
        CompoundTag compound = StackUtil.getOrCreateNbtData(remoteStack);
        if (!compound.contains(COORDS_KEY, 9)) {
            return -1;
        }

        ListTag linkedPositions = compound.getList(COORDS_KEY, 10);

        for (int i = 0; i < linkedPositions.size(); i++) {
            CompoundTag positionTag = linkedPositions.getCompound(i);
            if (positionTag.getInt("x") == pos.getX()
                    && positionTag.getInt("y") == pos.getY()
                    && positionTag.getInt("z") == pos.getZ()) {
                return i;
            }
        }

        return -1;
    }

    public static void removeRemote(int index, ItemStack remoteStack) {
        CompoundTag compound = StackUtil.getOrCreateNbtData(remoteStack);
        if (compound.contains(COORDS_KEY, 9)) {
            ListTag linkedPositions = compound.getList(COORDS_KEY, 10);
            ListTag newCoords = new ListTag();

            for (int i = 0; i < linkedPositions.size(); i++) {
                if (i != index) {
                    newCoords.add(linkedPositions.get(i));
                }
            }

            if (newCoords.isEmpty()) {
                compound.remove(COORDS_KEY);
            } else {
                compound.put(COORDS_KEY, newCoords);
            }
        }
    }

    public static int getLinkedCount(ItemStack remoteStack) {
        CompoundTag compound = StackUtil.getOrCreateNbtData(remoteStack);
        return !compound.contains(COORDS_KEY, 9) ? 0 : compound.getList(COORDS_KEY, 10).size();
    }
}
