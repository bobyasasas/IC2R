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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

/** Development-only test mod; never included in the production JAR. */
@Mod("ic2_tests")
public final class RegistrationTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, "ic2_tests");

    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks("ic2_tests");

    /** Fake ae2:energy_acceptor: merged into ic2:ae2_energy_acceptor by the test tag. */
    public static final DeferredBlock<Block> TEST_ACCEPTOR =
            BLOCKS.registerBlock(
                    "test_energy_acceptor", properties -> new Block(properties));

    private static final DeferredRegister<
                    com.mojang.serialization.MapCodec<
                            ? extends
                                    net.minecraft.gametest.framework.TestEnvironmentDefinition<?>>>
            ENVIRONMENTS =
                    DeferredRegister.create(
                            BuiltInRegistries.TEST_ENVIRONMENT_DEFINITION_TYPE, "ic2_tests");

    static {
        ENVIRONMENTS.register("strong_wind", () -> StrongWindEnvironment.CODEC);
        FUNCTIONS.register("generation_geothermal", () -> EntityTickingTests.wrap(GenerationTests::geothermal));
        FUNCTIONS.register("generation_semifluid", () -> EntityTickingTests.wrap(GenerationTests::semifluidPersistence));
        FUNCTIONS.register("generation_solar", () -> EntityTickingTests.wrap(GenerationTests::solar));

        FUNCTIONS.register("upgrade_rates", () -> EntityTickingTests.wrap(UpgradeTests::ratesAndPersistence));
        FUNCTIONS.register("upgrade_batch", () -> EntityTickingTests.wrap(UpgradeTests::batch));
        FUNCTIONS.register("upgrade_transfers", () -> EntityTickingTests.wrap(UpgradeTests::transfers));
        FUNCTIONS.register("upgrade_fluids", () -> EntityTickingTests.wrap(UpgradeTests::fluids));

        FUNCTIONS.register("building_redstone", () -> EntityTickingTests.wrap(BuildingTests::redstoneAndShapes));
        FUNCTIONS.register("building_signs", () -> EntityTickingTests.wrap(BuildingTests::signs));
        FUNCTIONS.register("rubber_leaf_support", () -> EntityTickingTests.wrap(WorldContentTests::leafSupport));
        FUNCTIONS.register("ore_placement", () -> EntityTickingTests.wrap(WorldContentTests::orePlacement));
        FUNCTIONS.register("world_loot", () -> EntityTickingTests.wrap(WorldContentTests::lootAndStripping));
        FUNCTIONS.register("rubber_resin", () -> EntityTickingTests.wrap(WorldContentTests::resin));
        FUNCTIONS.register("rubber_sapling", () -> EntityTickingTests.wrap(WorldContentTests::sapling));
        FUNCTIONS.register("world_features", () -> EntityTickingTests.wrap(WorldContentTests::loadedFeatures));
        FUNCTIONS.register("tin_can_consumption", () -> EntityTickingTests.wrap(ConsumptionTests::tinCans));
        FUNCTIONS.register("tool_mining", () -> EntityTickingTests.wrap(ToolTests::miningAndRetention));
        FUNCTIONS.register("chainsaw_speed_and_drops", () -> EntityTickingTests.wrap(ChainsawItemTests::constantsSpeedAndDrops));
        FUNCTIONS.register("chainsaw_shear_break", () -> EntityTickingTests.wrap(ChainsawItemTests::shearBreak));
        FUNCTIONS.register("chainsaw_mode_toggle", () -> EntityTickingTests.wrap(ChainsawItemTests::modeToggle));
        FUNCTIONS.register("chainsaw_entity_shear", () -> EntityTickingTests.wrap(ChainsawItemTests::entityShear));
        FUNCTIONS.register("batpack_spec_armor", () -> EntityTickingTests.wrap(BatpackTests::specArmorAndExternalOutput));
        FUNCTIONS.register("batpack_distribution", () -> EntityTickingTests.wrap(BatpackTests::armorDistribution));
        FUNCTIONS.register("batpack_tier_gating", () -> EntityTickingTests.wrap(BatpackTests::tierGating));
        FUNCTIONS.register("batpack_mining", () -> EntityTickingTests.wrap(BatpackTests::miningThroughBatpack));
        FUNCTIONS.register("nano_spec_charged_attributes", () -> EntityTickingTests.wrap(NanoArmorTests::specAndChargedAttributes));
        FUNCTIONS.register("nano_energy_absorption", () -> EntityTickingTests.wrap(NanoArmorTests::energyAbsorption));
        FUNCTIONS.register("nano_fall_absorption", () -> EntityTickingTests.wrap(NanoArmorTests::fallAbsorption));
        FUNCTIONS.register("nano_night_vision", () -> EntityTickingTests.wrap(NanoArmorTests::nightVisionToggleAndTick));
        FUNCTIONS.register("quantum_spec_attributes", () -> EntityTickingTests.wrap(QuantumArmorTests::specAndAttributes));
        FUNCTIONS.register("quantum_damage_absorption", () -> EntityTickingTests.wrap(QuantumArmorTests::damageAbsorption));
        FUNCTIONS.register("quantum_helmet_life_support", () -> EntityTickingTests.wrap(QuantumArmorTests::helmetLifeSupport));
        FUNCTIONS.register("quantum_jetpack_flight", () -> EntityTickingTests.wrap(QuantumArmorTests::jetpackFlight));
        FUNCTIONS.register("quantum_jump_fall", () -> EntityTickingTests.wrap(QuantumArmorTests::jumpAndFall));
        FUNCTIONS.register("quantum_legs_speed", () -> EntityTickingTests.wrap(QuantumArmorTests::legsSpeedBoost));
        FUNCTIONS.register("equipment_specs", () -> EntityTickingTests.wrap(EquipmentPackTests::equipmentSpecs));
        FUNCTIONS.register("equipment_solar_helmet", () -> EntityTickingTests.wrap(EquipmentPackTests::solarHelmetChargesChest));
        FUNCTIONS.register("equipment_static_boots", () -> EntityTickingTests.wrap(EquipmentPackTests::staticBootsChargesChest));
        FUNCTIONS.register("equipment_jetpack_electric", () -> EntityTickingTests.wrap(EquipmentPackTests::jetpackElectricFlight));
        FUNCTIONS.register("equipment_jetpack_classic", () -> EntityTickingTests.wrap(EquipmentPackTests::jetpackClassicFlight));
        FUNCTIONS.register("jetpack_attachment_recipe", () -> EntityTickingTests.wrap(JetpackAttachmentTests::attachmentRecipe));
        FUNCTIONS.register("jetpack_attached_flight", () -> EntityTickingTests.wrap(JetpackAttachmentTests::attachedFlight));
        FUNCTIONS.register("jetpack_pop_back", () -> EntityTickingTests.wrap(JetpackAttachmentTests::popBack));
        FUNCTIONS.register("jetpack_world_fill", () -> EntityTickingTests.wrap(JetpackAttachmentTests::worldFill));
        FUNCTIONS.register("tesla_coil_shock", () -> EntityTickingTests.wrap(TeslaCoilTests::shockAndLedger));
        FUNCTIONS.register("tesla_coil_gate", () -> EntityTickingTests.wrap(TeslaCoilTests::redstoneGateAndEmptyTank));
        FUNCTIONS.register("tesla_coil_hazmat", () -> EntityTickingTests.wrap(TeslaCoilTests::hazmatImmunityAndSplit));
        FUNCTIONS.register("luminator_ledger", () -> EntityTickingTests.wrap(LuminatorTests::redstoneLedger));
        FUNCTIONS.register("luminator_invert", () -> EntityTickingTests.wrap(LuminatorTests::invertToggle));
        FUNCTIONS.register("luminator_discharge", () -> EntityTickingTests.wrap(LuminatorTests::handDischargeQuirk));
        FUNCTIONS.register("luminator_ignite", () -> EntityTickingTests.wrap(LuminatorTests::igniteMonsters));
        FUNCTIONS.register("luminator_support", () -> EntityTickingTests.wrap(LuminatorTests::supportCheck));
        FUNCTIONS.register("luminator_craft", () -> EntityTickingTests.wrap(LuminatorTests::crafting));
        FUNCTIONS.register("tool_interactions", () -> EntityTickingTests.wrap(ToolTests::rotationAndInsulation));
        FUNCTIONS.register("tool_crafting", () -> EntityTickingTests.wrap(ToolTests::crafting));
        FUNCTIONS.register("transformer_profiles", () -> EntityTickingTests.wrap(EnergyDeviceTests::profiles));
        FUNCTIONS.register("storage_state", () -> EntityTickingTests.wrap(EnergyDeviceTests::stateAndMenu));
        FUNCTIONS.register("transformer_chain", () -> EntityTickingTests.wrap(EnergyDeviceTests::transformerChain));
        FUNCTIONS.register("storage_input", () -> EntityTickingTests.wrap(EnergyDeviceTests::storageInput));
        FUNCTIONS.register("storage_direction", () -> EntityTickingTests.wrap(EnergyDeviceTests::storageDirection));
        FUNCTIONS.register("canner_solid", () -> EntityTickingTests.wrap(CannerTests::solid));
        FUNCTIONS.register("canner_fill_empty", () -> EntityTickingTests.wrap(CannerTests::fillAndEmpty));
        FUNCTIONS.register("canner_enrichment_rollback", () -> EntityTickingTests.wrap(CannerTests::enrichmentRollback));
        FUNCTIONS.register("canner_state_buttons", () -> EntityTickingTests.wrap(CannerTests::stateAndButtons));

        FUNCTIONS.register("fluid_world", () -> EntityTickingTests.wrap(FluidTests::worldInteraction));
        FUNCTIONS.register("fluid_families", () -> EntityTickingTests.wrap(FluidTests::families));
        FUNCTIONS.register("fluid_cells", () -> EntityTickingTests.wrap(FluidTests::cells));
        FUNCTIONS.register("iron_furnace", () -> EntityTickingTests.wrap(MachineTests::ironFurnace));
        FUNCTIONS.register("crafting_charge", () -> EntityTickingTests.wrap(CraftingTests::charge));
        FUNCTIONS.register("crafting_remainders", () -> EntityTickingTests.wrap(CraftingTests::remainder));
        FUNCTIONS.register("crafting_power_armor", () -> EntityTickingTests.wrap(CraftingTests::powerArmorLine));
        FUNCTIONS.register("crafting_packs", () -> EntityTickingTests.wrap(CraftingTests::packsCraftTheirStorageIn));
        FUNCTIONS.register(
                "crafting_chainsaw_routes", () -> CraftingTests::chainsawCraftsFromBothRoutes);
        FUNCTIONS.register("crafting_utility", () -> EntityTickingTests.wrap(CraftingTests::utilityItemsCraft));
        FUNCTIONS.register("boat_drops", () -> EntityTickingTests.wrap(BoatTests::dropsMatchLegacySuppliers));
        FUNCTIONS.register("boat_lava", () -> EntityTickingTests.wrap(BoatTests::lavaJudgesTheFamily));
        FUNCTIONS.register("boat_power_draw", () -> EntityTickingTests.wrap(BoatTests::drivenBoatDrawsFromWornPack));
        FUNCTIONS.register("laser_mining_shot", () -> EntityTickingTests.wrap(LaserTests::miningShotBillsAndBreaks));
        FUNCTIONS.register("laser_superheat", () -> EntityTickingTests.wrap(LaserTests::superheatSmeltsSandToGlass));
        FUNCTIONS.register("laser_mode_switch", () -> EntityTickingTests.wrap(LaserTests::sneakUseCyclesTheModeFree));
        FUNCTIONS.register("laser_explosive_shot", () -> EntityTickingTests.wrap(LaserTests::explosiveShotDetonates));
        FUNCTIONS.register("bronze_tools", () -> EntityTickingTests.wrap(BronzeKitTests::toolsCraftWithLegacyStats));
        FUNCTIONS.register(
                "bronze_armor", () -> BronzeKitTests::armorCraftsAndCarriesLegacyDefence);

        FUNCTIONS.register("toolbox_storage", () -> EntityTickingTests.wrap(ToolboxTests::storage));
        FUNCTIONS.register("toolbox_menu_binding", () -> EntityTickingTests.wrap(ToolboxTests::menuBinding));
        FUNCTIONS.register("toolbox_crafting", () -> EntityTickingTests.wrap(ToolboxTests::crafting));
        FUNCTIONS.register("metal_former_modes", () -> EntityTickingTests.wrap(MetalFormerTests::modes));
        FUNCTIONS.register("metal_former_persistence", () -> EntityTickingTests.wrap(MetalFormerTests::persistence));
        FUNCTIONS.register("washing_atomic_outputs", () -> EntityTickingTests.wrap(WashingTests::atomicOutputs));
        FUNCTIONS.register("washing_containers", () -> EntityTickingTests.wrap(WashingTests::containers));
        FUNCTIONS.register("washing_pulling_upgrade", () -> EntityTickingTests.wrap(WashingTests::pullingUpgrade));
        FUNCTIONS.register("washing_recipe_codec", () -> EntityTickingTests.wrap(WashingTests::codec));
        FUNCTIONS.register(
                "centrifuge_processing", () -> CentrifugeTests::processingAndPersistence);
        FUNCTIONS.register("centrifuge_redstone", () -> EntityTickingTests.wrap(CentrifugeTests::redstoneAndOutputBlocking));
        FUNCTIONS.register("centrifuge_recipe_codec", () -> EntityTickingTests.wrap(CentrifugeTests::recipeCodec));
        FUNCTIONS.register("recycler_blacklist", () -> EntityTickingTests.wrap(RecyclerTests::blacklist));
        FUNCTIONS.register("recycler_probability", () -> EntityTickingTests.wrap(RecyclerTests::deterministicChance));
        FUNCTIONS.register("recycler_persistence", () -> EntityTickingTests.wrap(RecyclerTests::persistenceAndBlocking));
        FUNCTIONS.register("recycler_components", () -> EntityTickingTests.wrap(RecyclerTests::inputComponents));
        FUNCTIONS.register("induction_two_rows", () -> EntityTickingTests.wrap(InductionTests::twoRowsPersistence));
        FUNCTIONS.register("induction_blocked_row", () -> EntityTickingTests.wrap(InductionTests::blockedRow));
        FUNCTIONS.register("induction_menu_upgrades", () -> EntityTickingTests.wrap(InductionTests::menuAndUpgrades));
        FUNCTIONS.register(
                "water_bucket_persistence", () -> WaterGenerationTests::bucketPersistence);
        FUNCTIONS.register("water_cell_automation", () -> EntityTickingTests.wrap(WaterGenerationTests::cellAndAutomation));
        FUNCTIONS.register("water_ambient_rotor", () -> EntityTickingTests.wrap(WaterGenerationTests::ambientAndRotor));
        FUNCTIONS.register("water_network_supply", () -> EntityTickingTests.wrap(WaterGenerationTests::networkSupply));
        FUNCTIONS.register("wind_persistence", () -> EntityTickingTests.wrap(WindGenerationTests::persistence));
        FUNCTIONS.register("wind_obstructions", () -> EntityTickingTests.wrap(WindGenerationTests::obstructions));
        FUNCTIONS.register("wind_network_supply", () -> EntityTickingTests.wrap(WindGenerationTests::networkSupply));
        FUNCTIONS.register("heat_transactions", () -> EntityTickingTests.wrap(WorkEnergyTests::heatTransactions));
        FUNCTIONS.register("kinetic_persistence", () -> EntityTickingTests.wrap(WorkEnergyTests::kineticPersistence));
        FUNCTIONS.register("work_parts_menu", () -> EntityTickingTests.wrap(WorkEnergyTests::installedPartsMenu));
        FUNCTIONS.register("heat_conversion_chain", () -> EntityTickingTests.wrap(WorkEnergyTests::heatChain));
        FUNCTIONS.register("kinetic_conversion_chain", () -> EntityTickingTests.wrap(WorkEnergyTests::kineticChain));
        FUNCTIONS.register("solid_heat_reserve", () -> EntityTickingTests.wrap(FuelHeatTests::solidReserve));
        FUNCTIONS.register("solid_heat_output", () -> EntityTickingTests.wrap(FuelHeatTests::solidOutputAndFuel));
        FUNCTIONS.register("fluid_heat_prepaid", () -> EntityTickingTests.wrap(FuelHeatTests::fluidPrepayment));
        FUNCTIONS.register("fluid_heat_chain", () -> EntityTickingTests.wrap(FuelHeatTests::fluidChain));
        FUNCTIONS.register("heat_menu_transport", () -> EntityTickingTests.wrap(FuelHeatTests::menuTransport));
        FUNCTIONS.register("manual_interaction", () -> EntityTickingTests.wrap(ManualKineticTests::interaction));
        FUNCTIONS.register("manual_hunger_outputs", () -> EntityTickingTests.wrap(ManualKineticTests::hungerAndOutputs));
        FUNCTIONS.register("manual_network_supply", () -> EntityTickingTests.wrap(ManualKineticTests::networkSupply));
        FUNCTIONS.register(
                "electrolysis_network_reload", () -> ElectrolyzerTests::networkAndReload);
        FUNCTIONS.register("electrolysis_output_rollback", () -> EntityTickingTests.wrap(ElectrolyzerTests::blockedOutput));
        FUNCTIONS.register(
                "electrolysis_interruption",
                () -> ElectrolyzerTests::interruptionAndRecipeIdentity);
        FUNCTIONS.register("electrolysis_datapack", () -> EntityTickingTests.wrap(ElectrolyzerTests::dataPackAndPorts));
        FUNCTIONS.register("regulator_partial", () -> EntityTickingTests.wrap(FluidRegulatorTests::partialTarget));
        FUNCTIONS.register("regulator_cadence", () -> EntityTickingTests.wrap(FluidRegulatorTests::cadence));
        FUNCTIONS.register("regulator_ports", () -> EntityTickingTests.wrap(FluidRegulatorTests::portsAndBlockedPower));
        FUNCTIONS.register("regulator_menu", () -> EntityTickingTests.wrap(FluidRegulatorTests::menuSettings));
        FUNCTIONS.register("tank_storage", () -> EntityTickingTests.wrap(TankTests::storageAndComparator));
        FUNCTIONS.register("tank_cursor", () -> EntityTickingTests.wrap(TankTests::cursorAndPermissions));
        FUNCTIONS.register("tank_upgrade", () -> EntityTickingTests.wrap(TankTests::partialBucketAndUpgrade));
        FUNCTIONS.register("reactor_heat_component", () -> EntityTickingTests.wrap(CondenserTests::heatComponent));
        FUNCTIONS.register("condenser_ports", () -> EntityTickingTests.wrap(CondenserTests::portsAndContainers));
        FUNCTIONS.register("condenser_blocked_output", () -> EntityTickingTests.wrap(CondenserTests::blockedOutput));
        FUNCTIONS.register("condenser_vents_power", () -> EntityTickingTests.wrap(CondenserTests::ventsAndPower));
        FUNCTIONS.register("heat_blast_damage", () -> EntityTickingTests.wrap(HeatExplosionTests::thermalDamageAndBarriers));
        FUNCTIONS.register("heat_blast_filter", () -> EntityTickingTests.wrap(HeatExplosionTests::detonateFiltering));
        FUNCTIONS.register("boiler_partial_menu", () -> EntityTickingTests.wrap(SteamGeneratorTests::partialSteamAndMenu));
        FUNCTIONS.register(
                "boiler_overheat_reload", () -> SteamGeneratorTests::overheatingAndReload);
        FUNCTIONS.register("boiler_calcification", () -> EntityTickingTests.wrap(SteamGeneratorTests::scaleStopsHeat));
        FUNCTIONS.register("boiler_superheated", () -> EntityTickingTests.wrap(SteamGeneratorTests::superheatedSteam));
        FUNCTIONS.register("boiler_condenser_chain", () -> EntityTickingTests.wrap(SteamGeneratorTests::condenserChain));
        FUNCTIONS.register(
                "steam_turbine_two_stage_loop", () -> SteamTurbineTests::twoStageWaterLoop);
        FUNCTIONS.register(
                "steam_turbine_rotor_disabled", () -> SteamTurbineTests::rotorAndDisabledMode);
        FUNCTIONS.register("steam_turbine_throttle", () -> EntityTickingTests.wrap(SteamTurbineTests::throttleAndPorts));
        FUNCTIONS.register("steam_turbine_condensate", () -> EntityTickingTests.wrap(SteamTurbineTests::condensateBacklog));
        FUNCTIONS.register(
                "steam_turbine_budget_reload", () -> SteamTurbineTests::drawBudgetAndReload);
        FUNCTIONS.register(
                "steam_turbine_consumer_first", () -> SteamTurbineTests::consumerTicksFirst);
        FUNCTIONS.register("boiler_warmup_ports", () -> EntityTickingTests.wrap(SteamGeneratorTests::warmupAndPorts));
        FUNCTIONS.register("heat_blast_unloaded", () -> EntityTickingTests.wrap(HeatExplosionTests::unloadedTerrain));
        FUNCTIONS.register("heat_blast_cancel", () -> EntityTickingTests.wrap(HeatExplosionTests::cancellation));
        FUNCTIONS.register("condenser_power_chain", () -> EntityTickingTests.wrap(CondenserTests::nativePowerChain));
        FUNCTIONS.register("condenser_passive_reload", () -> EntityTickingTests.wrap(CondenserTests::passiveReload));
        FUNCTIONS.register("solar_distiller_specs", () -> EntityTickingTests.wrap(SolarDistillerTests::specs));
        FUNCTIONS.register("solar_distiller_daylight", () -> EntityTickingTests.wrap(SolarDistillerTests::daylight));
        FUNCTIONS.register("solar_distiller_containers", () -> EntityTickingTests.wrap(SolarDistillerTests::containers));
        FUNCTIONS.register(
                "repressurizer_idle", () -> SteamRepressurizerTests::idleWithoutCandidate);
        FUNCTIONS.register(
                "repressurizer_ratios", () -> SteamRepressurizerTests::ratiosAndConservation);
        FUNCTIONS.register(
                "repressurizer_prepaid", () -> SteamRepressurizerTests::blockedOutputPrepaysHeat);
        FUNCTIONS.register(
                "repressurizer_candidate", () -> SteamRepressurizerTests::candidateSelection);
        FUNCTIONS.register("repressurizer_zero_rate", () -> EntityTickingTests.wrap(SteamRepressurizerTests::zeroRateStops));
        FUNCTIONS.register(
                "repressurizer_reload", () -> SteamRepressurizerTests::reserveSurvivesReload);
        FUNCTIONS.register("radioisotope_heat_curve", () -> EntityTickingTests.wrap(RtGeneratorTests::heatOutputCurve));
        FUNCTIONS.register("radioisotope_heat_reload", () -> EntityTickingTests.wrap(RtGeneratorTests::heatSurvivesReload));
        FUNCTIONS.register("radioisotope_generator", () -> EntityTickingTests.wrap(RtGeneratorTests::generatorProduces));
        FUNCTIONS.register("radioisotope_charging", () -> EntityTickingTests.wrap(RtGeneratorTests::generatorChargesTool));
        FUNCTIONS.register("radioisotope_automation", () -> EntityTickingTests.wrap(RtGeneratorTests::pelletsAutomate));
        FUNCTIONS.register("nuclear_uranium_centrifuge", () -> EntityTickingTests.wrap(NuclearTests::uraniumCentrifuge));
        FUNCTIONS.register(
                "nuclear_rtg_pellet_centrifuge", () -> NuclearTests::rtgPelletCentrifuge);
        FUNCTIONS.register("chargepad_inventory", () -> EntityTickingTests.wrap(ChargepadTests::chargesPlayerInventory));
        FUNCTIONS.register("chargepad_order_limits", () -> EntityTickingTests.wrap(ChargepadTests::chargeOrderAndLimits));
        FUNCTIONS.register("chargepad_network", () -> EntityTickingTests.wrap(ChargepadTests::networkFeeding));
        FUNCTIONS.register(
                "storage_box_wooden", () -> StorageBoxTests::woodenCapacityAndAutomation);
        FUNCTIONS.register("storage_box_iridium", () -> EntityTickingTests.wrap(StorageBoxTests::iridiumHolds126Slots));
        FUNCTIONS.register("sorting_machine_filter", () -> EntityTickingTests.wrap(SortingMachineTests::filterRouting));
        FUNCTIONS.register(
                "sorting_machine_default", () -> SortingMachineTests::defaultRouteFallback);
        FUNCTIONS.register("magnetizer_lift", () -> EntityTickingTests.wrap(MagnetizerTests::poweredLift));
        FUNCTIONS.register("magnetizer_unpowered", () -> EntityTickingTests.wrap(MagnetizerTests::unpoweredStays));
        FUNCTIONS.register("trade_o_mat_infinite", () -> EntityTickingTests.wrap(TradeOMatTests::infiniteTrade));
        FUNCTIONS.register("trade_o_mat_supply", () -> EntityTickingTests.wrap(TradeOMatTests::suppliedTrade));
        FUNCTIONS.register("energy_o_mat_trade", () -> EntityTickingTests.wrap(EnergyOMatTests::tradePaysForCredit));
        FUNCTIONS.register("energy_o_mat_gate", () -> EntityTickingTests.wrap(EnergyOMatTests::unpaidGate));
        FUNCTIONS.register("energy_o_mat_charge", () -> EntityTickingTests.wrap(EnergyOMatTests::creditPaysForCharge));
        FUNCTIONS.register("energy_o_mat_price", () -> EntityTickingTests.wrap(EnergyOMatTests::priceKeypad));
        FUNCTIONS.register("terraformer_chilling_ledger", () -> EntityTickingTests.wrap(TerraformerTests::chillingLedger));
        FUNCTIONS.register("terraformer_energy_gate", () -> EntityTickingTests.wrap(TerraformerTests::energyGate));
        FUNCTIONS.register("terraformer_blank_blueprint", () -> EntityTickingTests.wrap(TerraformerTests::blankBlueprint));
        FUNCTIONS.register("terraformer_hand_insert_eject", () -> EntityTickingTests.wrap(TerraformerTests::handInsertEject));
        FUNCTIONS.register("terraformer_program_transforms", () -> EntityTickingTests.wrap(TerraformerTests::programTransforms));
        FUNCTIONS.register("pump_faced_water", () -> EntityTickingTests.wrap(PumpTests::pumpsFacedWater));
        FUNCTIONS.register("pump_fills_buckets", () -> EntityTickingTests.wrap(PumpTests::fillsBuckets));
        FUNCTIONS.register("pump_progress_reload", () -> EntityTickingTests.wrap(PumpTests::survivesReload));
        FUNCTIONS.register("personal_chest_claim", () -> EntityTickingTests.wrap(PersonalChestTests::claimAndDeny));
        FUNCTIONS.register("personal_chest_automation", () -> EntityTickingTests.wrap(PersonalChestTests::blocksAutomation));
        FUNCTIONS.register("mining_pipe_shape", () -> EntityTickingTests.wrap(MiningPipeTests::pipeShapeAndTools));
        FUNCTIONS.register("mining_pipe_tip", () -> EntityTickingTests.wrap(MiningPipeTests::tipIsPlaceOnly));
        FUNCTIONS.register("mining_pipe_no_drops", () -> EntityTickingTests.wrap(MiningPipeTests::breakingDropsNothing));
        FUNCTIONS.register("drill_speed_and_drops", () -> EntityTickingTests.wrap(DrillItemTests::speedAndDrops));
        FUNCTIONS.register("drill_discharge", () -> EntityTickingTests.wrap(DrillItemTests::dischargePerBlock));
        FUNCTIONS.register("drill_miner_constants", () -> EntityTickingTests.wrap(DrillItemTests::minerConstants));
        FUNCTIONS.register("scanner_layer_scan", () -> EntityTickingTests.wrap(ScannerItemTests::layerScan));
        FUNCTIONS.register("miner_digs_down", () -> EntityTickingTests.wrap(MinerTests::digsDownAndHarvests));
        FUNCTIONS.register("miner_scanner_tunnel", () -> EntityTickingTests.wrap(MinerTests::scannerDigsTowardsOre));
        FUNCTIONS.register("miner_withdraw", () -> EntityTickingTests.wrap(MinerTests::withdrawsColumnWithoutDrill));
        FUNCTIONS.register("miner_pump_mode", () -> EntityTickingTests.wrap(MinerTests::pumpModeDrainsMarkedLiquid));
        FUNCTIONS.register("teleporter_link", () -> EntityTickingTests.wrap(TeleporterTests::linksAndTeleports));
        FUNCTIONS.register(
                "teleporter_cooldown_shortage", () -> TeleporterTests::cooldownAndShortage);
        FUNCTIONS.register("teleporter_unlink", () -> EntityTickingTests.wrap(TeleporterTests::unlinksInAir));
        FUNCTIONS.register("adv_miner_sweep", () -> EntityTickingTests.wrap(AdvMinerTests::sweepsAndMines));
        FUNCTIONS.register("adv_miner_whitelist", () -> EntityTickingTests.wrap(AdvMinerTests::whitelistGates));
        FUNCTIONS.register("adv_miner_silk_reset", () -> EntityTickingTests.wrap(AdvMinerTests::silkAndReset));
        FUNCTIONS.register(
                "mining_filter_card_defer",
                () -> MiningFilterCardTests::uneditedCardDefersToMachineFilter);
        FUNCTIONS.register(
                "mining_filter_card_override",
                () -> MiningFilterCardTests::editedCardOverridesMachineFilter);
        FUNCTIONS.register(
                "mining_filter_card_menu", () -> MiningFilterCardTests::handheldMenuEditsCard);
        FUNCTIONS.register("item_buffer_eject", () -> EntityTickingTests.wrap(ItemBufferTests::ejectorSendsSidesOut));
        FUNCTIONS.register("item_buffer_pull", () -> EntityTickingTests.wrap(ItemBufferTests::pullingTakesFromAbove));
        FUNCTIONS.register("item_buffer_ports", () -> EntityTickingTests.wrap(ItemBufferTests::portsAndUpgradeSlots));
        FUNCTIONS.register("block_cutter_plates", () -> EntityTickingTests.wrap(BlockCutterTests::cutsBlockIntoPlates));
        FUNCTIONS.register(
                "block_cutter_weak_blade",
                () -> BlockCutterTests::weakBladeStallsAndDiamondResumes);
        FUNCTIONS.register("block_cutter_no_blade", () -> EntityTickingTests.wrap(BlockCutterTests::missingBladeStalls));
        FUNCTIONS.register("blast_furnace_steel", () -> EntityTickingTests.wrap(BlastFurnaceTests::smeltsIronIntoSteel));
        FUNCTIONS.register("blast_furnace_cold", () -> EntityTickingTests.wrap(BlastFurnaceTests::staysColdWithoutHeat));
        FUNCTIONS.register("blast_furnace_air_cells", () -> EntityTickingTests.wrap(BlastFurnaceTests::airCellsFillTank));
        FUNCTIONS.register("gradual_vent", () -> EntityTickingTests.wrap(GradualRecipeTests::ventsStoredHeat));
        FUNCTIONS.register(
                "gradual_partial_vent", () -> GradualRecipeTests::partialVentKeepsRemainder);
        FUNCTIONS.register(
                "gradual_empty_rejected", () -> GradualRecipeTests::freshCondensatorIsRejected);
        FUNCTIONS.register("fuel_rod_dual_craft", () -> EntityTickingTests.wrap(FuelRodTests::freshRodsCraftDualRod));
        FUNCTIONS.register("fuel_rod_used_rejected", () -> EntityTickingTests.wrap(FuelRodTests::usedRodsAreRejected));
        FUNCTIONS.register(
                "fuel_rod_depleted_chain", () -> FuelRodTests::depletedRodsChainToCentrifuge);
        FUNCTIONS.register(
                "reactor_rod_pulse", () -> NuclearReactorTests::uraniumRodPulsesAndDepletes);
        FUNCTIONS.register(
                "reactor_adjacent_rods", () -> NuclearReactorTests::adjacentRodMultipliesHeat);
        FUNCTIONS.register("reactor_vent_absorb", () -> EntityTickingTests.wrap(NuclearReactorTests::ventAbsorbsRodHeat));
        FUNCTIONS.register("reactor_meltdown", () -> EntityTickingTests.wrap(NuclearReactorTests::meltDownExplodesCore));
        FUNCTIONS.register("reactor_chamber_widen", () -> EntityTickingTests.wrap(ReactorChamberTests::chamberWidensGrid));
        FUNCTIONS.register(
                "reactor_chamber_shrink", () -> ReactorChamberTests::brokenChamberEjectsColumn);
        FUNCTIONS.register("reactor_mox_pulse", () -> EntityTickingTests.wrap(ReactorChamberTests::moxPulseScalesWithHeat));
        FUNCTIONS.register(
                "reactor_chamber_chain", () -> ReactorChamberTests::chamberChainLegacyCount);
        FUNCTIONS.register(
                "reactor_full_size", () -> ReactorChamberTests::sixChambersReachFullSize);
        FUNCTIONS.register(
                "rci_bonus_full_size", () -> ReactorAccessHatchTests::rciBonusRaisesConversion);
        FUNCTIONS.register(
                "reactor_vessel_ring", () -> ReactorAccessHatchTests::vesselRingDetection);
        FUNCTIONS.register(
                "vessel_ring_ports", () -> ReactorAccessHatchTests::vesselRingAcceptsWallPieces);
        FUNCTIONS.register("reactor_reflector", () -> EntityTickingTests.wrap(ReactorComponentTests::reflectorBouncesPulse));
        FUNCTIONS.register("reactor_plating", () -> EntityTickingTests.wrap(ReactorComponentTests::platingRaisesCoreLimits));
        FUNCTIONS.register(
                "reactor_heat_switch", () -> ReactorComponentTests::heatSwitchBalancesCoreHeat);
        FUNCTIONS.register(
                "reactor_vent_spread", () -> ReactorComponentTests::ventSpreadCoolsNeighbours);
        FUNCTIONS.register(
                "pattern_storage_transfer", () -> PatternStorageTests::scannerTransfersToStorage);
        FUNCTIONS.register(
                "pattern_storage_dedupe", () -> PatternStorageTests::deduplicatesPatterns);
        FUNCTIONS.register(
                "pattern_storage_disk", () -> PatternStorageTests::writesPatternBackToDisk);
        FUNCTIONS.register(
                "replicator_single", () -> ReplicatorTests::replicatesPatternFromStorage);
        FUNCTIONS.register("replicator_no_uu", () -> EntityTickingTests.wrap(ReplicatorTests::modeStopsWithoutUu));
        FUNCTIONS.register(
                "replicator_value_uu", () -> ReplicatorTests::replicatorChargesValueDerivedUu);
        FUNCTIONS.register(
                "replicator_overclock", () -> ReplicatorTests::overclockScalesRates);
        FUNCTIONS.register(
                "replicator_persistence", () -> ReplicatorTests::persistenceCarriesRunState);
        FUNCTIONS.register(
                "replicator_browse_stop", () -> ReplicatorTests::browseStopAndPatternReset);
        FUNCTIONS.register(
                "replicator_valueless", () -> ReplicatorTests::valuelessPatternDrainsForever);
        FUNCTIONS.register(
                "reactor_fluid_mode",
                () -> ReactorFluidModeTests::fluidModeConvertsHeatToHotCoolant);
        FUNCTIONS.register(
                "reactor_fluid_mode_gating",
                () -> ReactorFluidModeTests::fluidModeRequiresFullStructure);
        FUNCTIONS.register(
                "reactor_vessel_conflict",
                () -> ReactorFluidModeTests::conflictingFluidReactorBlocksMode);
        FUNCTIONS.register(
                "reactor_fluid_port_extract",
                () -> ReactorFluidModeTests::hotCoolantExtractsThroughPort);
        FUNCTIONS.register(
                "reactor_heatpack_warm", () -> ReactorHeatEffectTests::heatpackWarmsVentStorage);
        FUNCTIONS.register("dynamite_placement", () -> EntityTickingTests.wrap(DynamiteTests::placesWithFacingAndSupport));
        FUNCTIONS.register("itnt_redstone_prime", () -> EntityTickingTests.wrap(ItntTests::redstonePrimesFusedCharge));
        FUNCTIONS.register("itnt_fuse_detonation", () -> EntityTickingTests.wrap(ItntTests::fuseDetonates));
        FUNCTIONS.register("itnt_break_primes", () -> EntityTickingTests.wrap(ItntTests::playerBreakPrimes));
        FUNCTIONS.register("itnt_chain_reaction", () -> EntityTickingTests.wrap(ItntTests::chainReaction));
        FUNCTIONS.register(
                "ic2_explosion_ray_crater", () -> Ic2ExplosionTests::rayCraterStopsAtAbsorption);
        FUNCTIONS.register(
                "ic2_explosion_bedrock_passthrough",
                () -> Ic2ExplosionTests::ultraResistantBedrockIsPassedThrough);
        FUNCTIONS.register(
                "ic2_explosion_shield_stops_rays", () -> Ic2ExplosionTests::stoneShieldStopsRays);
        FUNCTIONS.register(
                "ic2_explosion_drop_rate_zero",
                () -> Ic2ExplosionTests::zeroDropRateSuppressesDrops);
        FUNCTIONS.register(
                "ic2_explosion_drop_rate_full", () -> Ic2ExplosionTests::fullDropRateKeepsDrops);
        FUNCTIONS.register(
                "ic2_explosion_ray_damage", () -> Ic2ExplosionTests::accumulatedRayDamageKillsMob);
        FUNCTIONS.register(
                "ic2_explosion_nuclear_source",
                () -> Ic2ExplosionTests::nuclearTypeResolvesNukeDamageSource);
        FUNCTIONS.register(
                "itnt_blast_drops_debris", () -> Ic2ExplosionTests::itntBlastDropsDebris);
        FUNCTIONS.register("nuke_power_formula", () -> EntityTickingTests.wrap(NukeTests::powerFormulaFollowsLegacyScaling));
        FUNCTIONS.register(
                "nuke_radioactive_payload",
                () -> NukeTests::radioactivePayloadBoostsPowerAndRadiation);
        FUNCTIONS.register("nuke_redstone_primes", () -> EntityTickingTests.wrap(NukeTests::redstonePrimesLoadedCharge));
        FUNCTIONS.register("nuke_unloaded_refuses", () -> EntityTickingTests.wrap(NukeTests::unloadedNukeRefusesToPrime));
        FUNCTIONS.register("nuke_chain_reaction", () -> EntityTickingTests.wrap(NukeTests::chainReactionShortFuse));
        FUNCTIONS.register("nuke_wrench_defuses", () -> EntityTickingTests.wrap(NukeTests::wrenchDefusesPrimedCharge));
        FUNCTIONS.register("nuke_menu_slots", () -> EntityTickingTests.wrap(NukeTests::menuAcceptsOnlyPayloadItems));
        FUNCTIONS.register(
                "foam_sprayer_places_ten", () -> FoamTests::sprayerPlacesUpToTenFoamBlocks);
        FUNCTIONS.register("foam_sprayer_single_mode", () -> EntityTickingTests.wrap(FoamTests::singleModeSpraysOneBlock));
        FUNCTIONS.register("foam_scaffolding_covered", () -> EntityTickingTests.wrap(FoamTests::scaffoldingIsCoveredInFoam));
        FUNCTIONS.register("foam_sand_hardens", () -> EntityTickingTests.wrap(FoamTests::sandInstantlyHardensFoam));
        FUNCTIONS.register("foam_pack_supplies", () -> EntityTickingTests.wrap(FoamTests::foamPackSuppliesSprayer));
        FUNCTIONS.register("foam_fluid_slows", () -> EntityTickingTests.wrap(FoamTests::foamFluidSlowsEntities));
        FUNCTIONS.register(
                "foam_sprayer_fluid_filter", () -> FoamTests::sprayerAcceptsOnlyConstructionFoam);
        FUNCTIONS.register("painter_recolors_wool", () -> EntityTickingTests.wrap(PainterTests::painterRecolorsWool));
        FUNCTIONS.register(
                "painter_recolors_glass", () -> PainterTests::painterRecolorsGlassFamily);
        FUNCTIONS.register(
                "painter_recolors_terracotta_concrete",
                () -> PainterTests::painterRecolorsTerracottaAndConcrete);
        FUNCTIONS.register(
                "painter_recolors_carpet_candle",
                () -> PainterTests::painterRecolorsCarpetAndCandle);
        FUNCTIONS.register(
                "painter_recolors_bed", () -> PainterTests::painterRecolorsBedBothHalves);
        FUNCTIONS.register(
                "painter_recolors_shulker",
                () -> PainterTests::painterRecolorsShulkerBoxKeepsContents);
        FUNCTIONS.register("painter_wear_reverts", () -> EntityTickingTests.wrap(PainterTests::painterWearRevertsToPlain));
        FUNCTIONS.register(
                "painter_auto_refill", () -> PainterTests::painterAutoRefillConsumesSpare);
        FUNCTIONS.register("painter_dyes_sheep", () -> EntityTickingTests.wrap(PainterTests::painterDyesSheep));
        FUNCTIONS.register("painter_plain_passes", () -> EntityTickingTests.wrap(PainterTests::plainPainterPasses));
        FUNCTIONS.register(
                "painter_toggles_auto_refill", () -> PainterTests::painterTogglesAutoRefill);
        FUNCTIONS.register(
                "cable_shock_uninsulated",
                () -> CableShockTests::uninsulatedCableShocksNearbyEntities);
        FUNCTIONS.register(
                "cable_shock_insulated", () -> CableShockTests::insulatedCableShieldsEntities);
        FUNCTIONS.register("cable_shock_glass", () -> EntityTickingTests.wrap(CableShockTests::glassFibreCableNeverShocks));
        FUNCTIONS.register(
                "cable_shock_gold_meltdown",
                () -> CableShockTests::overloadedGoldCableMeltsDownAndShocks);
        FUNCTIONS.register(
                "obscurator_retextures_wall", () -> ObscuratorTests::obscuratorRetexturesWall);
        FUNCTIONS.register(
                "obscurator_requires_energy", () -> ObscuratorTests::obscuratorRequiresEnergy);
        FUNCTIONS.register(
                "obscurator_requires_reference",
                () -> ObscuratorTests::obscuratorRequiresReference);
        FUNCTIONS.register(
                "obscurator_sneak_passes_server",
                () -> ObscuratorTests::obscuratorSneakPassesServer);
        FUNCTIONS.register("obscurator_scan_payload", () -> EntityTickingTests.wrap(ObscuratorTests::obscuratorScanPayload));
        FUNCTIONS.register(
                "obscured_wall_persistence", () -> ObscuratorTests::obscuredWallPersistence);
        FUNCTIONS.register(
                "obscured_wall_drops_color_wall",
                () -> ObscuratorTests::obscuredWallDropsColorWall);
        FUNCTIONS.register("dynamite_linked_toggle", () -> EntityTickingTests.wrap(DynamiteTests::linkedStateToggles));
        FUNCTIONS.register("dynamite_redstone_fuse", () -> EntityTickingTests.wrap(DynamiteTests::redstonePrimesFuse));
        FUNCTIONS.register("dynamite_break_fuse", () -> EntityTickingTests.wrap(DynamiteTests::playerBreakPrimesFuse));
        FUNCTIONS.register("dynamite_explosion_chain", () -> EntityTickingTests.wrap(DynamiteTests::explosionChainsFuse));
        FUNCTIONS.register(
                "thrown_dynamite_detonates", () -> ThrownDynamiteTests::thrownDynamiteDetonates);
        FUNCTIONS.register(
                "sticky_dynamite_accelerates",
                () -> ThrownDynamiteTests::stickyDynamiteFuseAccelerates);
        FUNCTIONS.register("dynamite_water_disarms", () -> EntityTickingTests.wrap(ThrownDynamiteTests::waterDisarmsFuse));
        FUNCTIONS.register("reactor_vessel_place", () -> EntityTickingTests.wrap(ReactorVesselTests::vesselPlaces));
        FUNCTIONS.register(
                "crystal_memory_roundtrip", () -> CrystalMemoryTests::recordsAndReadsPattern);
        FUNCTIONS.register(
                "crystal_memory_value", () -> CrystalMemoryTests::valueSnapshotRoundTrips);
        FUNCTIONS.register("remote_detonate", () -> EntityTickingTests.wrap(RemoteTests::remoteDetonatesLinkedDynamite));
        FUNCTIONS.register("uu_scanner_scan", () -> EntityTickingTests.wrap(UuScannerTests::scansSeededItemOntoMemory));
        FUNCTIONS.register("crop_seed_bag_roundtrip", () -> EntityTickingTests.wrap(CropTests::cropSeedBagRoundtrip));
        FUNCTIONS.register("crop_plant_harvest", () -> EntityTickingTests.wrap(CropTests::cropPlantGrowHarvest));
        FUNCTIONS.register("crop_weed_growth", () -> EntityTickingTests.wrap(CropTests::cropWeedGrowth));
        FUNCTIONS.register("crop_base_seed_planting", () -> EntityTickingTests.wrap(CropTests::cropBaseSeedPlanting));
        FUNCTIONS.register("crop_reed_age_gains", () -> EntityTickingTests.wrap(CropTests::cropReedAgeScaledGains));
        FUNCTIONS.register("crop_coffee_harvest_window", () -> EntityTickingTests.wrap(CropTests::cropCoffeeHarvestWindow));
        FUNCTIONS.register("crop_cocoa_nutrient_gate", () -> EntityTickingTests.wrap(CropTests::cropCocoaNutrientGate));
        FUNCTIONS.register(
                "crop_nether_wart_soul_sand", () -> CropTests::cropNetherWartSoulSand);
        FUNCTIONS.register(
                "crop_potato_harvest_band", () -> CropTests::cropPotatoHarvestBand);
        FUNCTIONS.register("crop_mushroom_base_seed", () -> EntityTickingTests.wrap(CropTests::cropMushroomBaseSeed));
        FUNCTIONS.register("crop_sapling_gains", () -> EntityTickingTests.wrap(CropTests::cropSaplingGains));
        FUNCTIONS.register(
                "crop_flower_dye_harvest", () -> CropTests::cropFlowerDyeHarvest);
        FUNCTIONS.register("crop_pumpkin_stem", () -> EntityTickingTests.wrap(CropTests::cropPumpkinStem));
        FUNCTIONS.register("crop_melon_stem", () -> EntityTickingTests.wrap(CropTests::cropMelonStem));
        FUNCTIONS.register(
                "crop_venomilia_poison", () -> CropTests::cropVenomiliaPoison);
        FUNCTIONS.register(
                "crop_sticky_reed_resin", () -> CropTests::cropStickyReedResin);
        FUNCTIONS.register("crop_terra_wart_snow", () -> EntityTickingTests.wrap(CropTests::cropTerraWartSnow));
        FUNCTIONS.register(
                "crop_wart_snow_transmutation", () -> CropTests::cropWartSnowTransmutation);
        FUNCTIONS.register("crop_terra_wart_cure", () -> EntityTickingTests.wrap(CropTests::cropTerraWartCure));
        FUNCTIONS.register(
                "crop_metal_ore_root_gate", () -> CropTests::cropMetalOreRootGate);
        FUNCTIONS.register(
                "crop_shining_uncommon_roots", () -> CropTests::cropShiningUncommonRoots);
        FUNCTIONS.register(
                "crop_red_wheat_dim_light", () -> CropTests::cropRedWheatDimLight);
        FUNCTIONS.register(
                "crop_eating_plant_lava", () -> CropTests::cropEatingPlantLava);
        FUNCTIONS.register(
                "crop_generic_corium_drops", () -> CropTests::cropGenericCoriumDrops);
        FUNCTIONS.register(
                "crop_generic_special_drops", () -> CropTests::cropGenericSpecialDrops);
        FUNCTIONS.register(
                "crop_generic_milk_wart_base_seed", () -> CropTests::cropGenericMilkWartBaseSeed);
        FUNCTIONS.register(
                "crop_crossing_base_interactions", () -> CropTests::cropCrossingBaseInteractions);
        FUNCTIONS.register("crop_crossing_breed", () -> EntityTickingTests.wrap(CropTests::cropCrossingBreed));
        FUNCTIONS.register("crop_crossing_spread", () -> EntityTickingTests.wrap(CropTests::cropCrossingSpread));
        FUNCTIONS.register("crop_analyzer_scan_ladder", () -> EntityTickingTests.wrap(CropAnalyzerTests::scanLadder));
        FUNCTIONS.register("crop_analyzer_report", () -> EntityTickingTests.wrap(CropAnalyzerTests::cropReport));
        FUNCTIONS.register("crop_analyzer_menu", () -> EntityTickingTests.wrap(CropAnalyzerTests::menuIntegration));
        FUNCTIONS.register(
                "crop_care_fertilizer", () -> CropCareTests::fertilizerUse);
        FUNCTIONS.register(
                "crop_care_hydration", () -> CropCareTests::hydrationUse);
        FUNCTIONS.register(
                "crop_care_weed_ex_trowel", () -> CropCareTests::weedExAndTrowel);
        FUNCTIONS.register(
                "cropmatron_serves_crop", () -> CropmatronTests::servesCrop);
        FUNCTIONS.register(
                "cropmatron_hydrates_farmland", () -> CropmatronTests::hydratesFarmland);
        FUNCTIONS.register(
                "cropmatron_containers_upgrades", () -> CropmatronTests::containersAndUpgrades);
        FUNCTIONS.register(
                "crop_harvester_harvests", () -> CropHarvesterTests::harvestsRipeCrop);
        FUNCTIONS.register(
                "crop_harvester_full_buffer", () -> CropHarvesterTests::fullBufferGuards);
        FUNCTIONS.register(
                "crop_harvester_ejector_upgrades", () -> CropHarvesterTests::ejectorAndUpgrades);
        FUNCTIONS.register(
                "radiation_hazmat_set", () -> RadiationTests::hazmatCompleteSetDetection);
        FUNCTIONS.register(
                "radiation_effect_damage", () -> RadiationTests::radiationEffectDamagesHost);
        FUNCTIONS.register(
                "radiation_explosion_ring",
                () -> RadiationTests::explosionRadiationAffectsUnprotectedMobs);
        FUNCTIONS.register(
                "reactor_heat_radiation", () -> RadiationTests::reactorHeatRadiationDamages);
        FUNCTIONS.register("uu_values_datapack", () -> EntityTickingTests.wrap(UuScannerTests::datapackSeedsDriveGraph));
        FUNCTIONS.register("uu_scanner_unknown", () -> EntityTickingTests.wrap(UuScannerTests::unknownItemFails));
        FUNCTIONS.register("uu_scanner_seed_coverage", () -> EntityTickingTests.wrap(UuScannerTests::expandedSeedCoverage));
        FUNCTIONS.register(
                "uu_scanner_already_recorded", () -> UuScannerTests::alreadyRecordedSkipsRescan);
        FUNCTIONS.register(
                "uu_scanner_persistence", () -> UuScannerTests::persistenceCarriesScanProgress);
        FUNCTIONS.register(
                "uu_scanner_input_change", () -> UuScannerTests::inputChangeRestartsScan);
        FUNCTIONS.register(
                "uu_scanner_record_failure", () -> UuScannerTests::recordFailureHoldsPattern);
        FUNCTIONS.register(
                "reactor_redstone_port", () -> ReactorAccessHatchTests::redstonePortPowersCore);
        FUNCTIONS.register("reactor_hatch_grid", () -> EntityTickingTests.wrap(ReactorAccessHatchTests::hatchExposesGrid));
        FUNCTIONS.register(
                "reactor_rci_recharge", () -> ReactorAccessHatchTests::rciRechargesCondensator);
        FUNCTIONS.register(
                "matter_generator_scrap", () -> MatterGeneratorTests::scrapAmplifiesAndGenerates);
        FUNCTIONS.register(
                "matter_generator_cells", () -> MatterGeneratorTests::fillsUuMatterCells);
        FUNCTIONS.register(
                "matter_generator_redstone",
                () -> MatterGeneratorTests::redstoneGateStopsGeneration);
        FUNCTIONS.register("cooling_capacity", () -> EntityTickingTests.wrap(HeatExchangerTests::nearlyFullOutput));
        FUNCTIONS.register("cooling_parts_budget", () -> EntityTickingTests.wrap(HeatExchangerTests::partsAndBudget));
        FUNCTIONS.register("cooling_stirling_chain", () -> EntityTickingTests.wrap(HeatExchangerTests::stirlingChain));
        FUNCTIONS.register("cooling_containers", () -> EntityTickingTests.wrap(HeatExchangerTests::containersAndPorts));
        FUNCTIONS.register("cooling_datapack", () -> EntityTickingTests.wrap(HeatExchangerTests::dataPackRecipe));
        FUNCTIONS.register("fermenter_biofuel_chain", () -> EntityTickingTests.wrap(FermenterTests::biofuelChain));
        FUNCTIONS.register("fermenter_heat_reload", () -> EntityTickingTests.wrap(FermenterTests::heatChainAndReload));
        FUNCTIONS.register("fermenter_blocked_gas", () -> EntityTickingTests.wrap(FermenterTests::blockedGas));
        FUNCTIONS.register("fermenter_blocked_fertilizer", () -> EntityTickingTests.wrap(FermenterTests::blockedFertilizer));
        FUNCTIONS.register("fermenter_containers", () -> EntityTickingTests.wrap(FermenterTests::containersAndPorts));
        FUNCTIONS.register("fermenter_datapack", () -> EntityTickingTests.wrap(FermenterTests::dataPackRecipe));
        FUNCTIONS.register("water_turbine_operation", () -> EntityTickingTests.wrap(WaterTurbineTests::operationAndWear));
        FUNCTIONS.register("water_turbine_obstructions", () -> EntityTickingTests.wrap(WaterTurbineTests::obstructions));
        FUNCTIONS.register("water_turbine_tides", () -> EntityTickingTests.wrap(WaterTurbineTests::tidesAndDeepOcean));
        FUNCTIONS.register("water_turbine_network", () -> EntityTickingTests.wrap(WaterTurbineTests::networkSupply));
        FUNCTIONS.register("turbine_operation", () -> EntityTickingTests.wrap(WindTurbineTests::operationAndWear));
        FUNCTIONS.register("turbine_obstructions", () -> EntityTickingTests.wrap(WindTurbineTests::obstructions));
        FUNCTIONS.register("turbine_network_supply", () -> EntityTickingTests.wrap(WindTurbineTests::networkSupply));
        FUNCTIONS.register("loaded_recipes", () -> EntityTickingTests.wrap(ProcessingTests::loadedRecipes));
        FUNCTIONS.register("processing_machines", () -> EntityTickingTests.wrap(ProcessingTests::processing));
        FUNCTIONS.register("weighted_persistence", () -> EntityTickingTests.wrap(ProcessingTests::weightedPersistence));
        FUNCTIONS.register("processing_recipe_codec", () -> EntityTickingTests.wrap(ProcessingTests::recipeCodec));
        FUNCTIONS.register("jei_categories_non_empty", () -> EntityTickingTests.wrap(ProcessingTests::jeiCategoriesNonEmpty));

        FUNCTIONS.register("machine_chain", () -> EntityTickingTests.wrap(MachineTests::chain));
        FUNCTIONS.register("wire_reconnect", () -> EntityTickingTests.wrap(MachineTests::reconnect));
        FUNCTIONS.register("furnace_persistence", () -> EntityTickingTests.wrap(MachineTests::furnacePersistence));
        FUNCTIONS.register("generator_persistence", () -> EntityTickingTests.wrap(MachineTests::generatorPersistence));
        FUNCTIONS.register("battery_menu", () -> EntityTickingTests.wrap(MachineTests::batteryAndMenu));

        FUNCTIONS.register("copper_plate", () -> EntityTickingTests.wrap(RegistrationTests::copperPlate));
        FUNCTIONS.register("core_available", () -> EntityTickingTests.wrap(RegistrationTests::coreAvailable));
        FUNCTIONS.register("materials", () -> EntityTickingTests.wrap(ComponentTests::materials));
        FUNCTIONS.register("components_round_trip", () -> EntityTickingTests.wrap(ComponentTests::roundTrip));
        FUNCTIONS.register("battery_transfer", () -> EntityTickingTests.wrap(ComponentTests::battery));
        FUNCTIONS.register("invalid_charge", () -> EntityTickingTests.wrap(ComponentTests::invalidCharge));
        FUNCTIONS.register("inventory_transactions", () -> EntityTickingTests.wrap(TransferTests::inventory));
        FUNCTIONS.register("fluid_transactions", () -> EntityTickingTests.wrap(TransferTests::fluid));
        FUNCTIONS.register("ae2_bridge_feeds", () -> EntityTickingTests.wrap(Ae2BridgeTests::bridgeFeedsAcceptorOverCable));
        FUNCTIONS.register("ae2_bridge_ratio", () -> EntityTickingTests.wrap(Ae2BridgeTests::bridgeChargesTwoAePerEuExactly));
        FUNCTIONS.register("ae2_bridge_no_path", () -> EntityTickingTests.wrap(Ae2BridgeTests::bridgeWithoutPathDrawsNothing));
        FUNCTIONS.register("saber_use_toggle", () -> EntityTickingTests.wrap(NanoSaberTests::useTogglesActiveFree));
        FUNCTIONS.register("saber_held_billing", () -> EntityTickingTests.wrap(NanoSaberTests::heldBillingDepletesAndShutsOff));
        FUNCTIONS.register("saber_attributes", () -> EntityTickingTests.wrap(NanoSaberTests::attributesFollowActivation));
        FUNCTIONS.register("saber_strike_armor_drain", () -> EntityTickingTests.wrap(NanoSaberTests::strikeDrainsNanoArmor));
        FUNCTIONS.register("single_use_battery_charge", () -> EntityTickingTests.wrap(ChargingBatteryTests::singleUseChargesHotbarAndConsumes));
        FUNCTIONS.register("charging_battery_tick_feed", () -> EntityTickingTests.wrap(ChargingBatteryTests::chargingBatteryTickChargesHotbar));
        FUNCTIONS.register("charging_battery_mode_gate", () -> EntityTickingTests.wrap(ChargingBatteryTests::modeCycleGatesFeeding));
        FUNCTIONS.register("mug_drink_effects", () -> EntityTickingTests.wrap(MugTests::mugDrinkAppliesEffectsAndReturnsEmpty));
        FUNCTIONS.register("mug_use_flow", () -> EntityTickingTests.wrap(MugTests::mugUseDurationAndAnimation));
        FUNCTIONS.register("mug_overdrink_backfire", () -> EntityTickingTests.wrap(MugTests::mugOverdrinkBackfire));
        FUNCTIONS.register("sheet_placement_rules", () -> EntityTickingTests.wrap(SheetTests::placementRules));
        FUNCTIONS.register("sheet_support_break", () -> EntityTickingTests.wrap(SheetTests::supportBreakAndWoolPersists));
        FUNCTIONS.register("sheet_resin_cushion", () -> EntityTickingTests.wrap(SheetTests::resinSheetCushionsFalls));
        FUNCTIONS.register("sheet_rubber_trampoline", () -> EntityTickingTests.wrap(SheetTests::rubberSheetBouncesItems));
        FUNCTIONS.register("sheet_rubber_weight_break", () -> EntityTickingTests.wrap(SheetTests::rubberSheetBreaksUnderLivingWeight));
        FUNCTIONS.register("sheet_wool_collision", () -> EntityTickingTests.wrap(SheetTests::woolSheetCollisionSemantics));
        FUNCTIONS.register("fluid_ingredient_shapeless", () -> EntityTickingTests.wrap(FluidIngredientTests::shapelessWaterCellCraftsAndDrains));
        FUNCTIONS.register("fluid_ingredient_shaped_coolant", () -> EntityTickingTests.wrap(FluidIngredientTests::shapedCoolantCellMatches));
        FUNCTIONS.register("fluid_ingredient_compressor", () -> EntityTickingTests.wrap(FluidIngredientTests::compressorWaterCellToSnow));
        FUNCTIONS.register("wind_meter_bare_use", () -> EntityTickingTests.wrap(WindMeterTests::bareUseReadsWindAndPays));
        FUNCTIONS.register("wind_meter_low_charge_passes", () -> EntityTickingTests.wrap(WindMeterTests::lowChargePasses));
        FUNCTIONS.register("wind_meter_generator_reading", () -> EntityTickingTests.wrap(WindMeterTests::readsWindGeneratorEffectiveWind));
        FUNCTIONS.register("wind_meter_stopped_turbine", () -> EntityTickingTests.wrap(WindMeterTests::readsStoppedTurbineWithoutDraining));
        FUNCTIONS.register("refractory_bricks_drop", () -> EntityTickingTests.wrap(WindMeterTests::refractoryBricksDropThemselves));
        FUNCTIONS.register("reinforced_door_half_semantics", () -> EntityTickingTests.wrap(WindMeterTests::reinforcedDoorHalfSemantics));
    }

    public RegistrationTests(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        ENVIRONMENTS.register(modBus);
        BLOCKS.register(modBus);
        modBus.addListener(
                (RegisterCapabilitiesEvent event) ->
                        event.registerBlock(
                                Capabilities.Energy.BLOCK,
                                (level, pos, state, blockEntity, side) ->
                                        TestEnergyStorage.at(level, pos),
                                TEST_ACCEPTOR.get()));
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
