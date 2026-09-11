package ic2.neoforge.test;

import com.mojang.authlib.GameProfile;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.ChainsawItem;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Legacy ItemElectricToolChainsaw behaviour: axe logger plus shear and attack energy costs. */
final class ChainsawItemTests {
    static void constantsSpeedAndDrops(GameTestHelper helper) {
        var level = helper.getLevel();
        var chainsaw = ModTools.CHAINSAW.get();
        helper.assertTrue(
                chainsaw.specification().equals(new ElectricItemSpec(30000, 100, 1, false)),
                "Chainsaw must keep legacy capacity 30000, transfer 100, tier 1");
        var charged = ModTools.CHAINSAW.toStack();
        ElectricItemEnergy.charge(charged, 10000, 1, true, false);
        var empty = ModTools.CHAINSAW.toStack();

        var log = Blocks.OAK_LOG.defaultBlockState();
        var wool = Blocks.WHITE_WOOL.defaultBlockState();
        var cobweb = Blocks.COBWEB.defaultBlockState();
        var stone = Blocks.STONE.defaultBlockState();
        var obsidian = Blocks.OBSIDIAN.defaultBlockState();
        helper.assertTrue(
                chainsaw.getDestroySpeed(charged, log) == ChainsawItem.MINING_SPEED,
                "Charged chainsaw must dig axe blocks at legacy speed 12");
        helper.assertTrue(
                chainsaw.getDestroySpeed(empty, log) == 1.0F,
                "Uncharged chainsaw must collapse to speed 1");
        helper.assertTrue(
                chainsaw.getDestroySpeed(charged, wool) == ChainsawItem.MINING_SPEED
                        && chainsaw.getDestroySpeed(charged, cobweb) == ChainsawItem.MINING_SPEED,
                "Shearable wool and cobweb must dig at legacy speed 12");
        var shearOff = charged.copy();
        shearOff.set(ModDataComponents.CHAINSAW_DISABLE_SHEAR, true);
        helper.assertTrue(
                chainsaw.getDestroySpeed(shearOff, wool) == ChainsawItem.MINING_SPEED,
                "Dig speed must ignore the shear mode like legacy");
        helper.assertTrue(
                chainsaw.getDestroySpeed(charged, stone) == 1.0F
                        && chainsaw.getDestroySpeed(charged, obsidian) == 1.0F,
                "Non-effective blocks must stay at speed 1");

        helper.assertTrue(
                chainsaw.isCorrectToolForDrops(empty, log) && chainsaw.isCorrectToolForDrops(empty, wool)
                        && chainsaw.isCorrectToolForDrops(empty, cobweb),
                "Chainsaw harvest check has no energy gate like legacy");
        helper.assertTrue(
                !chainsaw.isCorrectToolForDrops(charged, stone)
                        && !chainsaw.isCorrectToolForDrops(charged, obsidian),
                "Stone and diamond-tier blocks must not drop as chainsaw drops");

        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, log);
        chainsaw.mineBlock(charged, level, log, pos, null);
        helper.assertTrue(
                ElectricItemEnergy.charge(charged) == 10000,
                "Ordinary mining must stay free like legacy");

