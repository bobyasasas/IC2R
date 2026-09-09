package ic2.neoforge.machine;

import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import javax.annotation.Nullable;

/** Reactor access hatch: opens the adjacent reactor's grid menu and exposes its inventory. */
public final class ReactorAccessHatchBlockEntity extends MachineBlockEntity {
    public ReactorAccessHatchBlockEntity(BlockPos pos, BlockState state) {
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

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        NuclearReactorBlockEntity reactor = findReactor();
        return reactor == null ? null : reactor.createMenu(id, inventory, player);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        var reactor = findReactor();
        return reactor == null
                ? new ic2.neoforge.transfer.ResourcePort<>(
                        inventory, slot -> false, slot -> false)
                : reactor.automation(side);
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
