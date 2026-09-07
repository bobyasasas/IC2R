package ic2.neoforge.machine;

import ic2.core.energy.EnergyStore;
import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.energy.WorldEnergyNetworks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Adds EU storage and network membership only to machines that participate in the electric grid.
 */
public abstract class PoweredBlockEntity extends MachineBlockEntity {
    protected final EnergyStore energy;

    protected PoweredBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state, double capacity, int slots) {
        super(type, pos, state, slots);
        energy = new EnergyStore(capacity);
    }

    public final EnergyStore energy() {
        return energy;
    }

    @Override
    public final double storedEnergy() {
        return energy.stored();
    }

    public abstract EnergyNode.Terminal energyNode();

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) WorldEnergyNetworks.add(server, this);
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel server) WorldEnergyNetworks.remove(server, this);
        super.setRemoved();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        double stored = input.getDoubleOr("energy", 0);
        energy.restore(Double.isFinite(stored) ? Math.clamp(stored, 0, energy.capacity()) : 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("energy", energy.stored());
    }
}
