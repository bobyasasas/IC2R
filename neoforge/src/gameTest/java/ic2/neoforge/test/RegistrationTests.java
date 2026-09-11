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
        FUNCTIONS.register("chainsaw_speed_and_drops", () -> ChainsawItemTests::constantsSpeedAndDrops);
        FUNCTIONS.register("chainsaw_shear_break", () -> ChainsawItemTests::shearBreak);
        FUNCTIONS.register("chainsaw_mode_toggle", () -> ChainsawItemTests::modeToggle);
        FUNCTIONS.register("chainsaw_entity_shear", () -> ChainsawItemTests::entityShear);
        FUNCTIONS.register("batpack_spec_armor", () -> BatpackTests::specArmorAndExternalOutput);
        FUNCTIONS.register("batpack_distribution", () -> BatpackTests::armorDistribution);
        FUNCTIONS.register("batpack_tier_gating", () -> BatpackTests::tierGating);
        FUNCTIONS.register("batpack_mining", () -> BatpackTests::miningThroughBatpack);
        FUNCTIONS.register("nano_spec_charged_attributes", () -> NanoArmorTests::specAndChargedAttributes);
        FUNCTIONS.register("nano_energy_absorption", () -> NanoArmorTests::energyAbsorption);
        FUNCTIONS.register("nano_fall_absorption", () -> NanoArmorTests::fallAbsorption);
        FUNCTIONS.register("nano_night_vision", () -> NanoArmorTests::nightVisionToggleAndTick);
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
        FUNCTIONS.register(
                "electrolysis_network_reload", () -> ElectrolyzerTests::networkAndReload);
        FUNCTIONS.register("electrolysis_output_rollback", () -> ElectrolyzerTests::blockedOutput);
        FUNCTIONS.register(
                "electrolysis_interruption",
                () -> ElectrolyzerTests::interruptionAndRecipeIdentity);
        FUNCTIONS.register("electrolysis_datapack", () -> ElectrolyzerTests::dataPackAndPorts);
        FUNCTIONS.register("regulator_partial", () -> FluidRegulatorTests::partialTarget);
        FUNCTIONS.register("regulator_cadence", () -> FluidRegulatorTests::cadence);
        FUNCTIONS.register("regulator_ports", () -> FluidRegulatorTests::portsAndBlockedPower);
        FUNCTIONS.register("regulator_menu", () -> FluidRegulatorTests::menuSettings);
        FUNCTIONS.register("tank_storage", () -> TankTests::storageAndComparator);
        FUNCTIONS.register("tank_cursor", () -> TankTests::cursorAndPermissions);
        FUNCTIONS.register("tank_upgrade", () -> TankTests::partialBucketAndUpgrade);
        FUNCTIONS.register("reactor_heat_component", () -> CondenserTests::heatComponent);
        FUNCTIONS.register("condenser_ports", () -> CondenserTests::portsAndContainers);
        FUNCTIONS.register("condenser_blocked_output", () -> CondenserTests::blockedOutput);
        FUNCTIONS.register("condenser_vents_power", () -> CondenserTests::ventsAndPower);
        FUNCTIONS.register("heat_blast_damage", () -> HeatExplosionTests::thermalDamageAndBarriers);
        FUNCTIONS.register("heat_blast_filter", () -> HeatExplosionTests::detonateFiltering);
        FUNCTIONS.register("boiler_partial_menu", () -> SteamGeneratorTests::partialSteamAndMenu);
        FUNCTIONS.register(
                "boiler_overheat_reload", () -> SteamGeneratorTests::overheatingAndReload);
        FUNCTIONS.register("boiler_calcification", () -> SteamGeneratorTests::scaleStopsHeat);
        FUNCTIONS.register("boiler_superheated", () -> SteamGeneratorTests::superheatedSteam);
        FUNCTIONS.register("boiler_condenser_chain", () -> SteamGeneratorTests::condenserChain);
        FUNCTIONS.register(
                "steam_turbine_two_stage_loop", () -> SteamTurbineTests::twoStageWaterLoop);
        FUNCTIONS.register(
                "steam_turbine_rotor_disabled", () -> SteamTurbineTests::rotorAndDisabledMode);
        FUNCTIONS.register("steam_turbine_throttle", () -> SteamTurbineTests::throttleAndPorts);
        FUNCTIONS.register("steam_turbine_condensate", () -> SteamTurbineTests::condensateBacklog);
        FUNCTIONS.register(
                "steam_turbine_budget_reload", () -> SteamTurbineTests::drawBudgetAndReload);
        FUNCTIONS.register(
                "steam_turbine_consumer_first", () -> SteamTurbineTests::consumerTicksFirst);
        FUNCTIONS.register("boiler_warmup_ports", () -> SteamGeneratorTests::warmupAndPorts);
        FUNCTIONS.register("heat_blast_unloaded", () -> HeatExplosionTests::unloadedTerrain);
        FUNCTIONS.register("heat_blast_cancel", () -> HeatExplosionTests::cancellation);
        FUNCTIONS.register("condenser_power_chain", () -> CondenserTests::nativePowerChain);
        FUNCTIONS.register("condenser_passive_reload", () -> CondenserTests::passiveReload);
        FUNCTIONS.register(
                "repressurizer_idle", () -> SteamRepressurizerTests::idleWithoutCandidate);
        FUNCTIONS.register(
                "repressurizer_ratios", () -> SteamRepressurizerTests::ratiosAndConservation);
        FUNCTIONS.register(
                "repressurizer_prepaid", () -> SteamRepressurizerTests::blockedOutputPrepaysHeat);
        FUNCTIONS.register(
                "repressurizer_candidate", () -> SteamRepressurizerTests::candidateSelection);
        FUNCTIONS.register("repressurizer_zero_rate", () -> SteamRepressurizerTests::zeroRateStops);
        FUNCTIONS.register(
                "repressurizer_reload", () -> SteamRepressurizerTests::reserveSurvivesReload);
        FUNCTIONS.register("radioisotope_heat_curve", () -> RtGeneratorTests::heatOutputCurve);
        FUNCTIONS.register("radioisotope_heat_reload", () -> RtGeneratorTests::heatSurvivesReload);
        FUNCTIONS.register("radioisotope_generator", () -> RtGeneratorTests::generatorProduces);
        FUNCTIONS.register("radioisotope_charging", () -> RtGeneratorTests::generatorChargesTool);
        FUNCTIONS.register("radioisotope_automation", () -> RtGeneratorTests::pelletsAutomate);
        FUNCTIONS.register("nuclear_uranium_centrifuge", () -> NuclearTests::uraniumCentrifuge);
        FUNCTIONS.register(
                "nuclear_rtg_pellet_centrifuge", () -> NuclearTests::rtgPelletCentrifuge);
        FUNCTIONS.register("chargepad_inventory", () -> ChargepadTests::chargesPlayerInventory);
        FUNCTIONS.register("chargepad_order_limits", () -> ChargepadTests::chargeOrderAndLimits);
        FUNCTIONS.register("chargepad_network", () -> ChargepadTests::networkFeeding);
        FUNCTIONS.register(
                "storage_box_wooden", () -> StorageBoxTests::woodenCapacityAndAutomation);
        FUNCTIONS.register("storage_box_iridium", () -> StorageBoxTests::iridiumHolds126Slots);
        FUNCTIONS.register("sorting_machine_filter", () -> SortingMachineTests::filterRouting);
        FUNCTIONS.register(
                "sorting_machine_default", () -> SortingMachineTests::defaultRouteFallback);
        FUNCTIONS.register("magnetizer_lift", () -> MagnetizerTests::poweredLift);
        FUNCTIONS.register("magnetizer_unpowered", () -> MagnetizerTests::unpoweredStays);
        FUNCTIONS.register("trade_o_mat_infinite", () -> TradeOMatTests::infiniteTrade);
        FUNCTIONS.register("trade_o_mat_supply", () -> TradeOMatTests::suppliedTrade);
        FUNCTIONS.register("pump_faced_water", () -> PumpTests::pumpsFacedWater);
        FUNCTIONS.register("pump_fills_buckets", () -> PumpTests::fillsBuckets);
        FUNCTIONS.register("pump_progress_reload", () -> PumpTests::survivesReload);
        FUNCTIONS.register("personal_chest_claim", () -> PersonalChestTests::claimAndDeny);
        FUNCTIONS.register("personal_chest_automation", () -> PersonalChestTests::blocksAutomation);
        FUNCTIONS.register("mining_pipe_shape", () -> MiningPipeTests::pipeShapeAndTools);
        FUNCTIONS.register("mining_pipe_tip", () -> MiningPipeTests::tipIsPlaceOnly);
        FUNCTIONS.register("mining_pipe_no_drops", () -> MiningPipeTests::breakingDropsNothing);
        FUNCTIONS.register("drill_speed_and_drops", () -> DrillItemTests::speedAndDrops);
        FUNCTIONS.register("drill_discharge", () -> DrillItemTests::dischargePerBlock);
        FUNCTIONS.register("drill_miner_constants", () -> DrillItemTests::minerConstants);
        FUNCTIONS.register("scanner_layer_scan", () -> ScannerItemTests::layerScan);
        FUNCTIONS.register("miner_digs_down", () -> MinerTests::digsDownAndHarvests);
        FUNCTIONS.register("miner_scanner_tunnel", () -> MinerTests::scannerDigsTowardsOre);
        FUNCTIONS.register("miner_withdraw", () -> MinerTests::withdrawsColumnWithoutDrill);
        FUNCTIONS.register("miner_pump_mode", () -> MinerTests::pumpModeDrainsMarkedLiquid);
        FUNCTIONS.register("teleporter_link", () -> TeleporterTests::linksAndTeleports);
        FUNCTIONS.register(
                "teleporter_cooldown_shortage", () -> TeleporterTests::cooldownAndShortage);
        FUNCTIONS.register("teleporter_unlink", () -> TeleporterTests::unlinksInAir);
        FUNCTIONS.register("adv_miner_sweep", () -> AdvMinerTests::sweepsAndMines);
        FUNCTIONS.register("adv_miner_whitelist", () -> AdvMinerTests::whitelistGates);
        FUNCTIONS.register("adv_miner_silk_reset", () -> AdvMinerTests::silkAndReset);
        FUNCTIONS.register(
                "mining_filter_card_defer",
                () -> MiningFilterCardTests::uneditedCardDefersToMachineFilter);
        FUNCTIONS.register(
                "mining_filter_card_override",
                () -> MiningFilterCardTests::editedCardOverridesMachineFilter);
        FUNCTIONS.register(
                "mining_filter_card_menu", () -> MiningFilterCardTests::handheldMenuEditsCard);
        FUNCTIONS.register("item_buffer_eject", () -> ItemBufferTests::ejectorSendsSidesOut);
        FUNCTIONS.register("item_buffer_pull", () -> ItemBufferTests::pullingTakesFromAbove);
        FUNCTIONS.register("item_buffer_ports", () -> ItemBufferTests::portsAndUpgradeSlots);
        FUNCTIONS.register("block_cutter_plates", () -> BlockCutterTests::cutsBlockIntoPlates);
        FUNCTIONS.register(
                "block_cutter_weak_blade",
                () -> BlockCutterTests::weakBladeStallsAndDiamondResumes);
        FUNCTIONS.register("block_cutter_no_blade", () -> BlockCutterTests::missingBladeStalls);
        FUNCTIONS.register("blast_furnace_steel", () -> BlastFurnaceTests::smeltsIronIntoSteel);
        FUNCTIONS.register("blast_furnace_cold", () -> BlastFurnaceTests::staysColdWithoutHeat);
        FUNCTIONS.register("blast_furnace_air_cells", () -> BlastFurnaceTests::airCellsFillTank);
        FUNCTIONS.register("gradual_vent", () -> GradualRecipeTests::ventsStoredHeat);
        FUNCTIONS.register(
                "gradual_partial_vent", () -> GradualRecipeTests::partialVentKeepsRemainder);
        FUNCTIONS.register(
                "gradual_empty_rejected", () -> GradualRecipeTests::freshCondensatorIsRejected);
        FUNCTIONS.register("fuel_rod_dual_craft", () -> FuelRodTests::freshRodsCraftDualRod);
        FUNCTIONS.register("fuel_rod_used_rejected", () -> FuelRodTests::usedRodsAreRejected);
        FUNCTIONS.register(
                "fuel_rod_depleted_chain", () -> FuelRodTests::depletedRodsChainToCentrifuge);
        FUNCTIONS.register(
                "reactor_rod_pulse", () -> NuclearReactorTests::uraniumRodPulsesAndDepletes);
        FUNCTIONS.register(
                "reactor_adjacent_rods", () -> NuclearReactorTests::adjacentRodMultipliesHeat);
        FUNCTIONS.register("reactor_vent_absorb", () -> NuclearReactorTests::ventAbsorbsRodHeat);
        FUNCTIONS.register("reactor_meltdown", () -> NuclearReactorTests::meltDownExplodesCore);
        FUNCTIONS.register("reactor_chamber_widen", () -> ReactorChamberTests::chamberWidensGrid);
        FUNCTIONS.register(
                "reactor_chamber_shrink", () -> ReactorChamberTests::brokenChamberEjectsColumn);
        FUNCTIONS.register("reactor_mox_pulse", () -> ReactorChamberTests::moxPulseScalesWithHeat);
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
        FUNCTIONS.register("reactor_reflector", () -> ReactorComponentTests::reflectorBouncesPulse);
        FUNCTIONS.register("reactor_plating", () -> ReactorComponentTests::platingRaisesCoreLimits);
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
        FUNCTIONS.register("replicator_no_uu", () -> ReplicatorTests::modeStopsWithoutUu);
        FUNCTIONS.register(
                "replicator_value_uu", () -> ReplicatorTests::replicatorChargesValueDerivedUu);
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
        FUNCTIONS.register("dynamite_placement", () -> DynamiteTests::placesWithFacingAndSupport);
        FUNCTIONS.register("itnt_redstone_prime", () -> ItntTests::redstonePrimesFusedCharge);
        FUNCTIONS.register("itnt_fuse_detonation", () -> ItntTests::fuseDetonates);
        FUNCTIONS.register("itnt_break_primes", () -> ItntTests::playerBreakPrimes);
        FUNCTIONS.register("itnt_chain_reaction", () -> ItntTests::chainReaction);
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
        FUNCTIONS.register("nuke_power_formula", () -> NukeTests::powerFormulaFollowsLegacyScaling);
        FUNCTIONS.register(
                "nuke_radioactive_payload",
                () -> NukeTests::radioactivePayloadBoostsPowerAndRadiation);
        FUNCTIONS.register("nuke_redstone_primes", () -> NukeTests::redstonePrimesLoadedCharge);
        FUNCTIONS.register("nuke_unloaded_refuses", () -> NukeTests::unloadedNukeRefusesToPrime);
        FUNCTIONS.register("nuke_chain_reaction", () -> NukeTests::chainReactionShortFuse);
        FUNCTIONS.register("nuke_wrench_defuses", () -> NukeTests::wrenchDefusesPrimedCharge);
        FUNCTIONS.register("nuke_menu_slots", () -> NukeTests::menuAcceptsOnlyPayloadItems);
        FUNCTIONS.register(
                "foam_sprayer_places_ten", () -> FoamTests::sprayerPlacesUpToTenFoamBlocks);
        FUNCTIONS.register("foam_sprayer_single_mode", () -> FoamTests::singleModeSpraysOneBlock);
        FUNCTIONS.register("foam_scaffolding_covered", () -> FoamTests::scaffoldingIsCoveredInFoam);
        FUNCTIONS.register("foam_sand_hardens", () -> FoamTests::sandInstantlyHardensFoam);
        FUNCTIONS.register("foam_pack_supplies", () -> FoamTests::foamPackSuppliesSprayer);
        FUNCTIONS.register("foam_fluid_slows", () -> FoamTests::foamFluidSlowsEntities);
        FUNCTIONS.register(
                "foam_sprayer_fluid_filter", () -> FoamTests::sprayerAcceptsOnlyConstructionFoam);
        FUNCTIONS.register("painter_recolors_wool", () -> PainterTests::painterRecolorsWool);
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
        FUNCTIONS.register("painter_wear_reverts", () -> PainterTests::painterWearRevertsToPlain);
        FUNCTIONS.register(
                "painter_auto_refill", () -> PainterTests::painterAutoRefillConsumesSpare);
        FUNCTIONS.register("painter_dyes_sheep", () -> PainterTests::painterDyesSheep);
        FUNCTIONS.register("painter_plain_passes", () -> PainterTests::plainPainterPasses);
        FUNCTIONS.register(
                "painter_toggles_auto_refill", () -> PainterTests::painterTogglesAutoRefill);
        FUNCTIONS.register(
                "cable_shock_uninsulated",
                () -> CableShockTests::uninsulatedCableShocksNearbyEntities);
        FUNCTIONS.register(
                "cable_shock_insulated", () -> CableShockTests::insulatedCableShieldsEntities);
        FUNCTIONS.register("cable_shock_glass", () -> CableShockTests::glassFibreCableNeverShocks);
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
        FUNCTIONS.register("obscurator_scan_payload", () -> ObscuratorTests::obscuratorScanPayload);
        FUNCTIONS.register(
                "obscured_wall_persistence", () -> ObscuratorTests::obscuredWallPersistence);
        FUNCTIONS.register(
                "obscured_wall_drops_color_wall",
                () -> ObscuratorTests::obscuredWallDropsColorWall);
        FUNCTIONS.register("dynamite_linked_toggle", () -> DynamiteTests::linkedStateToggles);
        FUNCTIONS.register("dynamite_redstone_fuse", () -> DynamiteTests::redstonePrimesFuse);
        FUNCTIONS.register("dynamite_break_fuse", () -> DynamiteTests::playerBreakPrimesFuse);
        FUNCTIONS.register("dynamite_explosion_chain", () -> DynamiteTests::explosionChainsFuse);
        FUNCTIONS.register(
                "thrown_dynamite_detonates", () -> ThrownDynamiteTests::thrownDynamiteDetonates);
        FUNCTIONS.register(
                "sticky_dynamite_accelerates",
                () -> ThrownDynamiteTests::stickyDynamiteFuseAccelerates);
        FUNCTIONS.register("dynamite_water_disarms", () -> ThrownDynamiteTests::waterDisarmsFuse);
        FUNCTIONS.register("reactor_vessel_place", () -> ReactorVesselTests::vesselPlaces);
        FUNCTIONS.register(
                "crystal_memory_roundtrip", () -> CrystalMemoryTests::recordsAndReadsPattern);
        FUNCTIONS.register("remote_detonate", () -> RemoteTests::remoteDetonatesLinkedDynamite);
        FUNCTIONS.register("uu_scanner_scan", () -> UuScannerTests::scansSeededItemOntoMemory);
        FUNCTIONS.register("crop_seed_bag_roundtrip", () -> CropTests::cropSeedBagRoundtrip);
        FUNCTIONS.register("crop_plant_harvest", () -> CropTests::cropPlantGrowHarvest);
        FUNCTIONS.register("crop_weed_growth", () -> CropTests::cropWeedGrowth);
        FUNCTIONS.register("crop_base_seed_planting", () -> CropTests::cropBaseSeedPlanting);
        FUNCTIONS.register("crop_reed_age_gains", () -> CropTests::cropReedAgeScaledGains);
        FUNCTIONS.register("crop_coffee_harvest_window", () -> CropTests::cropCoffeeHarvestWindow);
        FUNCTIONS.register("crop_cocoa_nutrient_gate", () -> CropTests::cropCocoaNutrientGate);
        FUNCTIONS.register(
                "crop_nether_wart_soul_sand", () -> CropTests::cropNetherWartSoulSand);
        FUNCTIONS.register(
                "crop_potato_harvest_band", () -> CropTests::cropPotatoHarvestBand);
        FUNCTIONS.register("crop_mushroom_base_seed", () -> CropTests::cropMushroomBaseSeed);
        FUNCTIONS.register("crop_sapling_gains", () -> CropTests::cropSaplingGains);
        FUNCTIONS.register(
                "crop_flower_dye_harvest", () -> CropTests::cropFlowerDyeHarvest);
        FUNCTIONS.register("crop_pumpkin_stem", () -> CropTests::cropPumpkinStem);
        FUNCTIONS.register("crop_melon_stem", () -> CropTests::cropMelonStem);
        FUNCTIONS.register(
                "crop_venomilia_poison", () -> CropTests::cropVenomiliaPoison);
        FUNCTIONS.register(
                "crop_sticky_reed_resin", () -> CropTests::cropStickyReedResin);
        FUNCTIONS.register("crop_terra_wart_snow", () -> CropTests::cropTerraWartSnow);
        FUNCTIONS.register(
                "crop_wart_snow_transmutation", () -> CropTests::cropWartSnowTransmutation);
        FUNCTIONS.register("crop_terra_wart_cure", () -> CropTests::cropTerraWartCure);
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
        FUNCTIONS.register("crop_crossing_breed", () -> CropTests::cropCrossingBreed);
        FUNCTIONS.register("crop_crossing_spread", () -> CropTests::cropCrossingSpread);
        FUNCTIONS.register("crop_analyzer_scan_ladder", () -> CropAnalyzerTests::scanLadder);
        FUNCTIONS.register("crop_analyzer_report", () -> CropAnalyzerTests::cropReport);
        FUNCTIONS.register("crop_analyzer_menu", () -> CropAnalyzerTests::menuIntegration);
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
        FUNCTIONS.register("uu_values_datapack", () -> UuScannerTests::datapackSeedsDriveGraph);
        FUNCTIONS.register("uu_scanner_unknown", () -> UuScannerTests::unknownItemFails);
        FUNCTIONS.register("uu_scanner_seed_coverage", () -> UuScannerTests::expandedSeedCoverage);
        FUNCTIONS.register(
                "reactor_redstone_port", () -> ReactorAccessHatchTests::redstonePortPowersCore);
        FUNCTIONS.register("reactor_hatch_grid", () -> ReactorAccessHatchTests::hatchExposesGrid);
        FUNCTIONS.register(
                "reactor_rci_recharge", () -> ReactorAccessHatchTests::rciRechargesCondensator);
        FUNCTIONS.register(
                "matter_generator_scrap", () -> MatterGeneratorTests::scrapAmplifiesAndGenerates);
        FUNCTIONS.register(
                "matter_generator_cells", () -> MatterGeneratorTests::fillsUuMatterCells);
        FUNCTIONS.register(
                "matter_generator_redstone",
                () -> MatterGeneratorTests::redstoneGateStopsGeneration);
        FUNCTIONS.register("cooling_capacity", () -> HeatExchangerTests::nearlyFullOutput);
        FUNCTIONS.register("cooling_parts_budget", () -> HeatExchangerTests::partsAndBudget);
        FUNCTIONS.register("cooling_stirling_chain", () -> HeatExchangerTests::stirlingChain);
        FUNCTIONS.register("cooling_containers", () -> HeatExchangerTests::containersAndPorts);
        FUNCTIONS.register("cooling_datapack", () -> HeatExchangerTests::dataPackRecipe);
        FUNCTIONS.register("fermenter_biofuel_chain", () -> FermenterTests::biofuelChain);
        FUNCTIONS.register("fermenter_heat_reload", () -> FermenterTests::heatChainAndReload);
        FUNCTIONS.register("fermenter_blocked_gas", () -> FermenterTests::blockedGas);
        FUNCTIONS.register("fermenter_blocked_fertilizer", () -> FermenterTests::blockedFertilizer);
        FUNCTIONS.register("fermenter_containers", () -> FermenterTests::containersAndPorts);
        FUNCTIONS.register("fermenter_datapack", () -> FermenterTests::dataPackRecipe);
        FUNCTIONS.register("water_turbine_operation", () -> WaterTurbineTests::operationAndWear);
        FUNCTIONS.register("water_turbine_obstructions", () -> WaterTurbineTests::obstructions);
        FUNCTIONS.register("water_turbine_tides", () -> WaterTurbineTests::tidesAndDeepOcean);
        FUNCTIONS.register("water_turbine_network", () -> WaterTurbineTests::networkSupply);
        FUNCTIONS.register("turbine_operation", () -> WindTurbineTests::operationAndWear);
        FUNCTIONS.register("turbine_obstructions", () -> WindTurbineTests::obstructions);
        FUNCTIONS.register("turbine_network_supply", () -> WindTurbineTests::networkSupply);
        FUNCTIONS.register("loaded_recipes", () -> ProcessingTests::loadedRecipes);
        FUNCTIONS.register("processing_machines", () -> ProcessingTests::processing);
        FUNCTIONS.register("weighted_persistence", () -> ProcessingTests::weightedPersistence);
        FUNCTIONS.register("processing_recipe_codec", () -> ProcessingTests::recipeCodec);
        FUNCTIONS.register("jei_categories_non_empty", () -> ProcessingTests::jeiCategoriesNonEmpty);

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
        FUNCTIONS.register("ae2_bridge_feeds", () -> Ae2BridgeTests::bridgeFeedsAcceptorOverCable);
        FUNCTIONS.register("ae2_bridge_ratio", () -> Ae2BridgeTests::bridgeChargesTwoAePerEuExactly);
        FUNCTIONS.register("ae2_bridge_no_path", () -> Ae2BridgeTests::bridgeWithoutPathDrawsNothing);
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
