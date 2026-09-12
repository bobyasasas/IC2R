package ic2.neoforge.item;

import ic2.core.geometry.FaceSelection;
import ic2.neoforge.machine.LuminatorBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.ModSounds;

import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Shared rotation behavior; normal mining supplies protection hooks, drops and tool consumption.
 */
public interface WrenchTool {
    static InteractionResult rotate(UseOnContext context) {
        var level = context.getLevel();
        var player = context.getPlayer();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        if (player == null || !(state.getBlock() instanceof MachineBlock))
            return InteractionResult.PASS;
        if (!level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))
            return InteractionResult.FAIL;
        // Legacy luminator setFacingWrench: a wrench never rotates the lamp, it flips the
        // redstone inversion instead (and, unlike other machines, cannot dismantle it).
        if (((MachineBlock) state.getBlock()).kind() == MachineKind.LUMINATOR) {
            if (!level.isClientSide()) {
                if (level.getBlockEntity(pos) instanceof LuminatorBlockEntity luminator)
                    luminator.toggleInvert();
                level.playSound(
                        null, pos, ModSounds.ITEM_WRENCH_USE.get(), SoundSource.BLOCKS, 1, 1);
            }
            return InteractionResult.SUCCESS;
        }
        var hit = context.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        var selected =
                FaceSelection.select(
                        FaceSelection.Face.valueOf(context.getClickedFace().name()),
                        (float) hit.x,
                        (float) hit.y,
                        (float) hit.z);
        if (!((MachineBlock) state.getBlock()).kind().verticalFacing()
                && (selected == FaceSelection.Face.UP || selected == FaceSelection.Face.DOWN))
            return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(
                    pos, state.setValue(MachineBlock.FACING, Direction.valueOf(selected.name())));
            level.playSound(null, pos, ModSounds.ITEM_WRENCH_USE.get(), SoundSource.BLOCKS, 1, 1);
        }
        return InteractionResult.SUCCESS;
    }

    static boolean isWrench(ItemStack stack) {
        return stack.getItem() instanceof WrenchTool;
    }
}
