package ic2.neoforge.item;

import net.minecraft.world.item.ItemStack;

/**
 * MOX fuel rod item. The in-reactor pulse behaviour (heat-scaled output and the depleted
 * re-enrichment path) lands with the reactor slice two; until then it sits inert in the grid,
 * mirroring nothing in the legacy reactor loop.
 */
public class MoxFuelRodItem extends FuelRodItem {
    public MoxFuelRodItem(Properties properties, int cells, int duration) {
        super(properties, cells, duration);
    }

    @Override
    public void processChamber(
            ItemStack stack, ReactorHost reactor, int x, int y, boolean heatRun) {}
}
