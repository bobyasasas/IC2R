package ic2.neoforge.energy;

import ic2.core.energy.grid.EnergyMode;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EnergyConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue STORAGE_DROP_RETENTION;
    public static final ModConfigSpec.EnumValue<EnergyMode> MODE;
    public static final ModConfigSpec.BooleanValue ROUND_CLASSIC_LOSS;
    public static final ModConfigSpec.BooleanValue CABLE_MELTDOWN;
    public static final ModConfigSpec.BooleanValue MACHINE_EXPLOSIONS;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("energy");
        STORAGE_DROP_RETENTION = builder.defineInRange("storageDropRetention", 0.8, 0.0, 1.0);
        MODE =
                builder.comment("IC2: classic EU packets; GT: voltage and amperage limits.")
                        .defineEnum("mode", EnergyMode.IC2);
        ROUND_CLASSIC_LOSS = builder.define("roundClassicLoss", true);
        CABLE_MELTDOWN = builder.define("cableMeltdown", true);
        MACHINE_EXPLOSIONS = builder.define("machineExplosions", true);
        builder.pop();
        SPEC = builder.build();
    }

    private EnergyConfig() {}
}
