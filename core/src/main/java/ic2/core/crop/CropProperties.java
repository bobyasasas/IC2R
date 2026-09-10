package ic2.core.crop;

/**
 * Legacy CropProperties: the crop card trait values. {@code tier} drives growth difficulty and drop
 * chances; the remaining traits classify the crop for breeding (a later slice).
 */
public record CropProperties(
        int tier, int chemistry, int consumable, int defensive, int colorful, int weed) {

    /** Legacy getAllProperties: the five non-tier trait values feeding the breeding ratios. */
    public int[] getAllProperties() {
        return new int[] {chemistry, consumable, defensive, colorful, weed};
    }
}
