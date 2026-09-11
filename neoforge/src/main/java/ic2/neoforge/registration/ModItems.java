package ic2.neoforge.registration;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.item.BatteryItem;
import ic2.neoforge.item.JetpackAttachmentPlateItem;
import ic2.neoforge.item.TinCanItem;
import ic2.neoforge.item.tfbp.Chilling;
import ic2.neoforge.item.tfbp.Cultivation;
import ic2.neoforge.item.tfbp.Desertification;
import ic2.neoforge.item.tfbp.Flatification;
import ic2.neoforge.item.tfbp.Irrigation;
import ic2.neoforge.item.tfbp.Mushroom;
import ic2.neoforge.item.tfbp.TerraformingBlueprintItem;
import ic2.neoforge.item.tfbp.TerraformerProgram;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(IndustrialCraft.MOD_ID);

    public static final Map<MaterialDefinition, DeferredItem<Item>> MATERIALS = registerMaterials();
    public static final DeferredItem<Item> COPPER_PLATE =
            MATERIALS.get(MaterialDefinition.COPPER_PLATE);

    private static Map<MaterialDefinition, DeferredItem<Item>> registerMaterials() {
        var materials =
                new EnumMap<MaterialDefinition, DeferredItem<Item>>(MaterialDefinition.class);
        for (var definition : MaterialDefinition.values()) {
            materials.put(
                    definition, ITEMS.registerSimpleItem(definition.id(), definition::properties));
        }
        return Collections.unmodifiableMap(materials);
    }

    public static final DeferredItem<Item> JETPACK_ATTACHMENT_PLATE =
            ITEMS.registerItem("jetpack_attachment_plate", JetpackAttachmentPlateItem::new);
    public static final DeferredItem<TinCanItem> FILLED_TIN_CAN =
            ITEMS.registerItem("filled_tin_can", TinCanItem::new);

    public static final DeferredItem<BatteryItem> RE_BATTERY =
            battery("re_battery", 10000, 100, 1, 64, Rarity.COMMON);
    public static final DeferredItem<BatteryItem> ADVANCED_RE_BATTERY =
            battery("advanced_re_battery", 100000, 256, 2, 16, Rarity.COMMON);
    public static final DeferredItem<BatteryItem> ENERGY_CRYSTAL =
            battery("energy_crystal", 1000000, 2048, 3, 16, Rarity.COMMON);
    public static final DeferredItem<BatteryItem> LAPOTRON_CRYSTAL =
            battery("lapotron_crystal", 10000000, 8092, 4, 16, Rarity.UNCOMMON);

    public static final DeferredItem<TerraformingBlueprintItem> BLANK_TFBP =
            tfbp("blank_tfbp", 0.0, 0, null);
    public static final DeferredItem<TerraformingBlueprintItem> CHILLING_TFBP =
            tfbp("chilling_tfbp", 2000.0, 50, new Chilling());
    public static final DeferredItem<TerraformingBlueprintItem> CULTIVATION_TFBP =
            tfbp("cultivation_tfbp", 4000.0, 40, new Cultivation());
    public static final DeferredItem<TerraformingBlueprintItem> DESERTIFICATION_TFBP =
            tfbp("desertification_tfbp", 2500.0, 40, new Desertification());
    public static final DeferredItem<TerraformingBlueprintItem> FLATIFICATION_TFBP =
            tfbp("flatification_tfbp", 4000.0, 40, new Flatification());
    public static final DeferredItem<TerraformingBlueprintItem> IRRIGATION_TFBP =
            tfbp("irrigation_tfbp", 3000.0, 60, new Irrigation());
    public static final DeferredItem<TerraformingBlueprintItem> MUSHROOM_TFBP =
            tfbp("mushroom_tfbp", 8000.0, 25, new Mushroom());

    private static DeferredItem<TerraformingBlueprintItem> tfbp(
            String id, double consume, int range, @Nullable TerraformerProgram program) {
        return ITEMS.registerItem(
                id,
                properties ->
                        new TerraformingBlueprintItem(
                                properties.stacksTo(1), consume, range, program));
    }

    private static DeferredItem<BatteryItem> battery(
            String id, double capacity, double limit, int tier, int stackSize, Rarity rarity) {
        return ITEMS.registerItem(
                id,
                properties ->
                        new BatteryItem(
                                properties.stacksTo(stackSize).rarity(rarity),
                                new ElectricItemSpec(capacity, limit, tier, true)));
    }

    private ModItems() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(ModItems::addCreativeContents);
    }

    private static void addCreativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
            MATERIALS.values().forEach(event::accept);
            event.accept(JETPACK_ATTACHMENT_PLATE);
            event.accept(FILLED_TIN_CAN);
            event.accept(RE_BATTERY);
            event.accept(ADVANCED_RE_BATTERY);
            event.accept(ENERGY_CRYSTAL);
            event.accept(LAPOTRON_CRYSTAL);
        }
    }
}
