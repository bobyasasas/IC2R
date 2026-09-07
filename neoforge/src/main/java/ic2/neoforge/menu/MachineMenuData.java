package ic2.neoforge.menu;

import ic2.neoforge.machine.MachineBlockEntity;

import net.minecraft.world.inventory.ContainerData;

import java.util.Objects;

/** Every value uses two native 16-bit words, including machine-specific counters and fluid IDs. */
final class MachineMenuData implements ContainerData {
    static final int SIZE = 22;
    private final MachineBlockEntity machine;

    MachineMenuData(MachineBlockEntity machine) {
        this.machine = machine;
    }

    @Override
    public int get(int index) {
        Objects.checkIndex(index, SIZE);
        int field = index / 2;
        int value =
                switch (field) {
                    case 0 -> (int) machine.storedEnergy();
                    case 1 -> machine.progress();
                    case 2 -> machine.progressMaximum();
                    case 3 -> machine.fuelRemaining();
                    case 4 -> machine.fuelMaximum();
                    case 5, 6, 7, 8, 9 -> machine.menuValue(field - 5);
                    case 10 -> (int) machine.energyCapacity();
                    default -> throw new IndexOutOfBoundsException(index);
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
