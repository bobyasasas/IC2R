package ic2.neoforge.machine;

import net.minecraft.util.StringRepresentable;

public enum MachineKind implements StringRepresentable {
    GENERATOR("generator"),
    ELECTRIC_FURNACE("electric_furnace");
    public static final com.mojang.serialization.Codec<MachineKind> CODEC =
            StringRepresentable.fromEnum(MachineKind::values);
    private final String id;

    MachineKind(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
