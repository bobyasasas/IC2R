package ic2.core.energy.grid;

/** Electrical and physical properties copied from the recovered CableType/CableSpec tables. */
public record CableSpec(
        int voltageLimit,
        int ampLimit,
        double classicLoss,
        int gtLoss,
        int insulation,
        int maxInsulation,
        double diameter) {
    public enum Material {
        TIN(32, 1, 0.2, 1, 1, 0.25),
        COPPER(128, 2, 0.2, 1, 1, 0.25),
        GOLD(512, 3, 0.4, 2, 2, 0.1875),
        IRON(2048, 4, 0.8, 3, 3, 0.375),
        GLASS(8192, 8, 0.025, 0, 0, 0.25),
        DETECTOR(8192, 64, 0.5, 0, 0, 0.5),
        SPLITTER(8192, 64, 0.5, 0, 0, 0.5);
        private final int voltage, amps, gtLoss, maxInsulation;
        private final double classicLoss, diameter;

        Material(
                int voltage,
                int amps,
                double classicLoss,
                int gtLoss,
                int maxInsulation,
                double diameter) {
            this.voltage = voltage;
            this.amps = amps;
            this.classicLoss = classicLoss;
            this.gtLoss = gtLoss;
            this.maxInsulation = maxInsulation;
            this.diameter = diameter;
        }

        public CableSpec insulated(int insulation) {
            if (insulation < 0 || insulation > maxInsulation)
                throw new IllegalArgumentException("Invalid insulation");
            return new CableSpec(
                    voltage,
                    amps,
                    classicLoss,
                    insulation == 0 ? gtLoss * 2 : gtLoss,
                    insulation,
                    maxInsulation,
                    diameter + 0.125 * insulation);
        }
    }

    public CableSpec {
        if (voltageLimit <= 0
                || ampLimit <= 0
                || !Double.isFinite(classicLoss)
                || classicLoss < 0
                || gtLoss < 0
                || insulation < 0
                || maxInsulation < 0
                || insulation > maxInsulation
                || !Double.isFinite(diameter)
                || diameter <= 0
                || diameter > 1) throw new IllegalArgumentException("Invalid cable specification");
    }

    public double loss(EnergyMode mode) {
        return mode == EnergyMode.GT ? gtLoss : classicLoss;
    }

    /**
     * Packet power an unshielded conductor starts shocking entities at, from the recovered
     * AbstractCableBlock conductor: the never-shielding glass family returns positive infinity and
     * higher capacity cables gain one tier of protection over their insulation level.
     */
    public double insulationAbsorption() {
        if (maxInsulation == 0) return Double.POSITIVE_INFINITY;
        return powerFromTier(voltageLimit < 128 ? insulation : insulation + 1);
    }

    /** Legacy EnergyNet tier power: eight EU doubled by powers of four per tier step. */
    public static double powerFromTier(int tier) {
        if (tier < 14) return 8 << (tier * 2);
        return tier < 30 ? 8.0 * Math.pow(4.0, tier) : 9.223372E18;
    }
}
