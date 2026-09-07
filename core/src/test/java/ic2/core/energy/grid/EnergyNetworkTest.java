package ic2.core.energy.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ic2.core.energy.EnergyStore;

import org.junit.jupiter.api.Test;

import java.util.Optional;

class EnergyNetworkTest {
    private static GridPosition p(int x) {
        return new GridPosition(x, 0, 0);
    }

    private record Line(EnergyGraph graph, EnergyStore source, EnergyStore sink) {}

    private static Line line(int voltage, int amps, int receiverVoltage, int receiverAmps) {
        var graph = new EnergyGraph();
        var source = new EnergyStore(10000);
        var sink = new EnergyStore(10000);
        source.insert(1000);
        graph.put(p(0), EnergyNode.Terminal.source(source, voltage, amps));
        graph.put(p(1), new EnergyNode.Conductor(CableSpec.Material.COPPER.insulated(1)));
        graph.put(p(2), EnergyNode.Terminal.sink(sink, receiverVoltage, receiverAmps));
        graph.connect(p(0), p(1));
        graph.connect(p(1), p(2));
        return new Line(graph, source, sink);
    }

    @Test
    void classicAndGtModesKeepDifferentLossAndPacketRules() {
        var classic = line(32, 1, 32, 1);
        var ic2 = new PacketDistributor(classic.graph, true).tick(EnergyMode.IC2, 0);
        assertEquals(32, ic2.drawn());
        assertEquals(32, ic2.delivered()); // floor(0.2) in the default classic configuration
        assertEquals(0, ic2.dissipated());
        var gt = line(32, 1, 32, 1);
        var result = new PacketDistributor(gt.graph, true).tick(EnergyMode.GT, 0);
        assertEquals(32, result.drawn());
        assertEquals(31, result.delivered());
        assertEquals(1, result.dissipated());
        assertEquals(1000, gt.source.stored() + gt.sink.stored() + result.dissipated());
    }

    @Test
    void classicCanSendPartialPacketsWhileGtWaitsForAFullVoltagePacket() {
        for (var mode : EnergyMode.values()) {
            var line = line(32, 1, 32, 1);
            line.source.restore(10);
            var result = new PacketDistributor(line.graph, true).tick(mode, 0);
            assertEquals(mode == EnergyMode.IC2 ? 10 : 0, result.drawn());
        }
    }

    @Test
    void gtChecksSourceVoltageBeforeLossAndSharedCableAmpLimit() {
        var voltage = line(512, 1, 512, 1);
        var failed = new PacketDistributor(voltage.graph, true).tick(EnergyMode.GT, 0);
        assertEquals(0, failed.drawn());
        assertEquals(PacketDistributor.FaultKind.CABLE_VOLTAGE, failed.faults().getFirst().kind());
        var current = line(32, 3, 32, 4);
        var overloaded = new PacketDistributor(current.graph, true).tick(EnergyMode.GT, 0);
        assertEquals(64, overloaded.drawn());
        assertEquals(62, overloaded.delivered());
        assertEquals(
                PacketDistributor.FaultKind.CABLE_CURRENT, overloaded.faults().getFirst().kind());
    }

    @Test
    void sinkDemandAndAmpLimitsApplyAcrossAllSources() {
        var line = line(32, 2, 32, 1);
        var second = new EnergyStore(10000);
        second.insert(1000);
        line.graph.put(p(3), EnergyNode.Terminal.source(second, 32, 2));
        line.graph.connect(p(3), p(1));
        var result = new PacketDistributor(line.graph, true).tick(EnergyMode.GT, 0);
        assertEquals(32, result.drawn());
        assertEquals(31, result.delivered());
        assertTrue(result.faults().isEmpty());
        line.sink.restore(9990);
        assertEquals(0, new PacketDistributor(line.graph, true).tick(EnergyMode.GT, 1).drawn());
    }

    @Test
    void topologyChangesInvalidateRoutesIncludingDirectionAndCycles() {
        var line = line(32, 1, 32, 1);
        var distributor = new PacketDistributor(line.graph, true);
        line.graph.connect(p(1), p(0)); // cycle back to source cannot become a forwarding loop
        assertEquals(31, distributor.tick(EnergyMode.GT, 0).delivered());
        line.graph.disconnect(p(1), p(2));
        assertEquals(0, distributor.tick(EnergyMode.GT, 1).delivered());
        line.graph.connect(p(2), p(1)); // input-only reversed edge is not a path
        assertEquals(0, distributor.tick(EnergyMode.GT, 2).delivered());
        line.graph.connect(p(1), p(2));
        assertEquals(31, distributor.tick(EnergyMode.GT, 3).delivered());
        line.graph.remove(p(1));
        assertEquals(0, distributor.tick(EnergyMode.GT, 4).delivered());
    }

    @Test
    void lowestLossPathWinsAndTerminalsCannotRelayPower() {
        var line = line(32, 1, 32, 1);
        line.graph.put(p(3), new EnergyNode.Conductor(CableSpec.Material.GLASS.insulated(0)));
        line.graph.connect(p(0), p(3));
        line.graph.connect(p(3), p(2));
        var route = line.graph.routesFrom(p(0), EnergyMode.GT).getFirst();
        assertEquals(java.util.List.of(p(3)), route.conductors());
        var remote = new EnergyStore(10000);
        line.graph.put(p(4), EnergyNode.Terminal.sink(remote, 32, 1));
        line.graph.connect(p(2), p(4));
        assertEquals(1, line.graph.routesFrom(p(0), EnergyMode.GT).size());
    }

    @Test
    void classicPacketAmperageIsNotMistakenForSeparateLowVoltagePackets() {
        var line = line(32, 2, 32, 1);
        var result = new PacketDistributor(line.graph, true).tick(EnergyMode.IC2, 0);
        assertEquals(64, result.drawn());
        assertEquals(PacketDistributor.FaultKind.SINK_VOLTAGE, result.faults().getFirst().kind());
        line.graph.put(
                p(0),
                new EnergyNode.Terminal(
                        line.source,
                        Optional.of(new EnergyNode.Output(32, 2, 1, 2)),
                        Optional.empty()));
        result = new PacketDistributor(line.graph, true).tick(EnergyMode.IC2, 1);
        assertEquals(64, result.drawn());
        assertTrue(result.faults().isEmpty());
    }

    @Test
    void unroundedClassicLossAndPartialSinkAcceptanceConserveEnergy() {
        var line = line(32, 1, 32, 1);
        line.sink.restore(9995);
        var result = new PacketDistributor(line.graph, false).tick(EnergyMode.IC2, 0);
        assertEquals(5, result.delivered());
        assertEquals(5.2, result.drawn(), 1e-9);
        assertEquals(0.2, result.dissipated(), 1e-9);
        assertEquals(10995, line.source.stored() + line.sink.stored() + result.dissipated(), 1e-9);
    }
}
