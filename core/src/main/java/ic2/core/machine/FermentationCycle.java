package ic2.core.machine;

/** Paid heat and fertilizer remainder survive pauses, recipe changes and save reloads. */
public final class FermentationCycle {
    public record Batch(int inputAmount, int heat, int fertilizerInterval) {
        public Batch {
            if (inputAmount <= 0 || heat <= 0 || fertilizerInterval <= 0)
                throw new IllegalArgumentException("Fermentation quantities must be positive");
        }
    }

    public record State(int heat, int processed) {
        public State {
            if (heat < 0 || processed < 0)
                throw new IllegalArgumentException("Negative fermentation reserve");
        }
    }

    private int heat, processed;

    public State state() {
        return new State(heat, processed);
    }

    public void restore(State state) {
        heat = state.heat();
        processed = state.processed();
    }

    public int neededHeat(Batch batch) {
        return Math.max(0, batch.heat() - heat);
    }

    public int addHeat(int amount, Batch batch) {
        if (amount < 0) throw new IllegalArgumentException("Negative heat input");
        int accepted = Math.min(amount, neededHeat(batch));
        heat += accepted;
        return accepted;
    }

    public long fertilizer(Batch batch) {
        return ((long) processed + batch.inputAmount()) / batch.fertilizerInterval();
    }

    /**
     * Call only inside the platform transaction that consumes and produces the actual resources.
     */
    public boolean complete(Batch batch) {
        if (neededHeat(batch) != 0) return false;
        heat -= batch.heat();
        processed = (int) (((long) processed + batch.inputAmount()) % batch.fertilizerInterval());
        return true;
    }
}
