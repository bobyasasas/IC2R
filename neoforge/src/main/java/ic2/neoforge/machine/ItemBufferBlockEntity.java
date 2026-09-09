package ic2.neoforge.machine;

import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.ResourcePort;
import ic2.neoforge.transfer.UpgradeTransfers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.function.IntPredicate;

/**
 * Passive 48-slot logistics buffer. The left half (reached from top and bottom) and the right half
 * (reached from the horizontal sides) are independent stacks, so ejector and pulling upgrades can
 * bridge between two sides without mixing flows.
 */
public final class ItemBufferBlockEntity extends MachineBlockEntity {
    public static final int SIDE_START = 24;
    public static final int GROUP_SIZE = 24;

    public ItemBufferBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                ((MachineBlock) state.getBlock()).kind().slots());
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        // Legacy InvSide.SIDE (horizontal faces) saw the right group, NOTSIDE the left one.
        int lo = side.getAxis().isHorizontal() ? SIDE_START : 0;
        IntPredicate inGroup = slot -> slot >= lo && slot < lo + GROUP_SIZE;
        return new ResourcePort<>(inventory, inGroup, inGroup);
    }

    @Override
    public void serverTick(ServerLevel level) {
        UpgradeTransfers.tick(level, this);
    }

    @Override
    public int progress() {
        return 0;
    }

    @Override
    public int progressMaximum() {
        return 0;
    }
}
