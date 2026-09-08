package ic2.neoforge.transfer;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import org.jspecify.annotations.Nullable;

/** Native container access that returns the changed item to a separate slot atomically. */
public final class FluidContainerPort {
    public static boolean accepts(ItemResource resource) {
        return ItemAccess.forStack(resource.toStack()).getCapability(Capabilities.Fluid.ITEM)
                != null;
    }

    public static @Nullable ResourceHandler<FluidResource> of(
            MachineInventory inventory, int input, int output) {
        var port = new ResourcePort<>(inventory, slot -> slot == output, slot -> slot == input);
        return ItemAccess.forHandlerIndex(port, input)
                .oneByOne()
                .getCapability(Capabilities.Fluid.ITEM);
    }

    private FluidContainerPort() {}
}
