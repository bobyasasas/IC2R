package ic2.core.machine;

import ic2.core.energy.ElectricalProfile;
import ic2.core.energy.VoltageTier;

/** Legacy upgrade arithmetic, saturated before integer conversion or multiplication. */
public record UpgradeProfile(
        int ticks,
        int euPerTick,
        int capacity,
        int operations,
        int itemTier,
        int classicVoltage,
        int workingVoltage,
        int amperage) {
    public record Base(int ticks, int euPerTick, int capacity, int tier, int auxiliaryPower) {
        public Base {
            if (ticks <= 0
                    || euPerTick <= 0
                    || capacity <= 0
                    || tier < 0
                    || tier > 5
                    || auxiliaryPower < 0)
                throw new IllegalArgumentException("Invalid base machine specification");
        }
    }

    public static UpgradeProfile calculate(
            int baseTicks,
            int baseCost,
            int baseCapacity,
            int overclockers,
            int transformers,
            int storage) {
        return calculate(
                new Base(baseTicks, baseCost, baseCapacity, 1, 0),
                overclockers,
                transformers,
                storage);
    }

    public static UpgradeProfile calculate(
            Base base, int overclockers, int transformers, int storage) {
        int baseTicks = base.ticks(), baseCost = base.euPerTick(), baseCapacity = base.capacity();
        if (baseTicks <= 0
                || baseCost <= 0
                || baseCapacity <= 0
                || overclockers < 0
                || overclockers > 256
                || transformers < 0
                || transformers > 256
                || storage < 0
                || storage > 256) throw new IllegalArgumentException("Invalid upgrade parameters");
        double duration = baseTicks * Math.pow(.7, overclockers);
        int operations = saturate(Math.ceil(1 / duration));
        int ticks = Math.max(1, saturate(Math.round(duration * operations)));
        int cost = saturate(Math.round(baseCost * Math.pow(1.6, overclockers)));
        int capacity = saturate(baseCapacity + 10000L * storage + (long) ticks * cost);
        int tier = Math.min(5, base.tier() + transformers);
        var nativeVoltage = VoltageTier.fromIcTier(tier);
        int power = saturate((long) cost * operations + base.auxiliaryPower());
        var working = VoltageTier.fromPower(Math.max(nativeVoltage.getVoltage(), power));
        var electrical = new ElectricalProfile(working);
        electrical.setRecipePower(power);
        return new UpgradeProfile(
                ticks,
                cost,
                capacity,
                operations,
                tier,
                nativeVoltage.getVoltage(),
                working.getVoltage(),
                electrical.getMaxSinkAmperage());
    }

    private static int saturate(double value) {
        return (int) Math.clamp(value, 1, Integer.MAX_VALUE);
    }

    public int rescaleProgress(int progress, int previousTicks) {
        return Math.clamp(
                (int) Math.floor((double) progress / previousTicks * ticks + .1), 0, ticks - 1);
    }
}
