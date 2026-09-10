package ic2.neoforge.machine;

import ic2.core.energy.VoltageTier;
import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.crop.CropBlockEntity;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.ResourcePort;
import ic2.neoforge.transfer.UpgradeTransfers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Legacy TileEntityCropHarvester: the same 9x3x9 scan cursor as the cropmatron, but it harvests
 * tiles at their optimal or maximum age into a 15-slot buffer for 20 EU per collected stack.
 */
public final class CropHarvesterBlockEntity extends PoweredBlockEntity {
    public static final int CONTENT_END = 15;

    private int scanX = -4, scanY = -1, scanZ = -4;
    private int storageUpgrades = -1, transformerUpgrades = -1;

    public CropHarvesterBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(MachineKind.CROP_HARVESTER),
                pos,
                state,
                10000,
                ((MachineBlock) state.getBlock()).kind().slots());
    }

    /** Legacy content slot is Access.IO: automation may fill and empty the buffer. */
    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory, slot -> slot < CONTENT_END, slot -> slot < CONTENT_END);
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        refreshUpgrades();
        return EnergyNode.Terminal.sink(
                energy,
                VoltageTier.fromIcTier(Math.clamp(1 + transformerUpgrades, 1, 4)).getVoltage(),
                1);
    }

    @Override
    public void serverTick(ServerLevel level) {
        refreshUpgrades();
        UpgradeTransfers.tick(level, this);
        if (level.getGameTime() % 10L == 0L && energy.stored() >= 21.0) scan();
    }

    /** Legacy scan: one cursor step, then at most one harvest at the visited position. */
    public void scan() {
        scanX++;
        if (scanX > 4) {
            scanX = -4;
            scanZ++;
            if (scanZ > 4) {
                scanZ = -4;
                scanY++;
                if (scanY > 1) scanY = -1;
            }
        }
        energy.extract(1.0);
        if (level == null || isBufferFull()) return;
        if (!(level.getBlockEntity(worldPosition.offset(scanX, scanY, scanZ))
                instanceof CropBlockEntity crop)) return;
        if (crop.card() == null
                || crop.getCurrentAge() != crop.card().getOptimalHarvestAge()
                        && crop.getCurrentAge() != crop.card().getMaxAge()) return;
        var drops = crop.performHarvest();
        if (drops == null || !(level instanceof ServerLevel server)) return;
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            var resource = ItemResource.of(drop);
            int collected;
            try (var transaction = Transaction.openRoot()) {
                collected = inventory.insert(resource, drop.getCount(), transaction);
                if (collected > 0) transaction.commit();
            }
            if (collected > 0) {
                energy.extract(20.0);
            } else {
                dropAsEntity(server, drop);
            }
        }
    }

    private void dropAsEntity(ServerLevel level, ItemStack stack) {
        net.minecraft.world.Containers.dropItemStack(
                level,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5,
                stack);
    }

    private boolean isBufferFull() {
        for (int slot = 0; slot < CONTENT_END; slot++) {
            var stack = inventory.stack(slot);
            if (stack.isEmpty() || inventory.getAmountAsInt(slot) < stack.getMaxStackSize())
                return false;
        }
        return true;
    }

    /** Legacy upgrade effects: +10000 EU per storage upgrade, +1 input tier per transformer. */
    private void refreshUpgrades() {
        int storage = 0, transformers = 0;
        for (int slot = kind().upgradeStart(); slot < inventory.size(); slot++) {
            var resource = inventory.getResource(slot);
            int count = Math.min(64, inventory.getAmountAsInt(slot));
            if (!(resource.getItem() instanceof UpgradeItem item)) continue;
            switch (item.kind()) {
                case ENERGY_STORAGE -> storage += count;
                case TRANSFORMER -> transformers += count;
                default -> {}
            }
        }
        if (storage == storageUpgrades && transformers == transformerUpgrades) return;
        storageUpgrades = storage;
        transformerUpgrades = transformers;
        energy.resize(10000.0 + 10000.0 * storage);
        setChanged();
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        scanX = input.getIntOr("scanX", -4);
        scanY = input.getIntOr("scanY", -1);
        scanZ = input.getIntOr("scanZ", -4);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("scanX", scanX);
        output.putInt("scanY", scanY);
        output.putInt("scanZ", scanZ);
    }
}
