package ic2.neoforge.machine;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/** Shared machine specification; UI capacity, inventory size and processing cost derive from it. */
public enum MachineKind implements StringRepresentable {
    BATBOX("batbox", 40000, 2, 0, 0),
    CESU("cesu", 300000, 2, 0, 0),
    MFE("mfe", 4000000, 2, 0, 0),
    MFSU("mfsu", 40000000, 2, 0, 0),
    LV_TRANSFORMER("lv_transformer", 256, 0, 0, 0),
    MV_TRANSFORMER("mv_transformer", 1024, 0, 0, 0),
    HV_TRANSFORMER("hv_transformer", 4096, 0, 0, 0),
    EV_TRANSFORMER("ev_transformer", 16384, 0, 0, 0),
    IRON_FURNACE("iron_furnace", 0, 3, 160, 0),
    STEAM_KINETIC_GENERATOR("steam_kinetic_generator", 0, 1, 0, 0),
    STEAM_GENERATOR("steam_generator", 0, 0, 0, 0),
    STEAM_REPRESSURIZER("steam_repressurizer", 0, 2, 0, 0),
    CONDENSER("condenser", 100000, 7, 0, 0),
    FLUID_REGULATOR("fluid_regulator", 10000, 3, 0, 0),
    ELECTROLYZER("electrolyzer", 32000, 1, 200, 32),
    TANK("tank", 0, 0, 0, 0),
    BRONZE_TANK("bronze_tank", 0, 0, 0, 0),
    IRON_TANK("iron_tank", 0, 0, 0, 0),
    STEEL_TANK("steel_tank", 0, 0, 0, 0),
    IRIDIUM_TANK("iridium_tank", 0, 0, 0, 0),
    LIQUID_HEAT_EXCHANGER("liquid_heat_exchanger", 0, 14, 0, 0),
    FERMENTER("fermenter", 0, 5, 0, 0),
    SOLAR_DISTILLER("solar_distiller", 0, 4, 0, 0),
    CANNER("canner", 800, 4, 200, 4),
    WATER_KINETIC_GENERATOR("water_kinetic_generator", 0, 1, 0, 0),
    WIND_KINETIC_GENERATOR("wind_kinetic_generator", 0, 1, 0, 0),
    MANUAL_KINETIC_GENERATOR("manual_kinetic_generator", 0, 0, 0, 0),
    BATBOX_CHARGEPAD("batbox_chargepad", 40000, 2, 0, 0),
    CESU_CHARGEPAD("cesu_chargepad", 300000, 2, 0, 0),
    MFE_CHARGEPAD("mfe_chargepad", 4000000, 2, 0, 0),
    MFSU_CHARGEPAD("mfsu_chargepad", 40000000, 2, 0, 0),
    WOODEN_STORAGE_BOX("wooden_storage_box", 0, 27, 0, 0),
    BRONZE_STORAGE_BOX("bronze_storage_box", 0, 45, 0, 0),
    IRON_STORAGE_BOX("iron_storage_box", 0, 45, 0, 0),
    STEEL_STORAGE_BOX("steel_storage_box", 0, 63, 0, 0),
    IRIDIUM_STORAGE_BOX("iridium_storage_box", 0, 126, 0, 0),
    MAGNETIZER("magnetizer", 100, 1, 0, 0),
    PUMP("pump", 20, 2, 20, 1),
    MINER("miner", 1000, 18, 0, 0),
    ADV_MINER("advanced_miner", 4000000, 17, 0, 0),
    SORTING_MACHINE("sorting_machine", 15000, 11, 0, 0),
    TRADE_O_MAT("trade_o_mat", 0, 4, 0, 0),
    ENERGY_O_MAT("energy_o_mat", 10000, 3, 0, 0),
    ITEM_BUFFER("item_buffer", 0, 48, 0, 0),
    BLAST_FURNACE("blast_furnace", 0, 5, 0, 0),
    COKE_KILN("coke_kiln", 0, 1, 0, 0),
    COKE_KILN_HATCH("coke_kiln_hatch", 0, 1, 0, 0),
    COKE_KILN_GRATE("coke_kiln_grate", 0, 0, 0, 0),
    MATTER_GENERATOR("matter_generator", 1000000, 3, 0, 0),
    NUCLEAR_REACTOR("nuclear_reactor", 100000, 54, 0, 0),
    REACTOR_CHAMBER("reactor_chamber", 0, 0, 0, 0),
    REACTOR_FLUID_PORT("reactor_fluid_port", 0, 0, 0, 0),
    REACTOR_ACCESS_HATCH("reactor_access_hatch", 0, 0, 0, 0),
    REACTOR_REDSTONE_PORT("reactor_redstone_port", 0, 0, 0, 0),
    RCI_RSH("rci_rsh", 48000, 9, 0, 0),
    RCI_LZH("rci_lzh", 48000, 9, 0, 0),
    REPLICATOR("replicator", 2000000, 3, 0, 0),
    UU_SCANNER("uu_scanner", 512000, 2, 0, 0),
    PATTERN_STORAGE("pattern_storage", 0, 1, 0, 0),
    TELEPORTER("teleporter", 0, 0, 0, 0),
    PERSONAL_CHEST("personal_chest", 0, 54, 0, 0),
    RT_HEAT_GENERATOR("rt_heat_generator", 0, 6, 0, 0),
    RT_GENERATOR("rt_generator", 20000, 7, 0, 0),
    SOLID_HEAT_GENERATOR("solid_heat_generator", 0, 2, 0, 0),
    FLUID_HEAT_GENERATOR("fluid_heat_generator", 0, 2, 0, 0),
    ELECTRIC_HEAT_GENERATOR("electric_heat_generator", 10000, 11, 0, 0),
    ELECTRIC_KINETIC_GENERATOR("electric_kinetic_generator", 10000, 11, 0, 0),
    STIRLING_GENERATOR("stirling_generator", 16384, 0, 0, 0),
    KINETIC_GENERATOR("kinetic_generator", 16384, 0, 0, 0),
    STIRLING_KINETIC_GENERATOR("stirling_kinetic_generator", 0, 4, 0, 0),
    WIND_GENERATOR("wind_generator", 32, 1, 0, 0),
    WATER_GENERATOR("water_generator", 4, 2, 0, 0),
    SOLAR_GENERATOR("solar_generator", 32, 1, 0, 0),
    GEO_GENERATOR("geo_generator", 2400, 3, 0, 0),
    SEMIFLUID_GENERATOR("semifluid_generator", 32000, 3, 0, 0),
    GENERATOR("generator", 4000, 2, 0, 0),
    ELECTRIC_FURNACE("electric_furnace", 300, 3, 100, 3),
    INDUCTION_FURNACE("induction_furnace", 10000, 5, 4000, 0),
    RECYCLER("recycler", 45, 3, 45, 1),
    CHUNK_LOADER("chunk_loader", 2500, 1, 0, 0),
    CREATIVE_GENERATOR("creative_generator", 32000, 0, 0, 0),
    CENTRIFUGE("centrifuge", 24000, 5, 500, 48),
    ORE_WASHING_PLANT("ore_washing_plant", 8000, 7, 500, 16),
    METAL_FORMER("metal_former", 2000, 3, 200, 10),
    MACERATOR("macerator", 600, 3, 300, 2),
    EXTRACTOR("extractor", 600, 3, 300, 2),
    COMPRESSOR("compressor", 600, 3, 300, 2),
    BLOCK_CUTTER("block_cutter", 1800, 4, 450, 4),
    CROPMATRON("cropmatron", 10000, 11, 0, 0),
    CROP_HARVESTER("crop_harvester", 10000, 15, 0, 0),
    TESLA_COIL("tesla_coil", 10000, 0, 0, 0),
    TERRAFORMER("terraformer", 100000, 0, 0, 0),
    LUMINATOR("luminator", 5, 0, 0, 0),
    INDUSTRIAL_WORKBENCH("industrial_workbench", 0, 31, 0, 0),
    BATCH_CRAFTER("batch_crafter", 20000, 20, 40, 2),
    FLUID_DISTRIBUTOR("fluid_distributor", 0, 2, 0, 0),
    WEIGHTED_FLUID_DISTRIBUTOR("weighted_fluid_distributor", 0, 2, 0, 0),
    WEIGHTED_ITEM_DISTRIBUTOR("weighted_item_distributor", 0, 9, 0, 0);

