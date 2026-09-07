package ic2.neoforge.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import ic2.neoforge.IndustrialCraft;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.registries.DeferredRegister;

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
    public static final Supplier<DataComponentType<Integer>> HYDRATION_USES =
            TYPES.<Integer>registerComponentType(
                    "hydration_uses",
                    builder ->
                            builder.persistent(Codec.intRange(0, 10000))
                                    .networkSynchronized(
                                            ByteBufCodecs.VAR_INT.map(
                                                    ModDataComponents::validHydration,
                                                    ModDataComponents::validHydration)));

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
