package ic2.neoforge.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import ic2.neoforge.IndustrialCraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Supplier;

public final class ModDataComponents {
    private static final DeferredRegister.DataComponents TYPES =
            DeferredRegister.createDataComponents(
                    Registries.DATA_COMPONENT_TYPE, IndustrialCraft.MOD_ID);

    private static final Codec<Double> CHARGE_CODEC =
            Codec.DOUBLE.validate(
                    value ->
                            Double.isFinite(value) && value >= 0
                                    ? DataResult.success(value)
                                    : DataResult.error(
                                            () -> "Charge must be finite and non-negative"));

    public static final Supplier<DataComponentType<Double>> CHARGE =
            TYPES.<Double>registerComponentType(
                    "charge",
                    builder ->
                            builder.persistent(CHARGE_CODEC)
                                    .networkSynchronized(
                                            ByteBufCodecs.DOUBLE.map(
                                                    ModDataComponents::validCharge,
                                                    ModDataComponents::validCharge)));
    public static final Supplier<DataComponentType<Double>> STORED_ENERGY =
            TYPES.<Double>registerComponentType(
                    "stored_energy",
                    builder ->
                            builder.persistent(CHARGE_CODEC)
                                    .networkSynchronized(
                                            ByteBufCodecs.DOUBLE.map(
                                                    ModDataComponents::validCharge,
                                                    ModDataComponents::validCharge)));
    public static final Supplier<DataComponentType<Direction>> UPGRADE_DIRECTION =
            TYPES.<Direction>registerComponentType(
                    "upgrade_direction",
                    builder ->
                            builder.persistent(Direction.CODEC)
                                    .networkSynchronized(Direction.STREAM_CODEC));
    public static final Supplier<DataComponentType<BlockPos>> FREQUENCY_POS =
            TYPES.<BlockPos>registerComponentType(
                    "frequency_pos",
                    builder ->
                            builder.persistent(BlockPos.CODEC)
                                    .networkSynchronized(BlockPos.STREAM_CODEC));
    public static final Supplier<DataComponentType<Boolean>> FREQUENCY_JUST_SET =
            TYPES.<Boolean>registerComponentType(
                    "frequency_just_set",
                    builder ->
                            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final Supplier<DataComponentType<UUID>> TOOLBOX_ID =
            TYPES.<UUID>registerComponentType(
                    "toolbox_id",
                    builder ->
                            builder.persistent(UUIDUtil.CODEC)
                                    .networkSynchronized(UUIDUtil.STREAM_CODEC));
    public static final Supplier<DataComponentType<ItemContainerContents>> TOOLBOX_CONTENTS =
            TYPES.<ItemContainerContents>registerComponentType(
                    "toolbox_contents",
                    builder ->
                            builder.persistent(
                                            ItemContainerContents.CODEC.validate(
                                                    contents ->
                                                            contents.getSlots() <= 9
                                                                    ? DataResult.success(contents)
                                                                    : DataResult.error(
                                                                            () ->
                                                                                    "Toolbox"
                                                                                        + " exceeds"
                                                                                        + " nine"
                                                                                        + " slots")))
                                    .networkSynchronized(
                                            ItemContainerContents.STREAM_CODEC.map(
                                                    ModDataComponents::validToolbox,
                                                    ModDataComponents::validToolbox)));

    private static ItemContainerContents validToolbox(ItemContainerContents contents) {
        if (contents.getSlots() > 9)
            throw new IllegalArgumentException("Toolbox exceeds nine slots");
        return contents;
    }

    // A card without the blacklist marker has never been edited and defers to the machine filter,
    // mirroring the legacy NBT-absence check in TileEntityAdvMiner.
    public static final Supplier<DataComponentType<ItemContainerContents>> MINING_FILTER_ITEMS =
            TYPES.<ItemContainerContents>registerComponentType(
                    "mining_filter_items",
                    builder ->
                            builder.persistent(
                                            ItemContainerContents.CODEC.validate(
                                                    contents ->
                                                            contents.getSlots() <= 45
                                                                    ? DataResult.success(contents)
                                                                    : DataResult.error(
                                                                            () ->
                                                                                    "Mining filter"
                                                                                        + " card"
                                                                                        + " exceeds"
                                                                                        + " 45 entries")))
                                    .networkSynchronized(
                                            ItemContainerContents.STREAM_CODEC.map(
                                                    ModDataComponents::validFilter,
                                                    ModDataComponents::validFilter)));
    /** The crop analyzer's three handheld slots (input, output, battery). */
    public static final Supplier<DataComponentType<ItemContainerContents>> ANALYZER_CONTENTS =
            TYPES.<ItemContainerContents>registerComponentType(
                    "analyzer_contents",
                    builder ->
                            builder.persistent(
                                            ItemContainerContents.CODEC.validate(
                                                    contents ->
                                                            contents.getSlots() <= 3
                                                                    ? DataResult.success(contents)
                                                                    : DataResult.error(
                                                                            () ->
                                                                                    "Crop analyzer"
                                                                                        + " carries"
                                                                                        + " more"
                                                                                        + " than"
                                                                                        + " three"
                                                                                        + " slots")))
                                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    public static final Supplier<DataComponentType<Boolean>> MINING_FILTER_BLACKLIST =
            TYPES.<Boolean>registerComponentType(
                    "mining_filter_blacklist",
                    builder ->
                            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    private static ItemContainerContents validFilter(ItemContainerContents contents) {
        if (contents.getSlots() > 45)
            throw new IllegalArgumentException("Mining filter card exceeds 45 entries");
        return contents;
    }

    // Absence means empty; templates are immutable and safe to store in a component map.
    public static final Supplier<DataComponentType<FluidStackTemplate>> FLUID =
            TYPES.<FluidStackTemplate>registerComponentType(
                    "fluid",
                    builder ->
                            builder.persistent(FluidStackTemplate.CODEC)
                                    .networkSynchronized(FluidStackTemplate.STREAM_CODEC));
    public static final Supplier<DataComponentType<RemoteLinks>> REMOTE_LINKS =
            TYPES.<RemoteLinks>registerComponentType(
                    "remote_links",
                    builder ->
                            builder.persistent(RemoteLinks.CODEC)
                                    .networkSynchronized(RemoteLinks.STREAM_CODEC));
    public static final Supplier<DataComponentType<ObscuratorReference>> OBSCURATOR_REFERENCE =
            TYPES.<ObscuratorReference>registerComponentType(
                    "obscurator_reference",
                    builder ->
                            builder.persistent(ObscuratorReference.CODEC)
                                    .networkSynchronized(ObscuratorReference.STREAM_CODEC));
    public static final Supplier<DataComponentType<CropSeed>> CROP_SEED =
            TYPES.<CropSeed>registerComponentType(
                    "crop_seed",
                    builder ->
                            builder.persistent(CropSeed.CODEC)
                                    .networkSynchronized(CropSeed.STREAM_CODEC));
    public static final Supplier<DataComponentType<Integer>> HYDRATION_USES =
            TYPES.<Integer>registerComponentType(
                    "hydration_uses",
                    builder ->
                            builder.persistent(Codec.intRange(0, 10000))
                                    .networkSynchronized(
                                            ByteBufCodecs.VAR_INT.map(
                                                    ModDataComponents::validHydration,
                                                    ModDataComponents::validHydration)));

    public static final Supplier<DataComponentType<Integer>> REACTOR_HEAT =
            TYPES.<Integer>registerComponentType(
                    "reactor_heat",
                    builder ->
                            builder.persistent(Codec.intRange(0, Integer.MAX_VALUE))
                                    .networkSynchronized(
                                            ByteBufCodecs.VAR_INT.map(
                                                    ModDataComponents::validReactorHeat,
                                                    ModDataComponents::validReactorHeat)));

    private static int validReactorHeat(int heat) {
        if (heat < 0) throw new IllegalArgumentException("Negative reactor heat");
        return heat;
    }

    public static final Supplier<DataComponentType<ItemContainerContents>> CRYSTAL_MEMORY_PATTERN =
            TYPES.<ItemContainerContents>registerComponentType(
                    "crystal_memory_pattern",
                    builder ->
                            builder.persistent(ItemContainerContents.CODEC)
                                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    // Damage-style depletion of reactor components; 0 means fresh (recipes require fresh rods).
    public static final Supplier<DataComponentType<Integer>> REACTOR_USE =
            TYPES.<Integer>registerComponentType(
                    "reactor_use",
                    builder ->
                            builder.persistent(Codec.intRange(0, Integer.MAX_VALUE))
                                    .networkSynchronized(
                                            ByteBufCodecs.VAR_INT.map(
                                                    ModDataComponents::validReactorHeat,
                                                    ModDataComponents::validReactorHeat)));

    // 0 spreads up to ten blocks, 1 sprays a single block; mirrors the legacy sprayer NBT mode.
    public static final Supplier<DataComponentType<Integer>> SPRAY_MODE =
            TYPES.<Integer>registerComponentType(
                    "spray_mode",
                    builder ->
                            builder.persistent(Codec.intRange(0, 1))
                                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    // Mirrors the legacy painter NBT autoRefill flag toggled with shift use.
    public static final Supplier<DataComponentType<Boolean>> PAINTER_AUTO_REFILL =
            TYPES.<Boolean>registerComponentType(
                    "painter_auto_refill",
                    builder ->
                            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    // Mirrors the legacy chainsaw NBT disableShear flag toggled with shift use.
    public static final Supplier<DataComponentType<Boolean>> CHAINSAW_DISABLE_SHEAR =
            TYPES.<Boolean>registerComponentType(
                    "chainsaw_disable_shear",
                    builder ->
                            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    // Mirrors the legacy night vision NBT active flag (goggles and NanoSuit helmet).
    public static final Supplier<DataComponentType<Boolean>> NIGHT_VISION_ACTIVE =
            TYPES.<Boolean>registerComponentType(
                    "night_vision_active",
                    builder ->
                            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    // QuantumSuit per-piece NBT flags and the legs speed ticker, toggled with shift use
    // (legacy read the client keyboard instead; the port has no client keybinds yet).
    public static final Supplier<DataComponentType<Boolean>> JETPACK_ACTIVE =
            TYPES.<Boolean>registerComponentType(
                    "jetpack_active",
                    builder ->
                            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    public static final Supplier<DataComponentType<Boolean>> SPEED_ENABLED =
            TYPES.<Boolean>registerComponentType(
                    "speed_enabled",
                    builder ->
                            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    public static final Supplier<DataComponentType<Boolean>> JUMP_ENABLED =
            TYPES.<Boolean>registerComponentType(
                    "jump_enabled",
                    builder ->
                            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    public static final Supplier<DataComponentType<Integer>> SPEED_TICKER =
            TYPES.<Integer>registerComponentType(
                    "speed_ticker",
                    builder ->
                            builder.persistent(Codec.intRange(0, 9))
                                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModDataComponents() {}

    private static double validCharge(double value) {
        if (!Double.isFinite(value) || value < 0)
            throw new IllegalArgumentException("Invalid charge");
        return value;
    }

    private static int validHydration(int value) {
        if (value < 0 || value > 10000)
            throw new IllegalArgumentException("Invalid hydration usage");
        return value;
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
