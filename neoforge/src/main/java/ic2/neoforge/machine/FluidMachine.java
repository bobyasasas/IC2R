package ic2.neoforge.machine;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/** Shared sided fluid contract for capabilities and automatic upgrade transfers. */
public interface FluidMachine {
    ResourceHandler<FluidResource> fluidAutomation(Direction side);
}
