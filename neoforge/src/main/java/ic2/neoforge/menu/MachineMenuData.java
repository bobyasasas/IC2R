package ic2.neoforge.menu;

import ic2.neoforge.machine.MachineBlockEntity;

import net.minecraft.world.inventory.ContainerData;

import java.util.Objects;

/** Every value uses two native 16-bit words, including machine-specific counters and fluid IDs. */
final class MachineMenuData implements ContainerData {
    static final int FAMILY_VALUES = 7;
    static final int CAPACITY_FIELD = 5 + FAMILY_VALUES;
    static final int SIZE = (CAPACITY_FIELD + 1) * 2;
    private final MachineBlockEntity machine;

    MachineMenuData(MachineBlockEntity machine) {
        this.machine = machine;
    }

    @Override
    public int get(int index) {
        Objects.checkIndex(index, SIZE);
        int field = index / 2;
        int value =
                field >= 5 && field < CAPACITY_FIELD
                        ? machine.menuValue(field - 5)
                        : switch (field) {
                            case 0 -> (int) machine.storedEnergy();
                            case 1 -> machine.progress();
                            case 2 -> machine.progressMaximum();
                            case 3 -> machine.fuelRemaining();
                            case 4 -> machine.fuelMaximum();
                            default -> (int) machine.energyCapacity();
                        };
        return index % 2 == 0 ? value & 0xffff : value >>> 16;
    }

    @Override
    public void set(int index, int value) {
        /* Server owns machine state. */
    }

    @Override
    public int getCount() {
        return SIZE;
    }
}
