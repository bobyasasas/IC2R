package ic2.neoforge.machine;

import net.neoforged.neoforge.common.ModConfigSpec;

/** World-specific generator settings, separate from electrical network rules. */
public final class GenerationConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue SOLID_HEAT, FLUID_HEAT;
    public static final ModConfigSpec.DoubleValue ELECTRIC_HEAT,
            ELECTRIC_KINETIC,
            STIRLING_CONVERSION,
            KINETIC_CONVERSION;
    public static final ModConfigSpec.DoubleValue WATER_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue WATER_AUTOMATION;
    public static final ModConfigSpec.DoubleValue WIND_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue WIND_BREAKAGE;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("generation");
        SOLID_HEAT = builder.defineInRange("solidHeatMultiplier", 1, 0, 1000000.0);
        FLUID_HEAT = builder.defineInRange("fluidHeatMultiplier", 1, 0, 1000000.0);
        ELECTRIC_HEAT = builder.defineInRange("electricHeatMultiplier", 1, 0, 1000000.0);
        ELECTRIC_KINETIC = builder.defineInRange("electricKineticMultiplier", 1, 0, 1000000.0);
        STIRLING_CONVERSION = builder.defineInRange("stirlingMultiplier", 1, 0, 1000000.0);
        KINETIC_CONVERSION = builder.defineInRange("kineticMultiplier", 1, 0, 1000000.0);
        WIND_MULTIPLIER = builder.defineInRange("windMultiplier", 1, 0, 1000000.0);
        WIND_BREAKAGE =
                builder.comment(
                                "Enable intended overspeed damage; recovered releases never"
                                        + " triggered it.")
                        .define("windBreakage", false);
        WATER_MULTIPLIER = builder.defineInRange("waterMultiplier", 1, 0, 1000000.0);
        WATER_AUTOMATION = builder.define("watermillAutomation", false);
        builder.pop();
        SPEC = builder.build();
    }

    private GenerationConfig() {}
}
