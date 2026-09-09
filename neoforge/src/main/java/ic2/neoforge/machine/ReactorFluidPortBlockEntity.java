package ic2.neoforge.machine;

import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Reactor fluid port: a multiblock window over the nuclear reactor's coolant tanks. Inserting
 * coolant reaches the input tank; extracting pulls hot coolant from the output tank. An adjacent
 * port also switches the reactor into fluid-cooled mode.
 */
public final class ReactorFluidPortBlockEntity extends MachineBlockEntity {
    public ReactorFluidPortBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                ((MachineBlock) state.getBlock()).kind().slots());
    }

    @Nullable
    public NuclearReactorBlockEntity findReactor() {
        if (!(getLevel() instanceof ServerLevel level)) return null;
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(direction))
                    instanceof NuclearReactorBlockEntity reactor) return reactor;
        }
        return null;
    }

    public ResourceHandler<FluidResource> fluidAutomation(Direction side) {
        var reactor = findReactor();
        return reactor == null ? null : reactor.coolantTanks();
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ic2.neoforge.transfer.ResourcePort<>(
                inventory, slot -> false, slot -> false);
    }

    @Override
    public void serverTick(ServerLevel level) {}

    @Override
    public int progress() {
        return 0;
    }

    @Override
    public int progressMaximum() {
        return 0;
    }
}
