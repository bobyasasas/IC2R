package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyMode;
import ic2.core.machine.WaterMill;
import ic2.neoforge.energy.EnergyConfig;
import ic2.neoforge.item.FluidCellItem;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class WaterGeneratorBlockEntity extends RotorGeneratorBlockEntity {
    private final WaterMill mill = new WaterMill();
    private final MachineJournal<WaterMill.State> journal =
            new MachineJournal<>(energy, mill::state, mill::restore, this::setChanged);
    private int water, sampleTicks;
    private double generation;
    private boolean sampled;

    public WaterGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
        sampleTicks = Math.floorMod(pos.hashCode(), 128);
        ensurePacketCapacity();
    }

    private void ensurePacketCapacity() {
        double capacity = EnergyConfig.MODE.get() == EnergyMode.GT ? 32 : 4;
        if (energy.capacity() != capacity) energy.resize(capacity);
    }

    @Override
    protected void prepareEnergyLoad() {
        ensurePacketCapacity();
    }

    public static boolean containsWater(ItemResource resource) {
        if (resource.isEmpty()) return false;
        var handler =
                ItemAccess.forStack(resource.toStack()).getCapability(Capabilities.Fluid.ITEM);
        if (handler == null) return false;
        for (int slot = 0; slot < handler.size(); slot++)
            if (handler.getResource(slot).equals(FluidResource.of(Fluids.WATER))
                    && handler.getAmountAsInt(slot) > 0) return true;
        return false;
    }

    public void sampleWater(ServerLevel level) {
        int count = 0;
        for (var pos :
                BlockPos.betweenClosed(
                        worldPosition.offset(-1, -1, -1), worldPosition.offset(1, 1, 1))) {
            if (level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                    && level.getFluidState(pos).is(Fluids.WATER)) count++;
        }
        water = count;
        sampled = true;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot ->
                        GenerationConfig.WATER_AUTOMATION.get()
                                && slot == 0
                                && side == Direction.UP,
                slot ->
                        GenerationConfig.WATER_AUTOMATION.get()
                                && slot == 0
                                && !containsWater(inventory.getResource(0)),
                (slot, resource) -> containsWater(resource));
    }

    @Override
    protected boolean generate(ServerLevel level) {
        ensurePacketCapacity();
        if (!sampled || ++sampleTicks % 128 == 0) sampleWater(level);
        var input = inventory.stack(0);
        boolean active;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            if (mill.canAcceptContainer() && containsWater(ItemResource.of(input))) {
                // Legacy TileEntityWaterGenerator branches on the recipe remainder: buckets keep
                // theirs at one EU per tick, while classic water cells have no container item and
                // are consumed outright at two EU per tick. A cell's fluid crafting remainder
                // (FluidCellItem) belongs to crafting only and must not reroute the mill.
                var remainder =
                        input.getItem() instanceof FluidCellItem
                                ? null
                                : input.getItem().getCraftingRemainder(input);
                if ((remainder == null || input.getCount() == 1)
                        && inventory.extract(0, ItemResource.of(input), 1, transaction) == 1) {
                    if (remainder != null
                            && inventory.insert(
                                            0,
                                            ItemResource.of(remainder),
                                            remainder.count(),
                                            transaction)
                                    != remainder.count()) return false;
                    mill.acceptContainer(remainder != null);
                }
            } else if (input.isEmpty())
                mill.prepareAmbient(water, GenerationConfig.WATER_MULTIPLIER.get());
            active = mill.tick(energy);
            transaction.commit();
        }
        generation = active ? mill.state().production() : 0;
        setRotorSpeed(mill.state().fuel() > 0 ? 1 : water / 25f);
        return active;
    }

    @Override
    public int progress() {
        return mill.state().fuel();
    }

    @Override
    public int progressMaximum() {
        return WaterMill.MAX_FUEL;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> water;
            case 1 -> (int) Math.round(generation * 100);
            default -> 0;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        double production = input.getDoubleOr("production", 0);
        mill.restore(
                new WaterMill.State(
                        Math.clamp(input.getIntOr("fuel", 0), 0, WaterMill.MAX_FUEL),
                        Double.isFinite(production) && production >= 0 ? production : 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("fuel", mill.state().fuel());
        output.putDouble("production", mill.state().production());
    }
}
