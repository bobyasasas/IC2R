package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.component.CropSeed;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.crop.CropBlockEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Handheld crop analyzer (legacy ItemCropAnalyzer): air use opens the three-slot scanning GUI,
 * right-clicking a grown crop pays 900 EU for a chat report. Scanning a seed bag raises its scan
 * level 0..4, each step costing 10/90/900/9000 EU and unlocking one more GUI info tier.
 */
public class CropAnalyzerItem extends ElectricItem {
    /** Legacy energyForLevel: the cost of scanning from the given level to the next. */
    public static int energyForLevel(int level) {
        return switch (level) {
            case 1 -> 90;
            case 2 -> 900;
            case 3 -> 9000;
            default -> 10;
        };
    }

    public CropAnalyzerItem(Properties properties, ElectricItemSpec specification) {
        super(properties, specification);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof net.minecraft.server.level.ServerPlayer server) {
            int slot =
                    hand == InteractionHand.MAIN_HAND
                            ? player.getInventory().getSelectedSlot()
                            : Inventory.SLOT_OFFHAND;
            server.openMenu(
                    new net.minecraft.world.SimpleMenuProvider(
                            (id, inventory, owner) ->
                                    new ic2.neoforge.menu.CropAnalyzerMenu(id, inventory, slot),
                            Component.translatable("container.ic2.crop_analyzer")),
                    data -> data.writeVarInt(slot));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide() || player == null || player.isCrouching()) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof CropBlockEntity crop)
                || crop.card() == null) {
            return InteractionResult.PASS;
        }
        List<Component> report = analyzeCrop(stack, crop);
        if (report.isEmpty()) return InteractionResult.PASS;
        report.forEach(player::sendSystemMessage);
        return InteractionResult.SUCCESS;
    }

    /**
     * Legacy right-click report; empty when the crop or the 900 EU fee is missing. Returned as
     * components so tests can assert the content without a player.
     */
    public List<Component> analyzeCrop(ItemStack stack, CropBlockEntity crop) {
        var card = crop.card();
        if (card == null) return List.of();
        int fee = energyForLevel(2);
        if (ElectricItemEnergy.charge(stack) < fee) return List.of();
        ElectricItemEnergy.discharge(stack, fee, specification().tier(), true, false, false);
        return List.of(
                Component.translatable(
                        "ic2.crop_analyzer.crop_name",
                        Component.translatable("ic2.crop." + card.getId())),
                Component.translatable(
                        "ic2.crop.analyzer.crop_discovered_by", card.getDiscoveredBy()),
                Component.translatable("ic2.crop_analyzer.crop_age", crop.getCurrentAge()),
                Component.translatable(
                        "ic2.crop_analyzer.crop_nutrients", crop.getStorageNutrients()),
                Component.translatable("ic2.crop_analyzer.crop_water", crop.getStorageWater()),
                Component.translatable(
                        "ic2.crop_analyzer.crop_weed_extra", crop.getStorageWeedEx()),
                Component.translatable(
                        "ic2.crop_analyzer.crop.growth_points",
                        crop.getGrowthPoints(),
                        card.getGrowthDuration(crop)));
    }

    /**
     * Legacy tryScan over the GUI slots: [0] input seed bag, [1] output, [2] battery. Raises the
     * input seed's scan level by one and moves it to the output; a fully scanned bag moves for
     * free. The fee comes from the analyzer stack only — the legacy battery slot is a
     * SlotDischarge placement gate and never feeds the scan. Mutates the slot list; the caller
     * persists it.
     */
    public boolean tryScan(ItemStack analyzer, List<ItemStack> slots) {
        ItemStack input = slots.get(0);
        ItemStack output = slots.get(1);
        if (!output.isEmpty() || input.isEmpty()) return false;
        CropSeed seed = input.get(ModDataComponents.CROP_SEED.get());
        if (seed == null) return false;

        if (seed.scan() >= 4) {
            slots.set(1, input);
            slots.set(0, ItemStack.EMPTY);
            return true;
        }

        int fee = energyForLevel(seed.scan());
        if (ElectricItemEnergy.charge(analyzer) < fee) return false;
        ElectricItemEnergy.discharge(analyzer, fee, specification().tier(), true, false, false);
        input.set(ModDataComponents.CROP_SEED.get(), seed.incrementScan());
        slots.set(1, input);
        slots.set(0, ItemStack.EMPTY);
        return true;
    }
}
