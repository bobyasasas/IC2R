package ic2.neoforge.machine;

import ic2.core.machine.RotorMaterial;
import ic2.core.machine.RotorOperation;
import ic2.core.machine.WorkRate;
import ic2.neoforge.api.WorkSource;
import ic2.neoforge.item.RotorItem;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModRotors;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import org.jspecify.annotations.Nullable;

/** Installed rotor, continuous work budget, wear and compact observer synchronization. */
public abstract class TurbineBlockEntity extends MachineBlockEntity implements RotorVisual {
    private record Visual(@Nullable RotorMaterial material, int diameter, float speed) {}

    private Visual visual = new Visual(null, 0, 0);
    private final WorkRate work = new WorkRate();
    private final StateJournal<WorkRate.State> journal =
            new StateJournal<>(work::state, work::restore, this::setChanged);
    private RotorOperation operation = RotorOperation.stopped(0, RotorOperation.Status.NO_ROTOR);
    private @Nullable RotorMaterial sampledMaterial;
    private @Nullable Direction sampledFacing;
    private long lastWearTick = Long.MIN_VALUE;
    private boolean sampled;

    protected TurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(((MachineBlock) state.getBlock()).kind()), pos, state, 1);
    }

    protected abstract int sampleInterval();

    protected abstract RotorOperation sample(
            ServerLevel level, RotorMaterial material, Direction facing, RotorSpace space);

    protected boolean acceptsRotor(RotorMaterial material) {
        return true;
    }

    private @Nullable RotorMaterial material() {
        return inventory.getResource(0).getItem() instanceof RotorItem rotor
                        && acceptsRotor(rotor.material())
                ? rotor.material()
                : null;
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        return resource.getItem() instanceof RotorItem rotor && acceptsRotor(rotor.material());
    }

    @Override
    protected int inventorySlotLimit(int slot) {
        return 1;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(inventory, slot -> true, slot -> true);
    }

    public WorkSource output(@Nullable Direction side) {
        return new WorkOutput(
                this, side, work, this::outputRate, journal::updateSnapshots, WorkOutput.Face.REAR);
    }

    private int outputRate() {
        return material() == sampledMaterial
                        && sampledMaterial != null
                        && getBlockState().getValue(MachineBlock.FACING) == sampledFacing
                ? operation.output()
                : 0;
    }

    @Override
    public final void serverTick(ServerLevel level) {
        long tick = level.getGameTime();
        var material = material();
        var facing = getBlockState().getValue(MachineBlock.FACING);
        boolean interval =
                Math.floorMod(tick, sampleInterval())
                        == Math.floorMod(worldPosition.hashCode(), sampleInterval());
        if (!sampled || material != sampledMaterial || facing != sampledFacing || interval) {
            sampled = true;
            sampledMaterial = material;
            sampledFacing = facing;
            operation =
                    material == null
                            ? RotorOperation.stopped(0, RotorOperation.Status.NO_ROTOR)
                            : facing.getAxis().isVertical()
                                    ? RotorOperation.stopped(
                                            material.diameter(),
                                            RotorOperation.Status.INVALID_FACING)
                                    : sample(level, material, facing, new RotorSpace(level));
            if (interval && tick != lastWearTick && operation.wear() > 0 && material != null) {
                var rotor = inventory.stack(0);
                rotor.hurtAndBreak(operation.wear(), level, (LivingEntity) null, ignored -> {});
                inventory.set(0, ItemResource.of(rotor), rotor.getCount());
                lastWearTick = tick;
                setChanged();
                if (rotor.isEmpty()) {
                    material = null;
                    operation = RotorOperation.stopped(0, RotorOperation.Status.NO_ROTOR);
                }
            }
            var next = new Visual(material, operation.diameter(), operation.degreesPerTick());
            if (!visual.equals(next)) {
                visual = next;
                level.sendBlockUpdated(
                        worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        setActive(outputRate() > 0);
    }

    @Override
    public int progress() {
        var rotor = inventory.stack(0);
        return rotor.isEmpty()
                ? 0
                : (int)
                        Math.round(
                                1000.0
                                        * (rotor.getMaxDamage() - rotor.getDamageValue())
                                        / rotor.getMaxDamage());
    }

    @Override
    public int progressMaximum() {
        return 1000;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> outputRate();
            case 1 -> operation.status().ordinal();
            case 2 -> operation.obstructions();
            case 3 -> Float.floatToIntBits(operation.environment());
            default -> 0;
        };
    }

    @Override
    public int rotorDiameter() {
        return visual.diameter();
    }

    @Override
    public float rotorDegreesPerTick() {
        return visual.speed();
    }

    @Override
    public Identifier rotorTexture() {
        return ModRotors.texture(
                visual.material() == null ? RotorMaterial.WOODEN : visual.material());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        tag.putString("rotor", visual.material() == null ? "" : visual.material().id());
        tag.putInt("rotorDiameter", visual.diameter());
        tag.putFloat("rotorSpeed", visual.speed());
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void readVisual(ValueInput input) {
        String id = input.getStringOr("rotor", "");
        RotorMaterial material = null;
        for (var candidate : RotorMaterial.values())
            if (candidate.id().equals(id)) material = candidate;
        float speed = input.getFloatOr("rotorSpeed", 0);
        visual =
                new Visual(
                        material,
                        material == null
                                ? 0
                                : Math.clamp(input.getIntOr("rotorDiameter", 0), 0, 11),
                        Float.isFinite(speed) ? Math.clamp(speed, -100, 100) : 0);
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        readVisual(input);
    }

    @Override
    public void onDataPacket(Connection connection, ValueInput input) {
        readVisual(input);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        work.restore(
                new WorkRate.State(
                        input.getLongOr("outputTick", Long.MIN_VALUE),
                        Math.max(0, input.getIntOr("outputExtracted", 0))));
        lastWearTick = input.getLongOr("lastWearTick", Long.MIN_VALUE);
        sampled = false;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("outputTick", work.state().tick());
        output.putInt("outputExtracted", work.state().extracted());
        output.putLong("lastWearTick", lastWearTick);
    }
}
