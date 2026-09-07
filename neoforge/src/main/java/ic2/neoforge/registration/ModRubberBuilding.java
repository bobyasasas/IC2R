package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.world.RubberSignBlockEntity;
import ic2.neoforge.world.RubberStandingSignBlock;
import ic2.neoforge.world.RubberWallSignBlock;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Native block behavior keeps shape, waterlogging, redstone and placement rules together. */
public final class ModRubberBuilding {
    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(IndustrialCraft.MOD_ID);
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(IndustrialCraft.MOD_ID);
    private static final BlockSetType RUBBER = rubberType();
    public static final WoodType WOOD = WoodType.register(new WoodType("ic2:rubber", RUBBER));
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IndustrialCraft.MOD_ID);
    public static final DeferredBlock<RubberStandingSignBlock> SIGN =
            BLOCKS.registerBlock(
                    "rubber_sign",
                    RubberStandingSignBlock::new,
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN));
    public static final DeferredBlock<RubberWallSignBlock> WALL_SIGN =
            BLOCKS.registerBlock(
                    "rubber_wall_sign",
                    RubberWallSignBlock::new,
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RubberSignBlockEntity>>
            SIGN_ENTITY =
                    BLOCK_ENTITIES.register(
                            "sign",
                            () ->
                                    new BlockEntityType<>(
                                            RubberSignBlockEntity::new,
                                            SIGN.get(),
                                            WALL_SIGN.get()));
    public static final DeferredBlock<ButtonBlock> BUTTON =
            BLOCKS.registerBlock(
                    "rubber_button",
                    p -> new ButtonBlock(RUBBER, 30, p),
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON));
    public static final DeferredBlock<DoorBlock> DOOR =
            BLOCKS.registerBlock(
                    "rubber_door",
                    p -> new DoorBlock(BlockSetType.OAK, p),
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR));
    public static final DeferredBlock<FenceBlock> FENCE =
            BLOCKS.registerBlock(
                    "rubber_fence",
                    FenceBlock::new,
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE));
    public static final DeferredBlock<FenceGateBlock> FENCE_GATE =
            BLOCKS.registerBlock(
                    "rubber_fence_gate",
                    p -> new FenceGateBlock(WoodType.OAK, p),
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE));
    public static final DeferredBlock<PressurePlateBlock> PRESSURE_PLATE =
            BLOCKS.registerBlock(
                    "rubber_pressure_plate",
                    p -> new PressurePlateBlock(BlockSetType.OAK, p),
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE));
    public static final DeferredBlock<SlabBlock> SLAB =
            BLOCKS.registerBlock(
                    "rubber_slab",
                    SlabBlock::new,
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB));
    public static final DeferredBlock<StairBlock> STAIRS =
            BLOCKS.registerBlock(
                    "rubber_stairs",
                    p -> new StairBlock(ModWorldContent.RUBBER_PLANKS.get().defaultBlockState(), p),
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS));
    public static final DeferredBlock<TrapDoorBlock> TRAPDOOR =
            BLOCKS.registerBlock(
                    "rubber_trapdoor",
                    p -> new TrapDoorBlock(BlockSetType.OAK, p),
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR));

    private static BlockSetType rubberType() {
        var oak = BlockSetType.OAK;
        // The recovered rubber button explicitly disabled arrow activation.
        return BlockSetType.register(
                new BlockSetType(
                        "ic2:rubber",
                        true,
                        true,
                        false,
                        oak.pressurePlateSensitivity(),
                        oak.soundType(),
                        oak.doorClose(),
                        oak.doorOpen(),
                        oak.trapdoorClose(),
                        oak.trapdoorOpen(),
                        oak.pressurePlateClickOff(),
                        oak.pressurePlateClickOn(),
                        oak.buttonClickOff(),
                        oak.buttonClickOn()));
    }

    public static void register(IEventBus bus) {
        BLOCKS.getEntries().stream()
                .filter(holder -> holder != SIGN && holder != WALL_SIGN)
                .forEach(ITEMS::registerSimpleBlockItem);
        ITEMS.registerItem(
                "rubber_sign", p -> new SignItem(SIGN.get(), WALL_SIGN.get(), p.stacksTo(16)));
        BLOCK_ENTITIES.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(ModRubberBuilding::creativeContents);
    }

    private static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS))
            BLOCKS.getEntries().stream()
                    .filter(holder -> holder != WALL_SIGN)
                    .forEach(holder -> event.accept(holder.get()));
    }

    private ModRubberBuilding() {}
}
