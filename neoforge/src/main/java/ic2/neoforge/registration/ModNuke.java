package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.machine.NukeBlockEntity;
import ic2.neoforge.menu.NukeMenu;
import ic2.neoforge.world.NukeBlock;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The nuke: charge block with payload slots, primed entity and gui (P13 explosion chain). */
public final class ModNuke {
    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(IndustrialCraft.MOD_ID);
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(IndustrialCraft.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IndustrialCraft.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, IndustrialCraft.MOD_ID);

    public static final DeferredBlock<NukeBlock> NUKE =
            BLOCKS.registerBlock(
                    "nuke",
                    properties ->
                            new NukeBlock(
                                    properties
                                            .mapColor(MapColor.FIRE)
                                            .strength(0.0F)
                                            .sound(SoundType.GRASS)));

    public static final DeferredItem<BlockItem> NUKE_ITEM =
            ITEMS.registerSimpleBlockItem(
                    "nuke", NUKE, properties -> properties.rarity(Rarity.UNCOMMON));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NukeBlockEntity>>
            NUKE_ENTITY =
                    BLOCK_ENTITIES.register(
                            "nuke", () -> new BlockEntityType<>(NukeBlockEntity::new, NUKE.get()));

    public static final DeferredHolder<MenuType<?>, MenuType<NukeMenu>> NUKE_MENU =
            MENUS.register(
                    "nuke",
                    () ->
                            IMenuTypeExtension.create(
                                    (id, inventory, data) ->
                                            new NukeMenu(id, inventory, data.readBlockPos())));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        bus.addListener(
                (BuildCreativeModeTabContentsEvent event) -> {
                    if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
                        event.accept(NUKE_ITEM);
                    }
                });
    }

    private ModNuke() {}
}
