package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModFoam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Sprays construction foam over a small flood-filled area, one hundred mB per block. */
public class FoamSprayerItem extends Item {
    public static final int CAPACITY_MB = 8000;
    public static final int FLUID_PER_FOAM = 100;
    private static final int NORMAL_MODE_BLOCKS = 10;

    public FoamSprayerItem(Properties properties) {
        super(properties);
    }

    /** Only wet construction foam can be loaded; hardened foam is a block again. */
    public static Fluid foamFluid() {
        return ModFluids.FAMILIES.get(FluidDefinition.CONSTRUCTION_FOAM).source().get();
    }

    public static int getContentsMb(ItemStack stack) {
        FluidStackTemplate stored = stack.get(ModDataComponents.FLUID);
        return stored == null ? 0 : Math.min(CAPACITY_MB, stored.amount());
    }

    private static boolean isSingleMode(ItemStack stack) {
        Integer mode = stack.get(ModDataComponents.SPRAY_MODE);
        return mode != null && mode == 1;
    }

    private static void drainMb(ItemStack stack, int amount) {
        int remaining = getContentsMb(stack) - amount;
        if (remaining <= 0) {
            stack.remove(ModDataComponents.FLUID);
        } else {
            stack.set(ModDataComponents.FLUID, new FluidStackTemplate(foamFluid(), remaining));
        }
    }

    /**
     * Shift-use in the air toggles spray modes; the legacy M keybind has no ported keybind system
     * yet.
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide()) {
                int mode = isSingleMode(stack) ? 0 : 1;
                stack.set(ModDataComponents.SPRAY_MODE, mode);
                player.sendSystemMessage(
                        Component.translatable(
                                "ic2.tooltip.mode",
                                Component.translatable(
                                        mode == 0
                                                ? "ic2.tooltip.mode.normal"
                                                : "ic2.tooltip.mode.single")));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        int maxFoamBlocks = getContentsMb(context.getItemInHand()) / FLUID_PER_FOAM;
        ItemStack pack = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean hasPack = pack.getItem() instanceof CFPackItem;
        if (hasPack) {
            maxFoamBlocks += CFPackItem.getContentsMb(pack) / FLUID_PER_FOAM;
        }
        if (maxFoamBlocks == 0) {
            return InteractionResult.FAIL;
        }
        maxFoamBlocks =
                Math.min(
                        maxFoamBlocks,
                        isSingleMode(context.getItemInHand()) ? 1 : NORMAL_MODE_BLOCKS);

        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        // Spraying scaffolding covers the scaffold itself and spreads across the scaffold network;
        // every other target spreads through replaceable space in front of the clicked face.
        boolean replaceInPlace = state.is(Blocks.SCAFFOLDING);
        if (!replaceInPlace) {
            pos = pos.relative(context.getClickedFace());
        }
        Predicate<BlockPos> foamable =
                replaceInPlace
                        ? p -> level.getBlockState(p).is(Blocks.SCAFFOLDING)
                        : p -> level.getBlockState(p).canBeReplaced();

        // Foam must not flow back onto the face the player stands behind.
        Direction excludedDir = player.getDirection().getOpposite();
        Set<BlockPos> positions = collectFoamArea(level, pos, excludedDir, maxFoamBlocks, foamable);
        int placed = 0;
        for (BlockPos targetPos : positions) {
            if (replaceInPlace) {
                level.destroyBlock(targetPos, true);
                level.setBlockAndUpdate(targetPos, ModFoam.FOAM.get().defaultBlockState());
                placed++;
            } else if (level.setBlockAndUpdate(targetPos, ModFoam.FOAM.get().defaultBlockState())) {
                placed++;
            }
        }
        if (placed == 0) {
            return InteractionResult.PASS;
        }

        int drainedMb = placed * FLUID_PER_FOAM;
        if (hasPack && drainedMb > 0) {
            int fromPack = CFPackItem.drainMb(pack, drainedMb);
            drainedMb -= fromPack;
            player.setItemSlot(EquipmentSlot.CHEST, pack);
        }
        if (drainedMb > 0) {
            drainMb(context.getItemInHand(), drainedMb);
        }
        return InteractionResult.SUCCESS;
    }

    /** Flood fill bounded by the mode cap; skips already-collected and unplaceable positions. */
    private static Set<BlockPos> collectFoamArea(
            Level level,
            BlockPos origin,
            Direction excludedDir,
            int maxFoamBlocks,
            Predicate<BlockPos> foamable) {
        Queue<BlockPos> toCheck = new ArrayDeque<>();
        Set<BlockPos> positions = new HashSet<>();
        toCheck.add(origin);

        BlockPos current;
        while ((current = toCheck.poll()) != null && positions.size() < maxFoamBlocks) {
            if (foamable.test(current) && positions.add(current)) {
                for (Direction dir : Direction.values()) {
                    if (dir != excludedDir) {
                        toCheck.add(current.relative(dir));
                    }
                }
            }
        }
        return positions;
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
                        "item.ic2.foam_sprayer.tooltip.content", getContentsMb(stack)));
    }
}
