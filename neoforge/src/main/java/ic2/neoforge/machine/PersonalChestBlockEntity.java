package ic2.neoforge.machine;

import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * A 54-slot safe that belongs to whoever opens it first. Non-owners get no menu, cannot automate
 * contents and cannot break it while it holds items; the owner may break it once it is empty.
 */
public final class PersonalChestBlockEntity extends MachineBlockEntity {
    private UUID owner;
    private String ownerName = "";

    public PersonalChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.PERSONAL_CHEST), pos, state, 54);
    }

    public boolean owned() {
        return owner != null;
    }

    public String ownerName() {
        return ownerName;
    }

    /** The first player to open the safe claims it; afterwards only they may use it. */
    public boolean permits(Player player) {
        if (owner == null) {
            owner = player.getUUID();
            ownerName = player.getName().getString();
            setChanged();
            return true;
        }
        return owner.equals(player.getUUID());
    }

    public boolean permitsBreak(Player player, boolean holdingContents) {
        return permits(player) && !holdingContents;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int id, Inventory playerInventory, Player player) {
        return serverLevel() != null && !permits(player)
                ? null
                : super.createMenu(id, playerInventory, player);
    }

    private ServerLevel serverLevel() {
        return level instanceof ServerLevel server ? server : null;
    }

    @Override
    public void serverTick(ServerLevel level) {}

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory, slot -> false, slot -> false, (slot, resource) -> false);
    }

    @Override
    public int progress() {
        return 0;
    }

    @Override
    public int progressMaximum() {
        return 0;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        String uuid = input.getStringOr("owner", "");
        owner = uuid.isEmpty() ? null : UUID.fromString(uuid);
        ownerName = input.getStringOr("ownerName", "");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) {
            output.putString("owner", owner.toString());
            output.putString("ownerName", ownerName);
        }
    }
}
