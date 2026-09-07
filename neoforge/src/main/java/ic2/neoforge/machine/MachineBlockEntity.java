package ic2.neoforge.machine;

import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.transfer.MachineInventory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Inventory and world lifecycle shared by electric, fuel, heat and fluid machines. */
public abstract class MachineBlockEntity extends BlockEntity implements MenuProvider {
    protected final MachineInventory inventory;

    protected MachineBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state);
        inventory = new MachineInventory(slots, this::setChanged, (slot, resource) -> true);
    }

    public final MachineInventory inventory() {
        return inventory;
    }

    public final MachineKind kind() {
        return ((MachineBlock) getBlockState().getBlock()).kind();
    }

    public abstract ResourceHandler<ItemResource> automation(Direction side);

    public abstract void serverTick(ServerLevel level);

    public abstract int progress();

    public abstract int progressMaximum();

    public double storedEnergy() {
        return 0;
    }

    public int fuelRemaining() {
        return 0;
    }

    public int fuelMaximum() {
        return 0;
    }

    public void awardExperience(Player player) {}

    protected final void setActive(boolean active) {
        if (level != null && getBlockState().getValue(MachineBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, getBlockState().setValue(MachineBlock.ACTIVE, active), 3);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) Containers.dropContents(level, pos, inventory.copyToList());
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input.childOrEmpty("inventory"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
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
