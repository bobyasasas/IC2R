package ic2.neoforge.machine;

import net.neoforged.neoforge.common.ModConfigSpec;

/** World-specific generator settings, separate from electrical network rules. */
public final class GenerationConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue WATER_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue WATER_AUTOMATION;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("generation");
        WATER_MULTIPLIER = builder.defineInRange("waterMultiplier", 1, 0, 1000000.0);
        WATER_AUTOMATION = builder.define("watermillAutomation", false);
        builder.pop();
        SPEC = builder.build();
    }

    private GenerationConfig() {}
}
