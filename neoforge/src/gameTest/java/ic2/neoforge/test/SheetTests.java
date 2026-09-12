package ic2.neoforge.test;

import ic2.neoforge.block.SheetBlock;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMaterialBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Legacy Ic2SheetBlock semantics: placement rules, support breaks, cushioning, trampoline, wool. */
final class SheetTests {
    private static final BlockPos SHEET = new BlockPos(2, 2, 2);

    private SheetTests() {}

    private static BlockPlaceContext placeContext(
            GameTestHelper helper, Player player, BlockPos pos) {
        return new BlockPlaceContext(
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                new ItemStack(ModMaterialBlocks.RESIN_SHEET.get()),
                new BlockHitResult(
                        Vec3.atCenterOf(helper.absolutePos(pos)), Direction.UP,
                        helper.absolutePos(pos), false));
    }

    private static void setStone(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.STONE.defaultBlockState());
    }

    static void placementRules(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SheetBlock resin = ModMaterialBlocks.RESIN_SHEET.get();
        SheetBlock rubber = ModMaterialBlocks.RUBBER_SHEET.get();
        SheetBlock wool = ModMaterialBlocks.WOOL_SHEET.get();

        // Resin needs a full block below; floating in the air it must refuse to place.
        helper.assertTrue(
                resin.getStateForPlacement(placeContext(helper, player, SHEET)) == null,
                "Resin must refuse to place without a full block below");
        setStone(helper, SHEET.below());
        helper.assertTrue(
                resin.getStateForPlacement(placeContext(helper, player, SHEET)) != null,
                "Resin must place on a full block");

        // Rubber with air below needs a full block or rubber neighbor beside it.
        helper.assertTrue(
                rubber.getStateForPlacement(placeContext(helper, player, new BlockPos(4, 2, 4)))
                        == null,
                "Floating rubber without a neighbor must refuse to place");
        setStone(helper, new BlockPos(5, 2, 4));
        helper.assertTrue(
                rubber.getStateForPlacement(placeContext(helper, player, new BlockPos(4, 2, 4)))
                        != null,
                "Floating rubber beside a full block must place");
        setStone(helper, new BlockPos(4, 1, 6));
        helper.assertTrue(
                rubber.getStateForPlacement(placeContext(helper, player, new BlockPos(4, 2, 6)))
                        != null,
                "Rubber on a full block must place");

        // Wool is valid anywhere.
        helper.assertTrue(
                wool.getStateForPlacement(placeContext(helper, player, new BlockPos(6, 2, 6)))
                        != null,
                "Wool must place even with no support at all");
        helper.succeed();
    }

    static void supportBreakAndWoolPersists(GameTestHelper helper) {
        setStone(helper, new BlockPos(2, 1, 1));
        setStone(helper, new BlockPos(3, 1, 1));
        helper.setBlock(
                new BlockPos(2, 2, 1),
                ModMaterialBlocks.RESIN_SHEET.get().defaultBlockState());
        helper.setBlock(
                new BlockPos(3, 2, 1),
                ModMaterialBlocks.WOOL_SHEET.get().defaultBlockState());

        // Floating rubber held only by one horizontal stone neighbour.
        setStone(helper, new BlockPos(1, 2, 3));
        helper.setBlock(
                new BlockPos(2, 2, 3),
                ModMaterialBlocks.RUBBER_SHEET.get().defaultBlockState());

        helper.getLevel().destroyBlock(helper.absolutePos(new BlockPos(2, 1, 1)), false);
        helper.assertTrue(
                helper.getBlockState(new BlockPos(2, 2, 1)).isAir(),
                "Resin must break when its full block below is removed");
        helper.assertTrue(
                helper.getBlockState(new BlockPos(3, 2, 1))
                        .getBlock() == ModMaterialBlocks.WOOL_SHEET.get(),
                "Wool must never break from support loss");

        helper.getLevel().destroyBlock(helper.absolutePos(new BlockPos(1, 2, 3)), false);
        helper.assertTrue(
                helper.getBlockState(new BlockPos(2, 2, 3)).isAir(),
                "Floating rubber must break when its last horizontal anchor is removed");
        helper.succeed();
    }

    static void resinSheetCushionsFalls(GameTestHelper helper) {
        helper.setTime(18000);
        // Two sealed shafts, ten blocks of fall: one floors through resin, one bare stone.
        buildShaft(helper, 1, 1, true);
        buildShaft(helper, 1, 4, false);
        Zombie cushioned = dropZombie(helper, 1, 1);
        Zombie bare = dropZombie(helper, 1, 4);

        helper.runAfterDelay(
                60,
                () -> {
                    helper.assertTrue(cushioned.isAlive() && bare.isAlive(),
                            "Both zombies must survive the landing itself");
                    helper.assertTrue(
                            cushioned.getHealth() > bare.getHealth() + 1.0F,
                            "The resin sheet must absorb fall damage (cushioned "
                                    + cushioned.getHealth() + " vs bare " + bare.getHealth() + ")");
                    helper.succeed();
                });
    }

    private static void buildShaft(
            GameTestHelper helper, int x, int z, boolean withSheet) {
        for (int y = 1; y <= 12; y++) {
            setStone(helper, new BlockPos(x - 1, y, z));
            setStone(helper, new BlockPos(x + 1, y, z));
            setStone(helper, new BlockPos(x, y, z - 1));
            setStone(helper, new BlockPos(x, y, z + 1));
        }
        setStone(helper, new BlockPos(x, 1, z));
        if (withSheet) {
            helper.setBlock(
                    new BlockPos(x, 2, z),
                    ModMaterialBlocks.RESIN_SHEET.get().defaultBlockState());
        }
    }

    private static Zombie dropZombie(GameTestHelper helper, int x, int z) {
        return helper.spawn(EntityType.ZOMBIE, new BlockPos(x, 12, z));
    }

    static void rubberSheetBouncesItems(GameTestHelper helper) {
        // Floating sheet held by one eastern stone neighbour, air below: trampoline active.
        setStone(helper, new BlockPos(3, 2, 2));
        helper.setBlock(
                SHEET, ModMaterialBlocks.RUBBER_SHEET.get().defaultBlockState());
        double top = helper.absolutePos(SHEET).getY() + 0.125;
        ItemEntity item = new ItemEntity(
                helper.getLevel(),
                helper.absolutePos(SHEET).getX() + 0.5,
                top + 4.0,
                helper.absolutePos(SHEET).getZ() + 0.5,
                new ItemStack(ModItems.MATERIALS.get(MaterialDefinition.RUBBER).get()));
        helper.getLevel().addFreshEntity(item);

        // A falling item crosses the sheet with a steep negative speed, so the bounce must
        // reverse it upward; the item is not living, so the weight check never fires.
        helper.startSequence()
                .thenWaitUntil(
                        () -> helper.assertTrue(
                                item.getDeltaMovement().y() > 0.1,
                                "The rubber sheet must reverse a steep fall upward"))
                .thenExecute(
                        () -> helper.assertTrue(
                                helper.getBlockState(SHEET)
                                        .getBlock() == ModMaterialBlocks.RUBBER_SHEET.get(),
                                "A non-living bounce must never break the sheet"))
                .thenSucceed();
    }

    static void rubberSheetBreaksUnderLivingWeight(GameTestHelper helper) {
        // Single-side anchor: canSupportWeight fails, so any living occupant snaps the sheet.
        setStone(helper, new BlockPos(3, 2, 2));
        helper.setBlock(
                SHEET, ModMaterialBlocks.RUBBER_SHEET.get().defaultBlockState());
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, SHEET);
        zombie.setNoAi(true);
        zombie.setPos(
                helper.absolutePos(SHEET).getX() + 0.5,
                helper.absolutePos(SHEET).getY() + 0.05,
                helper.absolutePos(SHEET).getZ() + 0.5);

        helper.startSequence()
                .thenWaitUntil(
                        () -> helper.assertTrue(
                                helper.getBlockState(SHEET).isAir(),
                                "A living entity on an unsupported floating sheet must break it"))
                .thenSucceed();
    }

    static void woolSheetCollisionSemantics(GameTestHelper helper) {
        helper.setBlock(
                SHEET, ModMaterialBlocks.WOOL_SHEET.get().defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        double feet = helper.absolutePos(SHEET).getY() + 1.0;
        player.setPos(helper.absolutePos(SHEET).getX() + 0.5, feet,
                helper.absolutePos(SHEET).getZ() + 0.5);

        var wool = helper.getBlockState(SHEET);
        helper.assertTrue(
                !wool.getCollisionShape(
                                helper.getLevel(), helper.absolutePos(SHEET),
                                CollisionContext.of(player))
                        .isEmpty(),
                "A standing player must be held by the wool sheet");

        player.setShiftKeyDown(true);
        helper.assertTrue(
                wool.getCollisionShape(
                                helper.getLevel(), helper.absolutePos(SHEET),
                                CollisionContext.of(player))
                        .isEmpty(),
                "A sneaking player must fall through the wool sheet");
        player.setShiftKeyDown(false);

        // Below sheet top minus step height (e.g. jumping up from underneath) even a
        // non-sneaking player falls through.
        player.setPos(helper.absolutePos(SHEET).getX() + 0.5, feet - 1.5,
                helper.absolutePos(SHEET).getZ() + 0.5);
        helper.assertTrue(
                wool.getCollisionShape(
                                helper.getLevel(), helper.absolutePos(SHEET),
                                CollisionContext.of(player))
                        .isEmpty(),
                "A player below the sheet top minus step height must fall through");

        ItemEntity item = new ItemEntity(
                helper.getLevel(), 0.0, 0.0, 0.0,
                new ItemStack(ModItems.MATERIALS.get(MaterialDefinition.RUBBER).get()));
        item.setNoGravity(true);
        helper.assertTrue(
                !wool.getCollisionShape(
                                helper.getLevel(), helper.absolutePos(SHEET),
                                CollisionContext.of(item))
                        .isEmpty(),
                "Non-player entities must be held by the wool sheet");

        helper.setBlock(
                new BlockPos(2, 2, 4),
                ModMaterialBlocks.RESIN_SHEET.get().defaultBlockState());
        var resin = helper.getBlockState(new BlockPos(2, 2, 4));
        helper.assertTrue(
                resin.getCollisionShape(
                                helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 4)),
                                CollisionContext.of(player))
                        .isEmpty(),
                "The resin sheet must never collide");
        helper.assertTrue(
                !wool.getShape(helper.getLevel(), helper.absolutePos(SHEET),
                        CollisionContext.empty()).isEmpty(),
                "All sheets must keep the thin outline shape");
        helper.succeed();
    }
}
