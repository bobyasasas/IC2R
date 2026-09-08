package ic2.core.machine;

/** Steam credit is carried across batches, pauses and saves. One water mB costs 100 steam mB. */
public record Condensation(int steamCredit) {
    public static final int BATCH_STEAM = 10000, BATCH_WATER = 100, MAX_CREDIT = 10499;

    public Condensation {
        if (steamCredit < 0 || steamCredit > MAX_CREDIT)
            throw new IllegalArgumentException("Invalid condensed steam credit");
    }

    public record Step(Condensation next, int steam, int water, int energy) {}

    public Step plan(int availableSteam, int waterSpace, int vents, double energy) {
        if (availableSteam < 0
                || waterSpace < 0
                || vents < 0
                || vents > 4
                || !Double.isFinite(energy)
                || energy < 0) throw new IllegalArgumentException("Invalid condensation inputs");
        if (waterSpace < BATCH_WATER) return new Step(this, 0, 0, 0);
        int water = steamCredit >= BATCH_STEAM ? BATCH_WATER : 0;
        int cost = vents * 2;
        int steam = energy >= cost ? Math.min(availableSteam, 100 + vents * 100) : 0;
        return new Step(
                new Condensation(steamCredit - water * 100 + steam),
                steam,
                water,
                steam > 0 ? cost : 0);
    }
}
