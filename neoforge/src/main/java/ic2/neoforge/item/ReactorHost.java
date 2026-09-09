package ic2.neoforge.item;

import net.minecraft.world.item.ItemStack;

/**
 * The reactor host view handed to grid components (legacy IReactor subset). Coordinates are grid
 * columns (x, widened by chambers later) and rows (y, always six).
 */
public interface ReactorHost {
    boolean produceEnergy();

    int getHeat();

    void setHeat(int heat);

    int addHeat(int amount);

    int getMaxHeat();

    void setMaxHeat(int maxHeat);

    float getHeatEffectModifier();

    void setHeatEffectModifier(float hem);

    ItemStack getItemAt(int x, int y);

    void setItemAt(int x, int y, ItemStack stack);

    float getReactorEnergyOutput();

    void addOutput(float energy);

    /** Fluid-cooling emission buffer; the EU-mode core leaves it unaccounted. */
    default void addEmitHeat(int heat) {}
}
