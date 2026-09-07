package ic2.neoforge.registration;

import ic2.core.energy.grid.CableSpec;
import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.energy.CableBlock;
import ic2.neoforge.machine.*;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;

import java.util.Collections;
import java.util.LinkedHashMap;
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
    public static final DeferredBlock<MachineBlock> GENERATOR = machine(MachineKind.GENERATOR);
    public static final DeferredBlock<MachineBlock> ELECTRIC_FURNACE =
            machine(MachineKind.ELECTRIC_FURNACE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>>
            GENERATOR_ENTITY =
                    ENTITIES.register(
                            "generator",
                            () ->
                                    new BlockEntityType<>(
                                            GeneratorBlockEntity::new, GENERATOR.get()));
    public static final DeferredHolder<
                    BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>>
            ELECTRIC_FURNACE_ENTITY =
                    ENTITIES.register(
                            "electric_furnace",
                            () ->
                                    new BlockEntityType<>(
                                            ElectricFurnaceBlockEntity::new,
                                            ELECTRIC_FURNACE.get()));
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> GENERATOR_MENU =
            menu(MachineKind.GENERATOR);
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> ELECTRIC_FURNACE_MENU =
            menu(MachineKind.ELECTRIC_FURNACE);
    public static final Map<String, DeferredBlock<CableBlock>> CABLES = cables();

    private static DeferredBlock<MachineBlock> machine(MachineKind kind) {
        var block =
                BLOCKS.registerBlock(
                        kind.getSerializedName(),
                        properties ->
                                new MachineBlock(
                                        kind,
                                        properties
                                                .mapColor(MapColor.METAL)
                                                .strength(2, 10)
                                                .requiresCorrectToolForDrops()
                                                .sound(SoundType.METAL)));
        ITEMS.registerSimpleBlockItem(block);
        return block;
    }

    private static DeferredHolder<MenuType<?>, MenuType<MachineMenu>> menu(MachineKind kind) {
        return MENUS.register(
                kind.getSerializedName(),
                () ->
                        IMenuTypeExtension.create(
                                (id, inventory, data) ->
                                        new MachineMenu(id, inventory, data.readBlockPos(), kind)));
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
            String name = material.name().toLowerCase(java.util.Locale.ROOT) + "_cable";
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
        return (kind == MachineKind.GENERATOR ? GENERATOR_MENU : ELECTRIC_FURNACE_MENU).get();
    }

    public static MachineBlock block(MachineKind kind) {
        return (kind == MachineKind.GENERATOR ? GENERATOR : ELECTRIC_FURNACE).get();
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
                Capabilities.Item.BLOCK,
                GENERATOR_ENTITY.get(),
                (machine, side) -> machine.automation(side));
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ELECTRIC_FURNACE_ENTITY.get(),
                (machine, side) -> machine.automation(side));
    }

    private static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(GENERATOR);
            event.accept(ELECTRIC_FURNACE);
            CABLES.values().forEach(event::accept);
        }
    }

    private ModMachines() {}
}
