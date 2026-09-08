package ic2.neoforge.registration;

import ic2.core.machine.RotorMaterial;
import ic2.neoforge.item.RotorItem;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class ModRotors {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("ic2");
    public static final Map<RotorMaterial, DeferredItem<RotorItem>> ROTORS = create();

    private static Map<RotorMaterial, DeferredItem<RotorItem>> create() {
        var items = new EnumMap<RotorMaterial, DeferredItem<RotorItem>>(RotorMaterial.class);
        for (var material : RotorMaterial.values())
            items.put(
                    material,
                    ITEMS.registerItem(
                            material.id(), properties -> new RotorItem(properties, material)));
        return Collections.unmodifiableMap(items);
    }

    private static final Map<RotorMaterial, Identifier> TEXTURES =
            Map.of(
                    RotorMaterial.WOODEN,
                            Identifier.parse("ic2:textures/item/rotor/wood_rotor_model.png"),
                    RotorMaterial.BRONZE,
                            Identifier.parse("ic2:textures/item/rotor/bronze_rotor_model.png"),
                    RotorMaterial.IRON,
                            Identifier.parse("ic2:textures/item/rotor/iron_rotor_model.png"),
                    RotorMaterial.STEEL,
                            Identifier.parse("ic2:textures/item/rotor/steel_rotor_model.png"),
                    RotorMaterial.CARBON,
                            Identifier.parse("ic2:textures/item/rotor/carbon_rotor_model.png"));

    public static Identifier texture(RotorMaterial material) {
        return TEXTURES.get(material);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModRotors::creative);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ModRotors::tooltip);
    }

    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS))
            ROTORS.values().forEach(event::accept);
    }

    private static void tooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof RotorItem rotor)) return;
        var material = rotor.material();
        event.getToolTip()
                .add(
                        net.minecraft.network.chat.Component.translatable(
                                "ic2.rotor.wind_range",
                                material.minimumWind(),
                                material.maximumWind()));
        event.getToolTip()
                .add(
                        net.minecraft.network.chat.Component.translatable(
                                material.supportsWater()
                                        ? "ic2.rotor.wind_water"
                                        : "ic2.rotor.wind_only"));
    }

    private ModRotors() {}
}
