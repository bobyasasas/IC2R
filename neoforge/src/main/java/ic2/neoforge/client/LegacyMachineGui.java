package ic2.neoforge.client;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Legacy IC2 machine GUI atlas data.
 *
 * <p>The old client kept these coordinates in dozens of GUI classes and XML files. Keeping the
 * immutable rendering data here makes the port auditable without spreading magic coordinates
 * through every screen implementation.
 */
final class LegacyMachineGui {
    static final Identifier COMMON = texture("common.png");
    static final Identifier THERMAL_CENTRIFUGE = texture("guithermalcentrifuge.png");
    static final Identifier ORE_WASHING = texture("guiorewashingplant.png");
    static final Identifier SOLID_HEAT_GENERATOR = texture("guisolidheatgenerator.png");
    static final Identifier PUMP_ARROW = texture("overlay/pump_arrow.png");
    static final Identifier INDUCTION_INPUT = texture("overlay/induction_furnace_input.png");
    static final Identifier INDUCTION_OUTPUT = texture("overlay/induction_furnace_output.png");

    private LegacyMachineGui() {}

    static Identifier background(MachineMenu menu) {
        MachineKind kind = menu.kind();
        if (kind.storage()) return texture("guielectricblock.png");
        if (kind.transformer()) return texture("guitransfomer.png");
        if (kind.chargepad()) return texture("guichargepadblock.png");
        return switch (kind) {
            case STEAM_KINETIC_GENERATOR -> texture("guisteamkineticgenerator.png");
            case STEAM_GENERATOR -> texture("guisteamgenerator.png");
            case STEAM_REPRESSURIZER -> texture("guisteamrepressurizer.png");
            case CONDENSER -> texture("guicondenser.png");
            case FLUID_REGULATOR -> texture("guifluidregulator.png");
            case ELECTROLYZER -> texture("guielectrolyzer.png");
            case LIQUID_HEAT_EXCHANGER -> texture("guiheatsourcefluid.png");
            case FERMENTER -> texture("guifermenter.png");
            case SOLAR_DISTILLER -> texture("guisolardestiller.png");
            case CANNER -> texture("guicanner.png");
            case WATER_KINETIC_GENERATOR -> texture("guiwaterkineticgenerator.png");
            case WIND_KINETIC_GENERATOR -> texture("guiwindkineticgenerator.png");
            case CROPMATRON -> texture("guicropmatron.png");
            case CROP_HARVESTER -> texture("guicropharvester.png");
            case MAGNETIZER -> texture("guimagnetizer.png");
            case MINER -> texture("guiminer.png");
            case ADV_MINER -> texture("guiadvminer.png");
            case SORTING_MACHINE -> texture("guisortingmachine.png");
            case TRADE_O_MAT ->
                    texture(menu.tradeEditable() ? "guitradeomatopen.png" : "guitradeomatclosed.png");
            case ENERGY_O_MAT -> texture("guienergyomatopen.png");
            case ITEM_BUFFER -> texture("guiitembuffer.png");
            case MATTER_GENERATOR -> texture("guimatter.png");
            case NUCLEAR_REACTOR -> texture("guinuclearreactor.png");
            case REPLICATOR -> texture("guireplicator.png");
            case UU_SCANNER -> texture("guiscanner.png");
            case PATTERN_STORAGE -> texture("guipatternstorage.png");
            case RT_HEAT_GENERATOR -> texture("guirtheatgenerator.png");
            case FLUID_HEAT_GENERATOR -> texture("guifluidheatgenerator.png");
            case ELECTRIC_HEAT_GENERATOR -> texture("guielectricheatgenerator.png");
            case ELECTRIC_KINETIC_GENERATOR -> texture("guielectrickineticgenerator.png");
            case STIRLING_KINETIC_GENERATOR -> texture("guistirlingkineticgenerator.png");
            case METAL_FORMER -> texture("guimetalformer.png");
            case CHUNK_LOADER -> texture("guichunkloader.png");
            case FLUID_DISTRIBUTOR -> texture("guifluiddistributor.png");
            case WEIGHTED_FLUID_DISTRIBUTOR -> texture("guiweightedfluiddistributor.png");
            case WEIGHTED_ITEM_DISTRIBUTOR -> texture("guiweighteditemdistributor.png");
            case BATCH_CRAFTER -> texture("guibatchcrafter.png");
            case INDUSTRIAL_WORKBENCH -> texture("guiindustrialworkbench.png");
            default -> null;
        };
    }

