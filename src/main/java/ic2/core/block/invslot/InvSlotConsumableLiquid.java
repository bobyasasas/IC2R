package ic2.core.block.invslot;

import ic2.api.util.FluidContainerOutputMode;
import ic2.core.block.IInventorySlotHolder;
import ic2.core.block.invslot.InvSlot.Access;
import ic2.core.block.invslot.InvSlot.InvSide;
import ic2.core.fluid.Ic2FluidStack;
import ic2.core.fluid.Ic2FluidTank;
import ic2.core.util.LiquidUtil;
import ic2.core.util.StackUtil;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import org.apache.commons.lang3.mutable.MutableObject;

public class InvSlotConsumableLiquid extends InvSlotConsumable {
    private OpType opType;

    public InvSlotConsumableLiquid(IInventorySlotHolder<?> base, String name, int count) {
        this(base, name, Access.I, count, InvSide.TOP, OpType.Drain);
    }

    public InvSlotConsumableLiquid(
            IInventorySlotHolder<?> base,
            String name,
            Access access,
            int count,
            InvSide preferredSide,
            OpType opType) {
        super(base, name, access, count, preferredSide);
        this.opType = opType;
    }

    @Override
    public boolean accepts(ItemStack stack) {
        if (StackUtil.isEmpty(stack)) {
            return false;
        }

        if (!LiquidUtil.isFluidContainer(stack)) {
            return false;
        }

        if (this.opType == OpType.Drain || this.opType == OpType.Both) {
            Ic2FluidStack containedFluid = Ic2FluidStack.get(stack);
            if (containedFluid != null
                    && !containedFluid.isEmpty()
                    && this.acceptsLiquid(containedFluid.getFluid())) {
                return true;
            }
        }

        return (this.opType == OpType.Fill || this.opType == OpType.Both)
                && LiquidUtil.isFillableFluidContainer(stack, this.getPossibleFluids());
    }

    public Ic2FluidStack drain(
            Fluid fluid, int maxAmount, MutableObject<ItemStack> output, boolean simulate) {
        output.setValue(null);
        if (fluid != null && !this.acceptsLiquid(fluid)) {
            return null;
        }

        if (this.opType != OpType.Drain && this.opType != OpType.Both) {
            return null;
        }

        ItemStack stack = this.get();
        if (StackUtil.isEmpty(stack)) {
            return null;
        }

        LiquidUtil.FluidOperationResult result =
                LiquidUtil.drainContainer(
                        stack, fluid, maxAmount, FluidContainerOutputMode.EmptyFullToOutput);
        if (result == null) {
            return null;
        }

        if (fluid == null && !this.acceptsLiquid(result.fluidChange.getFluid())) {
            return null;
        }

        output.setValue(result.extraOutput);
        if (!simulate) {
            this.put(result.inPlaceOutput);
        }

        return result.fluidChange;
    }

    public int fill(
            Ic2FluidStack containedFluid, MutableObject<ItemStack> output, boolean simulate) {
        output.setValue(null);
        if (containedFluid == null || containedFluid.isEmpty()) {
            return 0;
        }

        if (this.opType != OpType.Fill && this.opType != OpType.Both) {
            return 0;
        }

        ItemStack stack = this.get();
        if (StackUtil.isEmpty(stack)) {
            return 0;
        }

        LiquidUtil.FluidOperationResult result =
                LiquidUtil.fillContainer(
                        stack, containedFluid, FluidContainerOutputMode.EmptyFullToOutput);
        if (result == null) {
            return 0;
        }

        output.setValue(result.extraOutput);
        if (!simulate) {
            this.put(result.inPlaceOutput);
        }

        return result.fluidChange.getAmountMb();
    }

