package ic2.neoforge.menu;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.NuclearReactorBlockEntity;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.FluidContainerPort;
import ic2.neoforge.transfer.MachineInventory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

import org.jspecify.annotations.Nullable;

/**
 * Legacy ContainerNuclearReactor: the 9x6 component grid opening at (26,25), the four fluid
 * container slots at the texture edges, and the player inventory where the legacy width-214
 * centering puts it (x=26, rows y=161, hotbar y=219). Heat, tank and mode values stream through
 * data slots exactly like the legacy networked fields heat/maxHeat/EmitHeat/inputTank/outputTank/
 * fluidCooled.
 */
public final class NuclearReactorMenu extends AbstractContainerMenu {
    public static final int MACHINE_SLOTS = NuclearReactorBlockEntity.FLUID_SLOTS;

    private static final int DATA_SIZE = 8;

    private final BlockPos position;
    private final @Nullable NuclearReactorBlockEntity reactor;
    private final int[] snapshot = new int[DATA_SIZE];

    private final ContainerData data =
            new ContainerData() {
                @Override
                public int get(int index) {
                    return reactor != null ? liveValue(index) : snapshot[index];
                }

                @Override
                public void set(int index, int value) {
                    snapshot[index] = value;
                }

                @Override
                public int getCount() {
                    return DATA_SIZE;
                }
            };

    public NuclearReactorMenu(int id, Inventory playerInventory, NuclearReactorBlockEntity reactor) {
        this(id, playerInventory, reactor.getBlockPos(), reactor, reactor.inventory());
    }

    /** The client binds a placeholder inventory; vanilla slot sync streams the real contents. */
    public NuclearReactorMenu(int id, Inventory playerInventory, BlockPos position) {
        this(
                id,
                playerInventory,
                position,
                null,
                new MachineInventory(MACHINE_SLOTS, () -> {}, (slot, item) -> true));
    }

    private NuclearReactorMenu(
            int id,
            Inventory playerInventory,
            BlockPos position,
            @Nullable NuclearReactorBlockEntity reactor,
            MachineInventory inventory) {
        super(ModMachines.NUCLEAR_REACTOR_MENU.get(), id);
        this.position = position.immutable();
        this.reactor = reactor;
        for (int y = 0; y < NuclearReactorBlockEntity.GRID_ROWS; y++)
            for (int x = 0; x < NuclearReactorBlockEntity.GRID_COLUMNS; x++)
                addSlot(
                        new ResourceHandlerSlot(
                                inventory,
                                inventory::set,
                                x + y * NuclearReactorBlockEntity.GRID_COLUMNS,
                                26 + x * 18,
                                25 + y * 18));
        addSlot(
                new ResourceHandlerSlot(
                        inventory, inventory::set, NuclearReactorBlockEntity.COOLANT_INPUT, 8, 25) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return holdsFluid(ItemResource.of(stack), coolantFluid());
                    }
                });
        addSlot(
                new ResourceHandlerSlot(
                        inventory,
                        inventory::set,
                        NuclearReactorBlockEntity.HOT_COOLANT_INPUT,
                        188,
                        25) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return takesFluid(ItemResource.of(stack), hotCoolantFluid());
                    }
                });
        addSlot(
                new ResourceHandlerSlot(
                        inventory,
                        inventory::set,
                        NuclearReactorBlockEntity.COOLANT_OUTPUT,
                        8,
                        115) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
        addSlot(
                new ResourceHandlerSlot(
                        inventory,
                        inventory::set,
                        NuclearReactorBlockEntity.HOT_COOLANT_OUTPUT,
                        188,
                        115) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(
                        new Slot(
                                playerInventory,
                                9 + column + row * 9,
                                26 + column * 18,
                                161 + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(playerInventory, column, 26 + column * 18, 219));
        addDataSlots(data);
    }

    public static Fluid coolantFluid() {
        return ModFluids.FAMILIES.get(FluidDefinition.COOLANT).source().get();
    }

    public static Fluid hotCoolantFluid() {
        return ModFluids.FAMILIES.get(FluidDefinition.HOT_COOLANT).source().get();
    }

    /** Legacy drain rule: the container must currently hold the fluid (whole 1000 mB cells). */
    public static boolean holdsFluid(ItemResource resource, Fluid fluid) {
        var handler = containerOf(resource);
        if (handler == null) return false;
        for (int tank = 0; tank < handler.size(); tank++) {
            var content = handler.getResource(tank);
            if (!content.isEmpty() && content.is(fluid)) return true;
        }
        return false;
    }

    /** Legacy fill rule: the container must have room the tank fluid may enter. */
    public static boolean takesFluid(ItemResource resource, Fluid fluid) {
        var handler = containerOf(resource);
        if (handler == null) return false;
        for (int tank = 0; tank < handler.size(); tank++) {
            if (handler.getResource(tank).isEmpty()
                    && handler.isValid(tank, FluidResource.of(fluid))) return true;
        }
        return false;
    }

    private static @Nullable ResourceHandler<FluidResource> containerOf(ItemResource resource) {
        if (resource.isEmpty()) return null;
        return ItemAccess.forStack(resource.toStack()).getCapability(Capabilities.Fluid.ITEM);
    }

    public int heat() {
        return data.get(0);
    }

    public int maxHeat() {
        return data.get(1);
    }

    public int emitHeat() {
        return data.get(2);
    }

    public int coolantAmount() {
        return data.get(3);
    }

    public int hotCoolantAmount() {
        return data.get(4);
    }

    public boolean fluidCooled() {
        return data.get(5) != 0;
    }

    public int euOutput() {
        return data.get(6);
    }

    public int reactorColumns() {
        return data.get(7);
    }

    private int liveValue(int index) {
        return switch (index) {
            case 0 -> reactor.getHeat();
            case 1 -> reactor.getMaxHeat();
            case 2 -> reactor.emitBuffer();
            case 3 -> reactor.coolantAmount();
            case 4 -> reactor.hotCoolantAmount();
            case 5 -> reactor.fluidCooled() ? 1 : 0;
            case 6 -> Math.round(reactor.getReactorEnergyOutput());
            case 7 -> reactor.columns();
            default -> 0;
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return (reactor == null || player.level().getBlockEntity(position) == reactor)
                && stillValid(
                        ContainerLevelAccess.create(player.level(), position),
                        player,
                        ModMachines.block(MachineKind.NUCLEAR_REACTOR));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem().copy();
        var original = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.setByPlayer(stack);
        slot.onTake(player, original.copyWithCount(original.getCount() - stack.getCount()));
        return original;
    }
}
