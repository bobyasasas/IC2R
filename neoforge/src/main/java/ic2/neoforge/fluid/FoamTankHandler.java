package ic2.neoforge.fluid;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.FoamSprayerItem;

import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Partial fluid tank stored in a data component; accepts wet construction foam only. */
public final class FoamTankHandler extends ItemAccessResourceHandler<FluidResource> {
    private final int capacity;

    public FoamTankHandler(ItemAccess access, int capacity) {
        super(access, 1);
        this.capacity = capacity;
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource item, int index) {
        var stored = item.get(ModDataComponents.FLUID);
        return stored == null ? FluidResource.EMPTY : FluidResource.of(stored);
    }

    @Override
    protected int getAmountFrom(ItemResource item, int index) {
        var stored = item.get(ModDataComponents.FLUID);
        return stored == null ? 0 : Math.min(this.capacity, stored.amount());
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return this.capacity;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return resource.isEmpty() || resource.getFluid() == FoamSprayerItem.foamFluid();
    }

    @Override
    protected ItemResource update(ItemResource item, int index, FluidResource fluid, int amount) {
        if (amount <= 0) {
            return item.without(ModDataComponents.FLUID);
        }
        return item.with(ModDataComponents.FLUID, new FluidStackTemplate(fluid.getFluid(), amount));
    }
}
