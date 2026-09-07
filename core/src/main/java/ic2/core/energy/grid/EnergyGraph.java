package ic2.core.energy.grid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Directed connectivity with cached lowest-loss routes. Only conductors can be transit nodes. */
public final class EnergyGraph {
    public record Route(GridPosition target, List<GridPosition> conductors, double loss) {
        public Route {
            conductors = List.copyOf(conductors);
        }
    }

    private record RouteKey(GridPosition source, EnergyMode mode) {}

    private record Visit(GridPosition position, double cost) {}

    private final Map<GridPosition, EnergyNode> nodes = new TreeMap<>();
    private final Map<GridPosition, Set<GridPosition>> edges = new HashMap<>();
    private final Map<RouteKey, List<Route>> routes = new HashMap<>();

    public void put(GridPosition position, EnergyNode node) {
        if (!node.equals(nodes.put(position, node))) routes.clear();
        edges.computeIfAbsent(position, ignored -> new TreeSet<>());
    }

    public void remove(GridPosition position) {
        if (nodes.remove(position) == null) return;
        edges.remove(position);
        edges.values().forEach(neighbors -> neighbors.remove(position));
        routes.clear();
    }

    public void connect(GridPosition from, GridPosition to) {
        if (!nodes.containsKey(from) || !nodes.containsKey(to) || from.equals(to)) {
            throw new IllegalArgumentException("Invalid graph edge");
        }
        if (edges.get(from).add(to)) routes.clear();
    }

    public void disconnect(GridPosition from, GridPosition to) {
        if (edges.containsKey(from) && edges.get(from).remove(to)) routes.clear();
    }

    public Map<GridPosition, EnergyNode> nodes() {
        return Map.copyOf(nodes);
    }

    public EnergyNode node(GridPosition position) {
        return nodes.get(position);
    }

    public List<Route> routesFrom(GridPosition source, EnergyMode mode) {
        return routes.computeIfAbsent(
                new RouteKey(source, mode), ignored -> findRoutes(source, mode));
    }

    private List<Route> findRoutes(GridPosition source, EnergyMode mode) {
        var costs = new HashMap<GridPosition, Double>();
        var previous = new HashMap<GridPosition, GridPosition>();
        var queue =
                new PriorityQueue<Visit>(
                        Comparator.comparingDouble(Visit::cost).thenComparing(Visit::position));
        var destinations = new LinkedHashMap<GridPosition, Double>();
        costs.put(source, 0.0);
        queue.add(new Visit(source, 0));
        while (!queue.isEmpty()) {
            Visit visit = queue.remove();
            if (visit.cost() > costs.getOrDefault(visit.position(), Double.POSITIVE_INFINITY))
                continue;
            for (GridPosition neighbor : edges.getOrDefault(visit.position(), Set.of())) {
                if (neighbor.equals(source)) continue;
                EnergyNode node = nodes.get(neighbor);
                double cost =
                        visit.cost()
                                + (node instanceof EnergyNode.Conductor cable
                                        ? cable.specification().loss(mode)
                                        : 0);
                if (cost >= costs.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) continue;
                costs.put(neighbor, cost);
                previous.put(neighbor, visit.position());
                if (node instanceof EnergyNode.Conductor) queue.add(new Visit(neighbor, cost));
                else if (node instanceof EnergyNode.Terminal terminal
                        && terminal.input().isPresent()) destinations.put(neighbor, cost);
            }
        }
        var result = new ArrayList<Route>();
        destinations.forEach(
                (target, cost) -> {
                    var conductors = new ArrayList<GridPosition>();
                    for (GridPosition step = previous.get(target);
                            !step.equals(source);
                            step = previous.get(step)) conductors.add(step);
                    result.add(new Route(target, conductors.reversed(), cost));
                });
        result.sort(Comparator.comparingDouble(Route::loss).thenComparing(Route::target));
        return List.copyOf(result);
    }
}
