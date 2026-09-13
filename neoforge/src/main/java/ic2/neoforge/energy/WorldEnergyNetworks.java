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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** World lifetime and effects adapter. Graph algorithms never receive a world or block entity. */
public final class WorldEnergyNetworks {
    private static final Map<ServerLevel, Network> NETWORKS = new IdentityHashMap<>();

    /** Provides sink terminals for energy consumers outside the IC2 machine set (AE2 bridge). */
    public interface ExternalTerminals {
        @javax.annotation.Nullable
        EnergyNode.Terminal terminal(ServerLevel level, BlockPos pos);

        /** Runs once per level tick after packet distribution: drain and stale cleanup. */
        void afterDistribution(ServerLevel level);
    }

    private static volatile ExternalTerminals externalTerminals;

    public static void setExternalTerminals(@javax.annotation.Nullable ExternalTerminals provider) {
        externalTerminals = provider;
    }

    public static final ResourceKey<DamageType> ELECTRICITY_TYPE =
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

    /**
     * Energy packets that traversed this conductor during the latest distribution, read by the
     * detector cables; legacy mirrors it with the per-tile NodeStats energy-in counter.
     */
    public static double conductorEnergyIn(ServerLevel level, BlockPos pos) {
        PacketDistributor.NodeStats stats = nodeStats(level, pos);
        return stats == null ? 0 : stats.energyIn();
    }

    /** Per-node flow statistics of the latest distribution, read by the EU-Reader. */
    @javax.annotation.Nullable
    public static PacketDistributor.NodeStats nodeStats(ServerLevel level, BlockPos pos) {
        Network network = NETWORKS.get(level);
        return network == null ? null : network.lastNodeStats.get(grid(pos));
    }

    /**
     * Legacy EnergyNetLocal.dumpDebugInfo for the Debug Item: describes the energy node at the
     * position into both sinks (chat for the player, console for the server log — legacy fed a
     * client console via packets instead). Returns false when nothing energized is there, so the
     * caller falls through untouched.
     */
    public static boolean dumpDebugInfo(
            ServerLevel level,
            BlockPos pos,
            java.util.function.Consumer<String> console,
            java.util.function.Consumer<String> chat) {
        Network network = NETWORKS.get(level);
        EnergyNode node = network == null ? null : network.graph.node(grid(pos));
        if (node == null) return false;
        List<String> lines = new ArrayList<>();
        lines.add(
                "Node %s at %s info:"
                        .formatted(
                                node.getClass().getSimpleName(),
                                level.dimension().identifier() + " " + pos.toShortString()));
        lines.add(
                " machines: %d, nodes: %d"
                        .formatted(network.machines.size(), network.graph.nodes().size()));
        if (node instanceof EnergyNode.Conductor cable) {
            var spec = cable.specification();
            lines.add(
                    " role: conductor, cable: %dV, %dA, loss %.3f/%d, insulation %d/%d, diameter %.4f"
                            .formatted(
                                    spec.voltageLimit(),
                                    spec.ampLimit(),
                                    spec.classicLoss(),
                                    spec.gtLoss(),
                                    spec.insulation(),
                                    spec.maxInsulation(),
                                    spec.diameter()));
        } else if (node instanceof EnergyNode.Terminal terminal) {
            lines.add(
                    " role: terminal, buffer: %.0f/%.0f EU"
                            .formatted(terminal.energy().stored(), terminal.energy().capacity()));
            terminal.output()
                    .ifPresent(output ->
                            lines.add(
                                    " output: %dV, %dA"
                                            .formatted(output.voltage(), output.maxAmps())));
            terminal.input()
                    .ifPresent(input ->
                            lines.add(
                                    " input: %dV, %dA".formatted(input.voltage(), input.maxAmps())));
        }
        PacketDistributor.NodeStats stats = nodeStats(level, pos);
        if (stats != null)
            lines.add(
                    " last packets: in %.2f EU, out %.2f EU, %.2f V, %d A"
                            .formatted(
                                    stats.energyIn(),
                                    stats.energyOut(),
                                    stats.voltage(),
                                    stats.amperage()));
        lines.forEach(console);
        lines.forEach(chat);
        return true;
    }

    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Network network = NETWORKS.get(level);
        if (network == null) return;
        if (network.dirty) network.rebuild(level);
        // A skipped distribution must read as zero flow, never as a stale previous tick.
        network.lastNodeStats = Map.of();
        if (network.machines.isEmpty()) return;
        Map<BlockPos, Double> previousEnergy = new HashMap<>();
        network.machines.forEach(
                (pos, machine) -> previousEnergy.put(pos, machine.energy().stored()));
        var result =
                new PacketDistributor(network.graph, EnergyConfig.ROUND_CLASSIC_LOSS.get())
                        .tick(EnergyConfig.MODE.get(), level.getGameTime());
        // Nodes without traffic this tick still read as zero flow, like legacy registered tiles.
        var stats = new HashMap<GridPosition, PacketDistributor.NodeStats>();
        network.graph.nodes().keySet().forEach(position -> stats.put(position, PacketDistributor.NodeStats.ZERO));
        stats.putAll(result.nodeStats());
        network.lastNodeStats = stats;
        network.machines.forEach(
                (pos, machine) -> {
                    if (previousEnergy.get(pos) != machine.energy().stored()) machine.setChanged();
                });
        ExternalTerminals externals = externalTerminals;
        if (externals != null) externals.afterDistribution(level);
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
        Map<GridPosition, PacketDistributor.NodeStats> lastNodeStats = Map.of();
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
                    BlockState neighborState = level.getBlockState(neighbor);
                    if (graph.node(grid(neighbor)) == null
                            && neighborState.getBlock() instanceof CableBlock cable
                            && conducts(cable, neighborState)) {
                        graph.put(grid(neighbor), new EnergyNode.Conductor(cable.specification()));
                    }
                    if (graph.node(grid(neighbor)) == null
                            && neighborState.getBlock() instanceof FoamCableBlock foam
                            && conducts(foam, neighborState)) {
                        graph.put(grid(neighbor), new EnergyNode.Conductor(foam.specification()));
                    }
                    if (graph.node(grid(neighbor)) == null) {
                        ExternalTerminals externals = externalTerminals;
                        if (externals != null) {
                            EnergyNode.Terminal external = externals.terminal(level, neighbor);
                            if (external != null) graph.put(grid(neighbor), external);
                        }
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

        /**
         * Legacy splitter gating: an unpowered splitter sits outside the grid, so no route can
         * cross it — the BFS never reaches a node behind it, exactly like removeFromEnet.
         */
        private static boolean conducts(CableBlock cable, BlockState state) {
            return !(cable instanceof SplitterCableBlock splitter)
                    || splitter.isConducting(state);
        }

        private static boolean conducts(FoamCableBlock foam, BlockState state) {
            return !(foam instanceof SplitterFoamCableBlock splitter)
                    || splitter.isConducting(state);
        }
    }
}
