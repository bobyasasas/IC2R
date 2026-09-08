package ic2.neoforge.machine;

import ic2.core.energy.VoltageTier;
import ic2.core.energy.grid.EnergyNode;
import ic2.core.machine.Condensation;
import ic2.neoforge.energy.WorldEnergyNetworks;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModReactorItems;
import ic2.neoforge.transfer.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Atomic condensation preserves the original passive/vent rates and delayed water batches. */
public final class CondenserBlockEntity extends PoweredBlockEntity implements FluidMachine {
    public static final int VENT_START = 3, VENT_END = 7;
    private Condensation condensation = new Condensation(0);
    private final MachineJournal<Condensation> journal =
            new MachineJournal<>(
                    energy, () -> condensation, value -> condensation = value, this::setChanged);
    private final MachineFluidTank inputTank =
            new MachineFluidTank(100000, this::setChanged, CondenserBlockEntity::steam);
    private final MachineFluidTank outputTank =
            new MachineFluidTank(1000, this::setChanged, resource -> true);
    private final ResourceHandler<FluidResource> fluids =
            new ResourcePort<>(
                    new CombinedResourceHandler<>(inputTank, outputTank),
                    slot -> slot == 0,
                    slot -> slot == 1);
    private int lastTier = -1;

    public CondenserBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(MachineKind.CONDENSER),
                pos,
                state,
                100000,
                MachineKind.CONDENSER.slots());
    }

    public MachineFluidTank inputTank() {
        return inputTank;
    }

    public MachineFluidTank outputTank() {
        return outputTank;
    }

    private static boolean steam(FluidResource resource) {
        return resource.getFluid() == ModFluids.FAMILIES.get(FluidDefinition.STEAM).source().get()
                || resource.getFluid()
                        == ModFluids.FAMILIES.get(FluidDefinition.SUPERHEATED_STEAM).source().get();
    }

    private static boolean vent(ItemResource resource) {
        return resource.getItem() == ModReactorItems.HEAT_VENT.get();
    }

    public int vents() {
        int vents = 0;
        for (int slot = VENT_START; slot < VENT_END; slot++)
            if (vent(inventory.getResource(slot))) vents++;
        return vents;
    }

    private int tier() {
        var upgrade = inventory.getResource(kind().upgradeStart());
        int count =
                upgrade.getItem() instanceof UpgradeItem item
                                && item.kind() == UpgradeItem.Kind.TRANSFORMER
                        ? Math.min(2, inventory.getAmountAsInt(kind().upgradeStart()))
                        : 0;
        return 3 + count;
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        return EnergyNode.Terminal.sink(energy, VoltageTier.fromIcTier(tier()).getVoltage(), 1);
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        return slot >= VENT_START && slot < VENT_END
                ? vent(resource)
                : super.acceptsInventorySlot(slot, resource);
    }

    @Override
    protected int inventorySlotLimit(int slot) {
        return slot >= VENT_START && slot < VENT_END ? 1 : super.inventorySlotLimit(slot);
    }

    @Override
    public ResourceHandler<FluidResource> fluidAutomation(Direction side) {
        return fluids;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> slot == 0 && side == Direction.DOWN,
                slot -> slot == 1,
                (slot, resource) -> FluidContainerPort.accepts(resource));
    }

    @Override
    public void serverTick(ServerLevel level) {
        int tier = tier();
        if (lastTier != tier) {
            lastTier = tier;
            WorldEnergyNetworks.invalidate(level);
        }
        var battery = inventory.stack(2);
        double charge =
                ElectricItemEnergy.discharge(battery, energy.free(), tier, false, true, false);
        if (charge > 0) {
            inventory.set(2, ItemResource.of(battery), battery.getCount());
            energy.insert(charge);
            setChanged();
        }
        if (!inventory.stack(0).isEmpty())
            ResourceHandlerUtil.move(
                    outputTank,
                    FluidContainerPort.of(inventory, 0, 1),
                    resource -> true,
                    1000,
                    null);
        boolean running = condense();
        setActive(running);
        UpgradeTransfers.tick(level, this);
    }

    private boolean condense() {
        var water =
                FluidResource.of(
                        ModFluids.FAMILIES.get(FluidDefinition.DISTILLED_WATER).source().get());
        if (!outputTank.getResource(0).isEmpty() && !outputTank.getResource(0).equals(water))
            return false;
        var step =
                condensation.plan(
                        steam(inputTank.getResource(0)) ? inputTank.getAmountAsInt(0) : 0,
                        1000 - outputTank.getAmountAsInt(0),
                        vents(),
                        energy.stored());
        if (step.steam() == 0 && step.water() == 0) return false;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            if (step.steam() > 0
                    && inputTank.extract(0, inputTank.getResource(0), step.steam(), transaction)
                            != step.steam()) return false;
            if (step.water() > 0
                    && outputTank.insert(0, water, step.water(), transaction) != step.water())
                return false;
            if (!energy.consume(step.energy())) return false;
            condensation = step.next();
            transaction.commit();
        }
        return true;
    }

    @Override
    public int progress() {
        return condensation.steamCredit();
    }

    @Override
    public int progressMaximum() {
        return Condensation.BATCH_STEAM;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> vents();
            case 1 -> inputTank.getAmountAsInt(0);
            case 2 -> outputTank.getAmountAsInt(0);
            case 3 -> BuiltInRegistries.FLUID.getId(inputTank.getResource(0).getFluid());
            case 4 -> BuiltInRegistries.FLUID.getId(outputTank.getResource(0).getFluid());
            default -> 0;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inputTank.deserialize(input.childOrEmpty("inputTank"));
        outputTank.deserialize(input.childOrEmpty("outputTank"));
        condensation =
                new Condensation(
                        Math.clamp(input.getIntOr("progress", 0), 0, Condensation.MAX_CREDIT));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inputTank.serialize(output.child("inputTank"));
        outputTank.serialize(output.child("outputTank"));
        output.putInt("progress", condensation.steamCredit());
    }
}
