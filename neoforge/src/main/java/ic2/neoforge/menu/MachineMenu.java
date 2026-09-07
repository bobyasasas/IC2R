package ic2.neoforge.menu;

import ic2.neoforge.item.ElectricItem;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.UpgradeItem;
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

import java.util.Objects;

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
                        ? new SimpleContainerData(MachineMenuData.SIZE)
                        : new MachineMenuData(machine);
        if ((kind == MachineKind.SOLAR_GENERATOR || kind == MachineKind.WIND_GENERATOR)) {
            addBatterySlot(inventory, 0, 56, 53);
        } else if (kind.storage()) {
            addBatterySlot(inventory, 0, 56, 17);
            addBatterySlot(inventory, 1, 56, 53);
        } else if (kind.transformer()) {
            // Transformers have no inventory.
        } else if (kind == MachineKind.GENERATOR || kind == MachineKind.WATER_GENERATOR) {
            addSlot(
                    new ResourceHandlerSlot(inventory, inventory::set, 0, 56, 53) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            if (kind == MachineKind.WATER_GENERATOR)
                                return WaterGeneratorBlockEntity.containsWater(
                                        net.neoforged.neoforge.transfer.item.ItemResource.of(
                                                stack));
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
                    new ResourceHandlerSlot(
                            inventory,
                            inventory::set,
                            1,
                            (kind == MachineKind.ORE_WASHING_PLANT
                                            || kind == MachineKind.CENTRIFUGE)
                                    ? 110
                                    : 116,
                            kind == MachineKind.INDUCTION_FURNACE ? 17 : 35) {
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
        if (kind == MachineKind.ORE_WASHING_PLANT || kind == MachineKind.CENTRIFUGE) {
            addOutputSlot(inventory, 3, 128, 35);
            addOutputSlot(inventory, 4, 146, 35);
        }
        if (kind == MachineKind.ORE_WASHING_PLANT) {
            addSlot(
                    new ResourceHandlerSlot(inventory, inventory::set, 5, 110, 53) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return ItemAccess.forStack(stack.copy())
                                            .getCapability(Capabilities.Fluid.ITEM)
                                    != null;
                        }
                    });
            addOutputSlot(inventory, 6, 146, 53);
        }
        if (kind == MachineKind.INDUCTION_FURNACE) {
            addSlot(new ResourceHandlerSlot(inventory, inventory::set, 3, 56, 35));
            addOutputSlot(inventory, 4, 116, 35);
        }
        if (kind == MachineKind.CANNER)
            addSlot(new ResourceHandlerSlot(inventory, inventory::set, 3, 92, 17));
        if (kind.upgradable()) {
            for (int index = 0; index < kind.upgradeSlots(); index++) {
                addSlot(
                        new ResourceHandlerSlot(
                                inventory,
                                inventory::set,
                                kind.upgradeStart() + index,
                                180,
                                17 + 18 * index) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return stack.getItem() instanceof UpgradeItem item
                                        && item.kind().suitable(kind);
                            }
                        });
            }
        }
        addStandardInventorySlots(playerInventory, 8, 84);
        addDataSlots(data);
    }

    private void addOutputSlot(MachineInventory inventory, int slot, int x, int y) {
        addSlot(
                new ResourceHandlerSlot(inventory, inventory::set, slot, x, y) {
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
        return integer(15);
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
        return player.containerMenu == this
                && stillValid(player)
                && machine != null
                && machine.menuAction(id);
    }

    public int familyValue(int index) {
        return data.get(10 + Objects.checkIndex(index, 5));
    }

    private int preferredSlot(ItemStack stack, Player player) {
        if (stack.getItem() instanceof UpgradeItem item)
            return item.kind().suitable(kind) ? kind.upgradeStart() : -1;
        if ((kind == MachineKind.SOLAR_GENERATOR || kind == MachineKind.WIND_GENERATOR))
            return stack.getItem() instanceof ElectricItem ? 0 : -1;
        if (kind.transformer()) return -1;
        if (kind.storage())
            return stack.getItem() instanceof ElectricItem
                    ? (ElectricItemEnergy.charge(stack) > 0 ? 1 : 0)
                    : -1;
        if (stack.getItem() instanceof ElectricItem)
            return kind == MachineKind.GENERATOR || kind == MachineKind.WATER_GENERATOR ? 1 : 2;
        if (kind == MachineKind.IRON_FURNACE
                && stack.getBurnTime(RecipeType.SMELTING, player.level().fuelValues()) > 0)
            return 2;
        if (kind == MachineKind.CANNER
                && (stack.is(ModItems.MATERIALS.get(MaterialDefinition.TIN_CAN).get())
                        || ItemAccess.forStack(stack.copy()).getCapability(Capabilities.Fluid.ITEM)
                                != null)) return 3;
        if (kind == MachineKind.ORE_WASHING_PLANT
                && ItemAccess.forStack(stack.copy()).getCapability(Capabilities.Fluid.ITEM) != null)
            return OreWashingBlockEntity.WATER_INPUT;
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

    private boolean moveIntoMachine(ItemStack stack, Player player) {
        int preferred = preferredSlot(stack, player);
        if (preferred < 0) return false;
        boolean moved =
                moveItemStackTo(
                        stack,
                        preferred,
                        preferred
                                + (kind.upgradable() && preferred == kind.upgradeStart()
                                        ? kind.upgradeSlots()
                                        : 1),
                        false);
        if (kind == MachineKind.INDUCTION_FURNACE && preferred == 0 && !stack.isEmpty())
            moved |= moveItemStackTo(stack, 3, 4, false);
        return moved;
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
        } else if (!moveIntoMachine(stack, player)) {
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
