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
    TANK("tank", 0, 0, 0, 0),
    LIQUID_HEAT_EXCHANGER("liquid_heat_exchanger", 0, 14, 0, 0),
    FERMENTER("fermenter", 0, 5, 0, 0),
    CANNER("canner", 800, 4, 200, 4),
    WATER_KINETIC_GENERATOR("water_kinetic_generator", 0, 1, 0, 0),
    WIND_KINETIC_GENERATOR("wind_kinetic_generator", 0, 1, 0, 0),
    MANUAL_KINETIC_GENERATOR("manual_kinetic_generator", 0, 0, 0, 0),
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
                || this == LIQUID_HEAT_EXCHANGER;
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
            case CESU, MV_TRANSFORMER, CENTRIFUGE, INDUCTION_FURNACE -> 2;
            case MFE, HV_TRANSFORMER -> 3;
            case MFSU, EV_TRANSFORMER, ELECTRIC_HEAT_GENERATOR, ELECTRIC_KINETIC_GENERATOR -> 4;
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

    public int menuHeight() {
        return this == LIQUID_HEAT_EXCHANGER ? 184 : 166;
    }

    public int inventoryY() {
        return menuHeight() - 82;
    }

    public int installedPartsStart() {
        return electricWork() ? 0 : this == LIQUID_HEAT_EXCHANGER ? 4 : -1;
    }

    public int upgradeSlots() {
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
