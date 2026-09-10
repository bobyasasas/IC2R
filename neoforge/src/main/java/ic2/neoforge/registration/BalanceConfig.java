package ic2.neoforge.registration;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Converted legacy balance values, starting with the steam re-pressurizer output rates. */
public final class BalanceConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue STEAM_PER_STEAM;
    public static final ModConfigSpec.IntValue STEAM_PER_SUPER_STEAM;
    // Legacy IC2Config.protection: nuke availability and blast power ceiling.
    public static final ModConfigSpec.BooleanValue ENABLE_NUKE;
    public static final ModConfigSpec.DoubleValue NUKE_EXPLOSION_POWER_LIMIT;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("balance");
        STEAM_PER_STEAM =
                builder.comment("External steam mB produced per 10 mB of ordinary IC2 steam.")
                        .defineInRange("steamRepressurizerPerSteam", 16, 0, Integer.MAX_VALUE);
        STEAM_PER_SUPER_STEAM =
                builder.comment("External steam mB produced per 10 mB of superheated IC2 steam.")
                        .defineInRange("steamRepressurizerPerSuperSteam", 32, 0, Integer.MAX_VALUE);
        builder.pop();
        builder.push("protection");
        ENABLE_NUKE =
                builder.comment("Whether priming a nuke spawns its charge at all.")
                        .define("enableNuke", true);
        NUKE_EXPLOSION_POWER_LIMIT =
                builder.comment("Upper bound for the computed nuke blast power.")
                        .defineInRange("nukeExplosionPowerLimit", 60.0, 0.0, Double.MAX_VALUE);
        builder.pop();
        SPEC = builder.build();
    }

    private BalanceConfig() {}
}
