package ic2.core.machine;

/** One water mB per tick, with a long-lived ledger for the steam already condensed. */
public final class SteamTurbineCycle {
    public static final int STEAM_CAPACITY = 21000, WATER_CAPACITY = 1000;
    public static final long MAX_CREDIT = Long.MAX_VALUE - STEAM_CAPACITY / 10;

    public record State(long condensedSteam, long tick) {
        public State {
            if (condensedSteam < 0 || condensedSteam > MAX_CREDIT)
                throw new IllegalArgumentException("Invalid turbine condensate credit");
        }
    }

    public record Step(State next, int steam, int exhaust, int water, int kinetic) {}

    public static Step plan(
            State state,
            long tick,
            int steam,
            boolean hot,
            int water,
            boolean canAcceptCondensate,
            double multiplier) {
        if (steam < 0
                || steam > STEAM_CAPACITY
                || water < 0
                || water > WATER_CAPACITY
                || !Double.isFinite(multiplier)
                || multiplier < 0) throw new IllegalArgumentException("Invalid turbine inputs");
        if (state.tick() == tick) return new Step(state, 0, 0, 0, 0);
        int kinetic =
                (int)
                        Math.min(
                                Integer.MAX_VALUE,
                                Math.floor(
                                        (long) steam
                                                * (hot ? 4 : 2)
                                                * (WATER_CAPACITY - water)
                                                / 1000.0
                                                * multiplier));
        if (kinetic == 0) steam = 0;
        int condensed = hot ? 0 : steam / 10;
        long credit = state.condensedSteam();
        if (credit > MAX_CREDIT - condensed || credit + condensed >= 100 && !canAcceptCondensate) {
            steam = 0;
            kinetic = 0;
            condensed = 0;
        }
        credit += condensed;
        int resultWater = credit >= 100 && canAcceptCondensate && water < WATER_CAPACITY ? 1 : 0;
        return new Step(
                new State(credit - resultWater * 100L, tick),
                steam,
                steam - condensed,
                resultWater,
                kinetic);
    }

    private SteamTurbineCycle() {}
}
