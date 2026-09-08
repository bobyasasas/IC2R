package ic2.neoforge.machine;

import ic2.core.machine.RotorMaterial;
import ic2.core.machine.RotorOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class WindTurbineBlockEntity extends TurbineBlockEntity {
    public WindTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    protected int sampleInterval() {
        return 32;
    }

    @Override
    protected RotorOperation sample(
            ServerLevel level, RotorMaterial material, Direction facing, RotorSpace space) {
        if (GenerationConfig.WIND_KINETIC.get() == 0)
            return RotorOperation.stopped(material.diameter(), RotorOperation.Status.DISABLED);
        int plane =
                space.obstructions(worldPosition, facing, material.diameter(), true, false, kind());
        if (plane != 0)
            return new RotorOperation(
                    0, material.diameter(), 0, RotorOperation.Status.NO_SPACE, 0, plane, 0);
        int obstructions =
                space.obstructions(
                        worldPosition, facing, material.diameter(), false, false, kind());
        return RotorOperation.wind(
                material,
                WorldWind.get(level).windAt(level, worldPosition.getY()),
                obstructions,
                GenerationConfig.WIND_KINETIC.get());
    }
}
