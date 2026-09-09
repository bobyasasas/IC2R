package ic2.core.machine;

/**
 * One heat unit re-pressurizes ten input steam mB per batch into configured external steam. The
 * heat reserve is bounded, persists across ticks and is only refilled up to the current demand.
 */
public record Repressurization(int reserve) {
    public static final int TANK = 10000, BATCH_INPUT = 10, BATCH_HEAT = 1, RESERVE_CAP = 1000;

    public Repressurization {
        if (reserve < 0 || reserve > RESERVE_CAP)
            throw new IllegalArgumentException("Invalid repressurizer heat reserve");
    }

    public record Step(Repressurization next, int input, int output, int heat) {}

    /**
     * Runs as many batches as input, reserve and output space allow. A zero rate disables the
     * machine outright; the recovered release would have consumed steam and heat without any
     * output, which is a misconfiguration rather than a mode.
     */
    public Step process(int inputMb, int outputSpace, int rate) {
        if (inputMb < 0 || outputSpace < 0 || rate < 0)
            throw new IllegalArgumentException("Invalid repressurization inputs");
        if (rate == 0 || reserve == 0) return new Step(this, 0, 0, 0);
        int input = 0, output = 0, heat = 0;
        while (reserve - heat >= BATCH_HEAT
                && inputMb - input >= BATCH_INPUT
                && outputSpace - output >= rate) {
            input += BATCH_INPUT;
            output += rate;
            heat += BATCH_HEAT;
        }
        return new Step(new Repressurization(reserve - heat), input, output, heat);
    }

    /**
     * Heat is drawn to hold the reserve at the whole current input demand and never beyond the cap.
     * Unlike the recovered release the request already excludes the stored reserve, so neighbours
     * are not charged twice for heat the machine still holds.
     */
    public int heatRequest(int inputMb) {
        if (inputMb < 0) throw new IllegalArgumentException("Invalid repressurization input");
        return Math.max(0, Math.min(inputMb / BATCH_INPUT, RESERVE_CAP) - reserve);
    }

    public int absorb(int drawn) {
        if (drawn < 0) throw new IllegalArgumentException("Invalid drawn heat");
        return Math.min(RESERVE_CAP, reserve + drawn);
    }
}
