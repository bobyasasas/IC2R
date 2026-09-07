package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Common electrical output and battery charging for the additional generator families. */
public abstract class GeneratingBlockEntity extends PoweredBlockEntity {
    protected GeneratingBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                ((MachineBlock) state.getBlock()).kind().capacity(),
                ((MachineBlock) state.getBlock()).kind().slots());
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        return EnergyNode.Terminal.source(energy, 32, 1);
    }

    protected abstract boolean generate(ServerLevel level);

    @Override
    public final void serverTick(ServerLevel level) {
        boolean active = generate(level);
        int slot =
                switch (kind()) {
                    case SOLAR_GENERATOR -> 0;
                    case WATER_GENERATOR -> 1;
                    default -> 2;
                };
        var battery = inventory.stack(slot);
        double charged = ElectricItemEnergy.charge(battery, energy.stored(), 1, false, false);
        if (charged > 0) {
            inventory.set(slot, ItemResource.of(battery), battery.getCount());
            energy.extract(charged);
            setChanged();
        }
        setActive(active);
    }
}
