package ic2.neoforge.test;

import ic2.neoforge.block.ObscuredWallBlock;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.component.ObscuratorReference;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.machine.ObscuredWallBlockEntity;
import ic2.neoforge.model.WallRenderState;
import ic2.neoforge.network.ObscuratorScanPayload;
import ic2.neoforge.registration.ModFoam;
import ic2.neoforge.registration.ModObscurator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

final class ObscuratorTests {
    // The floor sits at structure y=0; the wall needs the solid floor layer beneath it.
    private static final BlockPos WALL = new BlockPos(2, 1, 2);
    private static final int UP = Direction.UP.ordinal();

    private static ItemStack obscurator(double charge, boolean withReference) {
        ItemStack stack = new ItemStack(ModObscurator.OBSCURATOR.get());
        if (charge > 0) stack.set(ModDataComponents.CHARGE, charge);
        if (withReference)
            stack.set(
                    ModDataComponents.OBSCURATOR_REFERENCE,
                    new ObscuratorReference("minecraft:stone", "normal", UP, new int[] {-1}));
        return stack;
    }

    private static Player usePlayer(GameTestHelper helper, ItemStack stack, boolean sneaking) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setShiftKeyDown(sneaking);
        return player;
    }

    private static InteractionResult useOn(
            GameTestHelper helper, Player player, ItemStack stack, Direction face) {
        BlockPos absolute = helper.absolutePos(WALL);
        var context =
                new UseOnContext(
                        helper.getLevel(),
                        player,
                        InteractionHand.MAIN_HAND,
                        stack,
                        new BlockHitResult(Vec3.atCenterOf(absolute), face, absolute, false));
        return ModObscurator.OBSCURATOR.get().onItemUseFirst(stack, context);
    }

    private static void placeWall(GameTestHelper helper) {
        helper.setBlock(WALL, ModFoam.WALLS.get("light_gray").get().defaultBlockState());
    }

    static void obscuratorRetexturesWall(GameTestHelper helper) {
        placeWall(helper);
        ItemStack stack = obscurator(100000, true);
        Player player = usePlayer(helper, stack, false);
        useOn(helper, player, stack, Direction.UP);
        helper.assertTrue(
                helper.getBlockState(WALL).getBlock() instanceof ObscuredWallBlock,
                "Standing use must replace the foam wall with the obscured wall");
        var wall = helper.getBlockEntity(WALL, ObscuredWallBlockEntity.class);
        helper.assertTrue(
                wall.renderState().color().getName().equals("light_gray"),
                "The obscured wall must keep the wall color");
        var face = wall.renderState().face(Direction.UP.ordinal());
        helper.assertTrue(
                face != null && face.referenceState().is(Blocks.STONE),
                "The sampled reference must be stored on the clicked face");
        helper.assertTrue(
                ElectricItemEnergy.charge(stack) == 95000,
                "Applying the reference must cost five thousand EU");
        helper.succeed();
    }

    static void obscuratorRequiresEnergy(GameTestHelper helper) {
        placeWall(helper);
        ItemStack stack = obscurator(0, true);
        Player player = usePlayer(helper, stack, false);
        useOn(helper, player, stack, Direction.UP);
        helper.assertTrue(
                helper.getBlockState(WALL).is(ModFoam.WALLS.get("light_gray").get()),
                "An empty obscurator must not retexture the wall");
        helper.succeed();
    }

    static void obscuratorRequiresReference(GameTestHelper helper) {
        placeWall(helper);
        ItemStack stack = obscurator(100000, false);
        Player player = usePlayer(helper, stack, false);
        useOn(helper, player, stack, Direction.UP);
        helper.assertTrue(
                helper.getBlockState(WALL).is(ModFoam.WALLS.get("light_gray").get()),
                "A obscurator without a sample must pass");
        helper.assertTrue(
                ElectricItemEnergy.charge(stack) == 100000,
                "Passing must not drain the obscurator");
        helper.succeed();
    }

    static void obscuratorSneakPassesServer(GameTestHelper helper) {
        placeWall(helper);
        ItemStack stack = obscurator(100000, true);
        Player player = usePlayer(helper, stack, true);
        // Mock players never tick, so the crouch pose has to be set alongside the flag.
        player.setPose(net.minecraft.world.entity.Pose.CROUCHING);
        useOn(helper, player, stack, Direction.UP);
        helper.assertTrue(
                helper.getBlockState(WALL).is(ModFoam.WALLS.get("light_gray").get()),
                "Sneaking is the client scan gesture and must not apply server side");
        helper.assertTrue(
                ElectricItemEnergy.charge(stack) == 100000,
                "Sneaking must not drain the obscurator on the server");
        helper.succeed();
    }

    static void obscuratorScanPayload(GameTestHelper helper) {
        ItemStack stack = obscurator(100000, false);
        Player player = usePlayer(helper, stack, false);
        // Direction.UP ordinal plus stone's default state; the sample dedup happens client side.
        var payload =
                new ObscuratorScanPayload(
                        player.getInventory().getSelectedSlot(),
                        "minecraft:stone",
                        "normal",
                        UP,
                        new int[] {-1});
        helper.assertTrue(
                ObscuratorScanPayload.applyScan(payload, player),
                "A charged obscurator must accept the scan upload");
        var reference = stack.get(ModDataComponents.OBSCURATOR_REFERENCE);
        helper.assertTrue(
                reference != null
                        && reference.blockId().equals("minecraft:stone")
                        && reference.side() == UP,
                "The scan upload must store the sampled reference");
        helper.assertTrue(
                ElectricItemEnergy.charge(stack) == 80000, "The scan must cost twenty thousand EU");
        ItemStack other = new ItemStack(Blocks.STONE);
        player.getInventory().setItem(0, other);
        helper.assertTrue(
                !ObscuratorScanPayload.applyScan(payload, player),
                "Non-obscurator items must reject the scan");
        helper.succeed();
    }

    static void obscuredWallPersistence(GameTestHelper helper) {
        placeWall(helper);
        ItemStack stack = obscurator(100000, true);
        Player player = usePlayer(helper, stack, false);
        useOn(helper, player, stack, Direction.UP);
        var wall = helper.getBlockEntity(WALL, ObscuredWallBlockEntity.class);
        WallRenderState before = wall.renderState();
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        var tag = wall.saveWithFullMetadata(registries);
        BlockEntity reloaded =
                net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                        helper.absolutePos(WALL), helper.getBlockState(WALL), tag, registries);
        helper.assertTrue(
                reloaded instanceof ObscuredWallBlockEntity,
                "The obscured wall entity must persist its type");
        helper.assertTrue(
                ((ObscuredWallBlockEntity) reloaded).renderState().equals(before),
                "The render state must survive a save/load round trip");
        helper.succeed();
    }

    static void obscuredWallDropsColorWall(GameTestHelper helper) {
        placeWall(helper);
        ItemStack stack = obscurator(100000, true);
        Player player = usePlayer(helper, stack, false);
        useOn(helper, player, stack, Direction.UP);
        helper.assertTrue(
                helper.getBlockState(WALL).getBlock() instanceof ObscuredWallBlock,
                "The wall must be obscured before the drop check");
        var wall = helper.getBlockEntity(WALL, ObscuredWallBlockEntity.class);
        wall.initialize(
                net.minecraft.world.item.DyeColor.RED,
                Direction.UP,
                Blocks.STONE.defaultBlockState(),
                Direction.UP,
                new int[] {-1});
        ModObscurator.OBSCURED_WALL
                .get()
                .playerDestroy(
                        helper.getLevel(),
                        player,
                        helper.absolutePos(WALL),
                        helper.getBlockState(WALL),
                        wall,
                        net.minecraft.world.item.ItemStack.EMPTY);
        BlockPos center = helper.absolutePos(WALL);
        List<ItemEntity> drops =
                helper.getLevel()
                        .getEntitiesOfClass(
                                ItemEntity.class,
                                new AABB(
                                        center.getX() - 2,
                                        center.getY() - 2,
                                        center.getZ() - 2,
                                        center.getX() + 3,
                                        center.getY() + 3,
                                        center.getZ() + 3));
        helper.assertTrue(
                drops.size() == 1
                        && drops.getFirst().getItem().is(ModFoam.WALLS.get("red").get().asItem()),
                "The obscured wall must drop the foam wall of its stored color");
        helper.succeed();
    }
}
