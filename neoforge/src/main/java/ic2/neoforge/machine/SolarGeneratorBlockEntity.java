package ic2.neoforge.machine;

import ic2.core.machine.SolarGeneration;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

public final class SolarGeneratorBlockEntity extends GeneratingBlockEntity {
    private double sunlight;
    private int sampleTicks;
    private boolean sampled;

    public SolarGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
        sampleTicks = Math.floorMod(pos.hashCode(), 128);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(inventory, slot -> false, slot -> false);
    }

    public void sampleSunlight(ServerLevel level) {
        var sample = worldPosition.above();
        // Legacy gates the rain attenuation behind biomeHasType(SANDY), but the shipped
        // EnvProxyForge stub returns false unconditionally (bytecode: iconst_0), so every
        // biome attenuates; the sandy branch is unreachable and stays out here too.
        sunlight =
                SolarGeneration.brightness(
                        level.dimensionType().hasSkyLight(),
                        level.getBrightness(LightLayer.SKY, sample),
                        level.environmentAttributes()
                                .getValue(EnvironmentAttributes.SUN_ANGLE, sample),
                        false,
                        level.getRainLevel(1),
                        level.getThunderLevel(1));
        sampled = true;
    }

    @Override
    protected boolean generate(ServerLevel level) {
        if (!sampled || ++sampleTicks % 128 == 0) sampleSunlight(level);
        if (energy.insert(sunlight) > 0) setChanged();
        return sunlight > 0;
    }

    @Override
    public int progress() {
        return (int) Math.round(sunlight * 1000);
    }

    @Override
    public int progressMaximum() {
        return 1000;
    }
}
