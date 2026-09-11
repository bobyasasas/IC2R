package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;

/**
 * The classic jetpack's 30,000 mB biogas tank as a native fluid container (legacy
 * StandardFluidItem): world-side filling through tanks, pipes and the FluidUtil paths. The tank
 * only ever holds biogas; the contents live in the shared FLUID data component.
 */
public final class JetpackTankHandler extends ItemAccessResourceHandler<FluidResource> {
    public JetpackTankHandler(ItemAccess itemAccess) {
        super(itemAccess, 1);
    }

    private static FluidStackTemplate stored(ItemResource current) {
        return current.getComponents().get(ModDataComponents.FLUID.get());
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource current, int index) {
        FluidStackTemplate template = stored(current);
        return template == null || template.amount() <= 0
                ? FluidResource.EMPTY
                : FluidResource.of(template.fluid().value());
    }

    @Override
    protected int getAmountFrom(ItemResource current, int index) {
        FluidStackTemplate template = stored(current);
        return template == null
                ? 0
                : (int) Math.min(JetpackItem.CAPACITY_MB, template.amount());
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        // Legacy ItemArmorFluidTank.canFill: the tank only ever holds biogas.
        return !resource.isEmpty() && resource.getFluid() == JetpackItem.biogas();
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return JetpackItem.CAPACITY_MB;
    }

    @Override
    protected ItemResource update(
            ItemResource current, int index, FluidResource resource, int amount) {
        if (amount <= 0 || resource.isEmpty()) return current.without(ModDataComponents.FLUID);
        return current.with(ModDataComponents.FLUID, new FluidStackTemplate(
                resource.getFluid(), amount));
    }
}
