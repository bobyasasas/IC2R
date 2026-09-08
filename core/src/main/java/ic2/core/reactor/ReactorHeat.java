package ic2.core.reactor;

/** Immutable heat accounting; the host decides how to handle positive overflow or destruction. */
public record ReactorHeat(int capacity, int stored) {
    public ReactorHeat {
        if (capacity <= 0 || stored < 0 || stored > capacity)
            throw new IllegalArgumentException("Invalid reactor heat");
    }

    public record Exchange(ReactorHeat heat, int remainder) {}

    /**
     * Positive values add heat; negative values remove it. Remainder retains the request's sign.
     */
    public Exchange exchange(int amount) {
        int next = (int) Math.clamp((long) stored + amount, 0L, capacity);
        return new Exchange(new ReactorHeat(capacity, next), amount - (next - stored));
    }
}
