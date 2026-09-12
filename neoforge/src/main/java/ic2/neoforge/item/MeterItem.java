package ic2.neoforge.item;

import ic2.neoforge.energy.CableBlock;
import ic2.neoforge.energy.FoamCableBlock;
import ic2.neoforge.machine.PoweredBlockEntity;
import ic2.neoforge.menu.MeterMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Legacy ItemToolMeter (EU-Reader): using it on an energy net node opens the meter GUI fixed to
 * that position; anything else reports the legacy "not an energy net tile" message. Dropping the
 * meter that owns an open meter GUI closes it.
 */
public final class MeterItem extends Item {
    public MeterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (player == null || level.isClientSide()) return InteractionResult.PASS;
        if (!isEnergyNode(level, pos)) {
            player.sendSystemMessage(Component.translatable("ic2.meter.info.not_energy_net"));
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer server) {
            int slot =
                    context.getHand() == InteractionHand.MAIN_HAND
                            ? player.getInventory().getSelectedSlot()
                            : Inventory.SLOT_OFFHAND;
            server.openMenu(
                    new SimpleMenuProvider(
                            (id, inventory, owner) ->
                                    new MeterMenu(id, inventory, pos, slot, false),
                            Component.translatable("container.ic2.meter")),
                            data -> data.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        if (player.containerMenu instanceof MeterMenu menu
                && player.getInventory().getItem(menu.meterSlotIndex()) == stack)
            player.closeContainer();
        return true;
    }

    /** Sources and sinks are powered machines; conductors are the two cable families. */
    public static boolean isEnergyNode(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PoweredBlockEntity
                || level.getBlockState(pos).getBlock() instanceof CableBlock
                || level.getBlockState(pos).getBlock() instanceof FoamCableBlock;
    }
}
