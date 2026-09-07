package ic2.neoforge.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;

/** Only rotor speed is sent to observers; detailed fuel data uses the opened menu. */
public abstract class RotorGeneratorBlockEntity extends GeneratingBlockEntity {
    private float rotorSpeed;

    protected RotorGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    public float rotorSpeed() {
        return rotorSpeed;
    }

    protected final void setRotorSpeed(float speed) {
        if (!Float.isFinite(speed) || speed < 0)
            throw new IllegalArgumentException("Invalid rotor speed");
        if (rotorSpeed == speed) return;
        rotorSpeed = speed;
        if (level != null && !level.isClientSide())
            level.sendBlockUpdated(
                    worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        tag.putFloat("rotorSpeed", rotorSpeed);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        float speed = input.getFloatOr("rotorSpeed", 0);
        rotorSpeed = Float.isFinite(speed) ? Math.clamp(speed, 0, 100) : 0;
    }
}
