package ic2.core.machine;

import ic2.core.energy.EnergyStore;

import java.util.Objects;

/** One recipe operation. Inventory validation and atomic output insertion belong to the adapter. */
public final class MachineProcess {
    public record WorkOrder(String recipe, int ticks, double euPerTick) {
        public WorkOrder {
            Objects.requireNonNull(recipe);
            if (recipe.isBlank() || ticks <= 0 || !Double.isFinite(euPerTick) || euPerTick < 0) {
                throw new IllegalArgumentException("Invalid work order");
            }
        }
    }

    public record State(String recipe, int progress) {
        public State {
            Objects.requireNonNull(recipe);
            if (progress < 0) throw new IllegalArgumentException("Negative progress");
        }
    }

    public enum Outcome {
        IDLE,
        PAUSED,
        RUNNING,
        COMPLETED
    }

    private String recipe = "";
    private int progress;

    public State state() {
        return new State(recipe, progress);
    }

    public void restore(State state) {
        recipe = state.recipe();
        progress = state.progress();
    }

    public Outcome tick(WorkOrder order, boolean outputFits, EnergyStore energy) {
        if (order == null) {
            recipe = "";
            progress = 0;
            return Outcome.IDLE;
        }
        if (!recipe.equals(order.recipe())) {
            recipe = order.recipe();
            progress = 0;
        }
        if (!outputFits || !energy.consume(order.euPerTick())) return Outcome.PAUSED;
        if (++progress >= order.ticks()) {
            progress = 0;
            return Outcome.COMPLETED;
        }
        return Outcome.RUNNING;
    }
}
