package ic2.core.uu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure UU-value graph (legacy UuGraph/UuIndex): nodes are keyed by item id, initial values seed
 * base resources, and every recipe transformation propagates the minimal input value to its
 * outputs plus the transformation cost.
 */
public final class UuValueGraph {
    /** One recipe transformation: each input group lists alternative item keys. */
    public record Transformation(double cost, List<List<String>> inputGroups, List<String> outputs) {
        public Transformation {
            inputGroups = List.copyOf(inputGroups);
            outputs = List.copyOf(outputs);
        }
    }

    private static final class Node {
        double value = Double.POSITIVE_INFINITY;
    }

    private final Map<String, Node> nodes = new HashMap<>();
    private final List<Transformation> transformations = new ArrayList<>();

    public void setInitial(String itemKey, double value) {
        nodes.computeIfAbsent(itemKey, key -> new Node()).value = value;
    }

    public void addTransformation(Transformation transformation) {
        transformations.add(transformation);
    }

    /** Applies initial values, then transforms until the value assignment stabilises. */
    public void build() {
        for (var transformation : transformations) {
            for (var group : transformation.inputGroups()) {
                for (var key : group) nodes.computeIfAbsent(key, key2 -> new Node());
            }
            for (var output : transformation.outputs())
                nodes.computeIfAbsent(output, key -> new Node());
        }
        // Legacy min-assignment: keep applying transformations while any output value improves.
        boolean changed = true;
        int guard = 0;
        while (changed && guard++ < 10000) {
            changed = false;
            for (var transformation : transformations) {
                double inputCost = minInputCost(transformation);
                if (!Double.isFinite(inputCost)) continue;
                double perOutput = inputCost + transformation.cost();
                for (var output : transformation.outputs()) {
                    Node node = nodes.get(output);
                    if (node != null && node.value > perOutput) {
                        node.value = perOutput;
                        changed = true;
                    }
                }
            }
        }
    }

    private double minInputCost(Transformation transformation) {
        double total = 0;
        for (var group : transformation.inputGroups()) {
            double min = Double.POSITIVE_INFINITY;
            for (var key : group) {
                Node node = nodes.get(key);
                if (node != null && node.value < min) min = node.value;
            }
            if (!Double.isFinite(min)) return Double.POSITIVE_INFINITY;
            total += min;
        }
        return total;
    }

    /** The UU value of an item, or infinity when no recipe path or seed defines it. */
    public double get(String itemKey) {
        Node node = nodes.get(itemKey);
        return node == null ? Double.POSITIVE_INFINITY : node.value;
    }
}
