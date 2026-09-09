package ic2.neoforge.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


import net.minecraft.world.item.ItemStack;

/**
 * Reactor plating (legacy ItemReactorPlating): raises the core heat limit and softens the heat
 * effect rolls while present in the grid.
 */
public class ReactorPlatingItem extends Item implements ReactorComponent {
    private final int maxHeatAdd;
    private final float effectModifier;

    public ReactorPlatingItem(Properties properties, int maxHeatAdd, float effectModifier) {
        super(properties);
        this.maxHeatAdd = maxHeatAdd;
        this.effectModifier = effectModifier;
    }

    @Override
    public void processChamber(
            ItemStack stack, ReactorHost reactor, int x, int y, boolean heatRun) {
        if (heatRun) {
            reactor.setMaxHeat(reactor.getMaxHeat() + maxHeatAdd);
            reactor.setHeatEffectModifier(reactor.getHeatEffectModifier() * effectModifier);
        }
    }

    @Override
    public float influenceExplosion(ItemStack stack, ReactorHost reactor) {
        return effectModifier >= 1.0F ? 0.0F : effectModifier;
    }
}
