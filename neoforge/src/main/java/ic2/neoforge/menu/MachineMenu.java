package ic2.neoforge.menu;

import ic2.neoforge.item.ElectricItem;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.machine.*;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.FluidContainerPort;
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
import net.neoforged.neoforge.transfer.item.ItemResource;
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
        if (kind == MachineKind.FLUID_REGULATOR) {
            addFluidContainerSlot(inventory, 0, 48, 72);
            addOutputSlot(inventory, 1, 66, 72);
            addBatterySlot(inventory, 2, 8, 72);
        } else if (kind == MachineKind.STEAM_KINETIC_GENERATOR) {
            addSlot(
                    new ResourceHandlerSlot(inventory, inventory::set, 0, 80, 18) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return ic2.neoforge.machine.SteamTurbineBlockEntity.rotor(
                                    ItemResource.of(stack));
                        }

                        @Override
                        public int getMaxStackSize() {
                            return 1;
                        }
                    });
        } else if (kind == MachineKind.STEAM_GENERATOR) {
            // The boiler has valves and fluid ports, but no internal item slots.
        } else if (kind == MachineKind.PUMP) {
            addFluidContainerSlot(inventory, 0, 56, 35);
            addOutputSlot(inventory, 1, 116, 35);
        } else if (kind == MachineKind.SORTING_MACHINE) {
            var sorting = (SortingMachineBlockEntity) machine;
            for (int slot = 0; slot < 42; slot++)
                addSlot(
                        new ResourceHandlerSlot(
                                sorting.filters(),
                                sorting.filters()::set,
                                slot,
                                8 + (slot % 7) * 18,
                                18 + (slot / 7) * 18) {
                            @Override
                            public int getMaxStackSize() {
                                return 1;
                            }
                        });
            for (int slot = 0; slot < 11; slot++)
                addSlot(
                        new ResourceHandlerSlot(
                                inventory, inventory::set, slot, 8 + (slot % 11) * 16, 150) {
                            @Override
                            public int getMaxStackSize() {
                                return 64;
                            }
                        });
        } else if (kind == MachineKind.TRADE_O_MAT) {
            addSlot(
                    new ResourceHandlerSlot(inventory, inventory::set, 0, 56, 17) {
                        @Override
                        public int getMaxStackSize() {
                            return 1;
                        }
                    });
            addSlot(
                    new ResourceHandlerSlot(inventory, inventory::set, 1, 102, 17) {
                        @Override
                        public int getMaxStackSize() {
                            return 1;
                        }
                    });
            addSlot(new ResourceHandlerSlot(inventory, inventory::set, 2, 56, 53));
            addSlot(new ResourceHandlerSlot(inventory, inventory::set, 3, 102, 53));
        } else if (kind == MachineKind.ADV_MINER) {
            addSlot(
                    new ResourceHandlerSlot(inventory, inventory::set, 0, 8, 26) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return stack.getItem() instanceof ic2.neoforge.item.ScannerItem;
                        }
                    });
            addSlot(
                    new ResourceHandlerSlot(
                            inventory,
                            inventory::set,
                            ic2.neoforge.machine.AdvMinerBlockEntity.CARD_SLOT,
                            152,
                            8) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return stack.getItem()
                                    instanceof ic2.neoforge.item.MiningFilterCardItem;
                        }
                    });
            for (int slot = 0; slot < ic2.neoforge.machine.AdvMinerBlockEntity.FILTER_SIZE; slot++)
                addSlot(
                        new ResourceHandlerSlot(
                                inventory,
                                inventory::set,
                                ic2.neoforge.machine.AdvMinerBlockEntity.FILTER_START + slot,
                                36 + slot % 5 * 18,
                                44 + slot / 5 * 18));
        } else if (kind == MachineKind.MINER) {
            addSlot(
                    new ResourceHandlerSlot(inventory, inventory::set, 2, 8, 58) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return stack.getItem() instanceof ic2.neoforge.item.ScannerItem;
                        }
                    });
            addSlot(
                    new ResourceHandlerSlot(inventory, inventory::set, 1, 8, 40) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return stack.getItem()
                                            == ic2.neoforge.registration.ModMaterialBlocks
                                                    .MINING_PIPE
                                                    .get()
                                                    .asItem()
                                    || stack.getItem()
                                            instanceof net.minecraft.world.item.BlockItem;
                        }
                    });
            addSlot(
                    new ResourceHandlerSlot(inventory, inventory::set, 0, 8, 22) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return stack.getItem() instanceof ic2.neoforge.item.DrillItem;
                        }
                    });
            for (int slot = 0; slot < MinerBlockEntity.BUFFER_SIZE; slot++)
                addSlot(
                        new ResourceHandlerSlot(
                                inventory,
                                inventory::set,
                                MinerBlockEntity.BUFFER_START + slot,
                                44 + slot % 5 * 18,
                                22 + slot / 5 * 18));
        } else if (kind.storageBox()) {
            for (int slot = 0; slot < kind.slots(); slot++)
                addSlot(
                        new ResourceHandlerSlot(
                                inventory,
                                inventory::set,
                                slot,
                                8 + (slot % 9) * 18,
                                18 + (slot / 9) * 18) {
                            @Override
                            public int getMaxStackSize() {
                                return 64;
                            }
                        });
        } else if (kind == MachineKind.PERSONAL_CHEST) {
            for (int slot = 0; slot < 54; slot++)
                addSlot(
                        new ResourceHandlerSlot(
                                inventory,
                                inventory::set,
                                slot,
                                8 + (slot % 9) * 18,
                                18 + (slot / 9) * 18) {
                            @Override
                            public int getMaxStackSize() {
                                return 64;
                            }
                        });
        } else if (kind == MachineKind.BATBOX_CHARGEPAD
                || kind == MachineKind.CESU_CHARGEPAD
                || kind == MachineKind.MFE_CHARGEPAD
                || kind == MachineKind.MFSU_CHARGEPAD) {
            // The pad charges the player standing on it; it has no slots of its own.
        } else if (kind == MachineKind.STEAM_REPRESSURIZER) {
            addFluidContainerSlot(inventory, 0, 130, 72);
            addOutputSlot(inventory, 1, 152, 72);
        } else if (kind == MachineKind.RT_HEAT_GENERATOR) {
            for (int slot = 0; slot < 6; slot++)
                addSlot(
                        new ResourceHandlerSlot(
                                inventory, inventory::set, slot, 34 + slot * 18, 34) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return ItemResource.of(stack).getItem()
                                        == ic2.neoforge.registration.ModReactorItems.RTG_PELLET
                                                .get();
                            }

                            @Override
                            public int getMaxStackSize() {
                                return 1;
                            }
                        });
        } else if (kind == MachineKind.RT_GENERATOR) {
            for (int slot = 0; slot < 6; slot++)
                addSlot(
                        new ResourceHandlerSlot(
                                inventory, inventory::set, slot, 34 + slot * 18, 34) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return ItemResource.of(stack).getItem()
                                        == ic2.neoforge.registration.ModReactorItems.RTG_PELLET
                                                .get();
                            }

                            @Override
                            public int getMaxStackSize() {
                                return 1;
                            }
                        });
            addBatterySlot(inventory, 6, 8, 72);
        } else if (kind == MachineKind.CONDENSER) {
            addFluidContainerSlot(inventory, 0, 130, 72);
            addOutputSlot(inventory, 1, 152, 72);
            addBatterySlot(inventory, 2, 8, 72);
            addInstalledParts(
                    inventory,
                    3,
                    ic2.neoforge.registration.ModReactorItems.HEAT_VENT.get(),
                    54,
                    17);
        } else if (kind == MachineKind.ELECTROLYZER) {
            addBatterySlot(inventory, 0, 50, 53);
        } else if (kind == MachineKind.TANK) {
            // Tanks contain only the four upgrade slots added below.
        } else if (kind == MachineKind.LIQUID_HEAT_EXCHANGER) {
            addFluidContainerSlot(inventory, 0, 8, 65);
            addOutputSlot(inventory, 1, 26, 65);
            addFluidContainerSlot(inventory, 2, 134, 65);
            addOutputSlot(inventory, 3, 152, 65);
            addInstalledParts(
                    inventory,
                    4,
                    ModItems.MATERIALS.get(MaterialDefinition.HEAT_CONDUCTOR).get(),
                    45,
                    17);
        } else if (kind == MachineKind.FERMENTER) {
            addFluidContainerSlot(inventory, 0, 28, 17);
            addOutputSlot(inventory, 1, 28, 53);
            addFluidContainerSlot(inventory, 2, 130, 17);
            addOutputSlot(inventory, 3, 130, 53);
            addOutputSlot(inventory, 4, 80, 53);
        } else if (kind.turbine()) {
            addSlot(
                    new ResourceHandlerSlot(inventory, inventory::set, 0, 133, 24) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return stack.getItem() instanceof ic2.neoforge.item.RotorItem rotor
                                    && (kind != MachineKind.WATER_KINETIC_GENERATOR
                                            || rotor.material().supportsWater());
                        }
                    });
        } else if (kind.fuelHeat()) {
            addSlot(
                    new ResourceHandlerSlot(inventory, inventory::set, 0, 56, 17) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return kind == MachineKind.FLUID_HEAT_GENERATOR
                                    ? ItemAccess.forStack(stack.copy())
                                                    .getCapability(Capabilities.Fluid.ITEM)
                                            != null
                                    : ic2.neoforge.machine.FuelHeatBlockEntity.solidBurnTime(
                                                    stack, playerInventory.player.level())
                                            > 0;
                        }
                    });
            addOutputSlot(inventory, 1, 56, 53);
        } else if (kind.workConversion() || kind == MachineKind.MANUAL_KINETIC_GENERATOR) {
            // Conversion generators have no inventory.
        } else if (kind.electricWork()) {
            addInstalledParts(
                    inventory,
                    0,
                    ModItems.MATERIALS
                            .get(ic2.neoforge.machine.ElectricWorkBlockEntity.part(kind))
                            .get(),
                    56,
                    17);
            addBatterySlot(inventory, 10, 56, 53);
        } else if (kind == MachineKind.SOLAR_GENERATOR || kind == MachineKind.WIND_GENERATOR) {
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
        addStandardInventorySlots(playerInventory, 8, kind.inventoryY());
        addDataSlots(data);
    }

    private void addInstalledParts(
            MachineInventory inventory,
            int start,
            net.minecraft.world.item.Item item,
            int x,
            int y) {
        for (int part = 0; part < kind.installedPartsCount(); part++)
            addSlot(
                    new ResourceHandlerSlot(
                            inventory,
                            inventory::set,
                            start + part,
                            x + part % 5 * 18,
                            y + part / 5 * 18) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return stack.is(item);
                        }

                        @Override
                        public int getMaxStackSize() {
                            return 1;
                        }
                    });
    }

    private void addFluidContainerSlot(MachineInventory inventory, int slot, int x, int y) {
        addSlot(
                new ResourceHandlerSlot(inventory, inventory::set, slot, x, y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return ic2.neoforge.transfer.FluidContainerPort.accepts(
                                net.neoforged.neoforge.transfer.item.ItemResource.of(stack));
                    }
                });
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
        return integer(MachineMenuData.CAPACITY_FIELD * 2);
    }

    public int progress() {
        return integer(2);
    }

    public int progressMaximum() {
        return integer(4);
    }

    public int cannerMode() {
        return familyValue(0);
    }

    public int tankAmount(boolean output) {
        return familyValue(output ? 2 : 1);
    }

    public Fluid tankFluid(boolean output) {
        return BuiltInRegistries.FLUID.byId(familyValue(output ? 4 : 3));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.containerMenu != this || !stillValid(player) || machine == null) return false;
        if (machine instanceof ic2.neoforge.machine.TankBlockEntity tank)
            return (id == 0 || id == 1) && tank.transferCursor(player, this, id == 1);
        return machine.menuAction(id);
    }

    public float familyFloat(int index) {
        return Float.intBitsToFloat(familyValue(index));
    }

    public int familyValue(int index) {
        return integer(10 + Objects.checkIndex(index, MachineMenuData.FAMILY_VALUES) * 2);
    }

    private int preferredSlot(ItemStack stack, Player player) {
        if (kind == MachineKind.STEAM_GENERATOR
                || kind.workConversion()
                || kind == MachineKind.MANUAL_KINETIC_GENERATOR) return -1;
        if (kind.fuelHeat() || kind.turbine()) return 0;
        if (kind.electricWork())
            return stack.getItem() instanceof ElectricItem
                    ? 10
                    : stack.is(
                                    ModItems.MATERIALS
                                            .get(
                                                    ic2.neoforge.machine.ElectricWorkBlockEntity
                                                            .part(kind))
                                            .get())
                            ? 0
                            : -1;
        if (stack.getItem() instanceof UpgradeItem item)
            return item.kind().suitable(kind) ? kind.upgradeStart() : -1;
        if (kind == MachineKind.STEAM_KINETIC_GENERATOR)
            return ic2.neoforge.machine.SteamTurbineBlockEntity.rotor(ItemResource.of(stack))
                    ? 0
                    : -1;
        if (kind == MachineKind.CONDENSER) {
            if (stack.is(ic2.neoforge.registration.ModReactorItems.HEAT_VENT.get())) return 3;
            if (stack.getItem() instanceof ElectricItem) return 2;
            return FluidContainerPort.accepts(ItemResource.of(stack)) ? 0 : -1;
        }
        if (kind == MachineKind.ELECTROLYZER)
            return stack.getItem() instanceof ElectricItem ? 0 : -1;
        if (kind == MachineKind.TANK) return -1;
        if (kind == MachineKind.LIQUID_HEAT_EXCHANGER
                && stack.is(ModItems.MATERIALS.get(MaterialDefinition.HEAT_CONDUCTOR).get()))
            return 4;
        if (kind == MachineKind.FERMENTER || kind == MachineKind.LIQUID_HEAT_EXCHANGER) {
            var fluid = ItemAccess.forStack(stack.copy()).getCapability(Capabilities.Fluid.ITEM);
            if (fluid == null) return -1;
            for (int i = 0; i < fluid.size(); i++) if (fluid.getAmountAsLong(i) > 0) return 0;
            return 2;
        }
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
        if (kind.installedPartsStart() >= 0 && preferred == kind.installedPartsStart()) {
            boolean installed = false;
            for (int slot = preferred;
                    slot < preferred + kind.installedPartsCount() && !stack.isEmpty();
                    slot++) installed |= moveItemStackTo(stack, slot, slot + 1, false);
            return installed;
        }
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
