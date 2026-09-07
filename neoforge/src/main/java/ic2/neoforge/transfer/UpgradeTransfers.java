package ic2.neoforge.transfer;

import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.machine.FluidMachine;
import ic2.neoforge.machine.MachineBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;

/** Uses the same sided ports as external automation, with native transfer rollback. */
public final class UpgradeTransfers {
    public static void tick(ServerLevel level, MachineBlockEntity machine) {
        var inventory = machine.inventory();
        for (int slot = machine.kind().upgradeStart(); slot < inventory.size(); slot++) {
            var stack = inventory.stack(slot);
            if (!(stack.getItem() instanceof UpgradeItem item)
                    || !item.kind().directional()
                    || !item.kind().suitable(machine.kind())) continue;
            var configured = UpgradeItem.direction(stack);
            int rate = 1 << (2 * Math.min(4, stack.getCount() - 1));
            for (var side : Direction.values()) {
                if (configured != null && configured != side) continue;
                var target = machine.getBlockPos().relative(side);
                if (!level.getChunkSource().hasChunk(target.getX() >> 4, target.getZ() >> 4))
                    continue;
                if (item.kind().fluid() && machine instanceof FluidMachine fluids) {
                    var local = fluids.fluidAutomation(side);
                    var adjacent =
                            level.getCapability(
                                    Capabilities.Fluid.BLOCK, target, side.getOpposite());
                    ResourceHandlerUtil.move(
                            item.kind().pulling() ? adjacent : local,
                            item.kind().pulling() ? local : adjacent,
                            resource -> true,
                            rate * 50,
                            null);
                } else {
                    var local = machine.automation(side);
                    var adjacent =
                            level.getCapability(
                                    Capabilities.Item.BLOCK, target, side.getOpposite());
                    ResourceHandlerUtil.moveStacking(
                            item.kind().pulling() ? adjacent : local,
                            item.kind().pulling() ? local : adjacent,
                            resource -> true,
                            rate,
                            null);
                }
            }
        }
    }

    private UpgradeTransfers() {}
}
