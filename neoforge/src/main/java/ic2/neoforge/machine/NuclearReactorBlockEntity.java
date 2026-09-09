package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.item.ReactorComponent;
import ic2.neoforge.item.ReactorHost;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.MachineFluidTank;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * EU-mode nuclear reactor core, first migration slice: a 3-column by 6-row component grid running
 * the legacy two-pass cycle every 20 ticks. Fuel rods pulse and deplete, vent components absorb
 * their heat, heat above the limit melts the core down. Chamber columns, reflector/switch component
 * interactions, fluid cooling mode and the access ports land in later slices.
 */
public final class NuclearReactorBlockEntity extends PoweredBlockEntity implements ReactorHost {
    public static final int BASE_COLUMNS = 3;
    public static final int GRID_COLUMNS = 9;
    public static final int GRID_ROWS = 6;
    public static final int CYCLE_TICKS = 20;
    private static final int BASE_MAX_HEAT = 10000;

    public static final int COOLANT_TANK_CAPACITY = 10000;

    /** Legacy default: 40 EU-worth of rod output converts one mB of coolant (outputModifier 1). */
    private static final int HU_OUTPUT_MODIFIER = 40;

    private final MachineFluidTank coolantTank =
            new MachineFluidTank(
                    COOLANT_TANK_CAPACITY,
                    this::setChanged,
                    resource ->
                            resource.is(
                                    ModFluids.FAMILIES
                                            .get(FluidDefinition.COOLANT)
                                            .source()
                                            .get()));
    private final MachineFluidTank hotCoolantTank =
            new MachineFluidTank(
                    COOLANT_TANK_CAPACITY,
                    this::setChanged,
                    resource ->
                            resource.is(
                                    ModFluids.FAMILIES
                                            .get(FluidDefinition.HOT_COOLANT)
                                            .source()
                                            .get()));

    private int heat;
    private int maxHeat = BASE_MAX_HEAT;
    private float hem = 1.0F;
    private float output;
    private int cycleTicker;
    private boolean producing;
    private int emitHeatBuffer;
    private boolean fluidCooled;

