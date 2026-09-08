package ic2.neoforge.machine;

import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.MachineFluidTank;
import ic2.neoforge.transfer.ResourcePort;
import ic2.neoforge.transfer.UpgradeTransfers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Persistent 24-bucket store; all fluid faces are bidirectional and upgrades are fluid-only. */
public final class TankBlockEntity extends MachineBlockEntity implements FluidMachine {
    public static final int CAPACITY = 24000;
    private final MachineFluidTank tank =
            new MachineFluidTank(CAPACITY, this::contentsChanged, fluid -> true);
    private int previousComparator;

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.TANK), pos, state, MachineKind.TANK.slots());
    }

    public MachineFluidTank tank() {
        return tank;
    }

    public int comparator() {
        int amount = tank.getAmountAsInt(0);
        return amount == 0 ? 0 : 1 + (int) (14L * amount / CAPACITY);
    }

    private void contentsChanged() {
        setChanged();
        int signal = comparator();
        if (signal != previousComparator) {
            previousComparator = signal;
            if (level != null && !level.isClientSide())
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    public ResourceHandler<FluidResource> fluidAutomation(Direction side) {
        return tank;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(inventory, slot -> false, slot -> false);
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

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 1 -> tank.getAmountAsInt(0);
            case 3 -> BuiltInRegistries.FLUID.getId(tank.getResource(0).getFluid());
            default -> 0;
        };
    }

    /** Called only after the menu validates current-menu identity, distance and block identity. */
    public boolean transferCursor(Player player, AbstractContainerMenu menu, boolean batch) {
        if (level == null || level.isClientSide() || menu.getCarried().isEmpty()) return false;
        int count = batch ? Math.min(64, menu.getCarried().getCount()) : 1;
        boolean changed = false;
        // Preserve one direction for the entire click; a filled replacement must not be drained
        // straight back on the next iteration when the cursor has been exhausted.
        Boolean filling = null;
        for (int item = 0; item < count && !menu.getCarried().isEmpty(); item++) {
            var container =
                    ItemAccess.forPlayerCursor(player, menu)
                            .oneByOne()
                            .getCapability(Capabilities.Fluid.ITEM);
            if (container == null) break;
            try (var transaction = Transaction.openRoot()) {
                int moved = 0;
                if (filling == null || filling) {
                    moved =
                            ResourceHandlerUtil.move(
                                    tank,
                                    container,
                                    resource -> true,
                                    Integer.MAX_VALUE,
                                    transaction);
                    if (moved > 0) filling = true;
                }
                if (moved == 0 && (filling == null || !filling)) {
                    moved =
                            ResourceHandlerUtil.move(
                                    container,
                                    tank,
                                    resource -> true,
                                    Integer.MAX_VALUE,
                                    transaction);
                    if (moved > 0) filling = false;
                }
                if (moved == 0) break;
                transaction.commit();
                changed = true;
            }
        }
        if (changed) menu.broadcastChanges();
        return changed;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.deserialize(input.childOrEmpty("tank"));
        previousComparator = comparator();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
    }
}
