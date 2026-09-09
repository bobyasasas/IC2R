package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Magnetizes adjacent iron fence columns: two EU per boost share, paid from its small store. */
public final class MagnetizerBlockEntity extends PoweredBlockEntity {
    public MagnetizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.MAGNETIZER), pos, state, 100, 4);
    }

    public boolean canBoost() {
        return energy.stored() >= 2.0;
    }

    /** Spends the fence climber's share; several magnetizers split the cost. */
    public void boost(double multiplier) {
        energy.extract(2.0 * multiplier);
    }

    @Override
    public ic2.core.energy.grid.EnergyNode.Terminal energyNode() {
        return EnergyNode.Terminal.sink(
                energy,
                ic2.core.energy.VoltageTier.fromIcTier(kind().electricalTier()).getVoltage(),
                1);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory, slot -> false, slot -> false, (slot, resource) -> false);
    }

    @Override
    public void serverTick(ServerLevel level) {}

    @Override
    public int progress() {
        return 0;
    }

    @Override
    public int progressMaximum() {
        return 0;
    }
}