    public boolean transferToTank(
            Ic2FluidTank tank, MutableObject<ItemStack> output, boolean simulate) {
        if (this.isEmpty()) {
            return false;
        }

        int space = tank.getCapacity();
        Fluid fluidRequired = null;
        Ic2FluidStack tankFluid = tank.getFluidStack();
        if (tankFluid != null) {
            space -= tankFluid.getAmountMb();
            fluidRequired = tankFluid.getFluid();
        }

        if (space <= 0) {
            return false;
        }

        ItemStack stack = this.get();
        LiquidUtil.FluidOperationResult result =
                LiquidUtil.drainContainerComplete(
                        stack, fluidRequired, space, FluidContainerOutputMode.EmptyFullToOutput);
        if (result == null) {
            return false;
        }

        int amount = tank.fillMb(result.fluidChange, simulate);
        if (amount > 0 && amount == result.fluidChange.getAmountMb()) {
            if (!simulate) {
                result =
                        LiquidUtil.drainContainerComplete(
                                stack,
                                fluidRequired,
                                space,
                                FluidContainerOutputMode.EmptyFullToOutput);
                if (result == null) {
                    return false;
                }

                output.setValue(result.extraOutput);
                this.put(result.inPlaceOutput);
            } else {
                output.setValue(result.extraOutput);
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean transferFromTank(
            Ic2FluidTank tank, MutableObject<ItemStack> output, boolean simulate) {
        if (!this.isEmpty() && !tank.isEmpty()) {
            Ic2FluidStack tankFluid = tank.getFluidStack();
            if (tankFluid != null && !tankFluid.isEmpty()) {
                ItemStack stack = this.get();
                LiquidUtil.FluidOperationResult result =
                        LiquidUtil.fillContainerComplete(
                                stack,
                                tankFluid.copy(),
                                FluidContainerOutputMode.EmptyFullToOutput);
                if (result == null) {
                    return false;
                }

                if (!simulate) {
                    result =
                            LiquidUtil.fillContainerComplete(
                                    stack,
                                    tankFluid.copy(),
                                    FluidContainerOutputMode.EmptyFullToOutput);
                    if (result == null) {
                        return false;
                    }

                    tank.drainMb(result.fluidChange.getAmountMb(), false);
                    output.setValue(result.extraOutput);
                    this.put(result.inPlaceOutput);
                } else {
                    output.setValue(result.extraOutput);
                }

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean processIntoTank(Ic2FluidTank tank, InvSlotOutput outputSlot) {
        if (this.isEmpty()) {
            return false;
        }

        MutableObject<ItemStack> output = new MutableObject();
        boolean wasChange = false;
        if (this.transferToTank(tank, output, true)
                && (StackUtil.isEmpty((ItemStack) output.getValue())
                        || outputSlot.canAdd((ItemStack) output.getValue()))) {
            wasChange = this.transferToTank(tank, output, false);
            if (!StackUtil.isEmpty((ItemStack) output.getValue())) {
                outputSlot.add((ItemStack) output.getValue());
            }
        }

        return wasChange;
    }

    public boolean processFromTank(Ic2FluidTank tank, InvSlotOutput outputSlot) {
        if (!this.isEmpty() && !tank.isEmpty()) {
            MutableObject<ItemStack> output = new MutableObject();
            boolean wasChange = false;
            if (this.transferFromTank(tank, output, true)
                    && (StackUtil.isEmpty((ItemStack) output.getValue())
                            || outputSlot.canAdd((ItemStack) output.getValue()))) {
                wasChange = this.transferFromTank(tank, output, false);
                if (!StackUtil.isEmpty((ItemStack) output.getValue())) {
                    outputSlot.add((ItemStack) output.getValue());
                }
            }

            return wasChange;
        } else {
            return false;
        }
    }

    public void setOpType(OpType opType) {
        this.opType = opType;
    }

    protected boolean acceptsLiquid(Fluid fluid) {
        return true;
    }

    protected Iterable<Fluid> getPossibleFluids() {
        return null;
    }

    public enum OpType {
        Drain,
        Fill,
        Both,
        None;
    }
}
