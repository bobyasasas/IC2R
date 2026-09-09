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
    LIQUID_HEAT_EXCHANGER("liquid_heat_exchanger", 0, 14, 0, 0),
    FERMENTER("fermenter", 0, 5, 0, 0),
    CANNER("canner", 800, 4, 200, 4),
    WATER_KINETIC_GENERATOR("water_kinetic_generator", 0, 1, 0, 0),
    WIND_KINETIC_GENERATOR("wind_kinetic_generator", 0, 1, 0, 0),
    MANUAL_KINETIC_GENERATOR("manual_kinetic_generator", 0, 0, 0, 0),
    BATBOX_CHARGEPAD("batbox_chargepad", 40000, 0, 0, 0),
    CESU_CHARGEPAD("cesu_chargepad", 300000, 0, 0, 0),
    MFE_CHARGEPAD("mfe_chargepad", 4000000, 0, 0, 0),
    MFSU_CHARGEPAD("mfsu_chargepad", 40000000, 0, 0, 0),
    WOODEN_STORAGE_BOX("wooden_storage_box", 0, 27, 0, 0),
    BRONZE_STORAGE_BOX("bronze_storage_box", 0, 45, 0, 0),
    IRON_STORAGE_BOX("iron_storage_box", 0, 45, 0, 0),
    STEEL_STORAGE_BOX("steel_storage_box", 0, 63, 0, 0),
    IRIDIUM_STORAGE_BOX("iridium_storage_box", 0, 126, 0, 0),
    PERSONAL_CHEST("personal_chest", 0, 54, 0, 0),
    RT_HEAT_GENERATOR("rt_heat_generator", 0, 6, 0, 0),
    RT_GENERATOR("rt_generator", 20000, 7, 0, 0),
    SOLID_HEAT_GENERATOR("solid_heat_generator", 0, 2, 0, 0),
    FLUID_HEAT_GENERATOR("fluid_heat_generator", 0, 2, 0, 0),
    ELECTRIC_HEAT_GENERATOR("electric_heat_generator", 10000, 11, 0, 0),
    ELECTRIC_KINETIC_GENERATOR("electric_kinetic_generator", 10000, 11, 0, 0),
    STIRLING_GENERATOR("stirling_generator", 16384, 0, 0, 0),
    KINETIC_GENERATOR("kinetic_generator", 16384, 0, 0, 0),
    WIND_GENERATOR("wind_generator", 32, 1, 0, 0),
    WATER_GENERATOR("water_generator", 4, 2, 0, 0),
    SOLAR_GENERATOR("solar_generator", 32, 1, 0, 0),
    GEO_GENERATOR("geo_generator", 2400, 3, 0, 0),
    SEMIFLUID_GENERATOR("semifluid_generator", 32000, 3, 0, 0),
    GENERATOR("generator", 4000, 2, 0, 0),
    ELECTRIC_FURNACE("electric_furnace", 300, 3, 100, 3),
    INDUCTION_FURNACE("induction_furnace", 10000, 5, 4000, 0),
    RECYCLER("recycler", 45, 3, 45, 1),
    CENTRIFUGE("centrifuge", 24000, 5, 500, 48),
    ORE_WASHING_PLANT("ore_washing_plant", 8000, 7, 500, 16),
    METAL_FORMER("metal_former", 2000, 3, 200, 10),
    MACERATOR("macerator", 600, 3, 300, 2),
    EXTRACTOR("extractor", 600, 3, 300, 2),
    COMPRESSOR("compressor", 600, 3, 300, 2);

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
                || this == FERMENTER
                || this == LIQUID_HEAT_EXCHANGER
                || this == FLUID_REGULATOR;
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
            case BATBOX_CHARGEPAD -> 1;
            case CESU,
                    CESU_CHARGEPAD,
                    MV_TRANSFORMER,
                    CENTRIFUGE,
                    INDUCTION_FURNACE,
                    ELECTROLYZER ->
                    2;
            case MFE, HV_TRANSFORMER, CONDENSER -> 3;
            case MFE_CHARGEPAD -> 3;
            case MFSU,
                    MFSU_CHARGEPAD,
                    EV_TRANSFORMER,
                    ELECTRIC_HEAT_GENERATOR,
                    ELECTRIC_KINETIC_GENERATOR,
                    FLUID_REGULATOR ->
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
        return upgradable() || this == FLUID_REGULATOR || this == STEAM_GENERATOR ? 202 : 176;
    }

    public int menuHeight() {
        if (this == STEAM_GENERATOR) return 238;
        if (this == PERSONAL_CHEST) return 222;
        if (storageBox()) return 124 + slots() / 9 * 18;
        return this == LIQUID_HEAT_EXCHANGER
                        || this == FLUID_REGULATOR
                        || this == CONDENSER
                        || this == STEAM_REPRESSURIZER
                        || this == RT_HEAT_GENERATOR
                        || this == RT_GENERATOR
                        || this == STEAM_KINETIC_GENERATOR
                ? 184
                : 166;
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
        if (this == CONDENSER || this == STEAM_KINETIC_GENERATOR) return 1;
        if (this == TANK) return 4;
        if (this == LIQUID_HEAT_EXCHANGER) return 3;
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
