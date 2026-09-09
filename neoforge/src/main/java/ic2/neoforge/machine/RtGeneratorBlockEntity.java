package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyNode;
import ic2.core.machine.RadioisotopeOutput;
import ic2.neoforge.item.ElectricItem;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModReactorItems;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Radioisotope EU generator: pellets are never consumed and every additional pellet doubles the
 * whole machine's output, 2^(n-1) times the configured base. A storage buffer feeds the native LV
 * source and can charge a tool in the battery slot.
 */
public final class RtGeneratorBlockEntity extends PoweredBlockEntity {
    public static final int PELLETS = 6, BATTERY = 6;

    public RtGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.RT_GENERATOR), pos, state, 20000, 7);
    }

    public int installed() {
        int count = 0;
        for (int slot = 0; slot < PELLETS; slot++)
            if (isPellet(inventory.getResource(slot))) count++;
        return count;
    }

    private static boolean isPellet(ItemResource resource) {
        return resource.getItem() == ModReactorItems.RTG_PELLET.get();
    }

    public int outputRate() {
        return RadioisotopeOutput.output(
                installed(), GenerationConfig.RADIOISOTOPE_GENERATION.get());
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        return EnergyNode.Terminal.source(energy, 32, 1);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> slot < PELLETS,
                slot -> slot == BATTERY,
                (slot, resource) ->
                        slot < PELLETS
                                ? isPellet(resource)
                                : resource.getItem() instanceof ElectricItem);
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        if (slot < PELLETS) return isPellet(resource);
        if (slot == BATTERY) return resource.getItem() instanceof ElectricItem;
        return super.acceptsInventorySlot(slot, resource);
    }

    @Override
    protected int inventorySlotLimit(int slot) {
        return slot < PELLETS ? 1 : super.inventorySlotLimit(slot);
    }

    @Override
    public void serverTick(ServerLevel level) {
        double produced = 0;
        int rate = outputRate();
        if (rate > 0 && energy.free() > 0) produced = energy.insert(Math.min(rate, energy.free()));
        var battery = inventory.stack(BATTERY);
        double accepted = ElectricItemEnergy.charge(battery, energy.stored(), 1, false, false);
        if (accepted > 0) {
            inventory.set(BATTERY, ItemResource.of(battery), battery.getCount());
            energy.extract(accepted);
            setChanged();
        }
        setActive(produced > 0);
    }

    @Override
    public int progress() {
        return installed();
    }

    @Override
    public int progressMaximum() {
        return RadioisotopeOutput.SLOTS;
    }

    @Override
    public int fuelRemaining() {
        return (int) energy.stored();
    }

    @Override
    public int fuelMaximum() {
        return 20000;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> installed();
            case 1 -> outputRate();
            case 2 -> (int) Math.min(Integer.MAX_VALUE, energy.stored());
            default -> 0;
        };
    }
}
