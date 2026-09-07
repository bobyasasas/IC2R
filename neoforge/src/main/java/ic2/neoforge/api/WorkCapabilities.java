package ic2.neoforge.api;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.BlockCapability;

import org.jspecify.annotations.Nullable;

public final class WorkCapabilities {
    public static final BlockCapability<WorkSource, @Nullable Direction> HEAT =
            BlockCapability.createSided(
                    Identifier.fromNamespaceAndPath("ic2", "heat"), WorkSource.class);
    public static final BlockCapability<WorkSource, @Nullable Direction> KINETIC =
            BlockCapability.createSided(
                    Identifier.fromNamespaceAndPath("ic2", "kinetic"), WorkSource.class);

    private WorkCapabilities() {}
}
