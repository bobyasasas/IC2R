package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.menu.ToolboxMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.UUID;

public final class ToolboxItem extends Item {
    public static final TagKey<Item> TOOLS =
            TagKey.create(Registries.ITEM, Identifier.parse("ic2:toolbox_tools"));

    public ToolboxItem(Properties properties) {
        super(properties);
    }

    public static boolean accepts(ItemResource resource) {
        return !(resource.getItem() instanceof ToolboxItem) && resource.is(TOOLS);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) {
            var stack = player.getItemInHand(hand);
            var identity = stack.get(ModDataComponents.TOOLBOX_ID);
            if (identity == null) {
                identity = UUID.randomUUID();
                stack.set(ModDataComponents.TOOLBOX_ID, identity);
            }
            final UUID binding = identity;
            int slot =
                    hand == InteractionHand.MAIN_HAND
                            ? player.getInventory().getSelectedSlot()
                            : Inventory.SLOT_OFFHAND;
            server.openMenu(
                    new SimpleMenuProvider(
                            (id, inventory, owner) ->
                                    new ToolboxMenu(id, inventory, slot, binding, false),
                            stack.getHoverName()),
                    data -> {
                        data.writeVarInt(slot);
                        data.writeUUID(binding);
                    });
        }
        return InteractionResult.SUCCESS;
    }
}
