package ic2.neoforge.menu;

import ic2.core.machine.CannerMode;
import ic2.neoforge.item.ElectricItem;
import ic2.neoforge.machine.*;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.MachineInventory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/** Vanilla container synchronization keeps inventory and progress server authoritative. */
public final class MachineMenu extends AbstractContainerMenu {
    private final MachineKind kind;
    private final BlockPos position;
    private final MachineBlockEntity machine;
    private final ContainerData data;
    private final int machineSlots;

    public MachineMenu(int id, Inventory playerInventory, MachineBlockEntity machine) {
        this(
                id,
                playerInventory,
                machine.getBlockPos(),
                machine.kind(),
                machine,
                machine.inventory());
    }

    public MachineMenu(int id, Inventory playerInventory, BlockPos position, MachineKind kind) {
        this(
                id,
                playerInventory,
                position,
                kind,
                null,
                new MachineInventory(kind.slots(), () -> {}, (slot, item) -> true));
    }

    private MachineMenu(
            int id,
            Inventory playerInventory,
            BlockPos position,
            MachineKind kind,
            MachineBlockEntity machine,
            MachineInventory inventory) {
        super(ModMachines.menuType(kind), id);
        this.kind = kind;
        this.position = position.immutable();
        this.machine = machine;
        machineSlots = inventory.size();
        data =
                machine == null
                        ? new SimpleContainerData(15)
                        : new ContainerData() {
                            @Override
                            public int get(int index) {
                                return switch (index) {
                                    case 0 -> (int) machine.storedEnergy() & 0xffff;
                                    case 1 -> (int) machine.storedEnergy() >>> 16;
                                    case 2 -> machine.progress() & 0xffff;
                                    case 3 -> machine.progress() >>> 16;
                                    case 4 -> machine.progressMaximum() & 0xffff;
                                    case 5 -> machine.progressMaximum() >>> 16;
                                    case 6 -> machine.fuelRemaining() & 0xffff;
                                    case 7 -> machine.fuelRemaining() >>> 16;
                                    case 8 -> machine.fuelMaximum() & 0xffff;
                                    case 9 -> machine.fuelMaximum() >>> 16;
                                    case 10 ->
                                            machine instanceof CannerBlockEntity canner
                                                    ? canner.mode().id()
                                                    : 0;
                                    case 11 ->
                                            machine instanceof CannerBlockEntity canner
                                                    ? canner.inputTank().getAmountAsInt(0)
                                                    : 0;
                                    case 12 ->
                                            machine instanceof CannerBlockEntity canner
                                                    ? canner.outputTank().getAmountAsInt(0)
                                                    : 0;
                                    case 13 ->
                                            machine instanceof CannerBlockEntity canner
                                                    ? BuiltInRegistries.FLUID.getId(
                                                            canner.inputTank()
                                                                    .getResource(0)
                                                                    .getFluid())
                                                    : 0;
                                    case 14 ->
                                            machine instanceof CannerBlockEntity canner
                                                    ? BuiltInRegistries.FLUID.getId(
                                                            canner.outputTank()
                                                                    .getResource(0)
                                                                    .getFluid())
                                                    : 0;
                                    default -> throw new IndexOutOfBoundsException(index);
                                };
                            }

                            @Override
                            public void set(int index, int value) {
                                /* Server owns machine state. */
                            }

                            @Override
                            public int getCount() {
                                return 15;
                            }
                        };
        if (kind == MachineKind.GENERATOR) {
            addSlot(
                    new ResourceHandlerSlot(inventory, inventory::set, 0, 56, 53) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return stack.getBurnTime(
                                            RecipeType.SMELTING,
                                            playerInventory.player.level().fuelValues())
                                    > 0;
                        }
                    });
            addBatterySlot(inventory, 1, 56, 17);
        } else {
            addSlot(new ResourceHandlerSlot(inventory, inventory::set, 0, 56, 17));
            addSlot(
                    new ResourceHandlerSlot(inventory, inventory::set, 1, 116, 35) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return false;
                        }

                        @Override
                        public void onTake(Player player, ItemStack stack) {
                            super.onTake(player, stack);
                            if (machine != null) machine.awardExperience(player);
                        }
                    });
            if (kind == MachineKind.IRON_FURNACE) {
                addSlot(
                        new ResourceHandlerSlot(inventory, inventory::set, 2, 56, 53) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return stack.getBurnTime(
                                                RecipeType.SMELTING,
                                                playerInventory.player.level().fuelValues())
                                        > 0;
                            }
                        });
            } else addBatterySlot(inventory, 2, 56, 53);
        }
        if (kind == MachineKind.CANNER)
            addSlot(new ResourceHandlerSlot(inventory, inventory::set, 3, 92, 17));
        addStandardInventorySlots(playerInventory, 8, 84);
        addDataSlots(data);
    }

    private void addBatterySlot(MachineInventory inventory, int slot, int x, int y) {
        addSlot(
                new ResourceHandlerSlot(inventory, inventory::set, slot, x, y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem() instanceof ElectricItem;
                    }

                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }

                    @Override
                    public int getMaxStackSize(ItemStack stack) {
                        return 1;
                    }
                });
    }

    public MachineKind kind() {
        return kind;
    }

    public int fuelRemaining() {
        return integer(6);
    }

    public int fuelMaximum() {
        return integer(8);
    }

    public int energy() {
        return integer(0);
    }

    public int capacity() {
        return kind.capacity();
    }

    public int progress() {
        return integer(2);
    }

    public int progressMaximum() {
        return integer(4);
    }

    public int cannerMode() {
        return data.get(10);
    }

    public int tankAmount(boolean output) {
        return data.get(output ? 12 : 11);
    }

    public Fluid tankFluid(boolean output) {
        return BuiltInRegistries.FLUID.byId(data.get(output ? 14 : 13));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.containerMenu != this
                || !stillValid(player)
                || !(machine instanceof CannerBlockEntity canner)) return false;
        if (id >= 0 && id < CannerMode.values().length) {
            canner.setMode(CannerMode.byId(id));
            return true;
        }
        return id == 5 && canner.swapTanks();
    }

    private int preferredSlot(ItemStack stack, Player player) {
        if (stack.getItem() instanceof ElectricItem) return kind == MachineKind.GENERATOR ? 1 : 2;
        if (kind == MachineKind.IRON_FURNACE
                && stack.getBurnTime(RecipeType.SMELTING, player.level().fuelValues()) > 0)
            return 2;
        if (kind == MachineKind.CANNER
                && (stack.is(ModItems.MATERIALS.get(MaterialDefinition.TIN_CAN).get())
                        || ItemAccess.forStack(stack.copy()).getCapability(Capabilities.Fluid.ITEM)
                                != null)) return 3;
        return 0;
    }

    private int integer(int low) {
        return (data.get(low) & 0xffff) | (data.get(low + 1) & 0xffff) << 16;
    }

    @Override
    public boolean stillValid(Player player) {
        return (machine == null || player.level().getBlockEntity(position) == machine)
                && stillValid(
                        ContainerLevelAccess.create(player.level(), position),
                        player,
                        ModMachines.block(kind));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem().copy();
        var original = stack.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(
                stack, preferredSlot(stack, player), preferredSlot(stack, player) + 1, false)) {
            int hotbar = machineSlots + 27;
            if (!moveItemStackTo(
                    stack,
                    index < hotbar ? hotbar : machineSlots,
                    index < hotbar ? slots.size() : hotbar,
                    false)) return ItemStack.EMPTY;
        }
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.setByPlayer(stack);
        slot.onTake(player, original.copyWithCount(original.getCount() - stack.getCount()));
        return original;
    }
}
