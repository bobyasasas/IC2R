package ic2.neoforge.machine;

import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import javax.annotation.Nullable;

/**
 * Reactor chamber: a multiblock proxy that widens the adjacent nuclear reactor's grid by one column
 * per chamber. It holds no items of its own; right-clicking it opens the reactor menu and removing
 * it shrinks the grid (items outside the active columns are ejected by the reactor).
 */
public final class ReactorChamberBlockEntity extends MachineBlockEntity {
    public ReactorChamberBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                ((MachineBlock) state.getBlock()).kind().slots());
    }

    /** Locates the adjacent reactor core this chamber widens, scanning all six directions. */
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

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        NuclearReactorBlockEntity reactor = findReactor();
        return reactor == null ? null : reactor.createMenu(id, inventory, player);
    }

    @Override
    public Component getDisplayName() {
        NuclearReactorBlockEntity reactor = findReactor();
        return reactor != null ? reactor.getDisplayName() : super.getDisplayName();
    }
}
