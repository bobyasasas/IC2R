package ic2.neoforge.item;

import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModItems;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Preserves instant bulk eating, but commits returned containers before changing hunger. */
public final class TinCanItem extends Item {
    public TinCanItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.getFoodData().needsFood()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        int slot =
                hand == InteractionHand.MAIN_HAND
                        ? player.getInventory().getSelectedSlot()
                        : Inventory.SLOT_OFFHAND;
        var inventory = PlayerInventoryWrapper.of(player);
        var stack = player.getItemInHand(hand);
        int amount = Math.min(stack.getCount(), 20 - player.getFoodData().getFoodLevel());
        if (amount <= 0) return InteractionResult.PASS;
        try (var transaction = Transaction.openRoot()) {
            if (inventory.extract(slot, ItemResource.of(stack), amount, transaction) != amount
                    || inventory.insert(
                                    ItemResource.of(
                                            ModItems.MATERIALS
                                                    .get(MaterialDefinition.TIN_CAN)
                                                    .get()),
                                    amount,
                                    transaction)
                            != amount) return InteractionResult.FAIL;
            transaction.commit();
        }
        player.getFoodData().eat(amount, amount);
        return InteractionResult.SUCCESS;
    }

    /**
     * Legacy ItemTinCan.onEaten driven from the quantum helmet: bulk-eats the first filled can
     * stack found in the main inventory and stows the empty cans, like the legacy path.
     */
    public static boolean eatFromInventory(Player player) {
        var food = player.getFoodData();
        var items = player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.getItem() != ModItems.FILLED_TIN_CAN.get()) continue;
            int amount = Math.min(stack.getCount(), 20 - food.getFoodLevel());
            if (amount <= 0) return false;
            ItemStack empties =
                    new ItemStack(
                            ModItems.MATERIALS.get(MaterialDefinition.TIN_CAN).get(), amount);
            if (!player.getInventory().add(empties)) return false;
            food.eat(amount, amount);
            stack.shrink(amount);
            return true;
        }

        return false;
    }
}
