package ic2.core.crop;

/**
 * Legacy CropProperties: the crop card trait values. {@code tier} drives growth difficulty and drop
 * chances; the remaining traits classify the crop for breeding (a later slice).
 */
public record CropProperties(
        int tier, int chemistry, int consumable, int defensive, int colorful, int weed) {}
