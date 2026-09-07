package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.fluid.FluidDefinition;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.transfer.fluid.BucketResourceHandler;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class ModFluids {
    private static final DeferredRegister<FluidType> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, IndustrialCraft.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, IndustrialCraft.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(IndustrialCraft.MOD_ID);
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(IndustrialCraft.MOD_ID);

    public record Family(
            DeferredHolder<FluidType, FluidType> type,
            DeferredHolder<Fluid, BaseFlowingFluid.Source> source,
            DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing,
            DeferredBlock<LiquidBlock> block,
            DeferredItem<BucketItem> bucket) {}

    public static final Map<FluidDefinition, Family> FAMILIES = families();

    private static Map<FluidDefinition, Family> families() {
        var result = new EnumMap<FluidDefinition, Family>(FluidDefinition.class);
        for (var definition : FluidDefinition.values()) {
            String id = definition.id();
            var type = TYPES.register(id, () -> new FluidType(definition.properties()));
            var source =
                    FLUIDS.register(id, () -> new BaseFlowingFluid.Source(properties(definition)));
            var flowing =
                    FLUIDS.register(
                            "flowing_" + id,
                            () -> new BaseFlowingFluid.Flowing(properties(definition)));
            var block =
                    BLOCKS.registerBlock(
                            "fluid_block_" + id,
                            p -> new LiquidBlock(source.get(), p),
                            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable());
            var bucket =
                    ITEMS.registerItem(
                            id + "_bucket",
                            p ->
                                    new BucketItem(
                                            source.get(),
                                            p.craftRemainder(Items.BUCKET).stacksTo(1)));
            result.put(definition, new Family(type, source, flowing, block, bucket));
        }
        return Collections.unmodifiableMap(result);
    }

    private static BaseFlowingFluid.Properties properties(FluidDefinition definition) {
        var family = FAMILIES.get(definition);
        return new BaseFlowingFluid.Properties(family.type(), family.source(), family.flowing())
                .block(family.block())
                .bucket(family.bucket());
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(ModFluids::capabilities);
        bus.addListener(ModFluids::creativeContents);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        FAMILIES.values()
                .forEach(
                        family ->
                                event.registerItem(
                                        Capabilities.Fluid.ITEM,
                                        (stack, access) -> new BucketResourceHandler(access),
                                        family.bucket().get()));
    }

    private static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES))
            FAMILIES.values().forEach(family -> event.accept(family.bucket()));
    }

    private ModFluids() {}
}
