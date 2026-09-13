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
import net.minecraft.world.level.ChunkPos;
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
        // Two sealed shafts, eleven blocks of fall: one passes through a resin
        // curtain, one floors on bare stone.
        buildShaft(helper, 1, 1, true);
        buildShaft(helper, 1, 4, false);
        // The grid dimension only ticks entities whose chunks sit in the
        // entity-ticking ticket range, and the section bookkeeping that feeds
        // the tick list advances through asynchronous chunk promotions that do
        // not reliably happen inside the test window. So the test takes its own
        // FORCED tickets (exactly the entity-ticking level, and the simulation
        // tracker reads them straight from ticket storage) and the fallers
        // override isAlwaysTicking, the same bypass players get, which enters
        // them into the tick list regardless of section state. Gravity then
        // depends on nothing asynchronous but the chunk load itself.
        var shaftA = helper.absolutePos(new BlockPos(1, 13, 1));
        var shaftB = helper.absolutePos(new BlockPos(1, 13, 4));
        int ax = shaftA.getX() >> 4;
        int az = shaftA.getZ() >> 4;
        int bx = shaftB.getX() >> 4;
        int bz = shaftB.getZ() >> 4;
        var level = helper.getLevel();
        level.setChunkForced(ax, az, true);
        level.setChunkForced(bx, bz, true);
        var distanceManager = level.getChunkSource().chunkMap.getDistanceManager();
        Zombie[] cushioned = new Zombie[1];
        Zombie[] bare = new Zombie[1];
        for (int t = 1; t <= 60; t++) {
            int tick = t;
            helper.runAtTickTime(tick, () -> {
                if (cushioned[0] != null) {
                    return;
                }
                // Spawn only once both shaft chunks are fully loaded and their
                // forced tickets put them in the entity-ticking range.
                boolean loaded = level.getChunkSource().hasChunk(ax, az)
                        && level.getChunkSource().hasChunk(bx, bz);
                boolean ticking = distanceManager.inEntityTickingRange(ChunkPos.pack(ax, az))
                        && distanceManager.inEntityTickingRange(ChunkPos.pack(bx, bz));
                if (loaded && ticking) {
                    cushioned[0] = dropZombie(helper, shaftA);
                    bare[0] = dropZombie(helper, shaftB);
                    return;
                }
                if (tick == 60) {
                    helper.fail("Shaft chunks never reached the entity-ticking range (loaded="
                            + loaded + ", ticking=" + ticking + ")");
                }
            });
        }

        // Wait for the physics instead of a fixed deadline: both fallers must
        // cross the floor line, then a short settle for the landing damage.
        double floorY = helper.absolutePos(new BlockPos(1, 2, 4)).getY() + 0.5;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        cushioned[0] != null && bare[0] != null
                                && cushioned[0].getY() < floorY
                                && bare[0].getY() < floorY,
                        "Waiting for both zombies to floor"))
                .thenExecuteAfter(
                        3,
                        () -> {
                            level.setChunkForced(ax, az, false);
                            level.setChunkForced(bx, bz, false);
                            helper.assertTrue(
                                    cushioned[0].isAlive() && bare[0].isAlive(),
                                    "Both zombies must survive the landing itself");
                            helper.assertTrue(
                                    cushioned[0].getHealth()
                                            > bare[0].getHealth() + 1.0F,
                                    "The resin sheet must absorb fall damage (cushioned "
                                            + cushioned[0].getHealth() + " vs bare "
                                            + bare[0].getHealth() + ")");
                            helper.succeed();
                        });
    }

    private static void buildShaft(
            GameTestHelper helper, int x, int z, boolean withSheet) {
        for (int y = 1; y <= 13; y++) {
            setStone(helper, new BlockPos(x - 1, y, z));
            setStone(helper, new BlockPos(x + 1, y, z));
            setStone(helper, new BlockPos(x, y, z - 1));
            setStone(helper, new BlockPos(x, y, z + 1));
        }
        setStone(helper, new BlockPos(x, 1, z));
        if (withSheet) {
            // A three-sheet curtain: the faller stays inside the damping window for
            // several ticks. A single floor-level sheet only damps on the landing tick,
            // which puts the damage on a ceil() knife edge.
            for (int y = 2; y <= 4; y++) {
                helper.setBlock(
                        new BlockPos(x, y, z),
                        ModMaterialBlocks.RESIN_SHEET.get().defaultBlockState());
            }
        }
    }

    private static Zombie dropZombie(GameTestHelper helper, BlockPos shaftTop) {
        // Eleven blocks of fall: the landing speed stays well below the one step in
        // which a faller could cross the whole curtain window, so the resin always
        // gets at least one pre-landing damping tick, while the bare control still
        // takes heavy damage. Even a single damping tick keeps the margin at 2.0:
        // bare ceil(11-3)=8 damage (12.0) versus 0.75*(11-2.125)+2.125 -> 6 (14.0).
        Zombie faller = new Zombie(EntityType.ZOMBIE, helper.getLevel()) {
            @Override
            public boolean isAlwaysTicking() {
                return true;
            }
        };
        faller.setPos(shaftTop.getX() + 0.5, shaftTop.getY(), shaftTop.getZ() + 0.5);
        helper.getLevel().addFreshEntity(faller);
        return faller;
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
