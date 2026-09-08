package ic2.neoforge.test;

import com.mojang.logging.LogUtils;

import ic2.core.energy.ElectricalProfile;
import ic2.core.energy.VoltageTier;
import ic2.core.energy.grid.EnergyMode;
import ic2.neoforge.energy.EnergyConfig;
import ic2.neoforge.registration.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

/** Development-only test mod; never included in the production JAR. */
@Mod("ic2_tests")
public final class RegistrationTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, "ic2_tests");

    private static final DeferredRegister<
                    com.mojang.serialization.MapCodec<
                            ? extends
                                    net.minecraft.gametest.framework.TestEnvironmentDefinition<?>>>
            ENVIRONMENTS =
                    DeferredRegister.create(
                            BuiltInRegistries.TEST_ENVIRONMENT_DEFINITION_TYPE, "ic2_tests");

    static {
        ENVIRONMENTS.register("strong_wind", () -> StrongWindEnvironment.CODEC);
        FUNCTIONS.register("generation_geothermal", () -> GenerationTests::geothermal);
        FUNCTIONS.register("generation_semifluid", () -> GenerationTests::semifluidPersistence);
        FUNCTIONS.register("generation_solar", () -> GenerationTests::solar);

        FUNCTIONS.register("upgrade_rates", () -> UpgradeTests::ratesAndPersistence);
        FUNCTIONS.register("upgrade_batch", () -> UpgradeTests::batch);
        FUNCTIONS.register("upgrade_transfers", () -> UpgradeTests::transfers);
        FUNCTIONS.register("upgrade_fluids", () -> UpgradeTests::fluids);

        FUNCTIONS.register("building_redstone", () -> BuildingTests::redstoneAndShapes);
        FUNCTIONS.register("building_signs", () -> BuildingTests::signs);
        FUNCTIONS.register("rubber_leaf_support", () -> WorldContentTests::leafSupport);
        FUNCTIONS.register("ore_placement", () -> WorldContentTests::orePlacement);
        FUNCTIONS.register("world_loot", () -> WorldContentTests::lootAndStripping);
        FUNCTIONS.register("rubber_resin", () -> WorldContentTests::resin);
        FUNCTIONS.register("rubber_sapling", () -> WorldContentTests::sapling);
        FUNCTIONS.register("world_features", () -> WorldContentTests::loadedFeatures);
        FUNCTIONS.register("tin_can_consumption", () -> ConsumptionTests::tinCans);
        FUNCTIONS.register("tool_mining", () -> ToolTests::miningAndRetention);
        FUNCTIONS.register("tool_interactions", () -> ToolTests::rotationAndInsulation);
        FUNCTIONS.register("tool_crafting", () -> ToolTests::crafting);
        FUNCTIONS.register("transformer_profiles", () -> EnergyDeviceTests::profiles);
        FUNCTIONS.register("storage_state", () -> EnergyDeviceTests::stateAndMenu);
        FUNCTIONS.register("transformer_chain", () -> EnergyDeviceTests::transformerChain);
        FUNCTIONS.register("storage_input", () -> EnergyDeviceTests::storageInput);
        FUNCTIONS.register("storage_direction", () -> EnergyDeviceTests::storageDirection);
        FUNCTIONS.register("canner_solid", () -> CannerTests::solid);
        FUNCTIONS.register("canner_fill_empty", () -> CannerTests::fillAndEmpty);
        FUNCTIONS.register("canner_enrichment_rollback", () -> CannerTests::enrichmentRollback);
        FUNCTIONS.register("canner_state_buttons", () -> CannerTests::stateAndButtons);

        FUNCTIONS.register("fluid_world", () -> FluidTests::worldInteraction);
        FUNCTIONS.register("fluid_families", () -> FluidTests::families);
        FUNCTIONS.register("fluid_cells", () -> FluidTests::cells);
        FUNCTIONS.register("iron_furnace", () -> MachineTests::ironFurnace);
        FUNCTIONS.register("crafting_charge", () -> CraftingTests::charge);
        FUNCTIONS.register("crafting_remainders", () -> CraftingTests::remainder);

        FUNCTIONS.register("toolbox_storage", () -> ToolboxTests::storage);
        FUNCTIONS.register("toolbox_menu_binding", () -> ToolboxTests::menuBinding);
        FUNCTIONS.register("toolbox_crafting", () -> ToolboxTests::crafting);
        FUNCTIONS.register("metal_former_modes", () -> MetalFormerTests::modes);
        FUNCTIONS.register("metal_former_persistence", () -> MetalFormerTests::persistence);
        FUNCTIONS.register("washing_atomic_outputs", () -> WashingTests::atomicOutputs);
        FUNCTIONS.register("washing_containers", () -> WashingTests::containers);
        FUNCTIONS.register("washing_pulling_upgrade", () -> WashingTests::pullingUpgrade);
        FUNCTIONS.register("washing_recipe_codec", () -> WashingTests::codec);
        FUNCTIONS.register(
                "centrifuge_processing", () -> CentrifugeTests::processingAndPersistence);
        FUNCTIONS.register("centrifuge_redstone", () -> CentrifugeTests::redstoneAndOutputBlocking);
        FUNCTIONS.register("centrifuge_recipe_codec", () -> CentrifugeTests::recipeCodec);
        FUNCTIONS.register("recycler_blacklist", () -> RecyclerTests::blacklist);
        FUNCTIONS.register("recycler_probability", () -> RecyclerTests::deterministicChance);
        FUNCTIONS.register("recycler_persistence", () -> RecyclerTests::persistenceAndBlocking);
        FUNCTIONS.register("recycler_components", () -> RecyclerTests::inputComponents);
        FUNCTIONS.register("induction_two_rows", () -> InductionTests::twoRowsPersistence);
        FUNCTIONS.register("induction_blocked_row", () -> InductionTests::blockedRow);
        FUNCTIONS.register("induction_menu_upgrades", () -> InductionTests::menuAndUpgrades);
        FUNCTIONS.register(
                "water_bucket_persistence", () -> WaterGenerationTests::bucketPersistence);
        FUNCTIONS.register("water_cell_automation", () -> WaterGenerationTests::cellAndAutomation);
        FUNCTIONS.register("water_ambient_rotor", () -> WaterGenerationTests::ambientAndRotor);
        FUNCTIONS.register("water_network_supply", () -> WaterGenerationTests::networkSupply);
        FUNCTIONS.register("wind_persistence", () -> WindGenerationTests::persistence);
        FUNCTIONS.register("wind_obstructions", () -> WindGenerationTests::obstructions);
        FUNCTIONS.register("wind_network_supply", () -> WindGenerationTests::networkSupply);
        FUNCTIONS.register("heat_transactions", () -> WorkEnergyTests::heatTransactions);
        FUNCTIONS.register("kinetic_persistence", () -> WorkEnergyTests::kineticPersistence);
        FUNCTIONS.register("work_parts_menu", () -> WorkEnergyTests::installedPartsMenu);
        FUNCTIONS.register("heat_conversion_chain", () -> WorkEnergyTests::heatChain);
        FUNCTIONS.register("kinetic_conversion_chain", () -> WorkEnergyTests::kineticChain);
        FUNCTIONS.register("solid_heat_reserve", () -> FuelHeatTests::solidReserve);
        FUNCTIONS.register("solid_heat_output", () -> FuelHeatTests::solidOutputAndFuel);
        FUNCTIONS.register("fluid_heat_prepaid", () -> FuelHeatTests::fluidPrepayment);
        FUNCTIONS.register("fluid_heat_chain", () -> FuelHeatTests::fluidChain);
        FUNCTIONS.register("heat_menu_transport", () -> FuelHeatTests::menuTransport);
        FUNCTIONS.register("manual_interaction", () -> ManualKineticTests::interaction);
        FUNCTIONS.register("manual_hunger_outputs", () -> ManualKineticTests::hungerAndOutputs);
        FUNCTIONS.register("manual_network_supply", () -> ManualKineticTests::networkSupply);
        FUNCTIONS.register("turbine_operation", () -> WindTurbineTests::operationAndWear);
        FUNCTIONS.register("turbine_obstructions", () -> WindTurbineTests::obstructions);
        FUNCTIONS.register("turbine_network_supply", () -> WindTurbineTests::networkSupply);
        FUNCTIONS.register("loaded_recipes", () -> ProcessingTests::loadedRecipes);
        FUNCTIONS.register("processing_machines", () -> ProcessingTests::processing);
        FUNCTIONS.register("weighted_persistence", () -> ProcessingTests::weightedPersistence);
        FUNCTIONS.register("processing_recipe_codec", () -> ProcessingTests::recipeCodec);

        FUNCTIONS.register("machine_chain", () -> MachineTests::chain);
        FUNCTIONS.register("wire_reconnect", () -> MachineTests::reconnect);
        FUNCTIONS.register("furnace_persistence", () -> MachineTests::furnacePersistence);
        FUNCTIONS.register("generator_persistence", () -> MachineTests::generatorPersistence);
        FUNCTIONS.register("battery_menu", () -> MachineTests::batteryAndMenu);

        FUNCTIONS.register("copper_plate", () -> RegistrationTests::copperPlate);
        FUNCTIONS.register("core_available", () -> RegistrationTests::coreAvailable);
        FUNCTIONS.register("materials", () -> ComponentTests::materials);
        FUNCTIONS.register("components_round_trip", () -> ComponentTests::roundTrip);
        FUNCTIONS.register("battery_transfer", () -> ComponentTests::battery);
        FUNCTIONS.register("invalid_charge", () -> ComponentTests::invalidCharge);
        FUNCTIONS.register("inventory_transactions", () -> TransferTests::inventory);
        FUNCTIONS.register("fluid_transactions", () -> TransferTests::fluid);
    }

    public RegistrationTests(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        ENVIRONMENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(
                (ServerAboutToStartEvent event) -> {
                    var mode =
                            EnergyMode.valueOf(System.getProperty("ic2.tests.energyMode", "IC2"));
                    EnergyConfig.MODE.set(mode);
                    LogUtils.getLogger().info("IC2 GameTest energy mode: {}", mode);
                });
    }

    private static void copperPlate(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.COPPER_PLATE.get());
        helper.assertTrue(
                !stack.isEmpty(), Component.literal("Copper plate must create a non-empty stack"));
        helper.assertTrue(
                BuiltInRegistries.ITEM
                        .getKey(stack.getItem())
                        .equals(Identifier.fromNamespaceAndPath("ic2", "copper_plate")),
                Component.literal("Copper plate must preserve the legacy registry ID"));
        helper.succeed();
    }

    private static void coreAvailable(GameTestHelper helper) {
        ElectricalProfile profile = new ElectricalProfile(VoltageTier.LV);
        profile.setRecipePower(33);
        helper.assertTrue(
                profile.getWorkingCurrent() == 2,
                Component.literal("The mod runtime must load the core module"));
        helper.succeed();
    }
}
