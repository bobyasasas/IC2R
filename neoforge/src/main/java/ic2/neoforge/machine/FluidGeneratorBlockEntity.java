package ic2.neoforge.machine;

import ic2.core.machine.FluidFuelBurner;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.transfer.MachineFluidTank;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class FluidGeneratorBlockEntity extends GeneratingBlockEntity {
    private static final FluidFuelBurner.Fuel LAVA = new FluidFuelBurner.Fuel(2, 1, 20);
    private static final FluidFuelBurner.Fuel BIOGAS = new FluidFuelBurner.Fuel(10, 10, 16);
    private final FluidFuelBurner burner = new FluidFuelBurner();
    private final MachineJournal<FluidFuelBurner.State> journal =
            new MachineJournal<>(energy, burner::state, burner::restore, this::setChanged);
    private final MachineFluidTank tank;

    public FluidGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
        tank =
                new MachineFluidTank(
                        kind() == MachineKind.GEO_GENERATOR ? 8000 : 10000,
                        this::setChanged,
                        this::acceptsFuel);
    }

    public MachineFluidTank tank() {
        return tank;
    }

    private boolean acceptsFuel(FluidResource resource) {
        return resource.getFluid()
                == (kind() == MachineKind.GEO_GENERATOR
                        ? Fluids.LAVA
                        : ModFluids.FAMILIES.get(FluidDefinition.BIOGAS).source().get());
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> slot == 0,
                slot -> slot == 1,
                (slot, resource) ->
                        ItemAccess.forStack(resource.toStack())
                                        .getCapability(Capabilities.Fluid.ITEM)
                                != null);
    }

    public ResourceHandler<FluidResource> fluidAutomation(Direction side) {
        return new ResourcePort<>(tank, slot -> true, slot -> false);
    }

    @Override
    protected boolean generate(ServerLevel level) {
        // Container conversion enlists both the input and output slots along with the tank.
        if (!inventory.stack(0).isEmpty()) {
            var port = new ResourcePort<>(inventory, slot -> slot == 1, slot -> slot == 0);
            var container =
                    ItemAccess.forHandlerIndex(port, 0)
                            .oneByOne()
                            .getCapability(Capabilities.Fluid.ITEM);
            ResourceHandlerUtil.move(container, tank, this::acceptsFuel, 10000, null);
        }
        boolean active;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            var fuel = kind() == MachineKind.GEO_GENERATOR ? LAVA : BIOGAS;
            if (burner.needsFuel(energy, fuel)
                    && acceptsFuel(tank.getResource(0))
                    && tank.getAmountAsInt(0) >= fuel.millibuckets()
                    && tank.extract(0, tank.getResource(0), fuel.millibuckets(), transaction)
                            == fuel.millibuckets()) burner.accept(fuel);
            active = burner.tick(energy);
            transaction.commit();
        }
        return active;
    }

    @Override
    public int progress() {
        return burner.state().remaining();
    }

    @Override
    public int progressMaximum() {
        return burner.state().total();
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> tank.getAmountAsInt(0);
            case 1 -> BuiltInRegistries.FLUID.getId(tank.getResource(0).getFluid());
            case 2 -> kind() == MachineKind.GEO_GENERATOR ? 8000 : 10000;
            default -> 0;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.deserialize(input.childOrEmpty("fluidTank"));
        int total = Math.clamp(input.getIntOr("fuelTotal", 0), 0, 10);
        double production = input.getDoubleOr("production", 0);
        int remaining = Math.clamp(input.getIntOr("fuel", 0), 0, total);
        burner.restore(
                Double.isFinite(production) && production > 0 && production <= 32
                        ? new FluidFuelBurner.State(remaining, total, production)
                        : new FluidFuelBurner.State(0, 0, 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("fluidTank"));
        output.putInt("fuel", burner.state().remaining());
        output.putInt("fuelTotal", burner.state().total());
        output.putDouble("production", burner.state().euPerTick());
    }
}
