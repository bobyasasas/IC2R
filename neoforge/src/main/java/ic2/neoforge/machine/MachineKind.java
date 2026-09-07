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
    CANNER("canner", 800, 4, 200, 4),
    SOLAR_GENERATOR("solar_generator", 32, 1, 0, 0),
    GEO_GENERATOR("geo_generator", 2400, 3, 0, 0),
    SEMIFLUID_GENERATOR("semifluid_generator", 32000, 3, 0, 0),
    GENERATOR("generator", 4000, 2, 0, 0),
    ELECTRIC_FURNACE("electric_furnace", 300, 3, 100, 3),
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

    public boolean fluidGenerator() {
        return this == GEO_GENERATOR || this == SEMIFLUID_GENERATOR;
    }

    public boolean generating() {
        return this == GENERATOR || this == SOLAR_GENERATOR || fluidGenerator();
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
            case CESU, MV_TRANSFORMER -> 2;
            case MFE, HV_TRANSFORMER -> 3;
            case MFSU, EV_TRANSFORMER -> 4;
            default -> 1;
        };
    }

    public int capacity() {
        return capacity;
    }

    public boolean upgradable() {
        return euPerTick > 0;
    }

    public int upgradeStart() {
        return slots;
    }

    public int slots() {
        return slots + (upgradable() ? 4 : 0);
    }

    public int ticks() {
        return ticks;
    }

    public int euPerTick() {
        return euPerTick;
    }
}
