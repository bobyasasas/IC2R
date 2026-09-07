package ic2.neoforge.machine;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/** Shared machine specification; UI capacity, inventory size and processing cost derive from it. */
public enum MachineKind implements StringRepresentable {
    IRON_FURNACE("iron_furnace", 0, 3, 160, 0),
    GENERATOR("generator", 4000, 2, 0, 0),
    ELECTRIC_FURNACE("electric_furnace", 300, 3, 100, 3),
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

    public int capacity() {
        return capacity;
    }

    public int slots() {
        return slots;
    }

    public int ticks() {
        return ticks;
    }

    public int euPerTick() {
        return euPerTick;
    }
}
