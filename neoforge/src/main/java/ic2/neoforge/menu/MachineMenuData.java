package ic2.neoforge.menu;

import ic2.neoforge.machine.MachineBlockEntity;

import net.minecraft.world.inventory.ContainerData;

import java.util.Objects;

/** Vanilla properties transport 16-bit words. Energy and tick counters use paired words. */
final class MachineMenuData implements ContainerData {
    static final int SIZE = 15;
    private final MachineBlockEntity machine;

    MachineMenuData(MachineBlockEntity machine) {
        this.machine = machine;
    }

    @Override
    public int get(int index) {
        Objects.checkIndex(index, SIZE);
        if (index >= 10 && index < SIZE) return machine.menuValue(index - 10);
        int value =
                switch (index / 2) {
                    case 0 -> (int) machine.storedEnergy();
                    case 1 -> machine.progress();
                    case 2 -> machine.progressMaximum();
                    case 3 -> machine.fuelRemaining();
                    case 4 -> machine.fuelMaximum();
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
