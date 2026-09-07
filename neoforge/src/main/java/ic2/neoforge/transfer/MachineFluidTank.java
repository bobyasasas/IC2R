package ic2.neoforge.transfer;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

import java.util.function.Predicate;

public final class MachineFluidTank extends FluidStacksResourceHandler {
    private final Runnable changed;
    private final Predicate<FluidResource> accepts;

    public MachineFluidTank(int capacity, Runnable changed, Predicate<FluidResource> accepts) {
        super(1, capacity);
        if (capacity <= 0) throw new IllegalArgumentException("Invalid tank capacity");
        this.changed = changed;
        this.accepts = accepts;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return accepts.test(resource);
    }

    @Override
    protected void onContentsChanged(int index, FluidStack previousContents) {
        changed.run();
    }
}
