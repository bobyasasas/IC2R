package ic2.neoforge.item;

import ic2.neoforge.registration.ModReactorItems;

import net.minecraft.world.item.ItemStack;

/**
 * MOX fuel rod: same pulse loop as uranium but every pulse yields {@code 4 × heatRatio + 1} EU,
 * scaling with the reactor's stored heat. Fluid-mode heat doubling lands with fluid cooling.
 */
public class MoxFuelRodItem extends FuelRodItem {
    public MoxFuelRodItem(Properties properties, int cells, int duration) {
        super(properties, cells, duration);
    }

    @Override
    public boolean acceptUraniumPulse(
            ItemStack stack,
            ReactorHost reactor,
            ItemStack pulsingStack,
            int youX,
            int youY,
            int pulseX,
            int pulseY,
            boolean heatRun) {
        if (!heatRun) {
            float breederEffectiveness = (float) reactor.getHeat() / reactor.getMaxHeat();
            reactor.addOutput(4.0F * breederEffectiveness + 1.0F);
        }
        return true;
    }

    @Override
    protected ItemStack depletedStack() {
        return switch (cells()) {
            case 1 -> ModReactorItems.DEPLETED_MOX_FUEL_ROD.get().getDefaultInstance();
            case 2 -> ModReactorItems.DEPLETED_DUAL_MOX_FUEL_ROD.get().getDefaultInstance();
            case 4 -> ModReactorItems.DEPLETED_QUAD_MOX_FUEL_ROD.get().getDefaultInstance();
            default -> throw new IllegalStateException("Invalid fuel rod cell count: " + cells());
        };
    }
}