    static GaugeSpec energy(MachineKind kind) {
        if (kind.storage() || kind.chargepad()) return GaugeSpec.energyBar(79, 38);
        return switch (kind) {
            case GENERATOR -> GaugeSpec.energyBar(100, 39);
            case GEO_GENERATOR, SEMIFLUID_GENERATOR -> GaugeSpec.energyBar(110, 30);
            case RT_GENERATOR -> GaugeSpec.energyBar(115, 39);
            case ELECTRIC_FURNACE, MACERATOR, EXTRACTOR, COMPRESSOR, RECYCLER ->
                    GaugeSpec.energyBolt(59, 37);
            case BLOCK_CUTTER -> GaugeSpec.energyBolt(29, 37);
            case CENTRIFUGE, ORE_WASHING_PLANT -> GaugeSpec.energyBolt(15, 38);
            case INDUCTION_FURNACE -> GaugeSpec.energyBolt(55, 37);
            case METAL_FORMER -> GaugeSpec.energyBolt(20, 37);
            case PUMP -> GaugeSpec.energyBolt(12, 28);
            case RCI_RSH, RCI_LZH -> GaugeSpec.energyBolt(7, 55);
            case CANNER -> GaugeSpec.energyBolt(12, 62);
            case ADV_MINER -> GaugeSpec.energyBolt(12, 55);
            case BATCH_CRAFTER -> GaugeSpec.energyBolt(12, 45);
            case CONDENSER -> GaugeSpec.energyBolt(12, 26);
            case CROP_HARVESTER -> GaugeSpec.energyBolt(19, 37);
            case CROPMATRON -> GaugeSpec.energyBolt(138, 82);
            case ELECTROLYZER, ELECTRIC_HEAT_GENERATOR, ELECTRIC_KINETIC_GENERATOR ->
                    GaugeSpec.energyBolt(12, 44);
            case FLUID_REGULATOR -> GaugeSpec.energyBolt(12, 39);
            case MAGNETIZER -> GaugeSpec.energyBolt(11, 28);
            case MINER -> GaugeSpec.energyBolt(155, 41);
            case REPLICATOR -> GaugeSpec.energyBolt(136, 84);
            case UU_SCANNER -> GaugeSpec.energyBolt(12, 25);
            case SORTING_MACHINE -> GaugeSpec.energyBolt(174, 220);
            case CHUNK_LOADER -> GaugeSpec.energyBolt(12, 125);
            default -> null;
        };
    }

    static GaugeSpec progress(MachineKind kind) {
        return switch (kind) {
            case IRON_FURNACE, ELECTRIC_FURNACE, INDUCTION_FURNACE, BLAST_FURNACE ->
                    GaugeSpec.progressArrow(kind == MachineKind.INDUCTION_FURNACE ? 81 : 80, 35);
            case MACERATOR -> GaugeSpec.progressCrush(80, 38);
            case EXTRACTOR -> GaugeSpec.progressDrop(80, 35);
            case PUMP -> GaugeSpec.progressDrop(36, 34);
            case COMPRESSOR -> GaugeSpec.progressTriangle(80, 35);
            case RECYCLER -> GaugeSpec.progressRecycler(80, 35);
            case BLOCK_CUTTER -> GaugeSpec.progressBlockCutter(55, 33);
            case CENTRIFUGE -> GaugeSpec.progressCentrifuge(84, 25);
            case ORE_WASHING_PLANT -> GaugeSpec.progressOreWasher(103, 39);
            case METAL_FORMER -> GaugeSpec.progressMetalFormer(52, 39);
            case CANNER -> GaugeSpec.cannerProgress(74, 22);
            case BATCH_CRAFTER -> GaugeSpec.progressArrow(90, 35);
            case COKE_KILN -> GaugeSpec.progressArrow(80, 35);
            default -> null;
        };
    }

