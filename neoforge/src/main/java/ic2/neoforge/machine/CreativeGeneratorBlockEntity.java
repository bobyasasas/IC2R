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

/**
 * Legacy TileEntityCreativeGenerator (@NotClassic): an unbreakable LV source that offers infinite
 * energy. The port tops its store back up every tick, so any draw the voltage tier allows is
 * always satisfied; there are no slots and no legacy GUI.
 */
public final class CreativeGeneratorBlockEntity extends PoweredBlockEntity {
    public CreativeGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.CREATIVE_GENERATOR), pos, state,
                MachineKind.CREATIVE_GENERATOR.capacity(), MachineKind.CREATIVE_GENERATOR.slots());
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        return EnergyNode.Terminal.source(energy, 32, 1);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(inventory, slot -> false, slot -> false);
    }

    @Override
    public int progress() {
        return 0;
    }

    @Override
    public int progressMaximum() {
        return 1;
    }

    @Override
    public void serverTick(ServerLevel level) {
        energy.forceAdd(energy.capacity());
    }
}
