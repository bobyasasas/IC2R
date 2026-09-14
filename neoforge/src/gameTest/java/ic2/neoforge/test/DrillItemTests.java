package ic2.neoforge.test;

import ic2.neoforge.item.DrillItem;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.IridiumDrillItem;
import ic2.neoforge.registration.ModSounds;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

final class DrillItemTests {
    private static final BlockPos POSITION = new BlockPos(2, 2, 2);
    private static final Identifier DAMAGE_ID =
            Identifier.fromNamespaceAndPath("ic2", "drill_attack_damage");
    private static final Identifier SPEED_ID =
            Identifier.fromNamespaceAndPath("ic2", "drill_attack_speed");

    static void speedAndDrops(GameTestHelper helper) {
        helper.setBlock(POSITION, Blocks.STONE.defaultBlockState());
        var stone = helper.getBlockState(POSITION);
        var drill = ModTools.DRILL.toStack();
        var item = ModTools.DRILL.get();
        helper.assertTrue(
                item.getDestroySpeed(drill, stone) == 1.0F,
                "An uncharged drill keeps the hand-mining speed on stone");
        ElectricItemEnergy.charge(drill, 1000, 1, true, false);
        helper.assertTrue(
                item.getDestroySpeed(drill, stone) == 8.0F,
                "A charged drill mines stone at the legacy 8.0 speed");
        var diamondDrill = ModTools.DIAMOND_DRILL.toStack();
        ElectricItemEnergy.charge(diamondDrill, 1000, 1, true, false);
        helper.assertTrue(
                ModTools.DIAMOND_DRILL.get().getDestroySpeed(diamondDrill, stone) == 16.0F,
                "The diamond drill mines stone at the legacy 16.0 speed");
        helper.setBlock(POSITION, Blocks.DIRT.defaultBlockState());
        helper.assertTrue(
                item.getDestroySpeed(drill, helper.getBlockState(POSITION)) == 8.0F,
                "The drill covers shovel blocks like the legacy effective list");
        helper.assertTrue(
                item.isCorrectToolForDrops(drill, stone),
                "Drops stay correct while uncharged; only the speed collapses");
        helper.setBlock(POSITION, Blocks.OBSIDIAN.defaultBlockState());
        var obsidian = helper.getBlockState(POSITION);
        helper.assertTrue(
                !item.isCorrectToolForDrops(drill, obsidian),
                "The iron-tier drill cannot harvest obsidian");
        helper.assertTrue(
                ModTools.DIAMOND_DRILL.get().isCorrectToolForDrops(diamondDrill, obsidian),
                "The diamond-tier drill harvests obsidian");
        helper.setBlock(POSITION, Blocks.OAK_LOG.defaultBlockState());
        helper.assertTrue(
                !item.isCorrectToolForDrops(drill, helper.getBlockState(POSITION)),
                "Blocks outside the pickaxe and shovel lists stay ineffective");
        helper.succeed();
    }

    static void dischargePerBlock(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.setBlock(POSITION, Blocks.STONE.defaultBlockState());
        var state = helper.getBlockState(POSITION);
        var pos = helper.absolutePos(POSITION);
        var drill = ModTools.DRILL.toStack();
        ElectricItemEnergy.charge(drill, 1000, 1, true, false);
        ModTools.DRILL.get().mineBlock(drill, level, state, pos, null);
        helper.assertTrue(
                ElectricItemEnergy.charge(drill) == 950,
                "One mined block costs the legacy 50 EU operation charge");
        var empty = ModTools.DRILL.toStack();
        ElectricItemEnergy.charge(empty, 10, 1, true, false);
        ModTools.DRILL.get().mineBlock(empty, level, state, pos, null);
        helper.assertTrue(
                ElectricItemEnergy.charge(empty) == 10,
                "An insufficient charge is not partially drained");
        var iridium = ModTools.IRIDIUM_DRILL.toStack();
        ElectricItemEnergy.charge(iridium, 1000, 3, true, false);
        ModTools.IRIDIUM_DRILL.get().mineBlock(iridium, level, state, pos, null);
        helper.assertTrue(
                ElectricItemEnergy.charge(iridium) == 200,
                "The iridium drill pays the legacy 800 EU operation charge");
        helper.setBlock(POSITION, Blocks.DANDELION.defaultBlockState());
        ModTools.DRILL.get().mineBlock(drill, level, helper.getBlockState(POSITION), pos, null);
        helper.assertTrue(
                ElectricItemEnergy.charge(drill) == 950, "Zero-hardness blocks consume no energy");
        helper.succeed();
    }

