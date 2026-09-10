package ic2.neoforge.machine;

import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Reactor redstone port: the core reads this block's own redstone input as its own. */
public final class ReactorRedstonePortBlockEntity extends MachineBlockEntity {
    public ReactorRedstonePortBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                ((MachineBlock) state.getBlock()).kind().slots());
    }

    public boolean hasRedstoneInput(ServerLevel level) {
        return level.hasNeighborSignal(worldPosition);
    }

    /** The core reads any powered port inside its Chebyshev radius 2 casing (legacy range 2). */
    public static ReactorRedstonePortBlockEntity poweredPortNear(
            NuclearReactorBlockEntity reactor, ServerLevel level) {
        var center = reactor.getBlockPos();
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = -2; dy <= 2; dy++)
                for (int dz = -2; dz <= 2; dz++) {
                    if (level.getBlockEntity(center.offset(dx, dy, dz))
                                    instanceof ReactorRedstonePortBlockEntity port
                            && port.hasRedstoneInput(level)) return port;
                }
        return null;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ic2.neoforge.transfer.ResourcePort<>(inventory, slot -> false, slot -> false);
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
