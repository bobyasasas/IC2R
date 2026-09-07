package ic2.core.block.machine.tileentity;

import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.api.recipe.IEmptyFluidContainerRecipeManager.Output;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.upgrade.UpgradableProperty;
import ic2.api.util.FluidContainerOutputMode;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlotConsumableCanner;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotProcessableCanner;
import ic2.core.block.machine.container.ContainerCanner;
import ic2.core.fluid.Ic2FluidStack;
import ic2.core.fluid.Ic2FluidTank;
import ic2.core.network.GrowingBuffer;
import ic2.core.ref.Ic2BlockEntities;
import ic2.core.ref.Ic2SoundEvents;
import ic2.core.util.LiquidUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class TileEntityCanner extends TileEntityStandardMachine<Object, Object, Object>
        implements INetworkClientTileEntityEventListener {
    public static final int eventSwapTanks = Mode.values.length + 1;
    public final Ic2FluidTank inputTank;
    public final Ic2FluidTank outputTank;
    public final InvSlotConsumableCanner canInputSlot;
    protected final Fluids fluids;
    private Mode mode = Mode.BottleSolid;
    private Mode appliedMode;

    public TileEntityCanner(BlockPos pos, BlockState state) {
        super(Ic2BlockEntities.CANNER, pos, state, 4, 200, 1);
        this.inputSlot = new InvSlotProcessableCanner(this, "input", 1);
        this.canInputSlot = new InvSlotConsumableCanner(this, "canInput", 1);
        this.fluids = this.addComponent(new Fluids(this));
        this.inputTank = this.fluids.addTankInsert("inputTank", 8000);
        this.outputTank = this.fluids.addTankExtract("outputTank", 8000);
        this.canInputSlot.setOpType(InvSlotConsumableLiquid.OpType.None);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.setMode(Mode.values[nbt.getInt("mode")]);
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putInt("mode", this.mode.ordinal());
    }

    @Override
    public void operateOnce(
            MachineRecipeResult<Object, Object, Object> result,
            Collection<ItemStack> processResult) {
        super.operateOnce(result, processResult);
        if (this.mode == Mode.EmptyLiquid) {
            Output output = (Output) result.getOutput();
            this.getOutputTank().fillMbUnchecked(output.fluid(), false);
        } else if (this.mode == Mode.EnrichLiquid) {
            Ic2FluidStack output = ((Ic2FluidStack) result.getOutput()).copy();
            LiquidUtil.FluidOperationResult outcome;
            if (!this.canInputSlot.isEmpty()) {
                do {
                    outcome =
                            LiquidUtil.fillContainer(
                                    this.canInputSlot.get(),
                                    output,
                                    FluidContainerOutputMode.EmptyFullToOutput);
                    if (outcome != null) {
                        if (outcome.extraOutput != null
                                && !this.outputSlot.canAdd(outcome.extraOutput)) {
                            outcome = null;
                        } else {
                            this.canInputSlot.put(outcome.inPlaceOutput);
                            if (outcome.extraOutput != null) {
                                this.outputSlot.add(outcome.extraOutput);
                            }

                            output.setAmountMb(
                                    output.getAmountMb() - outcome.fluidChange.getAmountMb());
                        }
                    }
                } while (outcome != null && !output.isEmpty());
            }

            this.getOutputTank().fillMbUnchecked(output, false);
        }
    }

    @Override
    protected Collection<ItemStack> getOutput(Object output) {
        if (output instanceof ItemStack stack) {
            return Collections.singletonList(stack);
        }
        if (output instanceof Ic2FluidStack) {
            return Collections.emptyList();
        }
        if (output instanceof Output emptiedContainer) {
            return emptiedContainer.container();
        }
        return super.getOutput(output);
    }

    @Override
    public MachineRecipeResult<Object, Object, Object> getRecipeResult() {
        if (this.mode != Mode.EmptyLiquid && this.mode != Mode.BottleLiquid) {
            if (this.inputSlot.isEmpty()) {
                return null;
            }
        } else if (this.canInputSlot.isEmpty()) {
            return null;
        }

        MachineRecipeResult<Object, Object, Object> result = this.inputSlot.process();
        if (result == null) {
            return null;
        }

        if (!this.outputSlot.canAdd(this.getOutput(result.getOutput()))) {
            return null;
        }

        if (this.mode == Mode.EmptyLiquid) {
            Output output = (Output) result.getOutput();
            if (this.getOutputTank().fillMbUnchecked(output.fluid(), true)
                    != output.fluid().getAmountMb()) {
                return null;
            }
        } else if (this.mode == Mode.EnrichLiquid) {
            Ic2FluidStack output = ((Ic2FluidStack) result.getOutput()).copy();
            LiquidUtil.FluidOperationResult outcome;
            if (!this.canInputSlot.isEmpty()) {
                do {
                    outcome =
                            LiquidUtil.fillContainer(
                                    this.canInputSlot.get(),
                                    output,
                                    FluidContainerOutputMode.EmptyFullToOutput);
                    if (outcome != null) {
                        if (outcome.extraOutput != null
                                && !this.outputSlot.canAdd(outcome.extraOutput)) {
                            outcome = null;
                        } else {
                            output.setAmountMb(
                                    output.getAmountMb() - outcome.fluidChange.getAmountMb());
                        }
                    }
                } while (outcome != null && !output.isEmpty());
            }

            if (this.getOutputTank().fillMbUnchecked(output, true) != output.getAmountMb()) {
                return null;
            }
        }

        return result;
    }

    public Ic2FluidTank getInputTank() {
        return this.inputTank;
    }

    public Ic2FluidTank getOutputTank() {
        return this.outputTank;
    }

    @Override
    public List<String> getNetworkedFields() {
        List<String> ret = new ArrayList<>();
        ret.add("canInputSlot");
        ret.addAll(super.getNetworkedFields());
        return ret;
    }

    @Override
    public SoundEvent getLoopingSoundEvent() {
        return switch (this.mode) {
            case BottleSolid, BottleLiquid -> Ic2SoundEvents.MACHINE_CANNER_OPERATE;
            case EmptyLiquid -> Ic2SoundEvents.MACHINE_CANNER_REVERSE;
            default -> null;
        };
    }

    @Override
    public SoundEvent getInterruptSoundEvent() {
        return switch (this.mode) {
            case BottleSolid, BottleLiquid, EmptyLiquid -> Ic2SoundEvents.MACHINE_INTERRUPT1;
            default -> null;
        };
    }

    @Override
    public ContainerBase<TileEntityCanner> createServerScreenHandler(int syncId, Player player) {
        return new ContainerCanner(syncId, player.getInventory(), this);
    }

    @Override
    public ContainerBase<?> createClientScreenHandler(
            int syncId, Inventory inventory, GrowingBuffer data) {
        return new ContainerCanner(syncId, inventory, this);
    }

    @Override
    public void onNetworkUpdate(String field) {
        super.onNetworkUpdate(field);
        if (field.equals("mode")) {
            this.setMode(this.mode);
        }
    }

    @Override
    public void onNetworkEvent(Player player, int event) {
        if (event >= 0 && event < Mode.values.length) {
            this.setMode(Mode.values[event]);
        } else if (event == eventSwapTanks) {
            this.switchTanks();
        }
    }

    public Mode getMode() {
        return this.mode;
    }

    public void setMode(Mode mode) {
        // Network updates may already have assigned mode; appliedMode tracks the last applied
        // state.
        boolean modeChanged = this.appliedMode != mode;
        this.mode = mode;
        this.appliedMode = mode;
        switch (mode) {
            case BottleSolid -> this.canInputSlot.setOpType(InvSlotConsumableLiquid.OpType.None);
            case BottleLiquid -> this.canInputSlot.setOpType(InvSlotConsumableLiquid.OpType.Fill);
            case EmptyLiquid -> this.canInputSlot.setOpType(InvSlotConsumableLiquid.OpType.Drain);
            case EnrichLiquid -> this.canInputSlot.setOpType(InvSlotConsumableLiquid.OpType.Both);
        }

        if (modeChanged && IC2.sideProxy.isRendering()) {
            this.refreshModeDependentSounds();
        }
    }

    private void refreshModeDependentSounds() {
        if (this.loopingSound != null) {
            IC2.soundManager.removeSound(this, this.loopingSound);
            this.loopingSound = null;
        }

        if (this.interruptSound != null) {
            IC2.soundManager.removeSound(this, this.interruptSound);
            this.interruptSound = null;
        }

        this.initSound();
        if (this.shouldSoundActive() && this.loopingSound != null) {
            this.playLoopingSound(false);
        }
    }

    private void switchTanks() {
        if (this.progress != 0) {
            return;
        }
        Ic2FluidStack inputStack = this.inputTank.getFluidStack();
        Ic2FluidStack outputStack = this.outputTank.getFluidStack();
        this.inputTank.setFluidStack(outputStack);
        this.outputTank.setFluidStack(inputStack);
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(
                UpgradableProperty.Processing,
                UpgradableProperty.Transformer,
                UpgradableProperty.EnergyStorage,
                UpgradableProperty.ItemConsuming,
                UpgradableProperty.ItemProducing,
                UpgradableProperty.FluidConsuming,
                UpgradableProperty.FluidProducing);
    }

    public enum Mode {
        BottleSolid,
        EmptyLiquid,
        BottleLiquid,
        EnrichLiquid;

        public static final Mode[] values = values();
    }
}
