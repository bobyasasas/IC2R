package ic2.neoforge.item;

import net.minecraft.world.item.ItemStack;

/**
 * Reactor grid component (legacy IReactorComponent). The host runs two passes per reactor cycle: a
 * pulse pass (heatRun=false) that produces EU, and a heat pass (heatRun=true) that distributes
 * generated heat to neighbouring acceptors.
 */
public interface ReactorComponent {
    default void processChamber(
            ItemStack stack, ReactorHost reactor, int x, int y, boolean heatRun) {}

    /** Reflectors accept pulses on behalf of their neighbours; rods add EU per pulse. */
    default boolean acceptUraniumPulse(
            ItemStack stack,
            ReactorHost reactor,
            ItemStack pulsingStack,
            int youX,
            int youY,
            int pulseX,
            int pulseY,
            boolean heatRun) {
        return false;
    }

    default boolean canStoreHeat(ItemStack stack, ReactorHost reactor, int x, int y) {
        return false;
    }

    /** Applies heat to this component and returns the amount it could not absorb. */
    default int alterHeat(ItemStack stack, ReactorHost reactor, int x, int y, int heat) {
        return heat;
    }

    default float influenceExplosion(ItemStack stack, ReactorHost reactor) {
        return 0.0F;
    }
}
