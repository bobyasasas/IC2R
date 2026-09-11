package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.energy.WorldEnergyNetworks;
import ic2.neoforge.item.HazmatLike;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import org.joml.Vector3f;

import java.util.List;

/**
 * Legacy TileEntityTesla: with a redstone signal the coil pays a 1 EU/t idle drain and, every
 * 32 ticks, converts a quarter of its thousandth charge — stored/400 — into damage split evenly
 * across every living being in four blocks (complete hazmat suits are immune). The shock pays
 * totalDamage × 400 EU.
 */
public final class TeslaCoilBlockEntity extends PoweredBlockEntity {
    public static final int CAPACITY = 10000;
    private static final int RANGE = 4;
    private static final DustParticleOptions EFFECT =
            new DustParticleOptions(0xFF1919FF, 1.0F);

    private int ticker = Math.floorMod(
            worldPosition == null ? 0 : worldPosition.getX() * 31 + worldPosition.getZ(), 32);

    public TeslaCoilBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.TESLA_COIL), pos, state, CAPACITY, 0);
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        return EnergyNode.Terminal.sink(
                energy, ic2.core.energy.VoltageTier.fromIcTier(kind().electricalTier()).getVoltage(),
                1);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!level.hasNeighborSignal(worldPosition)) return;
        if (!energy.consume(1.0)) return;
        if (++ticker % 32 != 0) return;
        int totalDamage = (int) energy.stored() / 400;
        if (totalDamage > 0 && shock(level, totalDamage)) {
            energy.extract(totalDamage * 400.0);
        }
    }

    private boolean shock(ServerLevel level, int totalDamage) {
        List<LivingEntity> entities = level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(
                        worldPosition.getX() - RANGE,
                        worldPosition.getY() - RANGE,
                        worldPosition.getZ() - RANGE,
                        worldPosition.getX() + RANGE + 1,
                        worldPosition.getY() + RANGE + 1,
                        worldPosition.getZ() + RANGE + 1),
                EntitySelector.NO_CREATIVE_OR_SPECTATOR);
        if (entities.isEmpty()) return false;
        boolean shocked = false;
        int damage = totalDamage / entities.size();
        for (LivingEntity entity : entities) {
            if (HazmatLike.hasCompleteHazmat(entity)) continue;
            if (entity.hurtServer(
                    level,
                    level.damageSources().source(WorldEnergyNetworks.ELECTRICITY_TYPE),
                    damage)) {
                var random = level.getRandom();
                for (int i = 0; i < damage; i++) {
                    level.addParticle(
                            EFFECT,
                            entity.getX() + random.nextFloat() - 0.5,
                            entity.getY() + random.nextFloat() * 2.0F - 1.0,
                            entity.getZ() + random.nextFloat() - 0.5,
                            0.0,
                            0.0,
                            0.0);
                }
                shocked = true;
            }
        }
        return shocked;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory, slot -> false, slot -> false, (slot, resource) -> false);
    }

    @Override
    public int progress() {
        return 0;
    }

    @Override
    public int progressMaximum() {
        return 0;
    }
}
