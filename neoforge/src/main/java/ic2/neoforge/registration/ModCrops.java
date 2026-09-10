package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.crop.CocoaCropBlock;
import ic2.neoforge.crop.CocoaCropCard;
import ic2.neoforge.crop.CoffeeCropBlock;
import ic2.neoforge.crop.CoffeeCropCard;
import ic2.neoforge.crop.CropBlock;
import ic2.neoforge.crop.CropBlockEntity;
import ic2.neoforge.crop.CropCard;
import ic2.neoforge.crop.CropSeedItem;
import ic2.neoforge.crop.CropStickItem;
import ic2.neoforge.crop.FlaxCropBlock;
import ic2.neoforge.crop.FlaxCropCard;
import ic2.neoforge.crop.HopsCropBlock;
import ic2.neoforge.crop.HopsCropCard;
import ic2.neoforge.crop.ReedCropBlock;
import ic2.neoforge.crop.ReedCropCard;
import ic2.neoforge.crop.WeedCropBlock;
import ic2.neoforge.crop.WeedCropCard;
import ic2.neoforge.crop.WheatCropBlock;
import ic2.neoforge.crop.WheatCropCard;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;

/**
 * Legacy crop registrations (slice one): the crop stick, the wheat and weed crops with their cards,
 * and the stat-carrying seed bag. Legacy moisture/nutrient biome bonuses stay out until the
 * environment biome classification is ported; watered farmland and storage carry humidity.
 */
public final class ModCrops {
    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(IndustrialCraft.MOD_ID);
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(IndustrialCraft.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IndustrialCraft.MOD_ID);

    private static BlockBehaviour.Properties cropSettings(BlockBehaviour.Properties properties) {
        return properties.strength(0.8F, 0.2F).sound(SoundType.CROP).noCollision().noLootTable();
    }

    public static final DeferredBlock<CropBlock> CROP_STICK =
            BLOCKS.registerBlock(
                    "crop_stick", properties -> new CropBlock(null, cropSettings(properties)));
    public static final DeferredBlock<CropBlock> WHEAT_CROP =
            BLOCKS.registerBlock(
                    "wheat_crop", properties -> new WheatCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> WEED_CROP =
            BLOCKS.registerBlock(
                    "weed_crop", properties -> new WeedCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> REED_CROP =
            BLOCKS.registerBlock(
                    "reed_crop", properties -> new ReedCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> FLAX_CROP =
            BLOCKS.registerBlock(
                    "flax_crop", properties -> new FlaxCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> HOPS_CROP =
            BLOCKS.registerBlock(
                    "hops_crop", properties -> new HopsCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> COFFEE_CROP =
            BLOCKS.registerBlock(
                    "coffee_crop", properties -> new CoffeeCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> COCOA_CROP =
            BLOCKS.registerBlock(
                    "cocoa_crop", properties -> new CocoaCropBlock(cropSettings(properties)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CropBlockEntity>>
            CROP_ENTITY =
                    BLOCK_ENTITIES.register(
                            "crop",
                            () ->
                                    new BlockEntityType<>(
                                            CropBlockEntity::new,
                                            CROP_STICK.get(),
                                            WHEAT_CROP.get(),
                                            WEED_CROP.get(),
                                            REED_CROP.get(),
                                            FLAX_CROP.get(),
                                            HOPS_CROP.get(),
                                            COFFEE_CROP.get(),
                                            COCOA_CROP.get()));

    public static final DeferredItem<CropStickItem> CROP_STICK_ITEM =
            ITEMS.registerItem(
                    "crop_stick", properties -> new CropStickItem(CROP_STICK.get(), properties));
    public static final DeferredItem<CropSeedItem> CROP_SEED_BAG =
            ITEMS.registerItem(
                    "crop_seed_bag", properties -> new CropSeedItem(properties.stacksTo(1)));

    public static final CropCard WHEAT_CARD = new WheatCropCard();
    public static final WeedCropCard WEED_CARD = new WeedCropCard();
    public static final CropCard REED_CARD = new ReedCropCard();
    public static final CropCard FLAX_CARD = new FlaxCropCard();
    public static final CropCard HOPS_CARD = new HopsCropCard();
    public static final CropCard COFFEE_CARD = new CoffeeCropCard();
    public static final CropCard COCOA_CARD = new CocoaCropCard();

    /**
     * Legacy registerBaseSeed: a plain produce item plants its crop with fixed stats; the size is
     * both the planted age and the consumed stack count (zero consumes nothing, legacy quirk).
     */
    public record BaseSeed(CropCard card, int size, int growth, int gain, int resistance) {}


    /** Resolves the crop card planted in a block, or null for the empty stick. */
    public static CropCard cardFor(Block block) {
        if (block == WHEAT_CROP.get()) return WHEAT_CARD;
        if (block == WEED_CROP.get()) return WEED_CARD;
        if (block == REED_CROP.get()) return REED_CARD;
        if (block == FLAX_CROP.get()) return FLAX_CARD;
        if (block == HOPS_CROP.get()) return HOPS_CARD;
        if (block == COFFEE_CROP.get()) return COFFEE_CARD;
        if (block == COCOA_CROP.get()) return COCOA_CARD;
        return null;
    }

    public static CropCard card(String id) {
        return switch (id) {
            case "wheat" -> WHEAT_CARD;
            case "weed" -> WEED_CARD;
            case "reed" -> REED_CARD;
            case "flax" -> FLAX_CARD;
            case "hops" -> HOPS_CARD;
            case "coffee" -> COFFEE_CARD;
            case "cocoa" -> COCOA_CARD;
            default -> null;
        };
    }

    /** Legacy Crops.getBaseSeed: the base seed planted by a held produce item. */
    public static BaseSeed baseSeedFor(Item item) {
        return baseSeeds().get(item);
    }

    private static volatile Map<Item, BaseSeed> baseSeeds;

    private static Map<Item, BaseSeed> baseSeeds() {
        // Built lazily: the coffee beans item only binds after item registration.
        Map<Item, BaseSeed> map = baseSeeds;
        if (map == null) {
            map = Map.of(
                    Items.SUGAR_CANE, new BaseSeed(REED_CARD, 0, 3, 0, 2),
                    Items.COCOA_BEANS, new BaseSeed(COCOA_CARD, 0, 0, 0, 0),
                    ModItems.MATERIALS.get(MaterialDefinition.COFFEE_BEANS).get(),
                    new BaseSeed(COFFEE_CARD, 0, 1, 1, 1));
            baseSeeds = map;
        }
        return map;
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(ModCrops::creativeTab);
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent postEvent) {
        if (postEvent.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            postEvent.accept(CROP_STICK_ITEM);
            postEvent.accept(CROP_SEED_BAG);
        }
    }

    private ModCrops() {}
}