    public NuclearReactorBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                100000,
                54);
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        // LV source: the legacy reactor emits its output as LV packets.
        return EnergyNode.Terminal.source(energy, 32, 30);
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        return resource.getItem() instanceof ic2.neoforge.item.ReactorComponent;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(inventory, slot -> true, slot -> true);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (++cycleTicker % CYCLE_TICKS != 0) return;
        fluidCooled = fluidPortNear(level);
        // The legacy loop always runs the two passes; the rods themselves only pulse while the
        // reactor receives a redstone signal.
        ejectInactiveColumns(level);
        processChambers();
        if (meltDown(level)) return;
        producing = heat >= 1000 || output > 0.0F;
        if (fluidCooled) {
            convertEmitHeatToHotCoolant();
            producing = heat >= 1000;
        } else if (output > 0) {
            energy.insert(output * CYCLE_TICKS);
        }
        setActive(producing);
    }

    /**
     * Fluid mode: the pass heat becomes hot coolant at 40 HU per mB (legacy huOutputModifier);
     * whatever the tanks cannot absorb heats the core instead.
     */
    private void convertEmitHeatToHotCoolant() {
        int huOutput = HU_OUTPUT_MODIFIER * emitHeatBuffer;
        emitHeatBuffer = 0;
        if (huOutput <= 0) return;
        int hotRoom = hotCoolantTank.getAmountAsInt(0);
        int converted = Math.min(huOutput, COOLANT_TANK_CAPACITY - hotRoom);
        if (converted <= 0) {
            heat += huOutput;
            return;
        }
        try (var transaction = Transaction.openRoot()) {
            if (hotCoolantTank.insert(0, hotCoolant(), converted, transaction) != converted) {
                return;
            }
            coolantTank.extract(0, coolant(), converted, transaction);
            transaction.commit();
        }
        int unconverted = huOutput - converted;
        if (unconverted > 0) heat += unconverted;
    }

    private FluidResource coolant() {
        return FluidResource.of(ModFluids.FAMILIES.get(FluidDefinition.COOLANT).source().get());
    }

    private FluidResource hotCoolant() {
        return FluidResource.of(ModFluids.FAMILIES.get(FluidDefinition.HOT_COOLANT).source().get());
    }

    /** Fluid mode engages when a reactor fluid port sits next to the core. */
    private boolean fluidPortNear(ServerLevel level) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(direction))
                            instanceof ReactorFluidPortBlockEntity port
                    && port.findReactor() == this) return true;
        }
        return false;
    }

    /** Coolant in / hot coolant out, exposed through reactor fluid ports. */
    public ResourceHandler<FluidResource> coolantTanks() {
        return new ResourceHandler<>() {
            @Override
            public int size() {
                return 2;
            }

            @Override
            public FluidResource getResource(int index) {
                return index == 0 ? coolant() : hotCoolant();
            }

            @Override
            public long getAmountAsLong(int index) {
                return index == 0
                        ? coolantTank.getAmountAsInt(0)
                        : hotCoolantTank.getAmountAsInt(0);
            }

            @Override
            public long getCapacityAsLong(int index, FluidResource resource) {
                return COOLANT_TANK_CAPACITY;
            }

            @Override
            public boolean isValid(int index, FluidResource resource) {
                return resource.is(index == 0 ? coolant().getFluid() : hotCoolant().getFluid());
            }

            @Override
            public int insert(
                    int index,
                    FluidResource resource,
                    int amount,
                    net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
                if (index != 0) return 0;
                return coolantTank.insert(0, resource, amount, transaction);
            }

            @Override
            public int extract(
                    int index,
                    FluidResource resource,
                    int amount,
                    net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
                if (index != 1) return 0;
                return hotCoolantTank.extract(0, resource, amount, transaction);
            }
        };
    }

    public boolean fluidCooled() {
        return fluidCooled;
    }

    public int emitBuffer() {
        return emitHeatBuffer;
    }

    public int coolantAmount() {
        return coolantTank.getAmountAsInt(0);
    }

    public int hotCoolantAmount() {
        return hotCoolantTank.getAmountAsInt(0);
    }

    /** Each adjacent chamber widens the grid by one column, exactly like the legacy count. */
    public int columns() {
        int cols = BASE_COLUMNS;
        if (getLevel() instanceof ServerLevel level) {
            for (Direction direction : Direction.values()) {
                if (level.getBlockEntity(worldPosition.relative(direction))
                                instanceof ReactorChamberBlockEntity chamber
                        && chamber.findReactor() == this) cols++;
            }
        }
        return Math.min(cols, GRID_COLUMNS);
    }

    /**
     * Items beyond the active columns are ejected at cycle start (legacy dropAllUnfittingStuff).
     */
    private void ejectInactiveColumns(ServerLevel level) {
        for (int y = 0; y < GRID_ROWS; y++) {
            for (int x = columns(); x < GRID_COLUMNS; x++) {
                var stack = inventory.stack(y * GRID_COLUMNS + x);
                if (stack != null && !stack.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(
                            level,
                            worldPosition.getX() + 0.5,
                            worldPosition.getY() + 0.5,
                            worldPosition.getZ() + 0.5,
                            stack);
                    // setItemAt gates on active columns; ejecting must write the raw slot.
                    inventory.set(y * GRID_COLUMNS + x, ItemResource.EMPTY, 0);
                }
            }
        }
    }

    private void processChambers() {
        output = 0.0F;
        maxHeat = BASE_MAX_HEAT;
        hem = 1.0F;
        int columns = columns();
        for (int pass = 0; pass < 2; pass++) {
            boolean heatRun = pass == 0;
            for (int y = 0; y < GRID_ROWS; y++) {
                for (int x = 0; x < columns; x++) {
                    var stack = getItemAt(x, y);
                    if (stack == null || !(stack.getItem() instanceof ReactorComponent component))
                        continue;
                    component.processChamber(stack, this, x, y, heatRun);
                    var current = getItemAt(x, y);
                    // setItemAt replaced the slot (depleted swap): keep it. Otherwise persist the
                    // component's mutation (depletion ticks) on the stored stack.
                    if (current.getItem() == stack.getItem()
                            && !net.minecraft.world.item.ItemStack.matches(current, stack))
                        setItemAt(x, y, stack);
                }
            }
        }
    }

    /** Legacy heat effects: melt-down at 100%, fire and lava at 85%, radiation at 70%. */
    private boolean meltDown(ServerLevel level) {
        if (heat < 4000) return false;
        float power = (float) heat / maxHeat;
        if (power >= 1.0F) {
            clearGrid();
            setActive(false);
            getLevel().removeBlock(worldPosition, false);
            ic2.neoforge.explosion.HeatExplosion.trigger(level, worldPosition, 10, 0.01F, true);
            heat = 0;
            return true;
        }
        // Surface heat effects roll every cycle once the core passes each threshold.
        if (power >= 0.85F
                && hem > 0.0F
                && net.minecraft.util.RandomSource.create().nextFloat() <= 0.2F * hem)
            burnOrMelt(level, randomCoordination(level, 2));
        if (power >= 0.5F
                && hem > 0.0F
                && net.minecraft.util.RandomSource.create().nextFloat() <= hem)
            evaporateWater(level, randomCoordination(level, 2));
        if (power >= 0.4F
                && hem > 0.0F
                && net.minecraft.util.RandomSource.create().nextFloat() <= hem)
            igniteFlammable(level, randomCoordination(level, 2));
        producing = heat >= 1000 || output > 0.0F;
        return false;
    }

    private void burnOrMelt(ServerLevel level, BlockPos target) {
        if (target == null) return;
        var state = level.getBlockState(target);
        if (state.isAir()) {
            level.setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
        } else if (state.getDestroySpeed(level, target) >= 0.0F
                && level.getBlockEntity(target) == null) {
            if (!state.canOcclude() && !state.getFluidState().is(Fluids.LAVA))
                level.setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
            else
                level.setBlockAndUpdate(
                        target, Fluids.LAVA.defaultFluidState().createLegacyBlock());
        }
    }

    /** Test/diagnostic hook: rolls the surface effects once at the current heat. */
    public void rollSurfaceEffectsOnce(ServerLevel level) {
        float power = (float) heat / maxHeat;
        if (power >= 0.85F && net.minecraft.util.RandomSource.create().nextFloat() <= 0.2F * hem)
            burnOrMelt(level, randomCoordination(level, 2));
        if (power >= 0.5F && net.minecraft.util.RandomSource.create().nextFloat() <= hem)
            evaporateWater(level, randomCoordination(level, 2));
        if (power >= 0.4F && net.minecraft.util.RandomSource.create().nextFloat() <= hem)
            igniteFlammable(level, randomCoordination(level, 2));
    }

    private void evaporateWater(ServerLevel level, BlockPos target) {
        if (target == null) return;
        // Level.removeBlock would re-create the water legacy block; evaporation sets air.
        if (level.getFluidState(target).is(Fluids.WATER))
            level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
    }

    private void igniteFlammable(ServerLevel level, BlockPos target) {
        if (target == null) return;
        if (level.getBlockEntity(target) != null) return;
        var state = level.getBlockState(target);
        if (state.isFlammable(level, target, Direction.UP))
            level.setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
    }

    /** Random block near the core, never the core itself (legacy getRandCoordination). */
    private BlockPos randomCoordination(ServerLevel level, int radius) {
        var rng = net.minecraft.util.RandomSource.create();
        BlockPos ret;
        do {
            ret =
                    worldPosition.offset(
                            rng.nextInt(2 * radius + 1) - radius,
                            rng.nextInt(2 * radius + 1) - radius,
                            rng.nextInt(2 * radius + 1) - radius);
        } while (ret.equals(worldPosition));
        return ret;
    }

    /** A melt-down vaporises the whole charge; nothing drops, exactly like the legacy core. */
    private void clearGrid() {
        for (int slot = 0; slot < inventory.size(); slot++)
            inventory.set(slot, ItemResource.EMPTY, 0);
    }

    @Override
    public boolean produceEnergy() {
        return getLevel() != null && getLevel().hasNeighborSignal(worldPosition);
    }

    @Override
    public int getHeat() {
        return heat;
    }

    @Override
    public void setHeat(int heat) {
        this.heat = heat;
    }

    @Override
    public int addHeat(int amount) {
        heat += amount;
        return heat;
    }

    @Override
    public int getMaxHeat() {
        return maxHeat;
    }

    @Override
    public void setMaxHeat(int maxHeat) {
        this.maxHeat = maxHeat;
    }

    @Override
    public float getHeatEffectModifier() {
        return hem;
    }

    @Override
    public void setHeatEffectModifier(float hem) {
        this.hem = hem;
    }

    @Override
    public ItemStack getItemAt(int x, int y) {
        return x >= 0 && x < columns() && y >= 0 && y < GRID_ROWS
                ? inventory.stack(x + y * GRID_COLUMNS)
                : null;
    }

    @Override
    public void setItemAt(int x, int y, ItemStack stack) {
        if (x >= 0 && x < columns() && y >= 0 && y < GRID_ROWS) {
            if (stack.isEmpty()) inventory.set(x + y * GRID_COLUMNS, ItemResource.EMPTY, 0);
            else inventory.set(x + y * GRID_COLUMNS, ItemResource.of(stack), stack.getCount());
        }
    }

    @Override
    public float getReactorEnergyOutput() {
        return output;
    }

    @Override
    public void addOutput(float energy) {
        output += energy;
    }

    @Override
    public void addEmitHeat(int heat) {
        emitHeatBuffer += heat;
    }

    @Override
    public int menuValue(int index) {
        return index == 0 ? Float.floatToIntBits((float) heat / maxHeat) : 0;
    }

    @Override
    public int progress() {
        return heat;
    }

    @Override
    public int progressMaximum() {
        return maxHeat;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        heat = Math.clamp(input.getIntOr("heat", 0), 0, Integer.MAX_VALUE);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("heat", heat);
    }
}
