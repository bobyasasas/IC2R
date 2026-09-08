package ic2.core.machine;

/** Whole-millibucket cooling constrained by both tanks and the heat buffer. */
public record HeatExchange(int amount, int heat) {
    public static HeatExchange plan(int available, int outputSpace, int heatSpace, int heatPerMb) {
        if (available < 0 || outputSpace < 0 || heatSpace < 0 || heatPerMb <= 0)
            throw new IllegalArgumentException("Invalid heat exchange limits");
        int amount = Math.min(Math.min(available, outputSpace), heatSpace / heatPerMb);
        return new HeatExchange(amount, amount * heatPerMb);
    }
}
