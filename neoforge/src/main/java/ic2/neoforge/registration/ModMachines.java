package ic2.neoforge.registration;

import ic2.core.energy.grid.CableSpec;
import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.energy.CableBlock;
import ic2.neoforge.machine.*;
import ic2.neoforge.machine.CannerBlockEntity;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ModMachines {
    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(IndustrialCraft.MOD_ID);
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(IndustrialCraft.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IndustrialCraft.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, IndustrialCraft.MOD_ID);

    public record Registration(
            DeferredBlock<MachineBlock> block,
            DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlockEntity>> entity,
            DeferredHolder<MenuType<?>, MenuType<MachineMenu>> menu) {}

    public static final Map<MachineKind, Registration> MACHINES = machines();
    public static final DeferredBlock<MachineBlock> GENERATOR =
            MACHINES.get(MachineKind.GENERATOR).block();
    public static final DeferredBlock<MachineBlock> ELECTRIC_FURNACE =
            MACHINES.get(MachineKind.ELECTRIC_FURNACE).block();
    public static final Map<String, DeferredBlock<CableBlock>> CABLES = cables();

    private static Map<MachineKind, Registration> machines() {
        var result = new EnumMap<MachineKind, Registration>(MachineKind.class);
        for (MachineKind kind : MachineKind.values()) {
            String id = kind.getSerializedName();
            var block =
                    BLOCKS.registerBlock(
                            id,
                            properties ->
                                    new MachineBlock(
                                            kind,
                                            properties
                                                    .mapColor(MapColor.METAL)
                                                    .strength(2, 10)
                                                    .requiresCorrectToolForDrops()
                                                    .sound(SoundType.METAL)));
            ITEMS.registerSimpleBlockItem(block);
            DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlockEntity>> entity =
                    ENTITIES.register(
                            id,
                            () ->
                                    new BlockEntityType<>(
                                            (pos, state) -> createEntity(kind, pos, state),
                                            block.get()));
            DeferredHolder<MenuType<?>, MenuType<MachineMenu>> menu =
                    MENUS.register(
                            id,
                            () ->
                                    IMenuTypeExtension.create(
                                            (containerId, inventory, data) ->
                                                    new MachineMenu(
                                                            containerId,
                                                            inventory,
                                                            data.readBlockPos(),
                                                            kind)));
            result.put(kind, new Registration(block, entity, menu));
        }
        return Collections.unmodifiableMap(result);
    }

    public static MachineBlockEntity createEntity(
            MachineKind kind, BlockPos pos, BlockState state) {
        return switch (kind) {
            case BATBOX, CESU, MFE, MFSU -> new EnergyStorageBlockEntity(pos, state);
            case LV_TRANSFORMER, MV_TRANSFORMER, HV_TRANSFORMER, EV_TRANSFORMER ->
                    new TransformerBlockEntity(pos, state);
            case IRON_FURNACE -> new IronFurnaceBlockEntity(pos, state);
            case CANNER -> new CannerBlockEntity(pos, state);
            case SOLAR_GENERATOR -> new SolarGeneratorBlockEntity(pos, state);
            case GEO_GENERATOR, SEMIFLUID_GENERATOR -> new FluidGeneratorBlockEntity(pos, state);
            case GENERATOR -> new GeneratorBlockEntity(pos, state);
            case ELECTRIC_FURNACE -> new ElectricFurnaceBlockEntity(pos, state);
            case MACERATOR, EXTRACTOR, COMPRESSOR -> new SingleInputBlockEntity(pos, state);
        };
    }

    private static Map<String, DeferredBlock<CableBlock>> cables() {
        var result = new LinkedHashMap<String, DeferredBlock<CableBlock>>();
        cable(result, "glass_fibre_cable", CableSpec.Material.GLASS, 0);
        for (var material :
                new CableSpec.Material[] {
                    CableSpec.Material.COPPER,
                    CableSpec.Material.GOLD,
                    CableSpec.Material.IRON,
                    CableSpec.Material.TIN
                }) {
            String name = material.name().toLowerCase(Locale.ROOT) + "_cable";
            int max =
                    switch (material) {
                        case GOLD -> 2;
                        case IRON -> 3;
                        default -> 1;
                    };
            for (int insulation = 0; insulation <= max; insulation++) {
                String prefix =
                        switch (insulation) {
                            case 0 -> "";
                            case 1 -> "insulated_";
                            case 2 -> "double_insulated_";
                            default -> "triple_insulated_";
                        };
                cable(result, prefix + name, material, insulation);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static void cable(
            Map<String, DeferredBlock<CableBlock>> cables,
            String id,
            CableSpec.Material material,
            int insulation) {
        var block =
                BLOCKS.registerBlock(
                        id,
                        properties ->
                                new CableBlock(
                                        material,
                                        insulation,
                                        properties
                                                .mapColor(MapColor.METAL)
                                                .strength(0.2f)
                                                .sound(SoundType.WOOL)
                                                .noOcclusion()));
        ITEMS.registerSimpleBlockItem(block);
        cables.put(id, block);
    }

    public static MenuType<MachineMenu> menuType(MachineKind kind) {
        return MACHINES.get(kind).menu().get();
    }

    public static MachineBlock block(MachineKind kind) {
        return MACHINES.get(kind).block().get();
    }

    public static BlockEntityType<MachineBlockEntity> entityType(MachineKind kind) {
        return MACHINES.get(kind).entity().get();
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        MENUS.register(bus);
        bus.addListener(ModMachines::capabilities);
        bus.addListener(ModMachines::creativeContents);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                entityType(MachineKind.CANNER),
                (machine, side) -> ((CannerBlockEntity) machine).fluidAutomation(side));
        for (var kind : MachineKind.values())
            if (kind.fluidGenerator())
                event.registerBlockEntity(
                        Capabilities.Fluid.BLOCK,
                        entityType(kind),
                        (machine, side) ->
                                ((FluidGeneratorBlockEntity) machine).fluidAutomation(side));
        MACHINES.values()
                .forEach(
                        registration ->
                                event.registerBlockEntity(
                                        Capabilities.Item.BLOCK,
                                        registration.entity().get(),
                                        (machine, side) -> machine.automation(side)));
    }

    private static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            MACHINES.values().forEach(registration -> event.accept(registration.block()));
            CABLES.values().forEach(event::accept);
        }
    }

    private ModMachines() {}
}
