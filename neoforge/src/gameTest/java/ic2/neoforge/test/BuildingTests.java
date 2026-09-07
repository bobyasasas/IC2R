package ic2.neoforge.test;

import ic2.neoforge.registration.ModRubberBuilding;
import ic2.neoforge.world.RubberSignBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

final class BuildingTests {
    static void redstoneAndShapes(GameTestHelper helper) {
        var level = helper.getLevel();
        var button = new BlockPos(2, 1, 2);
        helper.setBlock(
                button,
                ModRubberBuilding.BUTTON
                        .get()
                        .defaultBlockState()
                        .setValue(ButtonBlock.FACE, AttachFace.FLOOR));
        helper.pressButton(button);
        helper.assertTrue(
                helper.getBlockState(button).getValue(ButtonBlock.POWERED),
                "Rubber button must activate");
        var fence = new BlockPos(5, 1, 5);
        helper.setBlock(fence, ModRubberBuilding.FENCE.get());
        helper.setBlock(fence.east(), Blocks.OAK_FENCE);
        helper.assertTrue(
                helper.getBlockState(fence).getValue(FenceBlock.EAST),
                "Native wooden fences must connect across wood families");
        var lower = new BlockPos(8, 1, 8);
        helper.setBlock(lower, ModRubberBuilding.DOOR.get());
        helper.setBlock(
                lower.above(),
                ModRubberBuilding.DOOR
                        .get()
                        .defaultBlockState()
                        .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        helper.useBlock(lower);
        helper.assertTrue(
                helper.getBlockState(lower).getValue(DoorBlock.OPEN)
                        && helper.getBlockState(lower.above()).getValue(DoorBlock.OPEN),
                "Door interaction must update both halves");
        for (var half : DoubleBlockHalf.values()) {
            var state =
                    ModRubberBuilding.DOOR.get().defaultBlockState().setValue(DoorBlock.HALF, half);
            var doorDrops =
                    Block.getDrops(
                            state,
                            level,
                            helper.absolutePos(lower),
                            null,
                            null,
                            new ItemStack(Items.IRON_AXE));
            helper.assertTrue(
                    half == DoubleBlockHalf.UPPER
                            ? doorDrops.isEmpty()
                            : doorDrops.size() == 1
                                    && doorDrops.getFirst().is(ModRubberBuilding.DOOR.asItem()),
                    "Door must drop exactly once from its lower half");
        }
        var slab =
                ModRubberBuilding.SLAB
                        .get()
                        .defaultBlockState()
                        .setValue(SlabBlock.TYPE, SlabType.DOUBLE);
        var drops =
                Block.getDrops(
                        slab,
                        level,
                        helper.absolutePos(new BlockPos(10, 1, 10)),
                        null,
                        null,
                        new ItemStack(Items.IRON_AXE));
        helper.assertTrue(
                drops.size() == 1
                        && drops.getFirst().is(ModRubberBuilding.SLAB.asItem())
                        && drops.getFirst().getCount() == 2,
                "Double slab must return two slabs");
        helper.runAtTickTime(
                29,
                () ->
                        helper.assertTrue(
                                helper.getBlockState(button).getValue(ButtonBlock.POWERED),
                                "Button must remain on for thirty ticks"));
        helper.runAtTickTime(
                32,
                () -> {
                    helper.assertTrue(
                            !helper.getBlockState(button).getValue(ButtonBlock.POWERED),
                            "Button must release after thirty ticks");
                    helper.succeed();
                });
    }

    static void signs(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(3, 1, 3));
        level.setBlockAndUpdate(pos, ModRubberBuilding.SIGN.get().defaultBlockState());
        var sign = (RubberSignBlockEntity) level.getBlockEntity(pos);
        sign.updateText(
                text ->
                        text.setMessage(0, Component.literal("IndustrialCraft"))
                                .setColor(DyeColor.BLUE)
                                .setHasGlowingText(true),
                true);
        sign.updateText(text -> text.setMessage(1, Component.literal("橡胶木告示牌")), false);
        sign.setWaxed(true);
        var restored =
                (RubberSignBlockEntity)
                        BlockEntity.loadStatic(
                                pos,
                                sign.getBlockState(),
                                sign.saveWithFullMetadata(level.registryAccess()),
                                level.registryAccess());
        helper.assertTrue(
                restored != null && restored.getType() == ModRubberBuilding.SIGN_ENTITY.get(),
                "Sign must retain its IC2 block entity ID");
        helper.assertTrue(
                restored.getFrontText().getMessage(0, false).getString().equals("IndustrialCraft")
                        && restored.getFrontText().getColor() == DyeColor.BLUE
                        && restored.getFrontText().hasGlowingText()
                        && restored.getBackText().getMessage(1, false).getString().equals("橡胶木告示牌")
                        && restored.isWaxed(),
                "Sign must retain both faces, color, glow and wax");
        var wall = ModRubberBuilding.WALL_SIGN.get().defaultBlockState();
        var drops = Block.getDrops(wall, level, pos, null, null, new ItemStack(Items.IRON_AXE));
        helper.assertTrue(
                drops.size() == 1 && drops.getFirst().is(ModRubberBuilding.SIGN.asItem()),
                "Wall sign must drop the standing sign item");
        var wallPos = pos.east(3);
        level.setBlockAndUpdate(
                wallPos.relative(Direction.SOUTH), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(wallPos, wall);
        helper.assertTrue(
                level.getBlockEntity(wallPos) instanceof RubberSignBlockEntity,
                "Wall sign must use the same persistent entity type");
        helper.succeed();
    }

    private BuildingTests() {}
}
