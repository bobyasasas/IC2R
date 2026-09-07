package ic2.core.energy.grid;

import ic2.core.energy.EnergyStore;

import java.util.Objects;
import java.util.Optional;

public sealed interface EnergyNode {
    record Conductor(CableSpec specification) implements EnergyNode {
        public Conductor {
            Objects.requireNonNull(specification);
        }
    }

    record Output(int voltage, int maxAmps, int classicPacketAmps, int classicPacketCount) {
        public Output(int voltage, int maxAmps) {
            this(voltage, maxAmps, maxAmps, 1);
        }

        public Output {
            if (voltage <= 0
                    || maxAmps <= 0
                    || classicPacketAmps <= 0
                    || classicPacketCount <= 0
                    || (long) voltage * classicPacketAmps > Integer.MAX_VALUE)
                throw new IllegalArgumentException("Invalid output profile");
        }
    }

    record Input(int voltage, int maxAmps) {
        public Input {
            if (voltage <= 0 || maxAmps <= 0)
                throw new IllegalArgumentException("Invalid input profile");
        }
    }

    record Terminal(EnergyStore energy, Optional<Output> output, Optional<Input> input)
            implements EnergyNode {
        public Terminal {
            Objects.requireNonNull(energy);
            Objects.requireNonNull(output);
            Objects.requireNonNull(input);
            if (output.isEmpty() && input.isEmpty())
                throw new IllegalArgumentException("Terminal has no role");
        }

        public static Terminal source(EnergyStore energy, int voltage, int amps) {
            return new Terminal(energy, Optional.of(new Output(voltage, amps)), Optional.empty());
        }

        public static Terminal sink(EnergyStore energy, int voltage, int amps) {
            return new Terminal(energy, Optional.empty(), Optional.of(new Input(voltage, amps)));
        }
    }
}