    public static final Codec<MachineKind> CODEC =
            StringRepresentable.fromEnum(MachineKind::values);
    private final String id;
    private final int capacity, slots, ticks, euPerTick;

    MachineKind(String id, int capacity, int slots, int ticks, int euPerTick) {
        this.id = id;
        this.capacity = capacity;
        this.slots = slots;
        this.ticks = ticks;
        this.euPerTick = euPerTick;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public boolean verticalFacing() {
        return energyDevice()
                || electricWork()
                || workConversion()
                || fuelHeat()
                || this == MANUAL_KINETIC_GENERATOR
                || this == STIRLING_KINETIC_GENERATOR
                || this == FERMENTER
                || this == LIQUID_HEAT_EXCHANGER
                || this == FLUID_REGULATOR
                || this == LUMINATOR
                || this == COKE_KILN_HATCH
                || this == COKE_KILN_GRATE;
    }

    public boolean turbine() {
        return this == WIND_KINETIC_GENERATOR || this == WATER_KINETIC_GENERATOR;
    }

    public boolean fuelHeat() {
        return this == SOLID_HEAT_GENERATOR || this == FLUID_HEAT_GENERATOR;
    }

    public boolean electricWork() {
        return this == ELECTRIC_HEAT_GENERATOR || this == ELECTRIC_KINETIC_GENERATOR;
    }

    public boolean workConversion() {
        return this == STIRLING_GENERATOR || this == KINETIC_GENERATOR;
    }

    public boolean fluidGenerator() {
        return this == GEO_GENERATOR || this == SEMIFLUID_GENERATOR;
    }

    public boolean generating() {
        return this == GENERATOR
                || this == SOLAR_GENERATOR
                || this == WATER_GENERATOR
                || this == WIND_GENERATOR
                || workConversion()
                || fluidGenerator();
    }

    public boolean storage() {
        return switch (this) {
            case BATBOX, CESU, MFE, MFSU -> true;
            default -> false;
        };
    }

    public boolean storageBox() {
        return switch (this) {
            case WOODEN_STORAGE_BOX,
                    BRONZE_STORAGE_BOX,
                    IRON_STORAGE_BOX,
                    STEEL_STORAGE_BOX,
                    IRIDIUM_STORAGE_BOX ->
                    true;
            default -> false;
        };
    }

    /** The base tank plus its legacy bronze/iron/steel/iridium tiered variants. */
    public boolean isTank() {
        return switch (this) {
            case TANK,
                    BRONZE_TANK,
                    IRON_TANK,
                    STEEL_TANK,
                    IRIDIUM_TANK ->
                    true;
            default -> false;
        };
    }

    /**
     * Legacy TileEntityTank family: capacity is 1000 mB times the bucket multiplier
     * carried by each variant (base 24, bronze/iron 32, steel 128, iridium 1024).
     */
    public int tankCapacity() {
        return switch (this) {
            case BRONZE_TANK, IRON_TANK -> 32000;
            case STEEL_TANK -> 128000;
            case IRIDIUM_TANK -> 1024000;
            default -> 24000;
        };
    }

    public boolean chargepad() {
        return switch (this) {
            case BATBOX_CHARGEPAD, CESU_CHARGEPAD, MFE_CHARGEPAD, MFSU_CHARGEPAD -> true;
            default -> false;
        };
    }

    /** Chargepads are storage-shaped pads that push energy into whatever a player carries. */
    public int padOutput() {
        return switch (this) {
            case BATBOX_CHARGEPAD -> 32;
            case CESU_CHARGEPAD -> 128;
            case MFE_CHARGEPAD -> 512;
            case MFSU_CHARGEPAD -> 2048;
            default -> 0;
        };
    }

    public boolean transformer() {
        return switch (this) {
            case LV_TRANSFORMER, MV_TRANSFORMER, HV_TRANSFORMER, EV_TRANSFORMER -> true;
            default -> false;
        };
    }

    public boolean energyDevice() {
        return storage() || transformer();
    }

    public int electricalTier() {
        return switch (this) {
            case BATBOX, LV_TRANSFORMER -> 1;
            case BATBOX_CHARGEPAD, MAGNETIZER, PUMP, MINER -> 1;
            case CESU,
                    CESU_CHARGEPAD,
                    MV_TRANSFORMER,
                    CENTRIFUGE,
                    INDUCTION_FURNACE,
                    ELECTROLYZER,
                    SORTING_MACHINE,
                    TESLA_COIL ->
                    2;
            case MFE, HV_TRANSFORMER, CONDENSER, MATTER_GENERATOR -> 3;
            case MFE_CHARGEPAD -> 3;
            case MFSU,
                    MFSU_CHARGEPAD,
                    EV_TRANSFORMER,
                    ELECTRIC_HEAT_GENERATOR,
                    ELECTRIC_KINETIC_GENERATOR,
                    FLUID_REGULATOR,
                    TERRAFORMER ->
                    4;
            default -> 1;
        };
    }

    public int auxiliaryPower() {
        return this == CENTRIFUGE ? 1 : 0;
    }

    public int capacity() {
        return capacity;
    }

    public boolean upgradable() {
        return upgradeSlots() > 0;
    }

    public int menuWidth() {
        return switch (this) {
            case IRIDIUM_STORAGE_BOX -> 338;
            case SORTING_MACHINE, NUCLEAR_REACTOR -> 212;
            case INDUSTRIAL_WORKBENCH -> 194;
            default -> 176;
        };
    }

    public int menuHeight() {
        return switch (this) {
            case BATBOX, CESU, MFE, MFSU -> 196;
            case LV_TRANSFORMER, MV_TRANSFORMER, HV_TRANSFORMER, EV_TRANSFORMER -> 219;
            case BATBOX_CHARGEPAD, CESU_CHARGEPAD, MFE_CHARGEPAD, MFSU_CHARGEPAD -> 161;
            case BRONZE_STORAGE_BOX, IRON_STORAGE_BOX -> 202;
            case STEEL_STORAGE_BOX, IRIDIUM_STORAGE_BOX -> 238;
            case SORTING_MACHINE, NUCLEAR_REACTOR -> 243;
            case CHUNK_LOADER -> 250;
            case ITEM_BUFFER -> 232;
            case PERSONAL_CHEST -> 222;
            case STEAM_GENERATOR -> 220;
            case INDUSTRIAL_WORKBENCH -> 228;
            case WEIGHTED_FLUID_DISTRIBUTOR, WEIGHTED_ITEM_DISTRIBUTOR -> 211;
            case BATCH_CRAFTER -> 206;
            case LIQUID_HEAT_EXCHANGER, STIRLING_KINETIC_GENERATOR -> 204;
            case ADV_MINER -> 203;
            case CROPMATRON -> 192;
            case CANNER,
                    CONDENSER,
                    FERMENTER,
                    FLUID_DISTRIBUTOR,
                    FLUID_REGULATOR,
                    REPLICATOR,
                    SOLAR_DISTILLER ->
                    184;
            default -> 166;
        };
    }

    public int inventoryX() {
        return this == IRIDIUM_STORAGE_BOX ? 89 : this == NUCLEAR_REACTOR ? 26 : 8;
    }

    public int inventoryY() {
        return menuHeight() - 82;
    }

    public int installedPartsStart() {
        return electricWork() ? 0 : this == LIQUID_HEAT_EXCHANGER ? 4 : this == CONDENSER ? 3 : -1;
    }

    public int installedPartsCount() {
        return this == CONDENSER ? 4 : installedPartsStart() >= 0 ? 10 : 0;
    }

    public int upgradeSlots() {
        if (this == CROPMATRON || this == CHUNK_LOADER || this == MAGNETIZER) return 4;
        if (this == CROP_HARVESTER) return 4;
        if (this == ADV_MINER) return 4;
        if (this == ITEM_BUFFER || this == BLAST_FURNACE) return 2;
        if (this == SORTING_MACHINE) return 3;
        if (this == MATTER_GENERATOR) return 4;
        if (this == RCI_RSH || this == RCI_LZH) return 4;
        if (this == CONDENSER || this == STEAM_KINETIC_GENERATOR || this == MINER) return 1;
        if (this == ENERGY_O_MAT) return 1;
        if (isTank()) return 4;
        if (this == LIQUID_HEAT_EXCHANGER || this == STIRLING_KINETIC_GENERATOR) return 3;
        if (this == SOLAR_DISTILLER) return 2;
        if (this == REPLICATOR) return 4;
        return (this == INDUCTION_FURNACE || this == FERMENTER) ? 2 : euPerTick > 0 ? 4 : 0;
    }

    public int upgradeStart() {
        return slots;
    }

    public int slots() {
        return slots + upgradeSlots();
    }

    public int ticks() {
        return ticks;
    }

    public int euPerTick() {
        return euPerTick;
    }
}
