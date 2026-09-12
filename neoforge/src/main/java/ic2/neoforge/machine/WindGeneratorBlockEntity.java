package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyMode;
import ic2.core.machine.WindSimulation;
import ic2.neoforge.energy.EnergyConfig;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

public final class WindGeneratorBlockEntity extends RotorGeneratorBlockEntity {
    private int obstructions, ticks;
    private boolean sampled;
    private double production, overload;

    /** Rotor plane blockage count, read by the wind meter (legacy getObstructions). */
    public int obstructions() {
        return obstructions;
    }

    public WindGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
        ticks = Math.floorMod(pos.hashCode(), 128);
        ensureCapacity();
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(inventory, slot -> false, slot -> false);
    }

    private void ensureCapacity() {
        double capacity =
                WindSimulation.capacity(
                        EnergyConfig.MODE.get() == EnergyMode.GT,
                        GenerationConfig.WIND_MULTIPLIER.get());
        if (energy.capacity() != capacity) energy.resize(capacity);
    }

    @Override
    protected void prepareEnergyLoad() {
        ensureCapacity();
    }

    public void sampleObstructions(ServerLevel level) {
        int count = 0;
        for (var pos :
                BlockPos.betweenClosed(
                        worldPosition.offset(-4, -2, -4), worldPosition.offset(4, 4, 4))) {
            if (pos.equals(worldPosition)) continue;
            // Unloaded neighbors obstruct the rotor; sampling never loads chunks.
            if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                    || !level.isEmptyBlock(pos)) count++;
        }
        obstructions = count;
    }

    @Override
    protected boolean generate(ServerLevel level) {
        ensureCapacity();
        ticks = (ticks + 1) % 1024;
        if (!sampled || ticks == 0) sampleObstructions(level);
        if (!sampled || ticks % 128 == 0) {
            sampled = true;
            double wind = WorldWind.get(level).windAt(level, worldPosition.getY());
            production =
                    WindSimulation.production(
                            wind, obstructions, GenerationConfig.WIND_MULTIPLIER.get());
            double localWind = wind * (1 - obstructions / 567.0);
            overload = Math.max(0, (localWind / WindSimulation.MAX_WIND - .5) / .5);
            setRotorSpeed(production > 0 ? 1 : 0);
            // The recovered release never broke rotors because it checked a cleared production
            // value.
            // Keep that default; opt-in uses the calculated production and ordinary block loot.
            if (GenerationConfig.WIND_BREAKAGE.get()
                    && overload > 0
                    && level.getRandom().nextInt(5000) <= production - 5
                    && level.getBlockEntity(worldPosition) == this
                    && level.destroyBlock(worldPosition, true)) {
                int iron = level.getRandom().nextInt(5);
                if (iron > 0)
                    Block.popResource(level, worldPosition, new ItemStack(Items.IRON_INGOT, iron));
                return false;
            }
        }
        if (production <= 0 || energy.capacity() - energy.stored() < production) return false;
        energy.insert(production);
        setChanged();
        return true;
    }

    @Override
    public int progress() {
        return (int) Math.round(Math.min(1, overload) * 1000);
    }

    @Override
    public int progressMaximum() {
        return 1000;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> obstructions;
            case 1 -> (int) Math.round(production * 100);
            default -> 0;
        };
    }
}
