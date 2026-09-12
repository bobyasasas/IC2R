package ic2.neoforge.menu;

import ic2.neoforge.energy.WorldEnergyNetworks;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * Legacy ContainerMeter (EU-Reader): samples the per-node flow statistics of the grid position it
 * was opened on every broadcast, keeping a sliding average/min/max of the reading scaled by 1000
 * across the five legacy modes. Opening on a target only fixes it once; a target that leaves the
 * grid closes the GUI like the legacy null-NodeStats check.
 */
public final class MeterMenu extends AbstractContainerMenu {
    public enum Mode {
        ENERGY_IN,
        ENERGY_OUT,
        ENERGY_GAIN,
        VOLTAGE,
        AMPERAGE
    }

    static final int SCALE = 1000;
    /** One value per pair of native 16-bit words, mirroring MachineMenuData. */
    static final int SIZE = 2 * (1 + 4);
    public static final int RESET_BUTTON = Mode.values().length;

    private final BlockPos target;
    private final int meterSlotIndex;
    private final ItemStack meter;
    private final Inventory playerInventory;
    private final boolean client;
    private final ContainerData data = new SimpleContainerData(SIZE);
    private Mode mode = Mode.ENERGY_IN;
    private int resultAvg, resultMin, resultMax, resultCount;

    public MeterMenu(int id, Inventory inventory, BlockPos target, int meterSlotIndex, boolean client) {
        super(ModTools.METER_MENU.get(), id);
        this.target = target.immutable();
        this.meterSlotIndex = meterSlotIndex;
        this.meter = inventory.getItem(meterSlotIndex);
        this.playerInventory = inventory;
        this.client = client;
        addDataSlots(data);
    }

    public MeterMenu(int id, Inventory inventory, BlockPos target, boolean client) {
        this(id, inventory, target, 0, client);
    }

    public MeterMenu(int id, Inventory inventory, BlockPos target) {
        this(id, inventory, target, 0, false);
    }

    public Mode mode() {
        Mode[] modes = Mode.values();
        return modes[Math.floorMod(data.get(0), modes.length)];
    }

    public int resultCount() {
        return data.get(2 * 4);
    }

    public double resultAvg() {
        return unpack(1) / (double) SCALE;
    }

    public double resultMin() {
        return unpack(2) / (double) SCALE;
    }

    public double resultMax() {
        return unpack(3) / (double) SCALE;
    }

    public int meterSlotIndex() {
        return meterSlotIndex;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (client) return;
        var player = playerInventory.player;
        var stats =
                WorldEnergyNetworks.nodeStats(
                        (ServerLevel) player.level(), target);
        if (stats == null) {
            if (player.containerMenu == this) player.closeContainer();
            return;
        }
        double reading =
                switch (mode) {
                    case ENERGY_IN -> stats.energyIn();
                    case ENERGY_OUT -> stats.energyOut();
                    case ENERGY_GAIN -> stats.energyIn() - stats.energyOut();
                    case VOLTAGE -> stats.voltage();
                    case AMPERAGE -> stats.amperage();
                };
        int scaled = (int) Math.round(reading * SCALE);
        if (resultCount == 0) {
            resultAvg = resultMin = resultMax = scaled;
        } else {
            resultMin = Math.min(resultMin, scaled);
            resultMax = Math.max(resultMax, scaled);
            resultAvg = (int) (((long) resultAvg * resultCount + scaled) / (resultCount + 1));
        }
        resultCount++;
        pushFields();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < Mode.values().length) {
            setMode(Mode.values()[id]);
            return true;
        }
        if (id == RESET_BUTTON) {
            reset();
            return true;
        }
        return false;
    }

    private void setMode(Mode mode) {
        this.mode = mode;
        reset();
    }

    private void reset() {
        resultAvg = 0;
        resultMin = 0;
        resultMax = 0;
        resultCount = 0;
        pushFields();
    }

    private void pushFields() {
        data.set(0, mode.ordinal());
        pack(1, resultAvg);
        pack(2, resultMin);
        pack(3, resultMax);
        data.set(2 * 4, resultCount);
    }

    private void pack(int index, int value) {
        data.set(2 * index, value & 0xffff);
        data.set(2 * index + 1, value >>> 16);
    }

    private int unpack(int index) {
        return (data.get(2 * index) & 0xffff) | (data.get(2 * index + 1) << 16);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return client || player.isAlive() && player.getInventory().getItem(meterSlotIndex) == meter;
    }
}
