package ic2.neoforge.client;

import ic2.neoforge.component.CropSeed;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.crop.CropCard;
import ic2.neoforge.menu.CropAnalyzerMenu;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Legacy GuiCropAnalyzer: the info columns reveal themselves with the seed's scan level — name,
 * tier/discoverer, attribute lines, then the exact growth/gain/resistance stats.
 */
public final class CropAnalyzerScreen extends ContainerScreenBase<CropAnalyzerMenu> {
    private static final int WHITE = 0xFFFFFF;
    private static final int GROWTH_COLOR = 0xAE26E6;
    private static final int GAIN_COLOR = 0xEEC900;
    private static final int RESISTANCE_COLOR = 0x00CED1;

    public CropAnalyzerScreen(CropAnalyzerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 223);
    }

    private CropSeed seed() {
        return menu.analyzedStack().get(ModDataComponents.CROP_SEED.get());
    }

    @Override
    public void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        CropSeed seed = seed();
        int scannedLevel = seed == null ? -1 : seed.scan();
        if (scannedLevel < 0) return;

        CropCard card = ModCrops.card(seed.cropId());
        Component name =
                card == null
                        ? Component.literal("UNKNOWN")
                        : Component.translatable("ic2.crop." + card.getId());
        if (scannedLevel == 0) {
            graphics.text(font, Component.literal("UNKNOWN"), 8, 37, WHITE, false);
        } else {
            graphics.text(font, name, 8, 37, WHITE, false);
        }

        if (scannedLevel >= 2) {
            graphics.text(
                    font,
                    Component.literal("Tier: " + (card == null ? "0" : roman(card))),
                    8,
                    50,
                    WHITE,
                    false);
            graphics.text(font, Component.literal("Discovered by:"), 8, 73, WHITE, false);
            graphics.text(
                    font,
                    Component.literal(card == null ? "" : card.getDiscoveredBy()),
                    8,
                    86,
                    WHITE,
                    false);
        }

        if (scannedLevel >= 3) {
            graphics.text(
                    font,
                    Component.literal(card == null ? "" : card.desc(0)),
                    8,
                    109,
                    WHITE,
                    false);
            graphics.text(
                    font,
                    Component.literal(card == null ? "" : card.desc(1)),
                    8,
                    122,
                    WHITE,
                    false);
        }

        if (scannedLevel >= 4) {
            graphics.text(font, Component.literal("Growth:"), 118, 37, GROWTH_COLOR, false);
            graphics.text(
                    font, Component.literal(Integer.toString(seed.growth())), 118, 50, GROWTH_COLOR, false);
            graphics.text(font, Component.literal("Gain:"), 118, 73, GAIN_COLOR, false);
            graphics.text(
                    font, Component.literal(Integer.toString(seed.gain())), 118, 86, GAIN_COLOR, false);
            graphics.text(font, Component.literal("Resistance:"), 118, 109, RESISTANCE_COLOR, false);
            graphics.text(
                    font,
                    Component.literal(Integer.toString(seed.resistance())),
                    118,
                    122,
                    RESISTANCE_COLOR,
                    false);
        }
    }

    /** Legacy getSeedTier roman numerals; unknown tiers fall back to "0". */
    private static String roman(CropCard card) {
        return switch (card.getProperties().tier()) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            case 11 -> "XI";
            case 12 -> "XII";
            case 13 -> "XIII";
            case 14 -> "XIV";
            case 15 -> "XV";
            case 16 -> "XVI";
            default -> "0";
        };
    }
}