    static GaugeSpec fuel(MachineKind kind) {
        return switch (kind) {
            case IRON_FURNACE -> GaugeSpec.fuel(56, 36);
            case GENERATOR -> GaugeSpec.fuel(57, 36);
            case SOLID_HEAT_GENERATOR -> GaugeSpec.fuel(81, 29);
            default -> null;
        };
    }

    static void drawGauge(
            GuiGraphicsExtractor graphics, int left, int top, GaugeSpec gauge, long value, long max) {
        if (gauge == null) return;
        double ratio = max <= 0 ? 0.0 : Math.clamp((double) value / max, 0.0, 1.0);
        int x = left + gauge.x;
        int y = top + gauge.y;
        if (gauge.backgroundWidth > 0) {
            blit(
                    graphics,
                    gauge.texture,
                    x + gauge.backgroundX,
                    y + gauge.backgroundY,
                    gauge.backgroundU,
                    gauge.backgroundV,
                    gauge.backgroundWidth,
                    gauge.backgroundHeight);
        }
        int full = gauge.vertical ? gauge.height : gauge.width;
        int rendered = (int) Math.round(full * ratio);
        if (rendered <= 0) return;
        int drawX = x;
        int drawY = y;
        int u = gauge.u;
        int v = gauge.v;
        int width = gauge.width;
        int height = gauge.height;
        if (gauge.vertical) {
            height = rendered;
            if (gauge.reverse) {
                drawY += gauge.height - rendered;
                v += gauge.height - rendered;
            }
        } else {
            width = rendered;
            if (gauge.reverse) {
                drawX += gauge.width - rendered;
                u += gauge.width - rendered;
            }
        }
        blit(graphics, gauge.texture, drawX, drawY, u, v, width, height);
    }

    static boolean contains(GaugeSpec gauge, int left, int top, int mouseX, int mouseY) {
        return gauge != null
                && mouseX >= left + gauge.x
                && mouseX < left + gauge.x + gauge.width
                && mouseY >= top + gauge.y
                && mouseY < top + gauge.y + gauge.height;
    }

