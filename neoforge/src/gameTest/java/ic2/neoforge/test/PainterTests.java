package ic2.neoforge.test;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class PainterTests {
    // The heat room floor is the structure's bottom layer, so paintable blocks need y=1 for
    // vanilla support checks; the carpet test fails on unsupported ground.
    private static final BlockPos ORIGIN = new BlockPos(17, 1, 17);

    private static ItemStack painter(DyeColor color, int damage) {
        ItemStack stack = new ItemStack(ModTools.painter(color).get());
        stack.setDamageValue(damage);
        return stack;
    }

    private static Player painterPlayer(GameTestHelper helper, ItemStack stack) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static InteractionResult paint(GameTestHelper helper, Player player, BlockPos pos) {
        // The context needs real world coordinates; helper positions are structure-relative.
        BlockPos absolute = helper.absolutePos(pos);
        var context =
                new UseOnContext(
                        helper.getLevel(),
                        player,
                        InteractionHand.MAIN_HAND,
                        player.getItemInHand(InteractionHand.MAIN_HAND),
                        new BlockHitResult(
                                Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
        return player.getItemInHand(InteractionHand.MAIN_HAND).getItem().useOn(context);
    }

    static void painterRecolorsWool(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.WHITE_WOOL);
        ItemStack stack = painter(DyeColor.RED, 0);
        Player player = painterPlayer(helper, stack);
        paint(helper, player, ORIGIN);
        helper.assertTrue(
                helper.getBlockState(ORIGIN).is(Blocks.RED_WOOL),
                "The red painter recolors white wool");
        helper.assertTrue(
                stack.getDamageValue() == 1, "One successful paint wears the painter by one");

        // Same color paint is refused and keeps the painter fresh.
        int damageBefore = stack.getDamageValue();
        paint(helper, player, ORIGIN);
        helper.assertTrue(
                helper.getBlockState(ORIGIN).is(Blocks.RED_WOOL)
                        && stack.getDamageValue() == damageBefore,
                "Painting the same color again is refused without wear");
        helper.succeed();
    }

    static void painterRecolorsGlassFamily(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.GLASS);
        helper.setBlock(ORIGIN.east(), Blocks.WHITE_STAINED_GLASS_PANE);
        Player player = painterPlayer(helper, painter(DyeColor.RED, 0));
        paint(helper, player, ORIGIN);
        paint(helper, player, ORIGIN.east());
        helper.assertTrue(
                helper.getBlockState(ORIGIN).is(Blocks.RED_STAINED_GLASS),
                "Plain glass paints into stained glass");
        helper.assertTrue(
                helper.getBlockState(ORIGIN.east()).is(Blocks.RED_STAINED_GLASS_PANE),
                "A white glass pane paints into the red pane");
        helper.succeed();
    }

    static void painterRecolorsTerracottaAndConcrete(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.WHITE_TERRACOTTA);
        helper.setBlock(ORIGIN.east(), Blocks.WHITE_CONCRETE_POWDER);
        helper.setBlock(ORIGIN.west(), Blocks.WHITE_CONCRETE);
        Player player = painterPlayer(helper, painter(DyeColor.BLACK, 0));
        paint(helper, player, ORIGIN);
        paint(helper, player, ORIGIN.east());
        paint(helper, player, ORIGIN.west());
        helper.assertTrue(
                helper.getBlockState(ORIGIN).is(Blocks.BLACK_TERRACOTTA),
                "Terracotta paints into the dyed terracotta");
        helper.assertTrue(
                helper.getBlockState(ORIGIN.east()).is(Blocks.BLACK_CONCRETE_POWDER),
                "Concrete powder paints into the dyed powder");
        helper.assertTrue(
                helper.getBlockState(ORIGIN.west()).is(Blocks.BLACK_CONCRETE),
                "Concrete paints into the dyed concrete");
        helper.succeed();
    }

    static void painterRecolorsCarpetAndCandle(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.WHITE_CARPET);
        helper.setBlock(ORIGIN.east(), Blocks.WHITE_CANDLE);
        Player player = painterPlayer(helper, painter(DyeColor.RED, 0));
        paint(helper, player, ORIGIN);
        paint(helper, player, ORIGIN.east());
        helper.assertTrue(
                helper.getBlockState(ORIGIN).is(Blocks.RED_CARPET),
                "Wool carpets paint into the dyed carpet");
        helper.assertTrue(
                helper.getBlockState(ORIGIN.east()).is(Blocks.RED_CANDLE),
                "Candles paint into the dyed candle");
        helper.succeed();
    }

    static void painterRecolorsBedBothHalves(GameTestHelper helper) {
        helper.setBlock(
                ORIGIN,
                Blocks.WHITE_BED
                        .defaultBlockState()
                        .setValue(BedBlock.PART, BedPart.FOOT)
                        .setValue(BedBlock.FACING, Direction.EAST));
        helper.setBlock(
                ORIGIN.east(),
                Blocks.WHITE_BED
                        .defaultBlockState()
                        .setValue(BedBlock.PART, BedPart.HEAD)
                        .setValue(BedBlock.FACING, Direction.EAST));
        Player player = painterPlayer(helper, painter(DyeColor.RED, 0));
        paint(helper, player, ORIGIN);
        helper.assertTrue(
                helper.getBlockState(ORIGIN).is(Blocks.RED_BED)
                        && helper.getBlockState(ORIGIN.east()).is(Blocks.RED_BED),
                "Painting one half recolors the whole bed");
        helper.assertTrue(
                helper.getBlockState(ORIGIN).getValue(BedBlock.PART) == BedPart.FOOT
                        && helper.getBlockState(ORIGIN.east()).getValue(BedBlock.PART)
                                == BedPart.HEAD,
                "The recolored bed keeps its halves");
        helper.succeed();
    }

    static void painterRecolorsShulkerBoxKeepsContents(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.WHITE_SHULKER_BOX);
        ShulkerBoxBlockEntity box = helper.getBlockEntity(ORIGIN, ShulkerBoxBlockEntity.class);
        box.setItem(0, new ItemStack(Items.DIAMOND, 5));
        Player player = painterPlayer(helper, painter(DyeColor.RED, 0));
        paint(helper, player, ORIGIN);
        helper.assertTrue(
                helper.getBlockState(ORIGIN).is(Blocks.RED_SHULKER_BOX),
                "The shulker box paints into the dyed box");
        ShulkerBoxBlockEntity recolored =
                helper.getBlockEntity(ORIGIN, ShulkerBoxBlockEntity.class);
        helper.assertTrue(
                ItemStack.matches(new ItemStack(Items.DIAMOND, 5), recolored.getItem(0)),
                "The recolored shulker box keeps its stored items");
        helper.succeed();
    }

    static void painterWearRevertsToPlain(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.WHITE_WOOL);
        ItemStack stack = painter(DyeColor.RED, 31);
        Player player = painterPlayer(helper, stack);
        paint(helper, player, ORIGIN);
        helper.assertTrue(
                helper.getBlockState(ORIGIN).is(Blocks.RED_WOOL),
                "The last use still paints the block");
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        helper.assertTrue(
                held.is(ModTools.PAINTER.get()) && held.getDamageValue() == 0,
                "A spent painter reverts to the fresh plain painter");
        helper.succeed();
    }

    static void painterAutoRefillConsumesSpare(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.WHITE_WOOL);
        ItemStack stack = painter(DyeColor.RED, 31);
        stack.set(ModDataComponents.PAINTER_AUTO_REFILL, true);
        Player player = painterPlayer(helper, stack);
        player.getInventory().add(painter(DyeColor.RED, 0));
        paint(helper, player, ORIGIN);
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        helper.assertTrue(
                held.is(ModTools.painter(DyeColor.RED).get()) && held.getDamageValue() == 0,
                "Auto refill replaces a spent painter with the fresh spare of the same color");
        int plainCount = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(ModTools.PAINTER.get())) {
                plainCount++;
            }
        }
        helper.assertTrue(
                plainCount == 1, "The plain painter is stored into the inventory after refill");
        helper.succeed();
    }

    static void painterDyesSheep(GameTestHelper helper) {
        Sheep sheep = helper.spawn(EntityType.SHEEP, ORIGIN);
        ItemStack stack = painter(DyeColor.RED, 0);
        Player player = painterPlayer(helper, stack);
        stack.getItem().interactLivingEntity(stack, player, sheep, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                sheep.getColor() == DyeColor.RED, "The painter dyes a sheep like a dye item");
        helper.assertTrue(stack.getDamageValue() == 1, "Sheep dyeing wears the painter by one");
        helper.succeed();
    }

    static void plainPainterPasses(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.WHITE_WOOL);
        Player player = painterPlayer(helper, new ItemStack(ModTools.PAINTER.get()));
        paint(helper, player, ORIGIN);
        helper.assertTrue(
                helper.getBlockState(ORIGIN).is(Blocks.WHITE_WOOL),
                "The plain painter paints nothing");
        helper.succeed();
    }

    static void painterTogglesAutoRefill(GameTestHelper helper) {
        ItemStack stack = painter(DyeColor.RED, 0);
        Player player = painterPlayer(helper, stack);
        player.setShiftKeyDown(true);
        ModTools.painter(DyeColor.RED)
                .get()
                .use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                Boolean.TRUE.equals(stack.get(ModDataComponents.PAINTER_AUTO_REFILL)),
                "Shift use enables automatic refill");
        player.setShiftKeyDown(false);
        player.setShiftKeyDown(true);
        ModTools.painter(DyeColor.RED)
                .get()
                .use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                !Boolean.TRUE.equals(stack.get(ModDataComponents.PAINTER_AUTO_REFILL)),
                "Shift use again disables automatic refill");
        helper.succeed();
    }
}
