package ic2.integration.jade;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.IProgressStyle;

import java.util.Locale;

public final class JadeConfigHelper {
    private JadeConfigHelper() {}

    public static IPluginConfig plugin() {
        return IWailaConfig.get().getPlugin();
    }

    public static JadeDisplayMode voltageMode() {
        return getEnum(Ic2JadePluginConfigs.MACHINE_VOLTAGE, JadeDisplayMode.ALWAYS);
    }

    public static JadeDisplayMode powerMode() {
        return getEnum(Ic2JadePluginConfigs.MACHINE_POWER, JadeDisplayMode.ALWAYS);
    }

    public static JadeDisplayMode redstoneMode() {
        return getEnum(Ic2JadePluginConfigs.MACHINE_REDSTONE, JadeDisplayMode.ALWAYS);
    }

    public static JadeDisplayMode activeMode() {
        return getEnum(Ic2JadePluginConfigs.MACHINE_ACTIVE, JadeDisplayMode.SHIFT);
    }

    public static JadeDisplayMode energyMode() {
        return getEnum(Ic2JadePluginConfigs.ENERGY_DISPLAY, JadeDisplayMode.ALWAYS);
    }

    public static JadeDisplayMode progressMode() {
        return getEnum(Ic2JadePluginConfigs.PROGRESS_DISPLAY, JadeDisplayMode.ALWAYS);
    }

    public static JadeEnergyTextMode energyTextMode() {
        return getEnum(Ic2JadePluginConfigs.ENERGY_TEXT_MODE, JadeEnergyTextMode.AMOUNT);
    }

    public static JadeProgressTextMode progressTextMode() {
        return getEnum(Ic2JadePluginConfigs.PROGRESS_TEXT_MODE, JadeProgressTextMode.BOTH);
    }

    public static String energyUnit() {
        String unit = plugin().getString(Ic2JadePluginConfigs.ENERGY_UNIT);
        return unit != null && !unit.isBlank() ? unit.trim() : "EU";
    }

    public static int parseColor(String value, int defaultArgb) {
        if (value == null || value.isBlank()) {
            return defaultArgb;
        }
        String hex = value.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        } else if (hex.regionMatches(true, 0, "0x", 0, 2)) {
            hex = hex.substring(2);
        }

        try {
            long parsed = Long.parseLong(hex, 16);
            if (hex.length() <= 6) {
                // RGB input is opaque; longer input already includes an alpha channel.
                parsed |= 0xFF000000L;
            }

            return (int) parsed;
        } catch (NumberFormatException ignored) {
            return defaultArgb;
        }
    }

    public static int progressColor() {
        return parseColor(plugin().getString(Ic2JadePluginConfigs.PROGRESS_COLOR), 0xFF55FF55);
    }

    public static int progressColor2() {
        return parseColor(
                plugin().getString(Ic2JadePluginConfigs.PROGRESS_COLOR2), progressColor());
    }

    public static int energyColor() {
        return parseColor(plugin().getString(Ic2JadePluginConfigs.ENERGY_COLOR), 0xFFAA0000);
    }

    public static int energyColor2() {
        return parseColor(plugin().getString(Ic2JadePluginConfigs.ENERGY_COLOR2), 0xFF660000);
    }

    public static IProgressStyle progressStyle() {
        return IElementHelper.get().progressStyle().color(progressColor(), progressColor2());
    }

    public static IProgressStyle energyStyle() {
        return IElementHelper.get().progressStyle().color(energyColor(), energyColor2());
    }

    public static Component formatEnergyText(long stored, long capacity, float ratio) {
        JadeEnergyTextMode mode = energyTextMode();
        if (mode == JadeEnergyTextMode.NONE) {
            return null;
        }

        String unit = energyUnit();
        int percent = Math.round(Math.min(1.0F, Math.max(0.0F, ratio)) * 100.0F);
        IDisplayHelper display = IDisplayHelper.get();
        String current = display.humanReadableNumber(stored, unit, false);
        String max = display.humanReadableNumber(capacity, unit, false);

        return switch (mode) {
            case AMOUNT -> Component.translatable("ic2.jade.energy.amount", current, max);
            case PERCENT -> Component.translatable("ic2.jade.progress", percent);
            case BOTH -> Component.translatable("ic2.jade.energy.both", current, max, percent);
            case NONE -> null;
        };
    }

    public static Component formatProgressText(
            float ratio, long current, long max, boolean timeBased) {
        JadeProgressTextMode mode = progressTextMode();
        if (mode == JadeProgressTextMode.NONE) {
            return null;
        }

        int percent = Math.round(Math.min(1.0F, Math.max(0.0F, ratio)) * 100.0F);
        boolean hasAbsolute = max > 0L;
        if (!hasAbsolute || mode == JadeProgressTextMode.PERCENT) {
            return Component.translatable("ic2.jade.progress", percent);
        }

        if (timeBased) {
            String elapsed = formatSeconds(current);
            String total = formatSeconds(max);

            return switch (mode) {
                case FRACTION -> Component.translatable("ic2.jade.progress.time", elapsed, total);
                case BOTH ->
                        Component.translatable(
                                "ic2.jade.progress.time_both", elapsed, total, percent);
                default -> Component.translatable("ic2.jade.progress", percent);
            };
        } else {
            return switch (mode) {
                case FRACTION -> Component.translatable("ic2.jade.progress.fraction", current, max);
                case BOTH ->
                        Component.translatable("ic2.jade.progress.both", current, max, percent);
                default -> Component.translatable("ic2.jade.progress", percent);
            };
        }
    }

    static String formatSeconds(long ticks) {
        double seconds = Math.max(0L, ticks) / 20.0;
        if (seconds >= 100.0) {
            return String.format(Locale.ROOT, "%.0f", seconds);
        }
        if (seconds >= 10.0 || seconds == Math.rint(seconds)) {
            return String.format(Locale.ROOT, "%.1f", seconds);
        }
        return String.format(Locale.ROOT, "%.2f", seconds);
    }

    private static <T extends Enum<T>> T getEnum(ResourceLocation key, T fallback) {
        try {
            T value = plugin().getEnum(key);
            return value != null ? value : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
