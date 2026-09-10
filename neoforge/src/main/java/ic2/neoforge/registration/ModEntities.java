package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.entity.DynamiteEntity;
import ic2.neoforge.entity.ItntEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Non-living entities ported from legacy Ic2Entities. */
public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, IndustrialCraft.MOD_ID);

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(
                Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(IndustrialCraft.MOD_ID, name));
    }

    public static final Supplier<EntityType<ItntEntity>> ITNT =
            TYPES.register(
                    "itnt",
                    () ->
                            EntityType.Builder.<ItntEntity>of(ItntEntity::new, MobCategory.MISC)
                                    .fireImmune()
                                    .sized(0.98F, 0.98F)
                                    .clientTrackingRange(10)
                                    .updateInterval(10)
                                    .build(key("itnt")));

    public static final Supplier<EntityType<DynamiteEntity>> DYNAMITE =
            TYPES.register(
                    "dynamite",
                    () ->
                            EntityType.Builder.<DynamiteEntity>of(
                                            DynamiteEntity::new, MobCategory.MISC)
                                    .sized(0.5F, 0.5F)
                                    .clientTrackingRange(8)
                                    .updateInterval(5)
                                    .build(key("dynamite")));

    public static final Supplier<EntityType<DynamiteEntity>> STICKY_DYNAMITE =
            TYPES.register(
                    "sticky_dynamite",
                    () ->
                            EntityType.Builder.<DynamiteEntity>of(
                                            DynamiteEntity::new, MobCategory.MISC)
                                    .sized(0.5F, 0.5F)
                                    .clientTrackingRange(8)
                                    .updateInterval(5)
                                    .build(key("sticky_dynamite")));

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }

    private ModEntities() {}
}
