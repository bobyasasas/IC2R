package ic2.neoforge.client;

import ic2.neoforge.menu.MeterMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Legacy GuiToolMeter: five mode buttons, a reset region and the SI-scaled readouts. */
public final class MeterScreen extends ContainerScreenBase<MeterMenu> {
    private static final int TEXT_COLOR = 0xff404040;

    public MeterScreen(MeterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 217, 137);
    }

    @Override
    public void init() {
        super.init();
        MeterMenu.Mode[] modes = MeterMenu.Mode.values();
        int[][] bounds = {
            {112, 55}, {132, 55}, {112, 75}, {132, 75}, {152, 65}
        };
        for (int id = 0; id < modes.length; id++) {
            int x = bounds[id][0], y = bounds[id][1];
            final int buttonId = id;
            addRenderableWidget(
                    Button.builder(
                                    Component.literal(
                                            switch (modes[id]) {
                                                case ENERGY_IN -> "In";
                                                case ENERGY_OUT -> "Out";
                                                case ENERGY_GAIN -> "Gain";
                                                case VOLTAGE -> "V";
                                                case AMPERAGE -> "A";
                                            }),
                                    button -> send(buttonId))
                            .bounds(leftPos + x, topPos + y, 20, 20)
                            .build());
        }
        addRenderableWidget(
                Button.builder(
                                Component.translatable("ic2.meter.mode.reset"),
                                button -> send(MeterMenu.RESET_BUTTON))
                        .bounds(leftPos + 26, topPos + 110, 58, 14)
                        .build());
    }

    private void send(int id) {
        if (minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        String unit =
                switch (menu.mode()) {
                    case VOLTAGE -> Component.translatable("ic2.generic.text.v").getString();
                    case AMPERAGE -> Component.translatable("ic2.generic.text.a").getString();
                    default -> Component.translatable("ic2.generic.text.EUt").getString();
                };
        text(graphics, x + 15, y + 42, Component.translatable("ic2.meter.avg").getString());
        text(graphics, x + 15, y + 52, toSiString(menu.resultAvg(), 6) + unit);
        text(graphics, x + 15, y + 66, Component.translatable("ic2.meter.max/min").getString());
        text(graphics, x + 15, y + 76, toSiString(menu.resultMax(), 6) + unit);
        text(graphics, x + 15, y + 86, toSiString(menu.resultMin(), 6) + unit);
        text(
                graphics,
                x + 15,
                y + 100,
                Component.translatable("ic2.meter.cycle", menu.resultCount() / 20).getString());
        text(graphics, x + 115, y + 43, Component.translatable("ic2.meter.mode").getString());
        text(
                graphics,
                x + 115,
                y + 123,
                Component.translatable(
                                "ic2.meter.mode."
                                        + switch (menu.mode()) {
                                            case ENERGY_IN -> "EnergyIn";
                                            case ENERGY_OUT -> "EnergyOut";
                                            case ENERGY_GAIN -> "EnergyGain";
                                            case VOLTAGE -> "Voltage";
                                            case AMPERAGE -> "Amperage";
                                        })
                                .getString());
    }

    private void text(GuiGraphicsExtractor graphics, int x, int y, String value) {
        graphics.text(font, Component.literal(value), x, y, TEXT_COLOR, false);
    }

    /** Legacy Util.toSiString: six significant digits with the k/M/G/T engineering prefix. */
    static String toSiString(double value, int digits) {
        if (value == 0.0) return "0 ";
        if (Double.isNaN(value)) return "NaN ";
        String ret = "";
        if (value < 0.0) {
            ret = "-";
            value = -value;
        }
        if (Double.isInfinite(value)) return ret + "∞ ";

        double log = Math.log10(value);
        double mul;
        String si;
        if (log >= 0.0) {
            int reduce = (int) Math.floor(log / 3.0);
            mul = 1.0 / Math.pow(10.0, reduce * 3);
            si =
                    switch (reduce) {
                        case 0 -> "";
                        case 1 -> "k";
                        case 2 -> "M";
                        case 3 -> "G";
                        case 4 -> "T";
                        case 5 -> "P";
                        case 6 -> "E";
                        case 7 -> "Z";
                        case 8 -> "Y";
                        default -> "E" + reduce * 3;
                    };
        } else {
            int expand = (int) Math.ceil(-log / 3.0);
            mul = Math.pow(10.0, expand * 3);
            si =
                    switch (expand) {
                        case 0 -> "";
                        case 1 -> "m";
                        case 2 -> "µ";
                        case 3 -> "n";
                        case 4 -> "p";
                        case 5 -> "f";
                        case 6 -> "a";
                        case 7 -> "z";
                        case 8 -> "y";
                        default -> "E-" + expand * 3;
                    };
        }

        value *= mul;
        int iVal = (int) Math.floor(value);
        value -= iVal;
        int iDigits = 1;
        if (iVal > 0) iDigits = 1 + (int) Math.floor(Math.log10(iVal));

        mul = Math.pow(10.0, digits - iDigits);
        int dVal = (int) Math.round(value * mul);
        if (dVal >= mul) {
            iVal++;
            dVal = (int) (dVal - mul);
            iDigits = 1;
            if (iVal > 0) iDigits = 1 + (int) Math.floor(Math.log10(iVal));
        }

        ret = ret + iVal;
        if (digits > iDigits && dVal != 0)
            ret = ret + String.format(".%0" + (digits - iDigits) + "d", dVal);
        ret = ret.replaceFirst("(\\.\\d*?)0+$", "$1");
        return ret + " " + si;
    }
}
