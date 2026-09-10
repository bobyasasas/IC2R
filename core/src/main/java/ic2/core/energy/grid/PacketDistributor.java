package ic2.core.energy.grid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Packet allocation with explicit loss accounting and deferred world effects. */
public final class PacketDistributor {
    public enum FaultKind {
        CABLE_VOLTAGE,
        CABLE_CURRENT,
        SINK_VOLTAGE
    }

    public record Fault(GridPosition position, FaultKind kind, double power) {}

    /** Peak packet power carried by one route this tick; routes shock entities above insulation. */
    public record RouteLoad(GridPosition target, List<GridPosition> conductors, double maxPacket) {
        public RouteLoad {
            conductors = List.copyOf(conductors);
        }
    }

    public record Result(
            double drawn,
            double delivered,
            double dissipated,
            List<Fault> faults,
            List<RouteLoad> routeLoads,
            Map<GridPosition, Integer> cableAmps) {
        public Result {
            faults = List.copyOf(faults);
            routeLoads = List.copyOf(routeLoads);
            cableAmps = Map.copyOf(cableAmps);
        }
    }

    private final EnergyGraph graph;
    private final boolean roundClassicLoss;

    public PacketDistributor(EnergyGraph graph, boolean roundClassicLoss) {
        this.graph = graph;
        this.roundClassicLoss = roundClassicLoss;
    }

    public Result tick(EnergyMode mode, long tick) {
        var state = new TickState();
        var sources =
                graph.nodes().entrySet().stream()
                        .filter(
                                e ->
                                        e.getValue() instanceof EnergyNode.Terminal t
                                                && t.output().isPresent())
                        .map(Map.Entry::getKey)
                        .sorted()
                        .toList();
        for (int index = 0; index < sources.size(); index++) {
            GridPosition position =
                    sources.get(
                            (index + (int) Math.floorMod(tick, sources.size())) % sources.size());
            var source = (EnergyNode.Terminal) graph.node(position);
            var output = source.output().orElseThrow();
            var routes = graph.routesFrom(position, mode);
            if (routes.isEmpty()) continue;
            int pathOffset = (int) Math.floorMod(tick, routes.size());
            int packets = mode == EnergyMode.GT ? output.maxAmps() : output.classicPacketCount();
            double packetPower =
                    mode == EnergyMode.GT
                            ? output.voltage()
                            : (double) output.voltage() * output.classicPacketAmps();
            for (int packet = 0; packet < packets; packet++) {
                double budget = Math.min(source.energy().stored(), packetPower);
                if (mode == EnergyMode.GT && budget < output.voltage() || budget <= 0) break;
                boolean transferred = false;
                for (int path = 0; path < routes.size() && budget > 0; path++) {
                    var route = routes.get((path + pathOffset) % routes.size());
                    double drawn =
                            mode == EnergyMode.GT
                                    ? emitGt(source, route, output.voltage(), state)
                                    : emitClassic(source, route, budget, state);
                    budget -= drawn;
                    transferred |= drawn > 0;
                    if (mode == EnergyMode.GT && drawn > 0) break;
                }
                if (!transferred) break;
            }
        }
        return new Result(
                state.drawn,
                state.delivered,
                state.drawn - state.delivered,
                new ArrayList<>(state.faults.values()),
                new ArrayList<>(state.routeLoads.values()),
                state.cableAmps);
    }

    private double emitClassic(
            EnergyNode.Terminal source, EnergyGraph.Route route, double budget, TickState state) {
        var sink = (EnergyNode.Terminal) graph.node(route.target());
        if (source.energy() == sink.energy()) return 0;
        double loss = roundClassicLoss ? Math.floor(route.loss()) : route.loss();
        double accepted = Math.min(sink.energy().free(), budget - loss);
        if (accepted <= 0) return 0;
        double drawn = accepted + loss;
        source.energy().extract(drawn);
        sink.energy().insert(accepted);
        state.drawn += drawn;
        state.delivered += accepted;
        for (var position : route.conductors()) {
            var cable = ((EnergyNode.Conductor) graph.node(position)).specification();
            state.cableAmps.merge(position, 1, Integer::sum);
            // The recovered IC2 conductor threshold is capacity + 1, tested with strict >.
            if (drawn > cable.voltageLimit() + 1.0)
                state.fault(position, FaultKind.CABLE_VOLTAGE, drawn);
        }
        if (drawn > sink.input().orElseThrow().voltage())
            state.fault(route.target(), FaultKind.SINK_VOLTAGE, drawn);
        state.routeLoad(route, drawn);
        return drawn;
    }

    private double emitGt(
            EnergyNode.Terminal source, EnergyGraph.Route route, int voltage, TickState state) {
        var sink = (EnergyNode.Terminal) graph.node(route.target());
        var input = sink.input().orElseThrow();
        if (source.energy() == sink.energy()
                || sink.energy().free() < input.voltage()
                || state.sinkAmps.getOrDefault(route.target(), 0) >= input.maxAmps()) return 0;
        double packet = voltage;
        var traversed = new ArrayList<GridPosition>();
        for (var position : route.conductors()) {
            if (state.faults.containsKey(position)) return 0;
            var cable = ((EnergyNode.Conductor) graph.node(position)).specification();
            traversed.add(position);
            if (voltage > cable.voltageLimit()) {
                state.fault(position, FaultKind.CABLE_VOLTAGE, voltage);
                state.addAmpLoads(traversed);
                return 0;
            }
            if (state.cableAmps.getOrDefault(position, 0) >= cable.ampLimit()) {
                state.fault(position, FaultKind.CABLE_CURRENT, voltage);
                state.addAmpLoads(traversed);
                return 0;
            }
            packet -= cable.gtLoss();
            if (packet <= 0) {
                state.addAmpLoads(traversed);
                return 0;
            }
        }
        double accepted = Math.min(packet, sink.energy().free());
        source.energy().extract(voltage);
        sink.energy().insert(accepted);
        state.addAmpLoads(traversed);
        state.sinkAmps.merge(route.target(), 1, Integer::sum);
        state.drawn += voltage;
        state.delivered += accepted;
        if (voltage > input.voltage()) state.fault(route.target(), FaultKind.SINK_VOLTAGE, voltage);
        state.routeLoad(route, voltage);
        return voltage;
    }

    private static final class TickState {
        double drawn, delivered;
        final Map<GridPosition, Integer> cableAmps = new HashMap<>();
        final Map<GridPosition, Integer> sinkAmps = new HashMap<>();
        final Map<GridPosition, Fault> faults = new LinkedHashMap<>();
        final Map<GridPosition, RouteLoad> routeLoads = new LinkedHashMap<>();

        void fault(GridPosition position, FaultKind kind, double power) {
            faults.compute(
                    position,
                    (ignored, previous) ->
                            previous == null || previous.power() < power
                                    ? new Fault(position, kind, power)
                                    : previous);
        }

        /** Tracks the peak packet power per route, mirroring the legacy path energy statistics. */
        void routeLoad(EnergyGraph.Route route, double packet) {
            routeLoads.compute(
                    route.target(),
                    (ignored, previous) ->
                            previous == null || previous.maxPacket() < packet
                                    ? new RouteLoad(route.target(), route.conductors(), packet)
                                    : previous);
        }

        void addAmpLoads(List<GridPosition> conductors) {
            conductors.forEach(position -> cableAmps.merge(position, 1, Integer::sum));
        }
    }
}
