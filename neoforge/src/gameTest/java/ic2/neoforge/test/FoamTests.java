package ic2.neoforge.test;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.fluid.FoamTankHandler;
import ic2.neoforge.item.CFPackItem;
import ic2.neoforge.item.FoamSprayerItem;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModFoam;
import ic2.neoforge.registration.ModMaterialBlocks;
import ic2.neoforge.world.FoamBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

final class FoamTests {
    private static final BlockPos ORIGIN = new BlockPos(17, 17, 17);

    /** Carves an air pocket so the flood fill has replaceable space to spread through. */
    private static void carvePocket(GameTestHelper helper, int from, int to) {
        for (int x = from; x <= to; x++) {
            for (int y = from; y <= to; y++) {
                for (int z = from; z <= to; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    private static ItemStack loadedSprayer(int mB) {
        ItemStack stack = new ItemStack(ModFoam.FOAM_SPRAYER.get());
        if (mB > 0) {
            stack.set(
                    ModDataComponents.FLUID,
                    new FluidStackTemplate(FoamSprayerItem.foamFluid(), mB));
        }
        return stack;
    }

    private static Player sprayPlayer(GameTestHelper helper, ItemStack stack) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static void spray(
            GameTestHelper helper, Player player, BlockPos clicked, Direction face) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        // The context needs real world coordinates; helper positions are structure-relative.
        BlockPos absolute = helper.absolutePos(clicked);
        var context =
                new UseOnContext(
                        helper.getLevel(),
                        player,
                        InteractionHand.MAIN_HAND,
                        stack,
                        new BlockHitResult(Vec3.atCenterOf(absolute), face, absolute, false));
        ModFoam.FOAM_SPRAYER.get().useOn(context);
    }

    private static int countFoam(GameTestHelper helper, int from, int to) {
        int count = 0;
        for (int x = from; x <= to; x++) {
            for (int y = from; y <= to; y++) {
                for (int z = from; z <= to; z++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).is(ModFoam.FOAM.get())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /** The heat room floor leaves open air above, so a capped flood fill can wander two steps. */
    private static int countFoamAroundOrigin(GameTestHelper helper) {
        int count = 0;
        for (int x = 15; x <= 19; x++) {
            for (int y = 16; y <= 20; y++) {
                for (int z = 15; z <= 19; z++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).is(ModFoam.FOAM.get())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    static void sprayerPlacesUpToTenFoamBlocks(GameTestHelper helper) {
        carvePocket(helper, 16, 18);
        ItemStack sprayer = loadedSprayer(5000);
        Player player = sprayPlayer(helper, sprayer);
        spray(helper, player, ORIGIN, Direction.UP);
        helper.assertTrue(
                helper.getBlockState(ORIGIN.above()).is(ModFoam.FOAM.get()),
                "The spray starts at the face clicked by the player");
        helper.assertTrue(
                countFoamAroundOrigin(helper) == 10, "Normal mode spreads exactly ten foam blocks");
        helper.assertTrue(
                FoamSprayerItem.getContentsMb(sprayer) == 4000,
                "Each foam block drains one hundred mB");
        helper.succeed();
    }

    static void singleModeSpraysOneBlock(GameTestHelper helper) {
        carvePocket(helper, 16, 18);
        ItemStack sprayer = loadedSprayer(2000);
        Player player = sprayPlayer(helper, sprayer);
        player.setShiftKeyDown(true);
        ModFoam.FOAM_SPRAYER.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        // The mode toggle consumed the sneak; spraying requires the shift to be released again.
        player.setShiftKeyDown(false);
        spray(helper, player, ORIGIN, Direction.UP);
        helper.assertTrue(
                countFoamAroundOrigin(helper) == 1, "Single mode places only one foam block");
        helper.assertTrue(
                FoamSprayerItem.getContentsMb(sprayer) == 1900,
                "Single mode still consumes one hundred mB");
        helper.succeed();
    }

    static void scaffoldingIsCoveredInFoam(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.SCAFFOLDING);
        ItemStack sprayer = loadedSprayer(1000);
        Player player = sprayPlayer(helper, sprayer);
        spray(helper, player, ORIGIN, Direction.UP);
        helper.assertTrue(
                helper.getBlockState(ORIGIN).is(ModFoam.FOAM.get()),
                "Spraying scaffolding covers it with foam in place");
        helper.assertTrue(
                FoamSprayerItem.getContentsMb(sprayer) == 900,
                "Covered scaffolding costs one hundred mB");
        helper.succeed();
    }

    static void sandInstantlyHardensFoam(GameTestHelper helper) {
        helper.setBlock(ORIGIN, ModFoam.FOAM.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack sand = new ItemStack(Items.SAND, 64);
        player.setItemInHand(InteractionHand.MAIN_HAND, sand);
        helper.getBlockState(ORIGIN)
                .useWithoutItem(
                        helper.getLevel(),
                        player,
                        new BlockHitResult(
                                Vec3.atCenterOf(helper.absolutePos(ORIGIN)),
                                Direction.UP,
                                helper.absolutePos(ORIGIN),
                                false));
        helper.assertTrue(
                helper.getBlockState(ORIGIN).is(ModFoam.WALLS.get("light_gray").get()),
                "Sand hardens normal foam into the light gray foam wall");
        helper.assertTrue(
                sand.getCount() == 63, "Hardening consumes one sand outside creative mode");

        helper.setBlock(
                ORIGIN.east(),
                ModFoam.FOAM
                        .get()
                        .defaultBlockState()
                        .setValue(FoamBlock.TYPE, FoamBlock.FoamType.REINFORCED));
        helper.getBlockState(ORIGIN.east())
                .useWithoutItem(
                        helper.getLevel(),
                        player,
                        new BlockHitResult(
                                Vec3.atCenterOf(helper.absolutePos(ORIGIN.east())),
                                Direction.UP,
                                helper.absolutePos(ORIGIN.east()),
                                false));
        helper.assertTrue(
                helper.getBlockState(ORIGIN.east())
                        .is(ModMaterialBlocks.MATERIALS.get("reinforced_stone").get()),
                "Sand hardens reinforced foam into reinforced stone");
        helper.succeed();
    }

    static void foamPackSuppliesSprayer(GameTestHelper helper) {
        carvePocket(helper, 16, 18);
        ItemStack sprayer = loadedSprayer(0);
        ItemStack pack = new ItemStack(ModFoam.CF_PACK.get());
        CFPackItem.fillMb(pack);
        Player player = sprayPlayer(helper, sprayer);
        player.setItemSlot(EquipmentSlot.CHEST, pack);
        spray(helper, player, ORIGIN, Direction.UP);
        helper.assertTrue(countFoamAroundOrigin(helper) == 10, "The pack feeds a full spray");
        helper.assertTrue(
                CFPackItem.getContentsMb(player.getItemBySlot(EquipmentSlot.CHEST)) == 79000,
                "An empty sprayer drains the worn foam pack first");
        helper.assertTrue(
                FoamSprayerItem.getContentsMb(sprayer) == 0,
                "The sprayer stays empty while the pack has foam");
        helper.succeed();
    }

    static void foamFluidSlowsEntities(GameTestHelper helper) {
        helper.setBlock(
                ORIGIN,
                ModFluids.FAMILIES
                        .get(FluidDefinition.CONSTRUCTION_FOAM)
                        .block()
                        .get()
                        .defaultBlockState());
        var pig = helper.spawn(EntityType.PIG, ORIGIN);
        helper.succeedWhen(
                () ->
                        helper.assertTrue(
                                pig.hasEffect(MobEffects.SLOWNESS),
                                "Wading through construction foam slows entities"));
    }

    static void sprayerAcceptsOnlyConstructionFoam(GameTestHelper helper) {
        var handler =
                new FoamTankHandler(
                        ItemAccess.forStack(loadedSprayer(0)), FoamSprayerItem.CAPACITY_MB);
        helper.assertTrue(
                !handler.isValid(0, FluidResource.of(Fluids.WATER)),
                "The sprayer rejects plain water");
        helper.assertTrue(
                handler.isValid(0, FluidResource.of(FoamSprayerItem.foamFluid())),
                "The sprayer accepts construction foam");
        helper.succeed();
    }
}
