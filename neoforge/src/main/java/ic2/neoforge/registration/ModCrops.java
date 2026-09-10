package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.crop.AcaciaSaplingCropBlock;
import ic2.neoforge.crop.AureliaCropBlock;
import ic2.neoforge.crop.BeetrootsCropBlock;
import ic2.neoforge.crop.BirchSaplingCropBlock;
import ic2.neoforge.crop.BlackthornCropBlock;
import ic2.neoforge.crop.BrownMushroomCropBlock;
import ic2.neoforge.crop.CocoaCropBlock;
import ic2.neoforge.crop.CocoaCropCard;
import ic2.neoforge.crop.CoffeeCropBlock;
import ic2.neoforge.crop.CoffeeCropCard;
import ic2.neoforge.crop.ColorFlowerCropCard;
import ic2.neoforge.crop.CropBlock;
import ic2.neoforge.crop.CropBlockEntity;
import ic2.neoforge.crop.CropCard;
import ic2.neoforge.crop.CropSeedItem;
import ic2.neoforge.crop.CropStickItem;
import ic2.neoforge.crop.CarrotsCropBlock;
import ic2.neoforge.crop.CyazintCropBlock;
import ic2.neoforge.crop.CypriumCropBlock;
import ic2.neoforge.crop.DarkOakSaplingCropBlock;
import ic2.neoforge.crop.DandelionCropBlock;
import ic2.neoforge.crop.EatingPlantCropBlock;
import ic2.neoforge.crop.EatingPlantCropCard;
import ic2.neoforge.crop.FerruCropBlock;
import ic2.neoforge.crop.FlaxCropBlock;
import ic2.neoforge.crop.FlaxCropCard;
import ic2.neoforge.crop.HopsCropBlock;
import ic2.neoforge.crop.HopsCropCard;
import ic2.neoforge.crop.JungleSaplingCropBlock;
import ic2.neoforge.crop.MelonCropBlock;
import ic2.neoforge.crop.MelonCropCard;
import ic2.neoforge.crop.MetalCropCard;
import ic2.neoforge.crop.MushroomCropCard;
import ic2.neoforge.crop.NetherWartCropBlock;
import ic2.neoforge.crop.NetherWartCropCard;
import ic2.neoforge.crop.OakSaplingCropBlock;
import ic2.neoforge.crop.PoppyCropBlock;
import ic2.neoforge.crop.PlumbiscusCropBlock;
import ic2.neoforge.crop.PotatoCropBlock;
import ic2.neoforge.crop.PotatoCropCard;
import ic2.neoforge.crop.PumpkinCropBlock;
import ic2.neoforge.crop.PumpkinCropCard;
import ic2.neoforge.crop.RedWheatCropBlock;
import ic2.neoforge.crop.RedWheatCropCard;
import ic2.neoforge.crop.RedMushroomCropBlock;
import ic2.neoforge.crop.ReedCropBlock;
import ic2.neoforge.crop.ReedCropCard;
import ic2.neoforge.crop.SaplingCropCard;
import ic2.neoforge.crop.ShiningCropBlock;
import ic2.neoforge.crop.SpruceSaplingCropBlock;
import ic2.neoforge.crop.StagniumCropBlock;
import ic2.neoforge.crop.StickyReedCropBlock;
import ic2.neoforge.crop.StickyReedCropCard;
import ic2.neoforge.crop.TerraWartCropBlock;
import ic2.neoforge.crop.TerraWartCropCard;
import ic2.neoforge.crop.TulipCropBlock;
import ic2.neoforge.crop.VanillaProduceCropCard;
import ic2.neoforge.crop.VenomiliaCropBlock;
import ic2.neoforge.crop.VenomiliaCropCard;
import ic2.neoforge.crop.WeedCropBlock;
import ic2.neoforge.crop.WeedCropCard;
import ic2.neoforge.crop.WheatCropBlock;
import ic2.neoforge.crop.WheatCropCard;
import ic2.neoforge.item.TerraWartItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.food.FoodProperties;
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

