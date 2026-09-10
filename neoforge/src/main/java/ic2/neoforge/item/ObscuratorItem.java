package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.block.ObscuredWallBlock;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.component.ObscuratorReference;
import ic2.neoforge.machine.ObscuredWallBlockEntity;
import ic2.neoforge.registration.ModFoam;
import ic2.neoforge.registration.ModObscurator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * Electric face sampler, ported from ItemObscurator: standing use retextures a construction foam
 * wall with the stored reference for 5,000 EU; sneaking use samples the targeted block face on the
 * client for 20,000 EU via the scan payload.
 */
public class ObscuratorItem extends ElectricItem {
    public static final double APPLY_ENERGY = 5000.0;
    public static final double SCAN_ENERGY = 20000.0;

    private static volatile Map<Block, DyeColor> wallsByBlock;

    public ObscuratorItem(Item.Properties properties) {
        super(properties, new ElectricItemSpec(100000, 250, 2, false));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level level = context.getLevel();
        if (player.isCrouching()) {
            if (level.isClientSide()
                    && ElectricItemEnergy.charge(stack) >= SCAN_ENERGY
                    && ObscuratorSampling.scan(
                            stack,
                            player,
                            level,
                            context.getClickedPos(),
                            context.getClickedFace())) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()
                && ElectricItemEnergy.charge(stack) >= APPLY_ENERGY
                && applyReference(
                        stack, level, context.getClickedPos(), context.getClickedFace())) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * Legacy apply branch, restricted to the one block family IC2 itself makes retexturable:
     * construction foam walls. Other blocks fall through like the legacy RetextureEvent without
     * listeners.
     */
    public static boolean applyReference(
            ItemStack stack, Level level, BlockPos pos, Direction face) {
        ObscuratorReference reference = stack.get(ModDataComponents.OBSCURATOR_REFERENCE);
        if (reference == null) return false;
        BlockState referenceState = reference.resolveState();
        if (referenceState == null) {
            stack.remove(ModDataComponents.OBSCURATOR_REFERENCE);
            return false;
        }
        DyeColor wallColor = wallColor(level.getBlockState(pos));
        if (wallColor == null) return false;
        BlockState previous = level.getBlockState(pos);
        boolean changed =
                level.setBlock(
                        pos,
                        ModObscurator.OBSCURED_WALL.get().defaultBlockState(),
                        Block.UPDATE_ALL);
        System.out.println(
                "OBSDIAG2 changed="
                        + changed
                        + " be="
                        + level.getBlockEntity(pos)
                        + " refResolved="
                        + referenceState);
        if (!changed) return false;
        if (level.getBlockEntity(pos) instanceof ObscuredWallBlockEntity wall) {
            wall.initialize(
                    wallColor,
                    face,
                    referenceState,
                    Direction.values()[reference.side()],
                    reference.colorMultipliers());
            ElectricItemEnergy.discharge(stack, APPLY_ENERGY, 2, true, false, false);
            return true;
        }
        level.setBlockAndUpdate(pos, previous);
        return false;
    }

    private static DyeColor wallColor(BlockState state) {
        if (state.getBlock() instanceof ObscuredWallBlock) return null;
        var map = wallsByBlock;
        if (map == null) {
            var built = new HashMap<Block, DyeColor>();
            ModFoam.WALLS.forEach(
                    (name, block) -> {
                        DyeColor color = DyeColor.byName(name, null);
                        if (color != null) built.put(block.get(), color);
                    });
            map = built;
            wallsByBlock = built;
        }
        return map.get(state.getBlock());
    }
}
