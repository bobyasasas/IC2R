package ic2.neoforge.energy;

import ic2.core.energy.grid.EnergyGraph;
import ic2.core.energy.grid.EnergyNode;
import ic2.core.energy.grid.GridPosition;
import ic2.core.energy.grid.PacketDistributor;
import ic2.neoforge.machine.PoweredBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** World lifetime and effects adapter. Graph algorithms never receive a world or block entity. */
public final class WorldEnergyNetworks {
    private static final Map<ServerLevel, Network> NETWORKS = new IdentityHashMap<>();

    private static final ResourceKey<DamageType> ELECTRICITY_TYPE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("ic2", "electricity"));

    private WorldEnergyNetworks() {}

    private static GridPosition grid(BlockPos pos) {
        return new GridPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    private static BlockPos block(GridPosition pos) {
        return new BlockPos(pos.x(), pos.y(), pos.z());
    }

    public static void add(ServerLevel level, PoweredBlockEntity machine) {
        Network network = NETWORKS.computeIfAbsent(level, ignored -> new Network());
        if (network.machines.put(machine.getBlockPos(), machine) != machine) network.dirty = true;
    }

    public static void remove(ServerLevel level, PoweredBlockEntity machine) {
        Network network = NETWORKS.get(level);
        if (network != null && network.machines.remove(machine.getBlockPos(), machine))
            network.dirty = true;
    }

    public static void invalidate(ServerLevel level) {
        Network network = NETWORKS.get(level);
        if (network != null) network.dirty = true;
    }

    public static void onUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) NETWORKS.remove(level);
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) invalidate(level);
    }

    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) invalidate(level);
    }

    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Network network = NETWORKS.get(level);
        if (network == null) return;
        if (network.dirty) network.rebuild(level);
        if (network.machines.isEmpty()) return;
        Map<BlockPos, Double> previousEnergy = new HashMap<>();
        network.machines.forEach(
                (pos, machine) -> previousEnergy.put(pos, machine.energy().stored()));
        var result =
                new PacketDistributor(network.graph, EnergyConfig.ROUND_CLASSIC_LOSS.get())
                        .tick(EnergyConfig.MODE.get(), level.getGameTime());
        network.machines.forEach(
                (pos, machine) -> {
                    if (previousEnergy.get(pos) != machine.energy().stored()) machine.setChanged();
                });
        for (var fault : result.faults()) {
            BlockPos pos = block(fault.position());
            if (!level.isLoaded(pos)) continue;
            if (fault.kind() == PacketDistributor.FaultKind.SINK_VOLTAGE) {
                if (EnergyConfig.MACHINE_EXPLOSIONS.get()) {
                    level.removeBlock(pos, false);
                    level.explode(
                            null,
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5,
                            2.5f,
                            Level.ExplosionInteraction.BLOCK);
                }
            } else if (EnergyConfig.CABLE_MELTDOWN.get()) level.removeBlock(pos, false);
        }
        applyCableShocks(level, network.graph, result.routeLoads());
    }

    /**
     * Legacy applyCableEffects: a route whose peak packet exceeds the weakest insulation on the
     * path shocks living entities within one block of every overloaded conductor, summing the
     * per-route maxima and dealing one damage per sixty-four accumulated EU.
     */
    private static void applyCableShocks(
            ServerLevel level, EnergyGraph graph, List<PacketDistributor.RouteLoad> routeLoads) {
        if (!EnergyConfig.CABLE_SHOCKS.get() || routeLoads.isEmpty()) return;
        Map<LivingEntity, Double> shockEnergy = new IdentityHashMap<>();
        for (var load : routeLoads) {
            double weakestInsulation = Double.POSITIVE_INFINITY;
            for (GridPosition position : load.conductors())
                if (graph.node(position) instanceof EnergyNode.Conductor cable)
                    weakestInsulation =
                            Math.min(
                                    weakestInsulation,
                                    cable.specification().insulationAbsorption());
            double amount = load.maxPacket();
            if (amount <= weakestInsulation) continue;
            Map<LivingEntity, Double> routeShocks = new IdentityHashMap<>();
            for (GridPosition position : load.conductors()) {
                if (!(graph.node(position) instanceof EnergyNode.Conductor cable)) continue;
                double absorption = cable.specification().insulationAbsorption();
                if (amount <= absorption) continue;
                int shock = (int) (amount - absorption);
                BlockPos pos = block(position);
                var nearby =
                        level.getEntitiesOfClass(
                                LivingEntity.class,
                                new AABB(
                                        pos.getX() - 1,
                                        pos.getY() - 1,
                                        pos.getZ() - 1,
                                        pos.getX() + 2,
                                        pos.getY() + 2,
                                        pos.getZ() + 2),
                                LivingEntity::isAlive);
                for (LivingEntity entity : nearby)
                    routeShocks.merge(entity, (double) shock, Math::max);
            }
            routeShocks.forEach((entity, shock) -> shockEnergy.merge(entity, shock, Double::sum));
        }
        if (shockEnergy.isEmpty()) return;
        DamageSource source =
                new DamageSource(level.damageSources().damageTypes.getOrThrow(ELECTRICITY_TYPE));
        shockEnergy.forEach(
                (entity, shock) -> {
                    int damage = (int) Math.ceil(shock / 64.0);
                    if (entity.isAlive() && damage > 0) entity.hurtServer(level, source, damage);
                });
    }

    private static final class Network {
        final Map<BlockPos, PoweredBlockEntity> machines = new HashMap<>();
        EnergyGraph graph = new EnergyGraph();
        boolean dirty = true;

        void rebuild(ServerLevel level) {
            graph = new EnergyGraph();
            machines.entrySet()
                    .removeIf(
                            entry ->
                                    entry.getValue().isRemoved()
                                            || !level.isLoaded(entry.getKey()));
            var queue = new ArrayDeque<BlockPos>();
            var visited = new HashSet<BlockPos>();
            machines.forEach(
                    (pos, machine) -> {
                        graph.put(grid(pos), machine.energyNode());
                        queue.add(pos);
                        visited.add(pos);
                    });
            while (!queue.isEmpty()) {
                BlockPos pos = queue.remove();
                for (Direction side : Direction.values()) {
                    BlockPos neighbor = pos.relative(side);
                    if (!level.isLoaded(neighbor)) continue;
                    if (graph.node(grid(neighbor)) == null
                            && level.getBlockState(neighbor).getBlock()
                                    instanceof CableBlock cable) {
                        graph.put(grid(neighbor), new EnergyNode.Conductor(cable.specification()));
                    }
                    EnergyNode from = graph.node(grid(pos)), to = graph.node(grid(neighbor));
                    if (to == null) continue;
                    boolean emits =
                            from instanceof EnergyNode.Conductor
                                    || ((EnergyNode.Terminal) from).output().isPresent();
                    boolean accepts =
                            to instanceof EnergyNode.Conductor
                                    || ((EnergyNode.Terminal) to).input().isPresent();
                    if (emits
                            && accepts
                            && (!machines.containsKey(pos) || machines.get(pos).emitsTo(side))
                            && (!machines.containsKey(neighbor)
                                    || machines.get(neighbor).acceptsFrom(side.getOpposite())))
                        graph.connect(grid(pos), grid(neighbor));
                    if (visited.add(neighbor)) queue.add(neighbor);
                }
            }
            dirty = false;
        }
    }
}
