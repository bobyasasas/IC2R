package ic2.neoforge.machine;

import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.ScannerItem;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;

/**
 * Ports the legacy advanced miner: instead of digging a pipe shaft it sweeps a square area under
 * itself one layer at a time, spending 64 EU per inspected block from its scanner plus 512 EU per
 * mined block straight from its 4,000,000 EU buffer. A built-in blacklist/whitelist over the drops
 * gates every block; the sweep cursor survives reload.
 */
public final class AdvMinerBlockEntity extends PoweredBlockEntity {
    public static final int SCANNER_SLOT = 0;
    public static final int FILTER_START = 1;
    public static final int FILTER_SIZE = 15;
    private static final int SCAN_ENERGY = 64;
    private static final int MINE_ENERGY = 512;
    private static final int WORK_TICKS = 20;

    private BlockPos mineTarget;
    private boolean blacklist = true;
    private boolean silkTouch = false;
    private int ticker;
    private int maxBlockScanCount = 5;

    public AdvMinerBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                4000000,
                20);
    }

    @Override
    public ic2.core.energy.grid.EnergyNode.Terminal energyNode() {
        // Legacy sink tier: min(2 + minerDischargeTier, 5) with the default tier-one setting.
        return ic2.core.energy.grid.EnergyNode.Terminal.sink(energy, 512, 3);
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        if (slot == SCANNER_SLOT) return resource.getItem() instanceof ScannerItem;
        return super.acceptsInventorySlot(slot, resource);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ic2.neoforge.transfer.ResourcePort<>(inventory, slot -> false, slot -> false);
    }

    @Override
    public void serverTick(ServerLevel level) {
        chargeTool();
        setActive(work(level));
    }

    private void chargeTool() {
        var stack = inventory.stack(SCANNER_SLOT);
        if (stack.isEmpty()) return;
        double moved = ElectricItemEnergy.charge(stack, energy.stored(), 3, false, false);
        if (moved > 0) {
            inventory.set(SCANNER_SLOT, ItemResource.of(stack), stack.getCount());
            energy.extract(moved);
        }
    }

    private boolean work(ServerLevel level) {
        if (energy.stored() < MINE_ENERGY) return false;
        if (level.hasNeighborSignal(worldPosition)) return false;
        var scanner = inventory.stack(SCANNER_SLOT);
        if (scanner.isEmpty() || ElectricItemEnergy.charge(scanner) < SCAN_ENERGY) return false;
        if (++ticker != WORK_TICKS) return true;
        ticker = 0;
        int range =
                inventory.getResource(SCANNER_SLOT).getItem() == ModTools.ADVANCED_SCANNER.get()
                        ? 32
                        : 16;
        if (mineTarget == null) {
            mineTarget =
                    new BlockPos(
                            worldPosition.getX() - range - 1,
                            worldPosition.getY() - 1,
                            worldPosition.getZ() - range);
            if (mineTarget.getY() < level.getMinY()) return false;
        }
        int count = maxBlockScanCount;
        var scanPos =
                new BlockPos.MutableBlockPos(
                        mineTarget.getX(), mineTarget.getY(), mineTarget.getZ());
        do {
            if (scanPos.getX() < worldPosition.getX() + range) {
                scanPos.set(scanPos.getX() + 1, scanPos.getY(), scanPos.getZ());
            } else if (scanPos.getZ() < worldPosition.getZ() + range) {
                scanPos.set(worldPosition.getX() - range, scanPos.getY(), scanPos.getZ() + 1);
            } else {
                scanPos.set(
                        worldPosition.getX() - range,
                        scanPos.getY() - 1,
                        worldPosition.getZ() - range);
                if (scanPos.getY() < level.getMinY()) {
                    mineTarget = new BlockPos(scanPos);
                    return true;
                }
            }
            dischargeScanner(SCAN_ENERGY);
            var state = level.getBlockState(scanPos);
            if (!state.isAir() && canMine(level, scanPos, state)) {
                mineTarget = new BlockPos(scanPos);
                doMine(level, scanPos, state);
                break;
            }
            mineTarget = new BlockPos(scanPos);
        } while (--count > 0 && ElectricItemEnergy.charge(scanner) >= SCAN_ENERGY);
        return true;
    }

    private void dischargeScanner(double amount) {
        var stack = inventory.stack(SCANNER_SLOT);
        ElectricItemEnergy.discharge(stack, amount, Integer.MAX_VALUE, true, false, false);
        inventory.set(SCANNER_SLOT, ItemResource.of(stack), stack.getCount());
    }

    private boolean canMine(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof BucketPickup || !level.getFluidState(pos).isEmpty()) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0.0F) return false;
        var drops = Block.getDrops(state, level, pos, null, null, miningTool());
        if (drops.isEmpty() || state.getBlock() instanceof EntityBlock) return false;
        return evaluateFilter(drops);
    }

    private boolean evaluateFilter(List<ItemStack> drops) {
        for (var drop : drops) {
            for (int slot = FILTER_START; slot < FILTER_START + FILTER_SIZE; slot++) {
                var filter = inventory.stack(slot);
                if (!filter.isEmpty() && filter.getItem() == drop.getItem()) {
                    return !blacklist;
                }
            }
        }
        return blacklist;
    }

    private void doMine(ServerLevel level, BlockPos pos, BlockState state) {
        var drops = Block.getDrops(state, level, pos, null, null, miningTool());
        level.removeBlock(pos, false);
        energy.extract(MINE_ENERGY);
        for (var drop : drops) {
            if (drop.isEmpty()) continue;
            Containers.dropItemStack(
                    level,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5,
                    drop);
        }
        setChanged();
    }

    /** Silk touch switches the sweep to self-drops by mining with an enchanted tool. */
    private ItemStack miningTool() {
        if (!silkTouch) return ItemStack.EMPTY;
        var tool = new ItemStack(Items.IRON_PICKAXE);
        var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(
                level.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.SILK_TOUCH),
                1);
        tool.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        return tool;
    }

    public boolean blacklist() {
        return blacklist;
    }

    public boolean silkTouch() {
        return silkTouch;
    }

    public BlockPos mineTarget() {
        return mineTarget;
    }

    @Override
    public boolean menuAction(int id) {
        boolean active = getBlockState().getValue(MachineBlock.ACTIVE);
        switch (id) {
            case 0 -> mineTarget = null;
            case 1 -> {
                if (!active) blacklist = !blacklist;
            }
            case 2 -> {
                if (!active) silkTouch = !silkTouch;
            }
            default -> {
                return false;
            }
        }
        setChanged();
        return true;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> blacklist ? 1 : 0;
            case 1 -> silkTouch ? 1 : 0;
            default -> 0;
        };
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
        blacklist = input.getBooleanOr("blacklist", true);
        silkTouch = input.getBooleanOr("silkTouch", false);
        mineTarget = input.read("mineTarget", BlockPos.CODEC).orElse(null);
        maxBlockScanCount = input.getIntOr("maxBlockScanCount", 5);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("blacklist", blacklist);
        output.putBoolean("silkTouch", silkTouch);
        if (mineTarget != null) output.store("mineTarget", BlockPos.CODEC, mineTarget);
        output.putInt("maxBlockScanCount", maxBlockScanCount);
    }
}
