package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Neutron reflector (legacy ItemReactorReflector / iridium variant): bounces a rod's pulse back
 * into the rod, doubling its effective output. Thin reflectors deplete per reflected pulse and
 * vanish; the iridium reflector is permanent.
 */
public class ReflectorItem extends Item implements ReactorComponent {
    private final int maxUse;
    private final boolean depletes;

    public ReflectorItem(Properties properties, int maxUse, boolean depletes) {
        super(properties);
        this.maxUse = maxUse;
        this.depletes = depletes;
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
            if (pulsingStack.getItem() instanceof ReactorComponent source)
                source.acceptUraniumPulse(
                        pulsingStack, reactor, stack, pulseX, pulseY, youX, youY, heatRun);
        } else if (depletes) {
            int use = Math.clamp(stack.getOrDefault(ModDataComponents.REACTOR_USE, 0), 0, maxUse);
            if (use + 1 >= maxUse) {
                reactor.setItemAt(youX, youY, net.minecraft.world.item.ItemStack.EMPTY);
            } else {
                stack.set(ModDataComponents.REACTOR_USE, use + 1);
            }
        }
        return true;
    }

    @Override
    public float influenceExplosion(ItemStack stack, ReactorHost reactor) {
        return -1.0F;
    }
}
