package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;

import net.minecraft.world.item.ItemStack;

/**
 * Ore scanner. The miner draws one layer-scan pulse per mined level; the handheld result inventory
 * keeps following the toolbox and menu work and is not part of this.
 */
public class ScannerItem extends ElectricItem {
    private final double layerScanCost;
    private final int layerScanRange;

    public ScannerItem(
            Properties properties,
            ElectricItemSpec specification,
            int scanRange,
            double layerScanCost) {
        super(properties, specification);
        this.layerScanCost = layerScanCost;
        this.layerScanRange = scanRange / 2;
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
}