    static void minerConstants(GameTestHelper helper) {
        var drill = ModTools.DRILL.get().specification();
        helper.assertTrue(
                drill.capacity() == 30000 && drill.transferLimit() == 100 && drill.tier() == 1,
                "The drill keeps the legacy 30000 EU tier-1 buffer");
        helper.assertTrue(
                ModTools.DRILL.get().minerEnergyPerTick() == 6
                        && ModTools.DRILL.get().minerDuration() == 200
                        && ModTools.DRILL.get().harvestEnergyCost() == 50
                        && ModTools.DRILL.get().fortuneLevel() == 0,
                "The miner paces basic drill blocks over 200 ticks at 6 EU per tick");
        var diamond = ModTools.DIAMOND_DRILL.get();
        helper.assertTrue(
                diamond.minerEnergyPerTick() == 20
                        && diamond.minerDuration() == 50
                        && diamond.harvestEnergyCost() == 80
                        && diamond.fortuneLevel() == 0,
                "The diamond drill mines miner blocks over 50 ticks at 20 EU per tick");
        var iridium = ModTools.IRIDIUM_DRILL.get().specification();
        helper.assertTrue(
                iridium.capacity() == 300000
                        && iridium.transferLimit() == 1000
                        && iridium.tier() == 3,
                "The iridium drill keeps the legacy 300000 EU tier-3 buffer");
        helper.assertTrue(
                ModTools.IRIDIUM_DRILL.get().minerEnergyPerTick() == 200
                        && ModTools.IRIDIUM_DRILL.get().minerDuration() == 20
                        && ModTools.IRIDIUM_DRILL.get().harvestEnergyCost() == 800
                        && ModTools.IRIDIUM_DRILL.get().fortuneLevel() == 3,
                "The iridium drill mines miner blocks over 20 ticks with fortune III");
        helper.succeed();
    }

    static void attackAttributes(GameTestHelper helper) {
        assertDrillAttributes(helper, ModTools.DRILL.toStack(), 2.0F, "The iron drill");
        assertDrillAttributes(helper, ModTools.DIAMOND_DRILL.toStack(), 5.0F, "The diamond drill");
        assertDrillAttributes(helper, ModTools.IRIDIUM_DRILL.toStack(), 7.0F, "The iridium drill");
        helper.succeed();
    }

    private static void assertDrillAttributes(
            GameTestHelper helper, ItemStack stack, float damage, String name) {
        var modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        helper.assertTrue(modifiers != null, name + " carries attribute modifiers");
        if (modifiers == null) {
            return;
        }
        double foundDamage = Double.NaN;
        double foundSpeed = Double.NaN;
        for (var entry : modifiers.modifiers()) {
            Identifier id = entry.modifier().id();
            if (id.equals(DAMAGE_ID)) {
                foundDamage = entry.modifier().amount();
            } else if (id.equals(SPEED_ID)) {
                foundSpeed = entry.modifier().amount();
            }
        }
        helper.assertTrue(
                foundDamage == damage,
                name + " hits at its legacy tool material bonus (" + damage + ")");
        helper.assertTrue(
                foundSpeed == -3.0, name + " swings at the legacy -3.0 drill speed");
    }

