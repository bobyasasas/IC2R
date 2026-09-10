package ic2.neoforge.test;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.crop.CropBlockEntity;
import ic2.neoforge.item.WeedingTrowelItem;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModCells;
import ic2.neoforge.registration.ModCrops;
import ic2.neoforge.registration.ModItems;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class CropCareTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    /** Sets up farmland + stick and plants wheat; returns the fresh crop-block tile. */
    private static CropBlockEntity plantedWheat(GameTestHelper helper) {
        helper.setBlock(POSITION.below(), Blocks.FARMLAND);
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity stick = helper.getBlockEntity(POSITION, CropBlockEntity.class);
        helper.assertTrue(
                stick.tryPlantIn(ModCrops.WHEAT_CARD, 0, 2, 2, 2, 4),
                "Wheat plants into the crop stick");
        // Planting replaces the stick block, so the tile must be re-fetched from the crop block.
        return helper.getBlockEntity(POSITION, CropBlockEntity.class);
    }

    private static ItemStack fertilizer() {
        return new ItemStack(ModItems.MATERIALS.get(MaterialDefinition.FERTILIZER).get());
    }

    static void fertilizerUse(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        CropBlockEntity crop = plantedWheat(helper);
        var held = fertilizer();
        helper.assertTrue(
                crop.rightClick(player, held),
                "The fertilizer use is consumed by the crop tile");
        helper.assertTrue(
                crop.getStorageNutrients() == 100 && held.isEmpty(),
                "One fertilizer saturates an empty tile to 100 and is spent");
        var second = fertilizer();
        helper.assertTrue(
                crop.rightClick(player, second) && crop.getStorageNutrients() == 100,
                "A saturated tile stores no further nutrients");
        helper.assertTrue(second.isEmpty(), "The surplus fertilizer is consumed anyway");
        helper.succeed();
    }

    static void hydrationUse(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        CropBlockEntity crop = plantedWheat(helper);
        // The cell must sit in the clicked hand so the drain can swap it for an empty cell.
        var waterCell = ModCells.WATER.toStack();
        player.getInventory().setItem(player.getInventory().getSelectedSlot(), waterCell);
        helper.assertTrue(
                crop.rightClick(player, waterCell) && crop.getStorageWater() == 200,
                "A water cell fills the tile to its 200 storage and empties itself");
        helper.assertTrue(
                waterCell.isEmpty()
                        && player.getInventory()
                                .getItem(player.getInventory().getSelectedSlot())
                                .is(ModCells.EMPTY.get()),
                "The drained cell turns back into an empty cell");

        var cell = ModCrops.HYDRATION_CELL.toStack();
        helper.assertTrue(
                !crop.rightClick(player, cell),
                "A saturated tile refuses the hydration cell");
        helper.assertTrue(
                cell.getOrDefault(ModDataComponents.HYDRATION_USES.get(), 0) == 0 && !cell.isEmpty(),
                "The refused use leaves the cell untouched");

        // A dry tile takes 200 charges plus the one-off usage fee into the cell counter.
        var dry = plantedWheat(helper);
        var fresh = ModCrops.HYDRATION_CELL.toStack();
        helper.assertTrue(
                dry.rightClick(player, fresh) && dry.getStorageWater() == 200,
                "The hydration cell irrigates a dry tile to 200");
        helper.assertTrue(
                fresh.getOrDefault(ModDataComponents.HYDRATION_USES.get(), 0) == 201,
                "The cell books the tile space plus one charge as used");
        helper.succeed();
    }

    static void weedExAndTrowel(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        // A bare stick accepts weed-ex too: it is what keeps the random weed roll away.
        helper.setBlock(POSITION.below(), Blocks.FARMLAND);
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity stick = helper.getBlockEntity(POSITION, CropBlockEntity.class);
        var weedExCell = Objects.requireNonNull(ModCells.CELLS.get("weed_ex_cell")).toStack();
        player.getInventory().setItem(player.getInventory().getSelectedSlot(), weedExCell);
        helper.assertTrue(
                stick.rightClick(player, weedExCell) && stick.getStorageWeedEx() == 100,
                "A weed-ex cell caps a hand use at the manual 100 storage");
        helper.assertTrue(
                weedExCell.isEmpty()
                        && player.getInventory()
                                .getItem(player.getInventory().getSelectedSlot())
                                .is(ModCells.EMPTY.get()),
                "The weed-ex cell is drained whole");

        // The trowel clears a weed crop: one weed per age plus one, tile back to a stick.
        BlockPos weedPos = new BlockPos(4, 8, 4);
        helper.setBlock(weedPos.below(), Blocks.FARMLAND);
        helper.setBlock(weedPos, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity weedStick = helper.getBlockEntity(weedPos, CropBlockEntity.class);
        CropBlockEntity weedTile =
                weedStick.transformToWeed((ServerLevel) helper.getLevel());
        helper.assertTrue(weedTile != null, "The weed card takes over the crop stick");
        weedTile.setCurrentAge(3);
        var trowel = ModCrops.WEEDING_TROWEL.toStack();
        BlockHitResult hit =
                new BlockHitResult(
                        Vec3.atCenterOf(helper.absolutePos(weedPos)),
                        Direction.UP,
                        helper.absolutePos(weedPos),
                        false);
        var context = new UseOnContext(
                helper.getLevel(), player, InteractionHand.MAIN_HAND, trowel, hit);
        helper.assertTrue(
                ((WeedingTrowelItem) trowel.getItem()).onItemUseFirst(trowel, context)
                        == InteractionResult.SUCCESS,
                "The trowel clears the weed crop");
        helper.assertTrue(
                helper.getBlockState(weedPos).is(ModCrops.CROP_STICK.get()),
                "The weed tile falls back to a plain crop stick");
        long weeds =
                helper.getEntities(EntityType.ITEM).stream()
                        .filter(entity -> entity.getItem()
                                .is(ModItems.MATERIALS.get(MaterialDefinition.WEED).get()))
                        .mapToLong(entity -> entity.getItem().getCount())
                        .sum();
        helper.assertTrue(
                weeds == 4, "The trowel drops one weed per age plus one, saw " + weeds);
        helper.succeed();
    }

    private CropCareTests() {}
}
