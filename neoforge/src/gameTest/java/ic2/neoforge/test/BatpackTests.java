package ic2.neoforge.test;

import com.mojang.authlib.GameProfile;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.ModArmor;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Legacy batpack family: wearable externally dischargeable batteries whose energy feeds held
 * tools through the legacy manager.use / chargeFromArmor distribution.
 */
final class BatpackTests {
    static void specArmorAndExternalOutput(GameTestHelper helper) {
        var chainsaw = ModTools.CHAINSAW.toStack();
        var batpack = ModArmor.BATPACK.toStack();
        var advanced = ModArmor.ADVANCED_BATPACK.toStack();
        helper.assertTrue(
                ModArmor.BATPACK.get().specification().equals(new ElectricItemSpec(60000, 100, 1, true))
                        && ModArmor.ADVANCED_BATPACK.get()
                                .specification()
                                .equals(new ElectricItemSpec(600000, 1000, 2, true)),
                "Batpacks must keep legacy 60000/100/t1 and 600000/1000/t2 specs, externally output");
        helper.assertTrue(
                batpack.has(DataComponents.EQUIPPABLE) && advanced.has(DataComponents.EQUIPPABLE),
                "Batpacks must be equippable");

        List<Double> amounts = new ArrayList<>();
        batpack.get(DataComponents.ATTRIBUTE_MODIFIERS)
                .forEach(
                        EquipmentSlotGroup.CHEST,
                        (attribute, modifier) -> {
                            if (attribute.value() == Attributes.ARMOR.value()
                                    || attribute.value() == Attributes.ARMOR_TOUGHNESS.value()) {
                                amounts.add(modifier.amount());
                            }
                        });
        helper.assertTrue(
                amounts.size() == 2 && amounts.contains(8.0) && amounts.contains(2.0),
                "Batpack chestplate must carry legacy Ic2ArmorMaterials.BAT_PACK 8/2.0");

        ElectricItemEnergy.charge(batpack, 1000, 1, true, false);
        ElectricItemEnergy.charge(chainsaw, 1000, 1, true, false);
        helper.assertTrue(
                ElectricItemEnergy.discharge(batpack, 500, 1, true, true, false) == 500,
                "Batpack must discharge externally like legacy canProvideEnergy");
        helper.assertTrue(
                ElectricItemEnergy.discharge(chainsaw, 0, 1, true, true, true) == 0,
                "Tools must not discharge externally");
        helper.succeed();
    }

    static void armorDistribution(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "ic2-batpack"));
        var batpack = ModArmor.BATPACK.toStack();
        ElectricItemEnergy.charge(batpack, 5000, 1, true, false);
        player.setItemSlot(EquipmentSlot.CHEST, batpack);

        var drill = ModTools.DRILL.toStack();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, drill);
        helper.assertTrue(
                ElectricItemEnergy.use(drill, 50, player),
                "Use must succeed by pulling the worn batpack");
        helper.assertTrue(
                ElectricItemEnergy.charge(drill) == 4950,
                "Empty drill must be topped to capacity then spend one operation");
        helper.assertTrue(
                ElectricItemEnergy.charge(batpack) == 0,
                "Batpack must be drained into the drill up to its capacity");
        helper.assertTrue(
                player.getItemBySlot(EquipmentSlot.CHEST).getItem() == ModArmor.BATPACK.get(),
                "Emptied batpack must stay worn");
        helper.succeed();
    }

    static void tierGating(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "ic2-batpack-t"));
        var batpack = ModArmor.BATPACK.toStack();
        ElectricItemEnergy.charge(batpack, 60000, 1, true, false);
        player.setItemSlot(EquipmentSlot.CHEST, batpack);

        var iridium = ModTools.IRIDIUM_DRILL.toStack();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, iridium);
        helper.assertTrue(
                !ElectricItemEnergy.use(iridium, 800, player),
                "Tier 1 batpack must not feed a tier 3 device");
        helper.assertTrue(
                ElectricItemEnergy.charge(batpack) == 60000 && ElectricItemEnergy.charge(iridium) == 0,
                "Failed feed must not move energy");

        player.setItemSlot(EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY);
        var advanced = ModArmor.ADVANCED_BATPACK.toStack();
        ElectricItemEnergy.charge(advanced, 1000, 2, true, false);
        player.setItemSlot(EquipmentSlot.CHEST, advanced);
        var diamond = ModTools.DIAMOND_DRILL.toStack();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, diamond);
        helper.assertTrue(
                ElectricItemEnergy.use(diamond, 80, player),
                "Tier 2 advanced batpack must feed a tier 1 device");
        helper.assertTrue(
                ElectricItemEnergy.charge(diamond) == 920 && ElectricItemEnergy.charge(advanced) == 0,
                "Advanced batpack must top up the diamond drill");
        helper.succeed();
    }

    static void miningThroughBatpack(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "ic2-batpack-m"));
        var batpack = ModArmor.BATPACK.toStack();
        ElectricItemEnergy.charge(batpack, 2000, 1, true, false);
        player.setItemSlot(EquipmentSlot.CHEST, batpack);
        // Legacy consumeEnergy gates on the tool's own charge first: a fully empty drill
        // performs no operation and pulls nothing from the worn pack.
        var drill = ModTools.DRILL.toStack();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, drill);
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
        var state = Blocks.STONE.defaultBlockState();
        level.setBlockAndUpdate(pos, state);
        helper.assertTrue(
                ModTools.DRILL.get().mineBlock(drill, level, state, pos, player),
                "Mining with an empty held drill must still report the block broken");
        helper.assertTrue(
                ElectricItemEnergy.charge(drill) == 0 && ElectricItemEnergy.charge(batpack) == 2000,
                "Empty drill must not pull from the batpack, like legacy canUse gating");

        ElectricItemEnergy.charge(drill, 100, 1, true, false);
        helper.assertTrue(
                ModTools.DRILL.get().mineBlock(drill, level, state, pos, player),
                "Mining with a charged drill must succeed");
        helper.assertTrue(
                ElectricItemEnergy.charge(drill) == 2050,
                "Drill must be topped to capacity from the pack, then spend one 50 EU operation");
        helper.assertTrue(
                ElectricItemEnergy.charge(batpack) == 0,
                "Worn batpack must cover the topped-up energy");
        helper.succeed();
    }

    private BatpackTests() {}
}