import java.util.List;
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
    public static final DeferredBlock<CropBlock> PUMPKIN_CROP =
            BLOCKS.registerBlock(
                    "pumpkin_crop", properties -> new PumpkinCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> MELON_CROP =
            BLOCKS.registerBlock(
                    "melon_crop", properties -> new MelonCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> DANDELION_CROP =
            BLOCKS.registerBlock(
                    "dandelion_crop",
                    properties -> new DandelionCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> POPPY_CROP =
            BLOCKS.registerBlock(
                    "poppy_crop", properties -> new PoppyCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> BLACKTHORN_CROP =
            BLOCKS.registerBlock(
                    "blackthorn_crop",
                    properties -> new BlackthornCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> TULIP_CROP =
            BLOCKS.registerBlock(
                    "tulip_crop", properties -> new TulipCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> CYAZINT_CROP =
            BLOCKS.registerBlock(
                    "cyazint_crop", properties -> new CyazintCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> VENOMILIA_CROP =
            BLOCKS.registerBlock(
                    "venomilia_crop",
                    properties -> new VenomiliaCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> STICKY_REED_CROP =
            BLOCKS.registerBlock(
                    "sticky_reed_crop",
                    properties -> new StickyReedCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> TERRA_WART_CROP =
            BLOCKS.registerBlock(
                    "terra_wart_crop",
                    properties -> new TerraWartCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> FERRU_CROP =
            BLOCKS.registerBlock(
                    "ferru_crop", properties -> new FerruCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> CYPRIUM_CROP =
            BLOCKS.registerBlock(
                    "cyprium_crop", properties -> new CypriumCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> STAGNIUM_CROP =
            BLOCKS.registerBlock(
                    "stagnium_crop", properties -> new StagniumCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> PLUMBISCUS_CROP =
            BLOCKS.registerBlock(
                    "plumbiscus_crop",
                    properties -> new PlumbiscusCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> AURELIA_CROP =
            BLOCKS.registerBlock(
                    "aurelia_crop", properties -> new AureliaCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> SHINING_CROP =
            BLOCKS.registerBlock(
                    "shining_crop", properties -> new ShiningCropBlock(cropSettings(properties)));
    public static final DeferredBlock<CropBlock> RED_WHEAT_CROP =
            BLOCKS.registerBlock(
                    "red_wheat_crop",
                    properties ->
                            new RedWheatCropBlock(
                                    cropSettings(properties)
                                            .lightLevel(
                                                    state ->
                                                            state.hasProperty(
                                                                    RedWheatCropBlock.AGE)
                                                                    && state.getValue(
                                                                            RedWheatCropBlock.AGE)
                                                                            == RedWheatCropBlock
                                                                                    .MAX_AGE
                                                            ? 7
                                                            : 0)));
    public static final DeferredBlock<CropBlock> EATING_PLANT_CROP =
            BLOCKS.registerBlock(
                    "eating_plant_crop",
                    properties ->
                            new EatingPlantCropBlock(cropSettings(properties)));

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
                                            DARK_OAK_SAPLING_CROP.get(),
                                            PUMPKIN_CROP.get(),
                                            MELON_CROP.get(),
                                            DANDELION_CROP.get(),
                                            POPPY_CROP.get(),
                                            BLACKTHORN_CROP.get(),
                                            TULIP_CROP.get(),
                                            CYAZINT_CROP.get(),
                                            VENOMILIA_CROP.get(),
                                            STICKY_REED_CROP.get(),
                                            TERRA_WART_CROP.get(),
                                            FERRU_CROP.get(),
                                            CYPRIUM_CROP.get(),
                                            STAGNIUM_CROP.get(),
                                            PLUMBISCUS_CROP.get(),
                                            AURELIA_CROP.get(),
                                            SHINING_CROP.get(),
                                            RED_WHEAT_CROP.get(),
                                            EATING_PLANT_CROP.get()));

    public static final DeferredItem<CropStickItem> CROP_STICK_ITEM =
            ITEMS.registerItem(
                    "crop_stick", properties -> new CropStickItem(CROP_STICK.get(), properties));
    public static final DeferredItem<CropSeedItem> CROP_SEED_BAG =
            ITEMS.registerItem(
                    "crop_seed_bag", properties -> new CropSeedItem(properties.stacksTo(1)));
    public static final DeferredItem<TerraWartItem> TERRA_WART =
            ITEMS.registerItem(
                    "terra_wart",
                    properties ->
                            new TerraWartItem(
                                    properties.food(
                                            new FoodProperties.Builder()
                                                    .nutrition(0)
                                                    .saturationModifier(1.0F)
                                                    .alwaysEdible()
                                                    .build()).rarity(net.minecraft.world.item.Rarity.RARE)));

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
    public static final CropCard PUMPKIN_CARD = new PumpkinCropCard();
    public static final CropCard MELON_CARD = new MelonCropCard();
    public static final CropCard DANDELION_CARD =
            new ColorFlowerCropCard(
                    "dandelion", DANDELION_CROP::get, () -> new ItemStack(Items.YELLOW_DYE));
    public static final CropCard POPPY_CARD =
            new ColorFlowerCropCard(
                    "poppy", POPPY_CROP::get, () -> new ItemStack(Items.RED_DYE));
    public static final CropCard BLACKTHORN_CARD =
            new ColorFlowerCropCard(
                    "blackthorn", BLACKTHORN_CROP::get, () -> new ItemStack(Items.BLACK_DYE));
    public static final CropCard TULIP_CARD =
            new ColorFlowerCropCard(
                    "tulip", TULIP_CROP::get, () -> new ItemStack(Items.PURPLE_DYE));
    public static final CropCard CYAZINT_CARD =
            new ColorFlowerCropCard(
                    "cyazint", CYAZINT_CROP::get, () -> new ItemStack(Items.CYAN_DYE));
    public static final CropCard VENOMILIA_CARD = new VenomiliaCropCard();
    public static final CropCard STICKY_REED_CARD = new StickyReedCropCard();
    public static final CropCard TERRA_WART_CARD = new TerraWartCropCard();
    // Metal crops (legacy CropBaseMetalCommon/Uncommon): cross-breeding products without a base
    // seed; the final growth step needs ore or storage blocks of their metal in the root zone.
    public static final CropCard FERRU_CARD =
            new MetalCropCard(
                    "ferru",
                    FERRU_CROP::get,
                    List.of(BlockTags.IRON_ORES, Ic2BlockTags.IRON_BLOCKS),
                    () ->
                            new ItemStack(
                                    ModItems.MATERIALS
                                            .get(MaterialDefinition.SMALL_IRON_DUST)
                                            .get()),
                    false);
    public static final CropCard CYPRIUM_CARD =
            new MetalCropCard(
                    "cyprium",
                    CYPRIUM_CROP::get,
                    List.of(BlockTags.COPPER_ORES, Ic2BlockTags.COPPER_BLOCKS),
                    () ->
                            new ItemStack(
                                    ModItems.MATERIALS
                                            .get(MaterialDefinition.SMALL_COPPER_DUST)
                                            .get()),
                    false);
    public static final CropCard STAGNIUM_CARD =
            new MetalCropCard(
                    "stagnium",
                    STAGNIUM_CROP::get,
                    List.of(Ic2BlockTags.TIN_ORES, Ic2BlockTags.TIN_BLOCKS),
                    () ->
                            new ItemStack(
                                    ModItems.MATERIALS
                                            .get(MaterialDefinition.SMALL_TIN_DUST)
                                            .get()),
                    false);
    public static final CropCard PLUMBISCUS_CARD =
            new MetalCropCard(
                    "plumbiscus",
                    PLUMBISCUS_CROP::get,
                    List.of(Ic2BlockTags.LEAD_ORES, Ic2BlockTags.LEAD_BLOCKS),
                    () ->
                            new ItemStack(
                                    ModItems.MATERIALS
                                            .get(MaterialDefinition.SMALL_LEAD_DUST)
                                            .get()),
                    false);
    public static final CropCard AURELIA_CARD =
            new MetalCropCard(
                    "aurelia",
                    AURELIA_CROP::get,
                    List.of(BlockTags.GOLD_ORES, Ic2BlockTags.GOLD_BLOCKS),
                    () ->
                            new ItemStack(
                                    ModItems.MATERIALS
                                            .get(MaterialDefinition.SMALL_GOLD_DUST)
                                            .get()),
                    true);
    public static final CropCard SHINING_CARD =
            new MetalCropCard(
                    "shining",
                    SHINING_CROP::get,
                    List.of(Ic2BlockTags.SILVER_ORES, Ic2BlockTags.SILVER_BLOCKS),
                    () ->
                            new ItemStack(
                                    ModItems.MATERIALS
                                            .get(MaterialDefinition.SMALL_SILVER_DUST)
                                            .get()),
                    true);
    public static final CropCard RED_WHEAT_CARD = new RedWheatCropCard();
    public static final CropCard EATING_PLANT_CARD = new EatingPlantCropCard();

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
        if (block == PUMPKIN_CROP.get()) return PUMPKIN_CARD;
        if (block == MELON_CROP.get()) return MELON_CARD;
        if (block == DANDELION_CROP.get()) return DANDELION_CARD;
        if (block == POPPY_CROP.get()) return POPPY_CARD;
        if (block == BLACKTHORN_CROP.get()) return BLACKTHORN_CARD;
        if (block == TULIP_CROP.get()) return TULIP_CARD;
        if (block == CYAZINT_CROP.get()) return CYAZINT_CARD;
        if (block == VENOMILIA_CROP.get()) return VENOMILIA_CARD;
        if (block == STICKY_REED_CROP.get()) return STICKY_REED_CARD;
        if (block == TERRA_WART_CROP.get()) return TERRA_WART_CARD;
        if (block == FERRU_CROP.get()) return FERRU_CARD;
        if (block == CYPRIUM_CROP.get()) return CYPRIUM_CARD;
        if (block == STAGNIUM_CROP.get()) return STAGNIUM_CARD;
        if (block == PLUMBISCUS_CROP.get()) return PLUMBISCUS_CARD;
        if (block == AURELIA_CROP.get()) return AURELIA_CARD;
        if (block == SHINING_CROP.get()) return SHINING_CARD;
        if (block == RED_WHEAT_CROP.get()) return RED_WHEAT_CARD;
        if (block == EATING_PLANT_CROP.get()) return EATING_PLANT_CARD;
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
            case "pumpkin" -> PUMPKIN_CARD;
            case "melon" -> MELON_CARD;
            case "dandelion" -> DANDELION_CARD;
            case "poppy" -> POPPY_CARD;
            case "blackthorn" -> BLACKTHORN_CARD;
            case "tulip" -> TULIP_CARD;
            case "cyazint" -> CYAZINT_CARD;
            case "venomilia" -> VENOMILIA_CARD;
            case "sticky_reed" -> STICKY_REED_CARD;
            case "terra_wart" -> TERRA_WART_CARD;
            case "ferru" -> FERRU_CARD;
            case "cyprium" -> CYPRIUM_CARD;
            case "stagnium" -> STAGNIUM_CARD;
            case "plumbiscus" -> PLUMBISCUS_CARD;
            case "aurelia" -> AURELIA_CARD;
            case "shining" -> SHINING_CARD;
            case "red_wheat" -> RED_WHEAT_CARD;
            case "eating_plant" -> EATING_PLANT_CARD;
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
            seeds.put(Items.CACTUS, new BaseSeed(EATING_PLANT_CARD, 0, 1, 1, 1));
            seeds.put(Items.COCOA_BEANS, new BaseSeed(COCOA_CARD, 0, 0, 0, 0));
            seeds.put(Items.PUMPKIN_SEEDS, new BaseSeed(PUMPKIN_CARD, 0, 1, 1, 1));
            seeds.put(Items.MELON_SEEDS, new BaseSeed(MELON_CARD, 0, 1, 1, 1));
            // Legacy plants flowers at age three and consumes three of the held blooms.
            seeds.put(Items.POPPY.asItem(), new BaseSeed(POPPY_CARD, 3, 1, 1, 1));
            seeds.put(Items.DANDELION.asItem(), new BaseSeed(DANDELION_CARD, 3, 1, 1, 1));
            seeds.put(TERRA_WART.get(), new BaseSeed(TERRA_WART_CARD, 0, 1, 1, 1));
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
        if (postEvent.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            postEvent.accept(TERRA_WART);
        }
    }

    private ModCrops() {}
}
