package ic2.core.crop;

/**
 * Pure IC2 crop mechanics (legacy TileEntityCrop growth and terrain formulas). The quality model
 * compares a card's weight-influenced environment against the quality demanded by tier and stats;
 * plants short of it grow slower and can die off.
 */
public final class CropMath {
    public record GrowthResult(int points, boolean reset) {}

    /** Legacy performGrowthTick; {@code providedQuality} is the card's weight-influenced total. */
    public static GrowthResult growth(
            int statGrowth,
            int statGain,
            int statResistance,
            int tier,
            int providedQuality,
            CropRandom random) {
        int baseGrowth = 3 + random.nextInt(7) + statGrowth;
        int minimumQuality = (tier - 1) * 4 + statGrowth + statGain + statResistance;
        minimumQuality = Math.max(minimumQuality, 0);
        if (providedQuality >= minimumQuality) {
            int totalGrowth = baseGrowth * (100 + (providedQuality - minimumQuality)) / 100;
            return new GrowthResult(totalGrowth, false);
        }
        int aux = (minimumQuality - providedQuality) * 4;
        if (aux > 100 && random.nextInt(32) > statResistance) {
            return new GrowthResult(0, true);
        }
        int totalGrowth = baseGrowth * (100 - aux) / 100;
        return new GrowthResult(Math.max(totalGrowth, 0), false);
    }

    /** Legacy updateTerrainHumidity: biome bonus, watered farmland and stored water. */
    public static int humidity(int biomeBonus, boolean moistFarmlandBelow, int storageWater) {
        int humidity = biomeBonus;
        if (moistFarmlandBelow) humidity += 2;
        if (storageWater >= 5) humidity += 2;
        humidity += (storageWater + 24) / 25;
        return humidity;
    }

    /** Legacy updateTerrainNutrients: biome bonus plus bare dirt depth and stored nutrients. */
    public static int nutrients(int biomeBonus, int dirtDepth, int storageNutrients) {
        return biomeBonus + dirtDepth + (storageNutrients + 19) / 20;
    }

    /** Legacy updateTerrainAirQuality: altitude bonus, obstruction and open sky. */
    public static int airQuality(int altitude, int obstruction, boolean canSeeSky) {
        int value = Math.clamp(altitude, 0, 2);
        value += obstruction / 2;
        if (canSeeSky) value += 4;
        return value;
    }

    private CropMath() {}
}
