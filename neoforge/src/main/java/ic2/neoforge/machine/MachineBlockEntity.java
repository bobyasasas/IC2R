package ic2.neoforge.machine;

import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.ModGameEvents;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Inventory and world lifecycle shared by electric, fuel, heat and fluid machines. */
public abstract class MachineBlockEntity extends BlockEntity implements MenuProvider {
    protected final MachineInventory inventory;

    protected MachineBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state);
        inventory = new MachineInventory(slots, this::setChanged, this::acceptsInventorySlot);
    }

    private boolean acceptsInventorySlot(int slot, ItemResource resource) {
        if (!kind().upgradable() || slot < kind().upgradeStart()) return true;
        return resource.getItem() instanceof UpgradeItem item && item.kind().suitable(kind());
    }

    public double energyCapacity() {
        return kind().capacity();
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

    /**
     * Five family-specific menu values; common energy/progress/fuel fields live in MachineMenuData.
     */
    public int menuValue(int index) {
        return 0;
    }

    public boolean menuAction(int id) {
        return false;
    }

    protected final void setActive(boolean active) {
        if (level != null && getBlockState().getValue(MachineBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, getBlockState().setValue(MachineBlock.ACTIVE, active), 3);
            if (!level.isClientSide()) {
                var event =
                        kind().generating()
                                ? (active
                                        ? ModGameEvents.GENERATOR_ACTIVATE
                                        : ModGameEvents.GENERATOR_DEACTIVATE)
                                : (active
                                        ? ModGameEvents.MACHINE_ACTIVATE
                                        : ModGameEvents.MACHINE_DEACTIVATE);
                level.gameEvent(event, worldPosition, GameEvent.Context.of(getBlockState()));
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        NeoForge.EVENT_BUS.post(new MachineLoadedEvent(this));
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
