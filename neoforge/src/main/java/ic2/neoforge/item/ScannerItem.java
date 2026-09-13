package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.menu.ScannerMenu;
import ic2.neoforge.registration.ModSounds;

import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Ore scanner. The miner draws one layer-scan pulse per mined level; handheld use pays the legacy
 * scan cost and opens the result GUI (legacy ItemScanner / ContainerToolScanner).
 */
public class ScannerItem extends ElectricItem {
    private final double layerScanCost;
    private final int layerScanRange;
    private final int handScanRange;

    public ScannerItem(
            Properties properties,
            ElectricItemSpec specification,
            int scanRange,
            double layerScanCost) {
        super(properties, specification);
        this.layerScanCost = layerScanCost;
        this.layerScanRange = scanRange / 2;
        this.handScanRange = scanRange;
    }

    /** Spends one scan pulse and answers with the half-range radius, or 0 when unpowered. */
    public int startLayerScan(ItemStack stack) {
        if (ElectricItemEnergy.charge(stack) < layerScanCost) {
            return 0;
        }
        ElectricItemEnergy.discharge(
                stack, layerScanCost, specification().tier(), true, false, false);
        return layerScanRange;
    }

    public int layerScanRange() {
        return layerScanRange;
    }

    /** Full radius of the handheld GUI sweep; the miner works at half of it per level. */
    public int handScanRange() {
        return handScanRange;
    }

    /** Legacy use(): one paid scan opens the handheld result GUI, or FAIL when unpowered. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ElectricItemEnergy.charge(stack) < layerScanCost) {
            return InteractionResult.FAIL;
        }
        if (player instanceof ServerPlayer server) {
            ElectricItemEnergy.discharge(
                    stack, layerScanCost, specification().tier(), true, false, false);
            server.connection.send(
                    new ClientboundSoundEntityPacket(
                            ModSounds.ITEM_SCANNER_USE,
                            server.getSoundSource(),
                            server,
                            1.0F,
                            1.0F,
                            level.getRandom().nextLong()));
            int slot =
                    hand == InteractionHand.MAIN_HAND
                            ? player.getInventory().getSelectedSlot()
                            : Inventory.SLOT_OFFHAND;
            server.openMenu(
                    new SimpleMenuProvider(
                            (id, inventory, owner) -> new ScannerMenu(id, inventory, slot),
                            stack.getHoverName()),
                    data -> data.writeVarInt(slot));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        if (player.containerMenu instanceof ScannerMenu menu
                && player.getInventory().getItem(menu.scannerSlotIndex()) == stack)
            player.closeContainer();
        return true;
    }
}
