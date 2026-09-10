package ic2.neoforge.client;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.CraftingToolItem;
import ic2.neoforge.item.ElectricItem;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.item.WrenchTool;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModRubberBuilding;
import ic2.neoforge.registration.ModToolbox;
import ic2.neoforge.registration.ModTools;
import ic2.neoforge.registration.ModWorldContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.blockentity.StandingSignRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@Mod(value = IndustrialCraft.MOD_ID, dist = Dist.CLIENT)
public final class IndustrialCraftClient {
    public IndustrialCraftClient(IEventBus modBus) {
        modBus.addListener(IndustrialCraftClient::registerProperties);
        modBus.addListener(
                (EntityRenderersEvent.RegisterRenderers event) ->
                        event.registerBlockEntityRenderer(
                                ModRubberBuilding.SIGN_ENTITY.get(), StandingSignRenderer::new));
        modBus.addListener(
                (EntityRenderersEvent.RegisterRenderers event) ->
                        event.registerBlockEntityRenderer(
                                ModMachines.entityType(MachineKind.WATER_GENERATOR),
                                RotorRenderer::new));
        modBus.addListener(
                (EntityRenderersEvent.RegisterRenderers event) ->
                        event.registerBlockEntityRenderer(
                                ModMachines.entityType(MachineKind.WIND_GENERATOR),
                                RotorRenderer::new));
        modBus.addListener(
                (EntityRenderersEvent.RegisterRenderers event) -> {
                    for (var kind : MachineKind.values())
                        if (kind.turbine())
                            event.registerBlockEntityRenderer(
                                    ModMachines.entityType(kind), RotorRenderer::new);
                });
        modBus.addListener(
                (EntityRenderersEvent.RegisterRenderers event) ->
                        event.registerEntityRenderer(
                                ic2.neoforge.registration.ModEntities.ITNT.get(),
                                ic2.neoforge.client.ItntRenderer::new));
        modBus.addListener(FluidModels::register);
        modBus.addListener(
                (RegisterColorHandlersEvent.BlockTintSources event) ->
                        event.register(
                                List.of(BlockTintSources.constant(0xff000000 | 6723908)),
                                ModWorldContent.RUBBER_LEAVES.get()));
        modBus.addListener(MachineSounds::reloaded);
        modBus.addListener(IndustrialCraftClient::registerScreens);
        NeoForge.EVENT_BUS.addListener(IndustrialCraftClient::addTooltip);
        NeoForge.EVENT_BUS.addListener(MachineSounds::loaded);
        NeoForge.EVENT_BUS.addListener(MachineSounds::tick);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModToolbox.MENU.get(), ToolboxScreen::new);
        event.register(ModTools.MINING_FILTER_MENU.get(), MiningFilterScreen::new);
        ModMachines.MACHINES.forEach(
                (kind, registration) -> {
                    if (kind.energyDevice())
                        event.register(registration.menu().get(), EnergyDeviceScreen::new);
                    else if (kind.turbine())
                        event.register(ModMachines.menuType(kind), TurbineScreen::new);
                    else if (kind.electricWork() || kind.workConversion() || kind.fuelHeat())
                        event.register(ModMachines.menuType(kind), WorkMachineScreen::new);
                    else if (kind.fluidGenerator() || kind == MachineKind.SOLAR_GENERATOR)
                        event.register(registration.menu().get(), GeneratorScreen::new);
                    else if (kind == MachineKind.WATER_GENERATOR
                            || kind == MachineKind.WIND_GENERATOR)
                        event.register(registration.menu().get(), RotorGeneratorScreen::new);
                    else if (kind == MachineKind.ORE_WASHING_PLANT)
                        event.register(registration.menu().get(), OreWashingScreen::new);
                    else if (kind == MachineKind.CENTRIFUGE
                            || kind == MachineKind.INDUCTION_FURNACE)
                        event.register(registration.menu().get(), HeatedMachineScreen::new);
                    else if (kind == MachineKind.METAL_FORMER)
                        event.register(registration.menu().get(), MetalFormerScreen::new);
                    else if (kind == MachineKind.MINER)
                        event.register(registration.menu().get(), MinerScreen::new);
                    else if (kind == MachineKind.BLOCK_CUTTER)
                        event.register(registration.menu().get(), BlockCutterScreen::new);
                    else if (kind == MachineKind.BLAST_FURNACE)
                        event.register(registration.menu().get(), BlastFurnaceScreen::new);
                    else if (kind == MachineKind.MATTER_GENERATOR)
                        event.register(registration.menu().get(), MatterGeneratorScreen::new);
                    else if (kind == MachineKind.NUCLEAR_REACTOR)
                        event.register(registration.menu().get(), NuclearReactorScreen::new);
                    else if (kind == MachineKind.UU_SCANNER)
                        event.register(registration.menu().get(), UuScannerScreen::new);
                    else if (kind == MachineKind.REPLICATOR)
                        event.register(registration.menu().get(), MachineScreen::new);
                    else if (kind == MachineKind.ITEM_BUFFER)
                        event.register(registration.menu().get(), ItemBufferScreen::new);
                    else if (kind == MachineKind.ADV_MINER)
                        event.register(registration.menu().get(), AdvMinerScreen::new);
                    else if (kind == MachineKind.STEAM_KINETIC_GENERATOR)
                        event.register(registration.menu().get(), SteamTurbineScreen::new);
                    else if (kind == MachineKind.STEAM_GENERATOR)
                        event.register(registration.menu().get(), SteamGeneratorScreen::new);
                    else if (kind == MachineKind.STEAM_REPRESSURIZER)
                        event.register(registration.menu().get(), SteamRepressurizerScreen::new);
                    else if (kind == MachineKind.RT_HEAT_GENERATOR
                            || kind == MachineKind.RT_GENERATOR)
                        event.register(registration.menu().get(), RadioisotopeScreen::new);
                    else if (kind.storageBox())
                        event.register(registration.menu().get(), MachineScreen::new);
                    else if (kind == MachineKind.BATBOX_CHARGEPAD
                            || kind == MachineKind.CESU_CHARGEPAD
                            || kind == MachineKind.MFE_CHARGEPAD
                            || kind == MachineKind.MFSU_CHARGEPAD)
                        event.register(registration.menu().get(), EnergyDeviceScreen::new);
                    else if (kind == MachineKind.CONDENSER)
                        event.register(registration.menu().get(), CondenserScreen::new);
                    else if (kind == MachineKind.FLUID_REGULATOR)
                        event.register(registration.menu().get(), FluidRegulatorScreen::new);
                    else if (kind == MachineKind.ELECTROLYZER)
                        event.register(registration.menu().get(), ElectrolyzerScreen::new);
                    else if (kind == MachineKind.TANK)
                        event.register(registration.menu().get(), TankScreen::new);
                    else if (kind == MachineKind.LIQUID_HEAT_EXCHANGER)
                        event.register(registration.menu().get(), LiquidHeatExchangerScreen::new);
                    else if (kind == MachineKind.FERMENTER)
                        event.register(registration.menu().get(), FermenterScreen::new);
                    else if (kind == MachineKind.CANNER)
                        event.register(registration.menu().get(), CannerScreen::new);
                    else event.register(registration.menu().get(), MachineScreen::new);
                });
    }

    private static void addTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (stack.getItem() instanceof UpgradeItem item) {
            int count = stack.getCount();
            var format = new java.text.DecimalFormat("0.##");
            switch (item.kind()) {
                case OVERCLOCKER -> {
                    event.getToolTip()
                            .add(
                                    Component.translatable(
                                            "ic2.tooltip.upgrade.overclocker.time",
                                            format.format(100 * Math.pow(.7, count))));
                    event.getToolTip()
                            .add(
                                    Component.translatable(
                                            "ic2.tooltip.upgrade.overclocker.power",
                                            format.format(100 * Math.pow(1.6, count))));
                }
                case TRANSFORMER ->
                        event.getToolTip()
                                .add(
                                        Component.translatable(
                                                "ic2.tooltip.upgrade.transformer", count));
                case ENERGY_STORAGE ->
                        event.getToolTip()
                                .add(
                                        Component.translatable(
                                                "ic2.tooltip.upgrade.storage", 10000 * count));
                default ->
                        event.getToolTip()
                                .add(
                                        Component.translatable(
                                                item.kind().pulling()
                                                        ? "ic2.tooltip.upgrade.pulling"
                                                        : "ic2.tooltip.upgrade.ejector",
                                                UpgradeItem.directionName(stack)));
            }
        }
        if (stack.getItem() instanceof WrenchTool) {
            var options = Minecraft.getInstance().options;
            event.getToolTip()
                    .add(
                            Component.translatable(
                                    "item.ic2.wrench.tooltip.mine",
                                    options.keyAttack.getTranslatedKeyMessage()));
            event.getToolTip()
                    .add(
                            Component.translatable(
                                    "item.ic2.wrench.tooltip.rotate",
                                    options.keyUse.getTranslatedKeyMessage()));
        }
        if (stack.getItem() instanceof CraftingToolItem)
            event.getToolTip()
                    .add(
                            Component.translatable(
                                    "ic2.tooltip.tool.uses_left",
                                    stack.getMaxDamage() - stack.getDamageValue()));
        var stored = stack.get(ModDataComponents.STORED_ENERGY);
        if (stored != null)
            event.getToolTip().add(Component.translatable("Store", stored.longValue()));

        if (event.getItemStack().getItem() instanceof ElectricItem item) {
            event.getToolTip()
                    .add(
                            Component.translatable(
                                    "ic2.tooltip.energy",
                                    ElectricItemEnergy.charge(event.getItemStack()),
                                    item.specification().capacity()));
        }
    }

    private static void registerProperties(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(Identifier.parse("ic2:tool_box_open"), ToolboxOpenProperty.CODEC);
        event.register(
                Identifier.fromNamespaceAndPath(IndustrialCraft.MOD_ID, "upgrade_direction"),
                UpgradeDirectionProperty.CODEC);
        event.register(
                Identifier.fromNamespaceAndPath(IndustrialCraft.MOD_ID, "charge"),
                ChargeProperty.CODEC);
    }
}
