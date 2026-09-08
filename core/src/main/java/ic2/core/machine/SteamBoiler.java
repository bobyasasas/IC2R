package ic2.core.machine;

/** Recovered boiler thermodynamics, separated from fluid delivery and explosion side effects. */
public final class SteamBoiler {
    public static final int MAX_HEAT_INPUT = 1200, MAX_SCALE = 100000;
    private static final double HEAT_PER_HU = .0005, EPSILON = .0001;

    public record Settings(int waterPerTick, int pressure) {
        public Settings {
            if (waterPerTick < 0 || waterPerTick > 1000 || pressure < 0 || pressure > 300)
                throw new IllegalArgumentException("Invalid boiler settings");
        }

        /** Eight flow buttons, then six pressure buttons. Unknown requests are rejected. */
        public Settings configure(int button) {
            if (button < 0 || button > 13)
                throw new IllegalArgumentException("Unknown boiler button");
            int[] adjustments = {1, 10, 100, 1000, -1, -10, -100, -1000, 1, 10, 100, -1, -10, -100};
            return button < 8
                    ? new Settings(
                            Math.clamp(waterPerTick + adjustments[button], 0, 1000), pressure)
                    : new Settings(
                            waterPerTick, Math.clamp(pressure + adjustments[button], 0, 300));
        }
    }

    public record State(double temperature, int scale, long tick) {
        public State {
            if (!Double.isFinite(temperature)
                    || temperature < 0
                    || temperature > 500
                    || scale < 0
                    || scale > MAX_SCALE)
                throw new IllegalArgumentException("Invalid boiler state");
        }

        public boolean calcified() {
            return scale == MAX_SCALE;
        }
    }

    public enum Output {
        NONE,
        WATER,
        DISTILLED_WATER,
        STEAM,
        SUPERHEATED_STEAM;

        public boolean liquidWater() {
            return this == WATER || this == DISTILLED_WATER;
        }
    }

    /** Water is the planned consumption; warmup water delivery consumes only its accepted part. */
    public record Step(
            State next, int water, int outputAmount, Output output, boolean overheated) {}

    public static Step step(
            State state,
            Settings settings,
            long tick,
            int heatInput,
            int water,
            boolean distilled,
            int ambientTemperature) {
        if (heatInput < 0
                || heatInput > MAX_HEAT_INPUT
                || water < 0
                || ambientTemperature < 0
                || ambientTemperature > 500)
            throw new IllegalArgumentException("Invalid boiler tick inputs");
        if (state.tick() == tick) return new Step(state, 0, 0, Output.NONE, false);
        double temperature = Math.max(state.temperature(), ambientTemperature);
        if (state.calcified() || heatInput == 0)
            return result(
                    Math.max(ambientTemperature, temperature - .01),
                    state.scale(),
                    tick,
                    0,
                    0,
                    Output.NONE);
        if (water == 0 || settings.waterPerTick() == 0)
            return result(
                    temperature + heatInput * HEAT_PER_HU, state.scale(), tick, 0, 0, Output.NONE);

        int maximum = Math.min(water, settings.waterPerTick());
        double boilingCost = 100 + settings.pressure() / 220.0 * 100;
        double target = 100 + settings.pressure() / 220.0 * 274;
        double deficit = target - temperature, remaining = heatInput;
        if (deficit > EPSILON) {
            int warmup = (int) Math.ceil(deficit / HEAT_PER_HU);
            if (heatInput <= warmup) {
                temperature += heatInput * HEAT_PER_HU;
                boolean passWater = settings.pressure() == 0 && temperature < 100 - EPSILON;
                return result(
                        temperature,
                        state.scale(),
                        tick,
                        passWater ? maximum : 0,
                        passWater ? maximum : 0,
                        passWater
                                ? distilled ? Output.DISTILLED_WATER : Output.WATER
                                : Output.NONE);
            }
            temperature += warmup * HEAT_PER_HU;
            remaining -= warmup;
            deficit = target - temperature;
        }

        double storedHeat = Math.min(-deficit / HEAT_PER_HU, MAX_HEAT_INPUT - heatInput);
        int boiled = Math.max(0, Math.min(maximum, (int) ((remaining + storedHeat) / boilingCost)));
        int consumed = boiled;
        remaining -= boiled * boilingCost;
        if (remaining < 0) {
            temperature = Math.max(ambientTemperature, temperature + remaining * HEAT_PER_HU);
            deficit = target - temperature;
        }
        if (deficit <= -.1001) {
            int coolingWater = Math.min(maximum, Math.min(20, (int) (-deficit / .1)));
            temperature = Math.max(ambientTemperature, temperature - coolingWater * .1);
            consumed = Math.max(consumed, coolingWater);
        }
        if (remaining > 0) temperature += remaining * HEAT_PER_HU;
        int scale =
                distilled
                        ? state.scale()
                        : (int) Math.min(MAX_SCALE, (long) state.scale() + consumed);
        var output =
                boiled == 0
                        ? Output.NONE
                        : temperature >= 374 - EPSILON ? Output.SUPERHEATED_STEAM : Output.STEAM;
        return result(temperature, scale, tick, consumed, boiled * 100, output);
    }

    private static Step result(
            double temperature, int scale, long tick, int water, int output, Output type) {
        return new Step(
                new State(Math.min(500, temperature), scale, tick),
                water,
                output,
                type,
                temperature > 500);
    }

    private SteamBoiler() {}
}
