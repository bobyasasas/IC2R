package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.block.ObscuredWallBlock;
import ic2.neoforge.item.ObscuratorItem;
import ic2.neoforge.machine.ObscuredWallBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Obscurator tool and the obscured wall it creates on construction foam walls. */
public final class ModObscurator {
    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(IndustrialCraft.MOD_ID);
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(IndustrialCraft.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IndustrialCraft.MOD_ID);

    public static final DeferredBlock<ObscuredWallBlock> OBSCURED_WALL =
            BLOCKS.registerBlock(
                    "obscured_wall",
                    properties ->
                            new ObscuredWallBlock(
                                    properties
                                            .mapColor(MapColor.METAL)
                                            .strength(3.0F, 30.0F)
                                            .sound(SoundType.STONE)
                                            .noLootTable()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ObscuredWallBlockEntity>>
            OBSCURED_WALL_ENTITY =
                    BLOCK_ENTITIES.register(
                            "obscured_wall",
                            () ->
                                    new BlockEntityType<>(
                                            ObscuredWallBlockEntity::new, OBSCURED_WALL.get()));

    public static final DeferredItem<ObscuratorItem> OBSCURATOR =
            ITEMS.registerItem(
                    "obscurator",
                    properties -> new ObscuratorItem(properties.stacksTo(1).rarity(Rarity.COMMON)));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        bus.addListener(
                (BuildCreativeModeTabContentsEvent event) -> {
                    if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
                        event.accept(OBSCURATOR);
                    }
                });
    }

    private ModObscurator() {}
}
