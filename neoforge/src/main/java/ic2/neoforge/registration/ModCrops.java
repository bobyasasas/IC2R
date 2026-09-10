package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.crop.AcaciaSaplingCropBlock;
import ic2.neoforge.crop.BeetrootsCropBlock;
import ic2.neoforge.crop.BirchSaplingCropBlock;
import ic2.neoforge.crop.BrownMushroomCropBlock;
import ic2.neoforge.crop.CocoaCropBlock;
import ic2.neoforge.crop.CocoaCropCard;
import ic2.neoforge.crop.CoffeeCropBlock;
import ic2.neoforge.crop.CoffeeCropCard;
import ic2.neoforge.crop.CropBlock;
import ic2.neoforge.crop.CropBlockEntity;
import ic2.neoforge.crop.CropCard;
import ic2.neoforge.crop.CropSeedItem;
import ic2.neoforge.crop.CropStickItem;
import ic2.neoforge.crop.CarrotsCropBlock;
import ic2.neoforge.crop.DarkOakSaplingCropBlock;
import ic2.neoforge.crop.FlaxCropBlock;
import ic2.neoforge.crop.FlaxCropCard;
import ic2.neoforge.crop.HopsCropBlock;
import ic2.neoforge.crop.HopsCropCard;
import ic2.neoforge.crop.JungleSaplingCropBlock;
import ic2.neoforge.crop.MushroomCropCard;
import ic2.neoforge.crop.NetherWartCropBlock;
import ic2.neoforge.crop.NetherWartCropCard;
import ic2.neoforge.crop.OakSaplingCropBlock;
import ic2.neoforge.crop.PotatoCropBlock;
import ic2.neoforge.crop.PotatoCropCard;
import ic2.neoforge.crop.RedMushroomCropBlock;
import ic2.neoforge.crop.ReedCropBlock;
import ic2.neoforge.crop.ReedCropCard;
import ic2.neoforge.crop.SaplingCropCard;
import ic2.neoforge.crop.SpruceSaplingCropBlock;
import ic2.neoforge.crop.VanillaProduceCropCard;
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
import net.minecraft.world.item.ItemStack;

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
    public static final DeferredBlock<CropBlock> NETHER_WART_CROP =
            BLOCKS.registerBlock(
                    "nether_wart_crop",
                    properties -> new NetherWartCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> RED_MUSHROOM_CROP =
            BLOCKS.registerBlock(
                    "red_mushroom_crop",
                    properties -> new RedMushroomCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> BROWN_MUSHROOM_CROP =
            BLOCKS.registerBlock(
                    "brown_mushroom_crop",
                    properties -> new BrownMushroomCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> CARROTS_CROP =
            BLOCKS.registerBlock(
                    "carrots_crop", properties -> new CarrotsCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> POTATO_CROP =
            BLOCKS.registerBlock(
                    "potato_crop", properties -> new PotatoCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> BEETROOTS_CROP =
            BLOCKS.registerBlock(
                    "beetroots_crop",
                    properties -> new BeetrootsCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> OAK_SAPLING_CROP =
            BLOCKS.registerBlock(
                    "oak_sapling_crop",
                    properties -> new OakSaplingCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> SPRUCE_SAPLING_CROP =
            BLOCKS.registerBlock(
                    "spruce_sapling_crop",
                    properties -> new SpruceSaplingCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> BIRCH_SAPLING_CROP =
            BLOCKS.registerBlock(
                    "birch_sapling_crop",
                    properties -> new BirchSaplingCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> JUNGLE_SAPLING_CROP =
            BLOCKS.registerBlock(
                    "jungle_sapling_crop",
                    properties -> new JungleSaplingCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> ACACIA_SAPLING_CROP =
            BLOCKS.registerBlock(
                    "acacia_sapling_crop",
                    properties -> new AcaciaSaplingCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> DARK_OAK_SAPLING_CROP =
            BLOCKS.registerBlock(
                    "dark_oak_sapling_crop",
                    properties -> new DarkOakSaplingCropBlock(cropSettings(properties)));

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
                                            COCOA_CROP.get(),
                                            NETHER_WART_CROP.get(),
                                            RED_MUSHROOM_CROP.get(),
                                            BROWN_MUSHROOM_CROP.get(),
                                            CARROTS_CROP.get(),
                                            POTATO_CROP.get(),
                                            BEETROOTS_CROP.get(),
                                            OAK_SAPLING_CROP.get(),
                                            SPRUCE_SAPLING_CROP.get(),
                                            BIRCH_SAPLING_CROP.get(),
                                            JUNGLE_SAPLING_CROP.get(),
                                            ACACIA_SAPLING_CROP.get(),
                                            DARK_OAK_SAPLING_CROP.get()));

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
    public static final CropCard NETHER_WART_CARD = new NetherWartCropCard();
    public static final CropCard RED_MUSHROOM_CARD =
            new MushroomCropCard(
                    "red_mushroom",
                    RED_MUSHROOM_CROP::get,
                    () -> new ItemStack(net.minecraft.world.level.block.Blocks.RED_MUSHROOM));
    public static final CropCard BROWN_MUSHROOM_CARD =
            new MushroomCropCard(
                    "brown_mushroom",
                    BROWN_MUSHROOM_CROP::get,
                    () -> new ItemStack(net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM));
    public static final CropCard CARROTS_CARD =
            new VanillaProduceCropCard(
                    "carrots",
                    CARROTS_CROP::get,
                    3,
                    new ic2.core.crop.CropProperties(2, 0, 4, 0, 0, 2),
                    () -> new ItemStack(Items.CARROT),
                    () -> new ItemStack(Items.CARROT));
    public static final CropCard BEETROOTS_CARD =
            new VanillaProduceCropCard(
                    "beetroots",
                    BEETROOTS_CROP::get,
                    3,
                    new ic2.core.crop.CropProperties(1, 0, 4, 0, 1, 2),
                    () -> new ItemStack(Items.BEETROOT),
                    () -> new ItemStack(Items.BEETROOT_SEEDS));
    public static final CropCard POTATO_CARD = new PotatoCropCard();
    public static final CropCard OAK_SAPLING_CARD =
            new SaplingCropCard(
                    "oak_sapling",
                    OAK_SAPLING_CROP::get,
                    () -> new ItemStack(net.minecraft.world.level.block.Blocks.OAK_LOG),
                    () -> new ItemStack(net.minecraft.world.level.block.Blocks.OAK_SAPLING),
                    true);
    public static final CropCard SPRUCE_SAPLING_CARD =
            new SaplingCropCard(
                    "spruce_sapling",
                    SPRUCE_SAPLING_CROP::get,
                    () -> new ItemStack(net.minecraft.world.level.block.Blocks.SPRUCE_LOG),
                    () -> new ItemStack(net.minecraft.world.level.block.Blocks.SPRUCE_SAPLING),
                    false);
    public static final CropCard BIRCH_SAPLING_CARD =
            new SaplingCropCard(
                    "birch_sapling",
                    BIRCH_SAPLING_CROP::get,
                    () -> new ItemStack(net.minecraft.world.level.block.Blocks.BIRCH_LOG),
                    () -> new ItemStack(net.minecraft.world.level.block.Blocks.BIRCH_SAPLING),
                    false);
    public static final CropCard JUNGLE_SAPLING_CARD =
            new SaplingCropCard(
                    "jungle_sapling",
                    JUNGLE_SAPLING_CROP::get,
                    () -> new ItemStack(net.minecraft.world.level.block.Blocks.JUNGLE_LOG),
                    () -> new ItemStack(net.minecraft.world.level.block.Blocks.JUNGLE_SAPLING),
                    false);
    public static final CropCard ACACIA_SAPLING_CARD =
            new SaplingCropCard(
                    "acacia_sapling",
                    ACACIA_SAPLING_CROP::get,
                    () -> new ItemStack(net.minecraft.world.level.block.Blocks.ACACIA_LOG),
                    () -> new ItemStack(net.minecraft.world.level.block.Blocks.ACACIA_SAPLING),
                    false);
    public static final CropCard DARK_OAK_SAPLING_CARD =
            new SaplingCropCard(
                    "dark_oak_sapling",
                    DARK_OAK_SAPLING_CROP::get,
                    () -> new ItemStack(net.minecraft.world.level.block.Blocks.DARK_OAK_LOG),
                    () -> new ItemStack(net.minecraft.world.level.block.Blocks.DARK_OAK_SAPLING),
                    false);

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
        if (block == NETHER_WART_CROP.get()) return NETHER_WART_CARD;
        if (block == RED_MUSHROOM_CROP.get()) return RED_MUSHROOM_CARD;
        if (block == BROWN_MUSHROOM_CROP.get()) return BROWN_MUSHROOM_CARD;
        if (block == CARROTS_CROP.get()) return CARROTS_CARD;
        if (block == POTATO_CROP.get()) return POTATO_CARD;
        if (block == BEETROOTS_CROP.get()) return BEETROOTS_CARD;
        if (block == OAK_SAPLING_CROP.get()) return OAK_SAPLING_CARD;
        if (block == SPRUCE_SAPLING_CROP.get()) return SPRUCE_SAPLING_CARD;
        if (block == BIRCH_SAPLING_CROP.get()) return BIRCH_SAPLING_CARD;
        if (block == JUNGLE_SAPLING_CROP.get()) return JUNGLE_SAPLING_CARD;
        if (block == ACACIA_SAPLING_CROP.get()) return ACACIA_SAPLING_CARD;
        if (block == DARK_OAK_SAPLING_CROP.get()) return DARK_OAK_SAPLING_CARD;
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
            case "nether_wart" -> NETHER_WART_CARD;
            case "red_mushroom" -> RED_MUSHROOM_CARD;
            case "brown_mushroom" -> BROWN_MUSHROOM_CARD;
            case "carrots" -> CARROTS_CARD;
            case "potato" -> POTATO_CARD;
            case "beetroots" -> BEETROOTS_CARD;
            case "oak_sapling" -> OAK_SAPLING_CARD;
            case "spruce_sapling" -> SPRUCE_SAPLING_CARD;
            case "birch_sapling" -> BIRCH_SAPLING_CARD;
            case "jungle_sapling" -> JUNGLE_SAPLING_CARD;
            case "acacia_sapling" -> ACACIA_SAPLING_CARD;
            case "dark_oak_sapling" -> DARK_OAK_SAPLING_CARD;
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
            var seeds = new java.util.HashMap<Item, BaseSeed>();
            seeds.put(Items.SUGAR_CANE, new BaseSeed(REED_CARD, 0, 3, 0, 2));
            seeds.put(Items.COCOA_BEANS, new BaseSeed(COCOA_CARD, 0, 0, 0, 0));
            seeds.put(
                    ModItems.MATERIALS.get(MaterialDefinition.COFFEE_BEANS).get(),
                    new BaseSeed(COFFEE_CARD, 0, 1, 1, 1));
            seeds.put(Items.CARROT, new BaseSeed(CARROTS_CARD, 0, 1, 1, 1));
            seeds.put(Items.POTATO, new BaseSeed(POTATO_CARD, 0, 1, 1, 1));
            seeds.put(Items.BEETROOT_SEEDS, new BaseSeed(BEETROOTS_CARD, 0, 1, 1, 1));
            seeds.put(Items.NETHER_WART, new BaseSeed(NETHER_WART_CARD, 0, 1, 1, 1));
            seeds.put(
                    Items.BROWN_MUSHROOM.asItem(),
                    new BaseSeed(BROWN_MUSHROOM_CARD, 0, 1, 1, 1));
            seeds.put(
                    Items.RED_MUSHROOM.asItem(),
                    new BaseSeed(RED_MUSHROOM_CARD, 0, 1, 1, 1));
            seeds.put(Items.OAK_SAPLING, new BaseSeed(OAK_SAPLING_CARD, 0, 1, 1, 1));
            seeds.put(Items.SPRUCE_SAPLING, new BaseSeed(SPRUCE_SAPLING_CARD, 0, 1, 1, 1));
            seeds.put(Items.BIRCH_SAPLING, new BaseSeed(BIRCH_SAPLING_CARD, 0, 1, 1, 1));
            seeds.put(Items.JUNGLE_SAPLING, new BaseSeed(JUNGLE_SAPLING_CARD, 0, 1, 1, 1));
            seeds.put(Items.ACACIA_SAPLING, new BaseSeed(ACACIA_SAPLING_CARD, 0, 1, 1, 1));
            seeds.put(Items.DARK_OAK_SAPLING, new BaseSeed(DARK_OAK_SAPLING_CARD, 0, 1, 1, 1));
            map = Map.copyOf(seeds);
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