    static void breakSounds(GameTestHelper helper) {
        helper.assertTrue(
                DrillItem.breakSoundFor(Blocks.STONE.defaultBlockState())
                        == ModSounds.ITEM_DRILL_SOFT,
                "Stone below destroy time 3.0 takes the soft drill sound");
        helper.assertTrue(
                DrillItem.breakSoundFor(Blocks.DIRT.defaultBlockState())
                        == ModSounds.ITEM_DRILL_SOFT,
                "Dirt takes the soft drill sound");
        helper.assertTrue(
                DrillItem.breakSoundFor(Blocks.IRON_BLOCK.defaultBlockState())
                        == ModSounds.ITEM_DRILL_HARD,
                "Hard ore at destroy time 5.0 takes the hard drill sound");
        helper.assertTrue(
                DrillItem.breakSoundFor(Blocks.OBSIDIAN.defaultBlockState())
                        == ModSounds.ITEM_DRILL_HARD,
                "Obsidian takes the hard drill sound");
        helper.succeed();
    }

    static void penaltyCompensation(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var stone = Blocks.STONE.defaultBlockState();
        var drill = ModTools.DRILL.toStack();
        ElectricItemEnergy.charge(drill, 1000, 1, true, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        player.setOnGround(true);
        var grounded = new PlayerEvent.BreakSpeed(player, stone, 8.0F, null);
        DrillItem.onBreakSpeed(grounded);
        helper.assertTrue(
                grounded.getNewSpeed() == 8.0F,
                "A grounded dry drill keeps the legacy 8.0 speed");
        player.setOnGround(false);
        var airborne = new PlayerEvent.BreakSpeed(player, stone, 8.0F, null);
        DrillItem.onBreakSpeed(airborne);
        helper.assertTrue(
                airborne.getNewSpeed() == 24.0F,
                "An airborne drill only triples the vanilla fifth, like legacy");
        var unpowered = ModTools.DRILL.toStack();
        player.setItemInHand(InteractionHand.MAIN_HAND, unpowered);
        var idle = new PlayerEvent.BreakSpeed(player, stone, 1.0F, null);
        DrillItem.onBreakSpeed(idle);
        helper.assertTrue(
                idle.getNewSpeed() == 1.0F,
                "An unpowered drill keeps the full vanilla hand penalties");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        var pickaxe = new PlayerEvent.BreakSpeed(player, stone, 6.0F, null);
        DrillItem.onBreakSpeed(pickaxe);
        helper.assertTrue(
                pickaxe.getNewSpeed() == 6.0F, "Non-drill tools stay out of the compensation");
        helper.succeed();
    }

    static void iridiumModeToggle(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var level = helper.getLevel();
        var registries = level.registryAccess();
        var item = ModTools.IRIDIUM_DRILL.get();
        var stack = ModTools.IRIDIUM_DRILL.toStack();
        item.inventoryTick(stack, level, player, EquipmentSlot.MAINHAND);
        helper.assertTrue(
                IridiumDrillItem.modeLevel(stack, registries, Enchantments.FORTUNE) == 3
                        && IridiumDrillItem.modeLevel(stack, registries, Enchantments.SILK_TOUCH)
                                == 0,
                "A fresh iridium drill arms fortune III on its first inventory tick");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setShiftKeyDown(true);
        item.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                IridiumDrillItem.modeLevel(stack, registries, Enchantments.SILK_TOUCH) == 1
                        && IridiumDrillItem.modeLevel(stack, registries, Enchantments.FORTUNE) == 0,
                "Sneak use swaps fortune III for silk touch I");
        item.inventoryTick(stack, level, player, EquipmentSlot.MAINHAND);
        helper.assertTrue(
                IridiumDrillItem.modeLevel(stack, registries, Enchantments.SILK_TOUCH) == 1,
                "The lazy arming never overrides a user-chosen mode");
        item.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                IridiumDrillItem.modeLevel(stack, registries, Enchantments.FORTUNE) == 3
                        && IridiumDrillItem.modeLevel(stack, registries, Enchantments.SILK_TOUCH)
                                == 0,
                "A second toggle returns the drill to fortune III");
        var creative = IridiumDrillItem.fortuneStack(registries);
        helper.assertTrue(
                creative.getItem() == item
                        && IridiumDrillItem.modeLevel(creative, registries, Enchantments.FORTUNE)
                                == 3,
                "The creative page hands out the legacy fortune III stack");
        helper.succeed();
    }

    private DrillItemTests() {}
}
