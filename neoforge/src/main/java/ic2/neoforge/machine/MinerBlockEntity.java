package ic2.neoforge.machine;

import ic2.neoforge.item.DrillItem;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.ScannerItem;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModMaterialBlocks;
import ic2.neoforge.registration.ModTools;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Ports the legacy miner: one drill, one pipe and one scanner slot above a 15-slot buffer. Without
 * a scanner it digs straight down, consuming pipes and leaving tips; with one it mines towards ore
 * targets layer by layer. Removing the drill withdraws the pipe column. Liquids are only passed
 * when an adjacent pump can take them.
 */
public final class MinerBlockEntity extends PoweredBlockEntity {
    public static final int DRILL_SLOT = 0;
    public static final int PIPE_SLOT = 1;
    public static final int SCANNER_SLOT = 2;
    public static final int BUFFER_START = 3;
    public static final int BUFFER_SIZE = 15;
    private static final int WITHDRAW_ENERGY = 3;
    private static final int WITHDRAW_TICKS = 20;
    private static final int AIR_ENERGY = 3;
    private static final int AIR_TICKS = 20;
    private static final TagKey<Block> ORE_TARGETS =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));

    private int progress;
    private boolean pumpMode;
    private boolean canProvideLiquid;
    private BlockPos liquidPos;
    private Mode lastMode = Mode.NONE;
    private int scannedLevel = -1;
    private int scanRange;
    private int lastX;
    private int lastZ;
    private int sinkTier = kind().electricalTier();

    private enum Mode {
        NONE,
        WITHDRAW,
        MINE_AIR,
        MINE_DRILL,
        MINE_DIAMOND_DRILL,
        MINE_IRIDIUM_DRILL,
        MINE_CUSTOM_DRILL
    }

    private enum Result {
        WORKING,
        DONE,
        TEMP_FAIL,
        PERM_FAIL
    }

    public MinerBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                1000,
                19);
        lastX = pos.getX();
        lastZ = pos.getZ();
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        return switch (slot) {
            case DRILL_SLOT -> resource.getItem() instanceof DrillItem;
            case PIPE_SLOT ->
                    resource.getItem() == ModMaterialBlocks.MINING_PIPE.get().asItem()
                            || resource.getItem() instanceof BlockItem;
            case SCANNER_SLOT -> resource.getItem() instanceof ScannerItem;
            default -> super.acceptsInventorySlot(slot, resource);
        };
    }

    @Override
    public ic2.core.energy.grid.EnergyNode.Terminal energyNode() {
        return ic2.core.energy.grid.EnergyNode.Terminal.sink(energy, 32, sinkTier);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot ->
                        slot >= BUFFER_START
                                && slot < BUFFER_START + BUFFER_SIZE
                                && side != Direction.DOWN,
                slot -> slot >= BUFFER_START && slot < BUFFER_START + BUFFER_SIZE);
    }

    @Override
    public void serverTick(ServerLevel level) {
        chargeTools();
        refreshUpgrades();
        boolean worked = work(level);
        setActive(worked);
    }

    /** Legacy tools feed from the internal buffer: scanner at tier two, drill at tier three. */
    private void chargeTools() {
        for (int slot : new int[] {SCANNER_SLOT, DRILL_SLOT}) {
            var stack = inventory.stack(slot);
            if (stack.isEmpty()
                    || !(stack.getItem() instanceof ic2.neoforge.item.ElectricItem item)) {
                continue;
            }
            double moved =
                    ElectricItemEnergy.charge(
                            stack, energy.stored(), slot == SCANNER_SLOT ? 2 : 3, false, false);
            if (moved > 0) {
                inventory.set(slot, ItemResource.of(stack), stack.getCount());
                energy.extract(moved);
            }
        }
    }

    /**
     * The miner has no fixed process to overclock; storage upgrades widen the buffer and
     * transformers raise the sink tier, matching the legacy upgradable surface.
     */
    private void refreshUpgrades() {
        int storage = 0;
        int transformers = 0;
        for (int slot = kind().upgradeStart(); slot < inventory.size(); slot++) {
            int count = Math.min(64, inventory.getAmountAsInt(slot));
            if (count <= 0) continue;
            if (inventory.getResource(slot).getItem() instanceof UpgradeItem item) {
                switch (item.kind()) {
                    case ENERGY_STORAGE -> storage += count;
                    case TRANSFORMER -> transformers += count;
                    default -> {}
                }
            }
        }
        int tier = Math.min(5, kind().electricalTier() + transformers);
        double capacity = 1000.0 + 10000.0 * storage;
        if (tier != sinkTier || energy.capacity() != capacity) {
            sinkTier = tier;
            energy.resize(capacity);
            setChanged();
        }
    }

    private boolean work(ServerLevel level) {
        var operatingPos = operationPos(level);
        if (inventory.stack(DRILL_SLOT).isEmpty()) return withDrawPipe(level, operatingPos);
        if (operatingPos.getY() < level.getMinY()) return false;
        var state = level.getBlockState(operatingPos);
        if (state.getBlock() != ModMaterialBlocks.MINING_PIPE_TIP.get()) {
            return operatingPos.getY() > level.getMinY()
                    && digDown(level, operatingPos, state, false);
        }
        var result = mineLevel(level, operatingPos.getY());
        if (result == Result.DONE) {
            operatingPos.move(Direction.DOWN);
            return digDown(level, operatingPos, level.getBlockState(operatingPos), true);
        }
        return result == Result.WORKING;
    }

    /** First non-pipe cell under the miner, i.e. where the next pipe or the tip sits. */
    private BlockPos.MutableBlockPos operationPos(ServerLevel level) {
        var ret = worldPosition.mutable().move(Direction.DOWN);
        while (ret.getY() >= level.getMinY()) {
            if (level.getBlockState(ret).getBlock() != ModMaterialBlocks.MINING_PIPE.get())
                return ret;
            ret.move(Direction.DOWN);
        }
        return ret;
    }

    private boolean withDrawPipe(ServerLevel level, BlockPos.MutableBlockPos operatingPos) {
        if (lastMode != Mode.WITHDRAW) {
            lastMode = Mode.WITHDRAW;
            progress = 0;
        }
        if (operatingPos.getY() < level.getMinY()
                || level.getBlockState(operatingPos).getBlock()
                        != ModMaterialBlocks.MINING_PIPE_TIP.get()) {
            operatingPos.move(Direction.UP);
        }
        if (operatingPos.getY() != worldPosition.getY() && energy.stored() >= WITHDRAW_ENERGY) {
            if (progress < WITHDRAW_TICKS) {
                energy.extract(WITHDRAW_ENERGY);
                progress++;
            } else {
                progress = 0;
                removePipe(level, operatingPos);
            }
            return true;
        }
        return false;
    }

    private void removePipe(ServerLevel level, BlockPos.MutableBlockPos operatingPos) {
        level.removeBlock(operatingPos, false);
        storeDrop(level, new ItemStack(ModMaterialBlocks.MINING_PIPE.get().asItem()));
        var pipeStack = inventory.stack(PIPE_SLOT);
        if (!pipeStack.isEmpty()
                && pipeStack.getItem() != ModMaterialBlocks.MINING_PIPE.get().asItem()
                && takeFromPipeSlot()) {
            // Players can leave a filler block in the pipe slot to backfill withdrawn holes.
            if (pipeStack.getItem() instanceof BlockItem blockItem) {
                blockItem.place(
                        new DirectionalPlaceContext(
                                level,
                                operatingPos.above(),
                                Direction.DOWN,
                                pipeStack,
                                Direction.UP));
            }
        }
    }

    private boolean digDown(
            ServerLevel level,
            BlockPos.MutableBlockPos operatingPos,
            BlockState state,
            boolean removeTipAbove) {
        var pipeStack = inventory.stack(PIPE_SLOT);
        if (pipeStack.isEmpty()
                || pipeStack.getItem() != ModMaterialBlocks.MINING_PIPE.get().asItem()) {
            return false;
        }
        if (operatingPos.getY() < level.getMinY()) {
            if (removeTipAbove) {
                level.setBlockAndUpdate(
                        operatingPos.set(operatingPos.getX(), level.getMinY(), operatingPos.getZ()),
                        ModMaterialBlocks.MINING_PIPE.get().defaultBlockState());
            }
            return false;
        }
        var result = mineBlock(level, operatingPos, state);
        if (result != Result.TEMP_FAIL && result != Result.PERM_FAIL) {
            if (result == Result.DONE) {
                if (removeTipAbove) {
                    level.setBlockAndUpdate(
                            operatingPos.above(),
                            ModMaterialBlocks.MINING_PIPE.get().defaultBlockState());
                }
                takeFromPipeSlot();
                level.setBlockAndUpdate(
                        operatingPos, ModMaterialBlocks.MINING_PIPE_TIP.get().defaultBlockState());
            }
            return true;
        }
        if (removeTipAbove) {
            level.setBlockAndUpdate(
                    operatingPos.move(Direction.UP),
                    ModMaterialBlocks.MINING_PIPE.get().defaultBlockState());
        }
        return false;
    }

    private Result mineLevel(ServerLevel level, int y) {
        var scannerStack = inventory.stack(SCANNER_SLOT);
        if (scannerStack.isEmpty()) return Result.DONE;
        if (scannedLevel != y) {
            scanRange = ((ScannerItem) scannerStack.getItem()).startLayerScan(scannerStack);
            inventory.set(SCANNER_SLOT, ItemResource.of(scannerStack), scannerStack.getCount());
        }
        if (scanRange <= 0) return Result.TEMP_FAIL;
        scannedLevel = y;
        for (int x = worldPosition.getX() - scanRange; x <= worldPosition.getX() + scanRange; x++) {
            for (int z = worldPosition.getZ() - scanRange;
                    z <= worldPosition.getZ() + scanRange;
                    z++) {
                var target = new BlockPos(x, y, z);
                var state = level.getBlockState(target);
                boolean valid = isOreTarget(state) && canMine(level, target, state);
                if (!valid && pumpMode) {
                    var liquid = level.getFluidState(target);
                    if (!liquid.isEmpty() && isPumpConnected(level, target)) valid = true;
                }
                if (valid) {
                    var result = mineTowards(level, target);
                    if (result == Result.DONE) return Result.WORKING;
                    if (result != Result.PERM_FAIL) return result;
                }
            }
        }
        return Result.DONE;
    }

    private Result mineTowards(ServerLevel level, BlockPos destination) {
        int dx = Math.abs(destination.getX() - worldPosition.getX());
        int sx = worldPosition.getX() < destination.getX() ? 1 : -1;
        int dz = -Math.abs(destination.getZ() - worldPosition.getZ());
        int sz = worldPosition.getZ() < destination.getZ() ? 1 : -1;
        int err = dx + dz;
        var target = new BlockPos.MutableBlockPos();
        int cx = worldPosition.getX();
        int cz = worldPosition.getZ();
        while (cx != destination.getX() || cz != destination.getZ()) {
            boolean isCurrentPos = cx == lastX && cz == lastZ;
            int e2 = 2 * err;
            if (e2 > dz) {
                err += dz;
                cx += sx;
            } else if (e2 < dx) {
                err += dx;
                cz += sz;
            }
            target.set(cx, destination.getY(), cz);
            var state = level.getBlockState(target);
            boolean isBlocking;
            if (isCurrentPos) {
                isBlocking = true;
            } else if (!state.isAir()) {
                var liquid = level.getFluidState(target);
                isBlocking =
                        liquid.isEmpty()
                                || liquid.isSource()
                                || pumpMode && isPumpConnected(level, target);
            } else {
                isBlocking = false;
            }
            if (isBlocking) {
                var result = mineBlock(level, target, state);
                if (result == Result.DONE) {
                    lastX = cx;
                    lastZ = cz;
                }
                return result;
            }
        }
        lastX = worldPosition.getX();
        lastZ = worldPosition.getZ();
        return Result.DONE;
    }

    private Result mineBlock(ServerLevel level, BlockPos target, BlockState state) {
        boolean isAirBlock = state.isAir();
        if (!isAirBlock) {
            var liquid = level.getFluidState(target);
            if (!liquid.isEmpty()) {
                liquidPos = target.immutable();
                canProvideLiquid = true;
                boolean pumpable = liquid.isSource() || pumpMode && isPumpConnected(level, target);
                if (pumpable) handToPump(level);
                return !pumpMode && !canMine(level, target, state)
                        ? Result.PERM_FAIL
                        : Result.TEMP_FAIL;
            }
            if (!canMine(level, target, state)) return Result.PERM_FAIL;
        }
        canProvideLiquid = false;
        var drill = inventory.stack(DRILL_SLOT);
        var drillItem = (DrillItem) drill.getItem();
        int energyPerTick;
        int duration;
        Mode mode;
        if (isAirBlock) {
            mode = Mode.MINE_AIR;
            energyPerTick = AIR_ENERGY;
            duration = AIR_TICKS;
        } else if (drillItem == ModTools.DRILL.get()) {
            mode = Mode.MINE_DRILL;
            energyPerTick = drillItem.minerEnergyPerTick();
            duration = drillItem.minerDuration();
        } else if (drillItem == ModTools.DIAMOND_DRILL.get()) {
            mode = Mode.MINE_DIAMOND_DRILL;
            energyPerTick = drillItem.minerEnergyPerTick();
            duration = drillItem.minerDuration();
        } else if (drillItem == ModTools.IRIDIUM_DRILL.get()) {
            mode = Mode.MINE_IRIDIUM_DRILL;
            energyPerTick = drillItem.minerEnergyPerTick();
            duration = drillItem.minerDuration();
        } else {
            mode = Mode.MINE_CUSTOM_DRILL;
            energyPerTick = drillItem.minerEnergyPerTick();
            duration = drillItem.minerDuration();
        }
        if (lastMode != mode) {
            lastMode = mode;
            progress = 0;
        }
        if (progress < duration) {
            if (energy.stored() >= energyPerTick) {
                energy.extract(energyPerTick);
                progress++;
                return Result.WORKING;
            }
        } else if (isAirBlock || harvestBlock(level, target, state)) {
            progress = 0;
            return Result.DONE;
        }
        return Result.TEMP_FAIL;
    }

    private boolean harvestBlock(ServerLevel level, BlockPos target, BlockState state) {
        double energyCost = 2.0 * (worldPosition.getY() - target.getY());
        if (energy.stored() < energyCost) return false;
        var drill = inventory.stack(DRILL_SLOT);
        var drillItem = (DrillItem) drill.getItem();
        if (!drillItem.canUse(drill)) return false;
        ElectricItemEnergy.discharge(
                drill,
                drillItem.harvestEnergyCost(),
                drillItem.specification().tier(),
                true,
                false,
                false);
        inventory.set(DRILL_SLOT, ItemResource.of(drill), drill.getCount());
        energy.extract(energyCost);
        var tool = drill.copy();
        if (lastMode == Mode.MINE_IRIDIUM_DRILL) {
            tool.set(DataComponents.ENCHANTMENTS, fortuneIII(level));
        }
        for (var drop : Block.getDrops(state, level, target, null, null, tool)) {
            storeDrop(level, drop);
        }
        level.removeBlock(target, false);
        return true;
    }

    private ItemEnchantments fortuneIII(ServerLevel level) {
        var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        var holder =
                level.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE);
        enchantments.set(holder, 3);
        return enchantments.toImmutable();
    }

    private void storeDrop(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) return;
        try (var transaction = Transaction.openRoot()) {
            for (int slot = BUFFER_START; slot < BUFFER_START + BUFFER_SIZE; slot++) {
                int inserted =
                        inventory.insert(
                                slot, ItemResource.of(stack), stack.getCount(), transaction);
                if (inserted > 0) {
                    stack.shrink(inserted);
                    if (stack.isEmpty()) {
                        transaction.commit();
                        return;
                    }
                }
            }
            transaction.commit();
        }
        Containers.dropItemStack(
                level,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5,
                stack);
    }

    private boolean takeFromPipeSlot() {
        var resource = inventory.getResource(PIPE_SLOT);
        if (resource.isEmpty() || inventory.getAmountAsInt(PIPE_SLOT) <= 0) return false;
        try (var transaction = Transaction.openRoot()) {
            int extracted = inventory.extract(PIPE_SLOT, resource, 1, transaction);
            if (extracted == 1) {
                transaction.commit();
                return true;
            }
        }
        return false;
    }

    private void handToPump(ServerLevel level) {
        for (var direction : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(direction))
                    instanceof PumpBlockEntity pump) {
                pump.requestDrain(liquidPos);
                return;
            }
        }
    }

    /** Liquids are only targets when an adjacent pump can actually take them. */
    private boolean isPumpConnected(ServerLevel level, BlockPos target) {
        for (var direction : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(direction))
                            instanceof PumpBlockEntity pump
                    && pump.canDrain(level)) {
                return true;
            }
        }
        return false;
    }

    private boolean canMine(ServerLevel level, BlockPos target, BlockState state) {
        if (state.isAir()) return true;
        var block = state.getBlock();
        if (block == ModMaterialBlocks.MINING_PIPE.get()
                || block == ModMaterialBlocks.MINING_PIPE_TIP.get()
                || block == Blocks.CHEST) {
            return false;
        }
        if (!level.getFluidState(target).isEmpty() && isPumpConnected(level, target)) return true;
        if (state.getDestroySpeed(level, target) < 0.0F) return false;
        if (!state.requiresCorrectToolForDrops()) return true;
        if (block == Blocks.COBWEB) return true;
        var drill = inventory.stack(DRILL_SLOT);
        return !drill.isEmpty() && drill.getItem().isCorrectToolForDrops(drill, state);
    }

    private boolean isOreTarget(BlockState state) {
        return state.is(ORE_TARGETS)
                || state.getBlock() == net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS;
    }

    public boolean pumpMode() {
        return pumpMode;
    }

    @Override
    public boolean menuAction(int id) {
        if (id == 0) {
            pumpMode = !pumpMode;
            setChanged();
            return true;
        }
        return false;
    }

    @Override
    public int progress() {
        return progress;
    }

    @Override
    public int progressMaximum() {
        return switch (lastMode) {
            case WITHDRAW -> WITHDRAW_TICKS;
            case MINE_AIR -> AIR_TICKS;
            case MINE_DRILL -> ModTools.DRILL.get().minerDuration();
            case MINE_DIAMOND_DRILL -> ModTools.DIAMOND_DRILL.get().minerDuration();
            case MINE_IRIDIUM_DRILL -> ModTools.IRIDIUM_DRILL.get().minerDuration();
            default -> 0;
        };
    }

    @Override
    public int menuValue(int index) {
        return index == 0 && pumpMode ? 1 : 0;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getIntOr("progress", 0);
        pumpMode = input.getBooleanOr("pumpMode", false);
        lastMode = parseMode(input.getStringOr("lastMode", "NONE"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("progress", progress);
        output.putBoolean("pumpMode", pumpMode);
        output.putString("lastMode", lastMode.name());
    }

    private static Mode parseMode(String name) {
        try {
            return Mode.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return Mode.NONE;
        }
    }
}
