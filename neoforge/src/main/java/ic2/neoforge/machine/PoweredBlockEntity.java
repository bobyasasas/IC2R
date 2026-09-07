package ic2.neoforge.machine;

import ic2.core.energy.EnergyStore;
import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.energy.WorldEnergyNetworks;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.transfer.MachineInventory;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class PoweredBlockEntity extends BlockEntity implements MenuProvider {
    protected final EnergyStore energy;
    protected final MachineInventory inventory;

    protected PoweredBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state, double capacity, int slots) {
        super(type, pos, state);
        energy = new EnergyStore(capacity);
        inventory = new MachineInventory(slots, this::setChanged, (slot, resource) -> true);
    }

    public net.neoforged.neoforge.transfer.ResourceHandler<
                    net.neoforged.neoforge.transfer.item.ItemResource>
            automation(net.minecraft.core.Direction side) {
        return new ic2.neoforge.transfer.ResourcePort<>(
                inventory,
                slot -> slot == 0 && side != net.minecraft.core.Direction.DOWN,
                slot ->
                        kind() == MachineKind.ELECTRIC_FURNACE
                                ? slot == 1
                                : slot == 0
                                        && side == net.minecraft.core.Direction.DOWN
                                        && !inventory.stack(0).isEmpty()
                                        && level != null
                                        && inventory
                                                        .stack(0)
                                                        .getBurnTime(
                                                                net.minecraft.world.item.crafting
                                                                        .RecipeType.SMELTING,
                                                                level.fuelValues())
                                                == 0,
                (slot, item) ->
                        kind() != MachineKind.GENERATOR
                                || level != null
                                        && item.toStack()
                                                        .getBurnTime(
                                                                net.minecraft.world.item.crafting
                                                                        .RecipeType.SMELTING,
                                                                level.fuelValues())
                                                > 0);
    }

    public EnergyStore energy() {
        return energy;
    }

    public MachineInventory inventory() {
        return inventory;
    }

    public MachineKind kind() {
        return ((MachineBlock) getBlockState().getBlock()).kind();
    }

    public abstract EnergyNode.Terminal energyNode();

    public abstract void serverTick(ServerLevel level);

    public abstract int progress();

    public abstract int progressMaximum();

    protected void setActive(boolean active) {
        if (level != null && getBlockState().getValue(MachineBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, getBlockState().setValue(MachineBlock.ACTIVE, active), 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) WorldEnergyNetworks.add(server, this);
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel server) WorldEnergyNetworks.remove(server, this);
        super.setRemoved();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) Containers.dropContents(level, pos, inventory.copyToList());
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        double stored = input.getDoubleOr("energy", 0);
        energy.restore(Double.isFinite(stored) ? Math.clamp(stored, 0, energy.capacity()) : 0);
        inventory.deserialize(input.childOrEmpty("inventory"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("energy", energy.stored());
        inventory.serialize(output.child("inventory"));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ic2." + kind().getSerializedName());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new MachineMenu(id, playerInventory, this);
    }
}
