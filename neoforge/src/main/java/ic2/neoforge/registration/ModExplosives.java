package ic2.neoforge.registration;

import ic2.neoforge.item.DynamiteItem;
import ic2.neoforge.item.RemoteItem;
import ic2.neoforge.item.StickyDynamiteItem;
import ic2.neoforge.world.DynamiteBlock;
import ic2.neoforge.world.DynamiteDispenseBehavior;
import ic2.neoforge.world.ItntBlock;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Explosives: dynamite sticks with the remote detonator and industrial TNT (P13). */
public final class ModExplosives {
    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ic2.neoforge.IndustrialCraft.MOD_ID);
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ic2.neoforge.IndustrialCraft.MOD_ID);

    public static final DeferredBlock<DynamiteBlock> DYNAMITE =
            BLOCKS.registerBlock(
                    "dynamite",
                    properties ->
                            new DynamiteBlock(
                                    properties
                                            .mapColor(MapColor.FIRE)
                                            .strength(0.0F)
                                            .sound(SoundType.GRASS)
                                            .noCollision()
                                            .instabreak()
                                            .noLootTable()
                                            .pushReaction(PushReaction.DESTROY)));

    public static final DeferredItem<RemoteItem> REMOTE =
            ITEMS.registerItem("remote", properties -> new RemoteItem(properties.stacksTo(1)));

    public static final DeferredItem<DynamiteItem> DYNAMITE_ITEM =
            ITEMS.registerItem(
                    "dynamite", properties -> new DynamiteItem(DYNAMITE.get(), properties));

    public static final DeferredItem<StickyDynamiteItem> DYNAMITE_STICKY =
            ITEMS.registerItem("dynamite_sticky", StickyDynamiteItem::new);

    public static final DeferredBlock<ItntBlock> ITNT =
            BLOCKS.registerBlock(
                    "itnt",
                    properties ->
                            new ItntBlock(
                                    properties
                                            .mapColor(MapColor.FIRE)
                                            .strength(0.0F)
                                            .sound(SoundType.GRASS)
                                            .noLootTable()));

    public static void register(IEventBus bus) {
        ITEMS.registerSimpleBlockItem(ITNT);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(ModExplosives::commonSetup);
        bus.addListener(
                (BuildCreativeModeTabContentsEvent event) -> {
                    if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
                        event.accept(DYNAMITE);
                        event.accept(DYNAMITE_STICKY);
                        event.accept(REMOTE);
                        event.accept(ITNT);
                    }
                });
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        // Legacy registers these statically alongside the items.
        event.enqueueWork(
                () -> {
                    DispenserBlock.registerBehavior(
                            DYNAMITE_ITEM.get(), new DynamiteDispenseBehavior(false));
                    DispenserBlock.registerBehavior(
                            DYNAMITE_STICKY.get(), new DynamiteDispenseBehavior(true));
                });
    }

    private ModExplosives() {}
}
