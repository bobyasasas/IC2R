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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
 * their heat, heat above the limit melts the core down. Fluid-cooled mode engages only on the
 * legacy full-size multiblock: six chamber columns (6x9 grid), a complete reactor-vessel shell at
 * Chebyshev radius 2 and no competing full-size fluid core nearby.
 */
public final class NuclearReactorBlockEntity extends PoweredBlockEntity implements ReactorHost {
    public static final int BASE_COLUMNS = 3;
    public static final int GRID_COLUMNS = 9;
    public static final int GRID_ROWS = 6;
    public static final int CYCLE_TICKS = 20;
    private static final int BASE_MAX_HEAT = 10000;

    /** Legacy container fluid slots appended after the 9x6 component grid (raw indices 54-57). */
    public static final int COOLANT_INPUT = 54;
    public static final int COOLANT_OUTPUT = 55;
    public static final int HOT_COOLANT_INPUT = 56;
    public static final int HOT_COOLANT_OUTPUT = 57;
    public static final int FLUID_SLOTS = 58;

    public static final int COOLANT_TANK_CAPACITY = 10000;

    /** Legacy default: 40 EU-worth of rod output converts one heat point (outputModifier 1). */
    private static final int HU_OUTPUT_MODIFIER = 40;

    /** Legacy coolant heat-exchange property: 20 HU convert one mB of coolant. */
    private static final int HU_PER_MB = 20;

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
                // 54 component cells plus the four legacy container slots.
                FLUID_SLOTS);
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        // LV source: the legacy reactor emits its output as LV packets.
        return EnergyNode.Terminal.source(energy, 32, 30);
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        if (slot == COOLANT_INPUT)
            return ic2.neoforge.menu.NuclearReactorMenu.holdsFluid(resource, coolant().getFluid());
        if (slot == HOT_COOLANT_INPUT)
            return ic2.neoforge.menu.NuclearReactorMenu.takesFluid(
                    resource, hotCoolant().getFluid());
        // The container outputs are legacy InvSlotOutput: any item may land there through the
        // internal exchange or automation; hand insertion stays blocked by the menu slot rule.
        if (slot == COOLANT_OUTPUT || slot == HOT_COOLANT_OUTPUT) return true;
        return resource.getItem() instanceof ic2.neoforge.item.ReactorComponent component
                && component.canBePlacedIn(resource.toStack(), this);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        // Legacy slot access: the component grid is IO, the drain/fill inputs are insert-only
        // and the container outputs are extract-only.
        return new ResourcePort<>(
                inventory,
                slot ->
                        slot < COOLANT_INPUT
                                || slot == COOLANT_OUTPUT
                                || slot == HOT_COOLANT_OUTPUT,
                slot ->
                        slot < COOLANT_INPUT
                                || slot == COOLANT_INPUT
                                || slot == HOT_COOLANT_INPUT);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (++cycleTicker % CYCLE_TICKS != 0) return;
        fluidCooled = isFluidReactor(level);
        // The legacy loop always runs the two passes; the rods themselves only pulse while the
        // reactor receives a redstone signal.
        ejectInactiveColumns(level);
        processChambers();
        if (meltDown(level)) return;
        producing = heat >= 1000 || output > 0.0F;
        if (fluidCooled) {
            processFluidSlots();
            convertEmitHeatToHotCoolant();
            producing = heat >= 1000;
        } else if (output > 0) {
            energy.insert(output * CYCLE_TICKS);
        }
        setActive(producing);
    }

    /**
     * Legacy processFluidsSlots: coolant containers drain into the coolant tank and come back as
     * empties in the coolant output; containers in the hot coolant input fill from the hot tank
     * and move filled into the hot output.
     */
    private void processFluidSlots() {
        net.neoforged.neoforge.transfer.ResourceHandlerUtil.move(
                ic2.neoforge.transfer.FluidContainerPort.of(
                        inventory, COOLANT_INPUT, COOLANT_OUTPUT),
                coolantTank,
                resource -> resource.is(coolant().getFluid()),
                COOLANT_TANK_CAPACITY,
                null);
        net.neoforged.neoforge.transfer.ResourceHandlerUtil.move(
                hotCoolantTank,
                ic2.neoforge.transfer.FluidContainerPort.of(
                        inventory, HOT_COOLANT_INPUT, HOT_COOLANT_OUTPUT),
                resource -> resource.is(hotCoolant().getFluid()),
                COOLANT_TANK_CAPACITY,
                null);
    }

    /**
     * Fluid mode: the pass heat becomes hot coolant at 20 HU per mB (legacy hot-coolant heat
     * exchange property); only the coolant actually drained converts, and whatever the tanks cannot
     * absorb heats the core instead (legacy addHeat of the unconverted remainder).
     */
    private void convertEmitHeatToHotCoolant() {
        int huOutput = HU_OUTPUT_MODIFIER * emitHeatBuffer + rciOutputBonus() * 100;
        emitHeatBuffer = 0;
        if (huOutput <= 0) return;
        int hotRoom = COOLANT_TANK_CAPACITY - hotCoolantTank.getAmountAsInt(0);
        if (hotRoom > 0) {
            try (var transaction = Transaction.openRoot()) {
                int drained =
                        coolantTank.extract(
                                0, coolant(), Math.min(huOutput / HU_PER_MB, hotRoom), transaction);
                if (drained > 0) {
                    if (hotCoolantTank.insert(0, hotCoolant(), drained, transaction) != drained) {
                        return;
                    }
                    huOutput -= drained * HU_PER_MB;
                }
                transaction.commit();
            }
        }
        if (huOutput > 0) heat += huOutput / HU_OUTPUT_MODIFIER;
    }

    private FluidResource coolant() {
        return FluidResource.of(ModFluids.FAMILIES.get(FluidDefinition.COOLANT).source().get());
    }

    private FluidResource hotCoolant() {
        return FluidResource.of(ModFluids.FAMILIES.get(FluidDefinition.HOT_COOLANT).source().get());
    }

    /**
     * Legacy isFluidReactor: the 6x9 full-size grid, a complete vessel shell and no competing
     * full-size fluid core within range 4. Fluid ports ride along as the tank windows.
     */
    private boolean isFluidReactor(ServerLevel level) {
        return isFullSize() && hasVesselRing(level) && !conflictingFluidReactorNear(level);
    }

    /** Another full-size fluid reactor within Chebyshev range 4 blocks fluid mode (legacy). */
    private boolean conflictingFluidReactorNear(ServerLevel level) {
        for (int dx = -4; dx <= 4; dx++)
            for (int dy = -4; dy <= 4; dy++)
                for (int dz = -4; dz <= 4; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    if (level.getBlockEntity(worldPosition.offset(dx, dy, dz))
                                    instanceof NuclearReactorBlockEntity other
                            && other.isFullSize()
                            && other.hasVesselRing(level)) return true;
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

    /**
     * Direct hot coolant tank access for tests and tooling; in play the tank only fills through
     * heat conversion, the fluid ports see it as extract-only via {@link #coolantTanks()}.
     */
    public MachineFluidTank hotCoolantTankHandler() {
        return hotCoolantTank;
    }

    /** The legacy reactor opens its own screen handler, not the shared machine menu. */
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ic2.neoforge.menu.NuclearReactorMenu(id, playerInventory, this);
    }

    /**
     * RCI (RSH/LZH condensator injector) output bonus stacks per injector. Legacy RCIs face the
     * core or a chamber of the structure; in the full-size build the six core faces are all
     * chambers, so injectors behind a chamber wall still count.
     */
    public int rciOutputBonus() {
        if (!(getLevel() instanceof ServerLevel level)) return 0;
        var injectors = new java.util.HashSet<ReactorRciBlockEntity>();
        for (Direction direction : Direction.values()) {
            var neighborPos = worldPosition.relative(direction);
            var neighbor = level.getBlockEntity(neighborPos);
            if (neighbor instanceof ReactorRciBlockEntity rci && rci.findReactor(level) == this) {
                injectors.add(rci);
            } else if (neighbor instanceof ReactorChamberBlockEntity chamber
                    && chamber.findReactor() == this) {
                for (Direction rciSide : Direction.values()) {
                    if (level.getBlockEntity(chamber.getBlockPos().relative(rciSide))
                            instanceof ReactorRciBlockEntity rci) injectors.add(rci);
                }
            }
        }
        return injectors.size() * 10;
    }

    /** Full 6×9 form: chamber columns reach nine. */
    public boolean isFullSize() {
        return columns() >= GRID_COLUMNS;
    }

    /**
     * The vessel shell at Chebyshev radius 2 must be casing or wall-type vessel pieces (legacy
     * isFluidChamberBlock: the reactor vessel block or any isWall chamber — hatches and ports
     * qualify, fuel-rod chambers do not).
     */
    public boolean hasVesselRing(ServerLevel level) {
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = -2; dy <= 2; dy++)
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) != 2) continue;
                    if (!isVesselWallBlock(level, worldPosition.offset(dx, dy, dz))) return false;
                }
        return true;
    }

    /** Legacy isFluidChamberBlock: vessel casing or a wall-type vessel piece. */
    static boolean isVesselWallBlock(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos)
                .is(
                        ic2.neoforge.registration.ModMaterialBlocks.MATERIALS
                                .get("reactor_vessel")
                                .get())) return true;
        return level.getBlockEntity(pos) instanceof ReactorAccessHatchBlockEntity
                || level.getBlockEntity(pos) instanceof ReactorRedstonePortBlockEntity
                || level.getBlockEntity(pos) instanceof ReactorFluidPortBlockEntity;
    }

    /** Legacy getReactorSize: three base columns plus one per directly attached chamber. */
    public int columns() {
        int cols = BASE_COLUMNS;
        if (getLevel() instanceof ServerLevel level) {
            for (Direction direction : Direction.values()) {
                if (level.getBlockEntity(worldPosition.relative(direction))
                        instanceof ReactorChamberBlockEntity) cols++;
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
                    // setItemAt replaced the slot (depleted swap): keep it. A consumed component
                    // (heat-storage overflow) leaves the slot empty. Otherwise persist the
                    // component's mutation (depletion ticks) on the stored stack.
                    if (current != null
                            && current.getItem() == stack.getItem()
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
            // Legacy explosion power: base 10, + component additions, x multipliers, x hem.
            float boomPower = 10.0F;
            float boomMod = 1.0F;
            for (int slot = 0; slot < inventory.size(); slot++) {
                var stack = inventory.stack(slot);
                if (stack.getItem() instanceof ReactorComponent component) {
                    float factor = component.influenceExplosion(stack, this);
                    if (factor > 0.0F && factor < 1.0F) boomMod *= factor;
                    else boomPower += factor;
                }
            }
            boomPower *= hem * boomMod;
            clearGrid();
            setActive(false);
            getLevel().removeBlock(worldPosition, false);
            ic2.neoforge.explosion.HeatExplosion.trigger(
                    level, worldPosition, (int) Math.max(1.0F, boomPower), 0.01F, true);
            heat = 0;
            return true;
        }
        // Surface heat effects roll every cycle once the core passes each threshold.
        if (power >= 0.85F
                && hem > 0.0F
                && net.minecraft.util.RandomSource.create().nextFloat() <= 0.2F * hem)
            burnOrMelt(level, randomCoordination(level, 2));
        if (power >= 0.7F) {
            // Legacy: direct radiation damage to every living entity nearby; a complete hazmat
            // suit cancels the damage through the ic2:radiation player hook, not here.
            for (net.minecraft.world.entity.LivingEntity entity :
                    level.getEntitiesOfClass(
                            net.minecraft.world.entity.LivingEntity.class,
                            new net.minecraft.world.phys.AABB(
                                    worldPosition.getX() - 3,
                                    worldPosition.getY() - 3,
                                    worldPosition.getZ() - 3,
                                    worldPosition.getX() + 4,
                                    worldPosition.getY() + 4,
                                    worldPosition.getZ() + 4),
                            net.minecraft.world.entity.EntitySelector.NO_CREATIVE_OR_SPECTATOR)) {
                entity.hurtServer(
                        level,
                        ic2.neoforge.effect.RadiationEffect.radiationSource(level),
                        (int) (net.minecraft.util.RandomSource.create().nextInt(4) * hem));
            }
        }
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
        if (getLevel() == null) return false;
        if (getLevel().hasNeighborSignal(worldPosition)) return true;
        // A redstone port mirrors its own redstone input onto the core.
        return getLevel() instanceof ServerLevel level
                && ic2.neoforge.machine.ReactorRedstonePortBlockEntity.poweredPortNear(this, level)
                        != null;
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
            // Legacy passes null to consume a component (heat-storage overflow).
            if (stack == null || stack.isEmpty())
                inventory.set(x + y * GRID_COLUMNS, ItemResource.EMPTY, 0);
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