        List<Double> amounts = new ArrayList<>();
        ModTools.CHAINSAW.toStack()
                .get(DataComponents.ATTRIBUTE_MODIFIERS)
                .forEach(
                        EquipmentSlotGroup.MAINHAND,
                        (attribute, modifier) -> {
                            if (attribute.value() == Attributes.ATTACK_DAMAGE.value()
                                    || attribute.value() == Attributes.ATTACK_SPEED.value()) {
                                amounts.add(modifier.amount());
                            }
                        });
        helper.assertTrue(
                amounts.size() == 2 && amounts.contains(11.0) && amounts.contains(-3.0),
                "Chainsaw must carry the legacy DiggerItem attack attributes");
        helper.succeed();
    }

    static void shearBreak(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "ic2-chainsaw-test"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var stack = ModTools.CHAINSAW.toStack();
        ElectricItemEnergy.charge(stack, 1000, 1, true, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, Blocks.SHORT_GRASS.defaultBlockState());
        helper.assertTrue(
                !player.gameMode.destroyBlock(pos),
                "Shear break must cancel the vanilla removal like legacy onBlockStartBreak");
        helper.assertTrue(level.getBlockState(pos).isAir(), "Shear break must clear the block");
        helper.assertTrue(
                ElectricItemEnergy.charge(stack) == 900, "Shear break must cost exactly 100 EU");
        helper.assertTrue(
                countItems(helper, Blocks.SHORT_GRASS.asItem()) == 1,
                "Sheared grass must drop itself instead of loot-table drops");

        var plainPos = helper.absolutePos(new BlockPos(4, 2, 2));
        level.setBlockAndUpdate(plainPos, Blocks.SHORT_GRASS.defaultBlockState());
        stack.set(ModDataComponents.CHAINSAW_DISABLE_SHEAR, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.assertTrue(
                player.gameMode.destroyBlock(plainPos), "Disabled shear must still break the block");
        helper.assertTrue(
                countItems(helper, Blocks.SHORT_GRASS.asItem()) == 1,
                "Vanilla break must not drop the grass itself");
        helper.assertTrue(
                ElectricItemEnergy.charge(stack) == 900, "Vanilla break must stay free like legacy");
        helper.succeed();
    }

    static void modeToggle(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "ic2-chainsaw-mode"));
        player.setShiftKeyDown(true);
        var stack = ModTools.CHAINSAW.toStack();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var item = ModTools.CHAINSAW.get();

        var result = item.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                result == InteractionResult.SUCCESS
                        && Boolean.TRUE.equals(stack.get(ModDataComponents.CHAINSAW_DISABLE_SHEAR)),
                "Sneak use must disable shear break (result=" + result
                        + ", flag=" + stack.get(ModDataComponents.CHAINSAW_DISABLE_SHEAR)
                        + ", secondaryUse=" + player.isSecondaryUseActive() + ")");
        item.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                !Boolean.TRUE.equals(stack.get(ModDataComponents.CHAINSAW_DISABLE_SHEAR)),
                "Second sneak use must re-enable shear break");
        helper.assertTrue(
                item.isShearMode(stack) && item.isShearMode(ModTools.CHAINSAW.toStack()),
                "A fresh chainsaw must default to shear mode like legacy");
        helper.succeed();
    }

    static void entityShear(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "ic2-chainsaw-shear"));
        var stack = ModTools.CHAINSAW.toStack();
        ElectricItemEnergy.charge(stack, 1000, 1, true, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        // The wool entities themselves come from the vanilla shearing loot pipeline, which a
        // vanilla-shears control showed is inert in this game test world (mobInteract never
        // shears); drop behaviour stays part of manual gameplay verification.
        Sheep sheep = helper.spawn(EntityType.SHEEP, new BlockPos(2, 2, 2));
        helper.assertTrue(sheep.readyForShearing(), "Fresh sheep must carry shearable wool");
        var result =
                ModTools.CHAINSAW.get()
                        .interactLivingEntity(stack, player, sheep, InteractionHand.MAIN_HAND);
        helper.assertTrue(result == InteractionResult.SUCCESS, "Chainsaw must shear the sheep");
        helper.assertTrue(sheep.isSheared(), "Sheared sheep must lose its wool");
        helper.assertTrue(
                ElectricItemEnergy.charge(stack) == 900, "Shearing must cost exactly 100 EU");

        Sheep clothed = helper.spawn(EntityType.SHEEP, new BlockPos(2, 2, 2));
        stack.set(ModDataComponents.CHAINSAW_DISABLE_SHEAR, true);
        var disabled =
                ModTools.CHAINSAW.get()
                        .interactLivingEntity(stack, player, clothed, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                disabled == InteractionResult.PASS
                        && !clothed.isSheared()
                        && ElectricItemEnergy.charge(stack) == 900,
                "Disabled shear must leave the sheep and its EU alone");

        helper.succeed();
    }

    private static long countItems(GameTestHelper helper, ItemLike item) {
        return helper.getEntities(EntityType.ITEM)
                .stream()
                .filter(entity -> entity.getItem().is(item.asItem()))
                .count();
    }

    private ChainsawItemTests() {}
}