    static void blit(
            GuiGraphicsExtractor graphics,
            Identifier texture,
            int x,
            int y,
            int u,
            int v,
            int width,
            int height) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x,
                y,
                (float) u,
                (float) v,
                width,
                height,
                256,
                256);
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath("ic2", "textures/gui/" + name);
    }

    record GaugeSpec(
            Identifier texture,
            int x,
            int y,
            int u,
            int v,
            int width,
            int height,
            boolean vertical,
            boolean reverse,
            int backgroundX,
            int backgroundY,
            int backgroundWidth,
            int backgroundHeight,
            int backgroundU,
            int backgroundV) {
        static GaugeSpec energyBolt(int x, int y) {
            return common(x, y, 116, 65, 7, 13, true, true, -4, -1, 16, 16, 96, 64);
        }

        static GaugeSpec energyBar(int x, int y) {
            return common(x, y, 132, 43, 24, 9, false, false, -4, -11, 32, 32, 128, 0);
        }

        static GaugeSpec fuel(int x, int y) {
            return common(x, y, 112, 80, 13, 13, true, true, 0, 0, 16, 16, 96, 80);
        }

        static GaugeSpec progressArrow(int x, int y) {
            return common(x, y, 165, 16, 22, 15, false, false, -5, 0, 32, 16, 160, 0);
        }

        static GaugeSpec progressCrush(int x, int y) {
            return common(x, y, 165, 52, 21, 11, false, false, -5, -3, 32, 16, 160, 32);
        }

        static GaugeSpec progressTriangle(int x, int y) {
            return common(x, y, 165, 80, 22, 15, false, false, -5, 0, 32, 16, 160, 64);
        }

        static GaugeSpec progressDrop(int x, int y) {
            return common(x, y, 165, 112, 22, 15, false, false, -5, 0, 32, 16, 160, 96);
        }

        static GaugeSpec progressRecycler(int x, int y) {
            return common(x, y, 133, 80, 18, 15, false, false, -5, 0, 32, 16, 128, 64);
        }

        static GaugeSpec progressCentrifuge(int x, int y) {
            return common(x, y, 252, 33, 3, 28, true, true, -1, -1, 5, 30, 246, 32);
        }

        static GaugeSpec progressMetalFormer(int x, int y) {
            return custom("guimetalformer.png", x, y, 200, 19, 46, 9, -8, -3, 64, 16, 192, 0);
        }

        static GaugeSpec progressBlockCutter(int x, int y) {
            return custom("guiblockcutter.png", x, y, 176, 15, 46, 17, 0, 0, 46, 17, 55, 33);
        }

        static GaugeSpec progressOreWasher(int x, int y) {
            return custom("guiorewashingplant.png", x, y, 177, 118, 18, 18, -1, -1, 20, 19, 102, 38);
        }

        static GaugeSpec cannerProgress(int x, int y) {
            return custom("guicanner.png", x, y, 233, 0, 23, 14, 0, 0, 0, 0, 0, 0);
        }

        static GaugeSpec progressCondenser(int x, int y) {
            return custom("guicondenser.png", x, y, 1, 185, 82, 7, 0, 0, 0, 0, 0, 0);
        }

        static GaugeSpec heatFermenter(int x, int y) {
            return custom("guifermenter.png", x, y, 177, 10, 40, 3, 0, 0, 0, 0, 0, 0);
        }

        static GaugeSpec progressFermenter(int x, int y) {
            return custom("guifermenter.png", x, y, 177, 1, 40, 7, 0, 0, 0, 0, 0, 0);
        }

        static GaugeSpec heatCentrifuge(int x, int y) {
            return common(x, y, 225, 54, 20, 4, false, false, -1, -1, 22, 6, 224, 47);
        }

        static GaugeSpec waterBucket(int x, int y) {
            return common(x, y, 110, 111, 14, 16, true, true, 0, 0, 14, 16, 96, 111);
        }

        static GaugeSpec windProgress(int x, int y) {
            return common(x, y, 242, 91, 13, 13, true, true, 0, 0, 13, 13, 242, 63);
        }

        private static GaugeSpec common(
                int x,
                int y,
                int u,
                int v,
                int width,
                int height,
                boolean vertical,
                boolean reverse,
                int backgroundX,
                int backgroundY,
                int backgroundWidth,
                int backgroundHeight,
                int backgroundU,
                int backgroundV) {
            return new GaugeSpec(
                    COMMON,
                    x,
                    y,
                    u,
                    v,
                    width,
                    height,
                    vertical,
                    reverse,
                    backgroundX,
                    backgroundY,
                    backgroundWidth,
                    backgroundHeight,
                    backgroundU,
                    backgroundV);
        }

        private static GaugeSpec custom(
                String texture,
                int x,
                int y,
                int u,
                int v,
                int width,
                int height,
                int backgroundX,
                int backgroundY,
                int backgroundWidth,
                int backgroundHeight,
                int backgroundU,
                int backgroundV) {
            return new GaugeSpec(
                    LegacyMachineGui.texture(texture),
                    x,
                    y,
                    u,
                    v,
                    width,
                    height,
                    false,
                    false,
                    backgroundX,
                    backgroundY,
                    backgroundWidth,
                    backgroundHeight,
                    backgroundU,
                    backgroundV);
        }
    }
}
