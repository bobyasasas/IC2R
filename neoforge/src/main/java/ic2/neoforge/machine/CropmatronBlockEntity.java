package ic2.neoforge.machine;

import ic2.core.energy.VoltageTier;
import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.crop.CropBlockEntity;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.FluidContainerPort;
import ic2.neoforge.transfer.MachineFluidTank;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Legacy TileEntityCropmatron: a tier-1 scan cursor sweeps the 9x3x9 volume every ten ticks,
 * spending 1 EU per visited position and up to 10 EU per applied fertilizer, hydration or
 * weed-ex dose; bare farmland is hydrated instead.
 */
public final class CropmatronBlockEntity extends PoweredBlockEntity implements FluidMachine {
    public static final int FERTILIZER_START = 0,
            FERTILIZER_END = 7,
            EX_INPUT = 7,
            EX_OUTPUT = 8,
            WATER_INPUT = 9,
            WATER_OUTPUT = 10;
    private static final FluidResource WATER = FluidResource.of(Fluids.WATER);
    private static final FluidResource WEED_EX =
            FluidResource.of(ModFluids.FAMILIES.get(FluidDefinition.WEED_EX).source().get());

    private final MachineFluidTank waterTank =
            new MachineFluidTank(2000, this::setChanged, WATER::equals);
    private final MachineFluidTank exTank =
            new MachineFluidTank(2000, this::setChanged, WEED_EX::equals);
    private final ResourceHandler<FluidResource> fluids =
            new ResourcePort<>(
                    new CombinedResourceHandler<>(waterTank, exTank),
                    index -> index == 0 || index == 1,
                    index -> false);
    private int scanX = -4, scanY = -1, scanZ = -4;
    private int storageUpgrades = -1, transformerUpgrades = -1;

    public CropmatronBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(MachineKind.CROPMATRON),
                pos,
                state,
                10000,
                ((MachineBlock) state.getBlock()).kind().slots());
    }

    public MachineFluidTank waterTank() {
        return waterTank;
    }

    public MachineFluidTank exTank() {
        return exTank;
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        if (slot < FERTILIZER_END)
            return resource.is(ModItems.MATERIALS.get(MaterialDefinition.FERTILIZER).get());
        return super.acceptsInventorySlot(slot, resource);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot ->
                        slot < FERTILIZER_END
                                || slot == EX_INPUT && side == Direction.UP
                                || slot == WATER_INPUT && side == Direction.UP,
                slot -> slot == EX_OUTPUT || slot == WATER_OUTPUT,
                (slot, resource) ->
                        slot < FERTILIZER_END
                                ? resource.is(
                                        ModItems.MATERIALS.get(MaterialDefinition.FERTILIZER).get())
                                : FluidContainerPort.accepts(resource));
    }

    @Override
    public ResourceHandler<FluidResource> fluidAutomation(Direction side) {
        return fluids;
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
        if (!inventory.stack(WATER_INPUT).isEmpty())
            ResourceHandlerUtil.move(
                    FluidContainerPort.of(inventory, WATER_INPUT, WATER_OUTPUT),
                    waterTank,
                    WATER::equals,
                    2000,
                    null);
        if (!inventory.stack(EX_INPUT).isEmpty())
            ResourceHandlerUtil.move(
                    FluidContainerPort.of(inventory, EX_INPUT, EX_OUTPUT),
                    exTank,
                    WEED_EX::equals,
                    2000,
                    null);
        refreshUpgrades();
        if (level.getGameTime() % 10L == 0L && energy.stored() >= 31.0) scan();
    }

    /** Legacy scan: one cursor step, then every crop in range is served from the tanks. */
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
        var scan = worldPosition.offset(scanX, scanY, scanZ);
        if (level == null) return;
        if (level.getBlockEntity(scan) instanceof CropBlockEntity crop) {
            if (!inventory.stack(0).isEmpty() && crop.applyFertilizer(false)) {
                energy.extract(10.0);
                try (var transaction = Transaction.openRoot()) {
                    inventory.extract(0, inventory.getResource(0), 1, transaction);
                    transaction.commit();
                }
            }
            // Legacy applies the dose on the tile first and drains the tank to match.
            if (!waterTank.getResource(0).isEmpty()) {
                int applied = crop.applyHydration(waterTank.getAmountAsInt(0), false);
                if (applied > 0) {
                    drain(waterTank, applied);
                    energy.extract(10.0);
                }
            }
            if (!exTank.getResource(0).isEmpty()) {
                int applied = crop.applyWeedEx(exTank.getAmountAsInt(0), false, false, false);
                if (applied > 0) {
                    drain(exTank, applied);
                    energy.extract(10.0);
                }
            }
        } else if (!waterTank.getResource(0).isEmpty() && tryHydrateFarmland(scan)) {
            energy.extract(10.0);
        }
    }

    /** Legacy applyHydration on the tile runs in simulate mode, so the real dose equals it. */
    private void drain(MachineFluidTank tank, int amount) {
        try (var transaction = Transaction.openRoot()) {
            tank.extract(0, tank.getResource(0), amount, transaction);
            transaction.commit();
        }
    }

    private boolean tryHydrateFarmland(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() != Blocks.FARMLAND) return false;
        int moisture = state.getValue(FarmlandBlock.MOISTURE);
        if (moisture >= 7) return false;
        int drainAmount = Math.min(waterTank.getAmountAsInt(0), 7 - moisture);
        if (drainAmount <= 0) return false;
        drain(waterTank, drainAmount);
        level.setBlock(pos, state.setValue(FarmlandBlock.MOISTURE, moisture + drainAmount), 2);
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
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> waterTank.getAmountAsInt(0);
            case 1 -> BuiltInRegistries.FLUID.getId(waterTank.getResource(0).getFluid());
            case 2 -> 2000;
            case 3 -> exTank.getAmountAsInt(0);
            case 4 -> BuiltInRegistries.FLUID.getId(exTank.getResource(0).getFluid());
            case 5 -> 2000;
            default -> 0;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        waterTank.deserialize(input.childOrEmpty("waterTank"));
        exTank.deserialize(input.childOrEmpty("exTank"));
        scanX = input.getIntOr("scanX", -4);
        scanY = input.getIntOr("scanY", -1);
        scanZ = input.getIntOr("scanZ", -4);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        waterTank.serialize(output.child("waterTank"));
        exTank.serialize(output.child("exTank"));
        output.putInt("scanX", scanX);
        output.putInt("scanY", scanY);
        output.putInt("scanZ", scanZ);
    }
}
