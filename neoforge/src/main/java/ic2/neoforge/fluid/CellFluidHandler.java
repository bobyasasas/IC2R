package ic2.neoforge.fluid;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.FluidCellItem;
import ic2.neoforge.registration.ModCells;

import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Whole-cell exchanges only; simulations use the same 1000 mB boundary as commits. */
public final class CellFluidHandler extends ItemAccessResourceHandler<FluidResource> {
    public CellFluidHandler(ItemAccess access) {
        super(access, 1);
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource item, int index) {
        if (!(item.getItem() instanceof FluidCellItem cell)) return FluidResource.EMPTY;
        return cell.fixedFluid() == Fluids.EMPTY
                ? FluidResource.of(item.get(ModDataComponents.FLUID))
                : FluidResource.of(cell.fixedFluid());
    }

    @Override
    protected int getAmountFrom(ItemResource item, int index) {
        if (!(item.getItem() instanceof FluidCellItem cell)) return 0;
        if (cell.fixedFluid() != Fluids.EMPTY) return 1000;
        var stored = item.get(ModDataComponents.FLUID);
        return stored == null ? 0 : Math.min(1000, stored.amount());
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return 1000;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return itemAccess.getResource().getItem() instanceof FluidCellItem;
    }

    @Override
    protected ItemResource update(ItemResource item, int index, FluidResource fluid, int amount) {
        if (amount != 0 && amount != 1000) return ItemResource.EMPTY;
        var cell = amount == 0 ? ModCells.EMPTY.get() : ModCells.forFluid(fluid.getFluid());
        var result =
                ItemResource.of(cell, item.getComponentsPatch()).without(ModDataComponents.FLUID);
        if (amount == 1000
                && (cell.fixedFluid() == Fluids.EMPTY || !fluid.getComponentsPatch().isEmpty())) {
            result =
                    ItemResource.of(ModCells.EMPTY.get(), item.getComponentsPatch())
                            .with(
                                    ModDataComponents.FLUID,
                                    FluidStackTemplate.fromNonEmptyStack(fluid.toStack(1000)));
        }
        return result;
    }
}
