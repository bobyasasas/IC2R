package ic2.neoforge.test;

import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModItems;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

final class ConsumptionTests {
    static void tinCans(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(16);
        player.setItemInHand(
                InteractionHand.MAIN_HAND, new ItemStack(ModItems.FILLED_TIN_CAN.get(), 8));
        ModItems.FILLED_TIN_CAN.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                player.getFoodData().getFoodLevel() == 20
                        && player.getMainHandItem().getCount() == 4,
                "Bulk eating must consume only missing hunger points");
        int returned = 0;
        for (int slot = 0; slot < 36; slot++)
            if (player.getInventory()
                    .getItem(slot)
                    .is(ModItems.MATERIALS.get(MaterialDefinition.TIN_CAN).get()))
                returned += player.getInventory().getItem(slot).getCount();
        helper.assertTrue(returned == 4, "Each eaten can must return one empty can");
        for (int slot = 0; slot < 36; slot++)
            player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        player.setItemInHand(
                InteractionHand.MAIN_HAND, new ItemStack(ModItems.FILLED_TIN_CAN.get(), 8));
        player.getFoodData().setFoodLevel(19);
        ModItems.FILLED_TIN_CAN.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                player.getFoodData().getFoodLevel() == 19
                        && player.getMainHandItem().getCount() == 8,
                "Full inventory must roll back food consumption when empty cans cannot fit");
        helper.succeed();
    }

    private ConsumptionTests() {}
}
