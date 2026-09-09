package ic2.neoforge.registration;

import ic2.neoforge.world.DynamiteBlock;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Explosives: dynamite sticks paired to the remote detonator (P13). */
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
                                            .pushReaction(
                                                    net.minecraft.world.level.material.PushReaction
                                                            .DESTROY)));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(
                (BuildCreativeModeTabContentsEvent event) -> {
                    if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS))
                        event.accept(DYNAMITE);
                });
    }

    private ModExplosives() {}
}
