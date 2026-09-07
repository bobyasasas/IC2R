package ic2.core.item;

import ic2.core.crop.TileEntityCrop;
import ic2.core.fluid.FluidHandler;
import ic2.core.fluid.Ic2FluidItem;
import ic2.core.fluid.Ic2FluidStack;
import ic2.core.fluid.StandardFluidItem;
import ic2.core.ref.Ic2Items;
import ic2.core.util.Ic2Tooltip;
import ic2.core.util.LiquidUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ItemClassicCell extends Ic2BucketItem implements Ic2FluidItem {
    private static final int CELL_CAPACITY_MB = 1000;
    private static final Map<Fluid, ItemClassicCell> instances = new IdentityHashMap<>();
    private final Fluid fluid;

    public ItemClassicCell(Properties settings, Fluid fluid) {
        super(
                Objects.requireNonNull(
                        fluid,
                        "ItemClassicCell fluid must not be null; use Fluids.EMPTY or a plain Item"),
                settings);
        this.fluid = fluid;
        if (fluid != Fluids.EMPTY) {
            instances.put(fluid, this);
        }
    }

    @Nullable
    public static ItemClassicCell getInstance(Fluid fluid) {
        return fluid != null && fluid != Fluids.EMPTY ? instances.get(fluid) : null;
    }

    public static ItemStack createFilledStack(Fluid fluid) {
        if (fluid != null && fluid != Fluids.EMPTY) {
            ItemClassicCell dedicated = instances.get(fluid);
            if (dedicated != null) {
                return new ItemStack(dedicated);
            }

            ItemStack stack = new ItemStack(Ic2Items.FACADE_CELL);
            StandardFluidItem.setFs(stack, Ic2FluidStack.create(fluid, 1000));
            return stack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public Component getName(@NotNull ItemStack stack) {
        if (this.fluid == Fluids.EMPTY) {
            Ic2FluidStack stored = StandardFluidItem.getFs(stack);
            if (stored != null && !stored.isEmpty()) {
                return Component.translatable(
                        "ic2.item.fluid_cell.filled", stored.getFluidDisplayName());
            }
        }

        return super.getName(stack);
    }

    @Override
    public Item getEmptiedBucketItem() {
        return Ic2Items.FACADE_CELL;
    }

    @Override
    protected Fluid getContainedFluid(ItemStack stack) {
        if (this.fluid != Fluids.EMPTY) {
            return this.fluid;
        }

        Ic2FluidStack stored = StandardFluidItem.getFs(stack);
        return stored != null && !stored.isEmpty() ? stored.getFluid() : Fluids.EMPTY;
    }

    @Override
    public List<Fluid> getDrainableFluidList() {
        return this.fluid == Fluids.EMPTY
                ? LiquidUtil.getAllFluidsSorted()
                : List.copyOf(instances.keySet());
    }

    @Override
    public Item getBucketItem(Fluid fluid) {
        ItemClassicCell cell = instances.get(fluid);
        return cell != null ? cell : Ic2Items.FACADE_CELL;
    }

    @Override
    public boolean bucketUseOnBlock(UseOnContext context) {
        BlockEntity be;
        return (this == Ic2Items.WATER_CELL || this == Ic2Items.WEED_EX_CELL)
                && (be = context.getLevel().getBlockEntity(context.getClickedPos()))
                        instanceof TileEntityCrop
                && this.useOnCrop(context.getItemInHand(), (TileEntityCrop) be, true);
    }

    @Override
    public ItemStack tryDrainFluid(LevelAccessor world, BlockPos pos, BlockState state) {
        if (this.fluid != Fluids.EMPTY) {
            return super.tryDrainFluid(world, pos, state);
        } else if (!(world instanceof Level level)) {
            return ItemStack.EMPTY;
        } else {
            Ic2FluidStack drained = FluidHandler.drainWorldFluid(state, level, pos, true);
            if (drained == null || drained.isEmpty()) {
                if (state.getBlock() instanceof LiquidBlock
                        && state.getValue(LiquidBlock.LEVEL) == 0) {
                    drained = Ic2FluidStack.create(state.getFluidState().getType(), 1000);
                } else {
                    if (!(state.getBlock() instanceof SimpleWaterloggedBlock)
                            || !state.getValue(BlockStateProperties.WATERLOGGED)) {
                        return this.tryDrain(world, pos, state);
                    }

                    drained = Ic2FluidStack.create(Fluids.WATER, 1000);
                }
            }

            if (drained.getAmountMb() < 1000) {
                return ItemStack.EMPTY;
            }

            ItemStack testStack = new ItemStack(this);
            if (this.fillMb(testStack, drained.copyWithAmountMb(1000), true, null) < 1000) {
                return ItemStack.EMPTY;
            }

            Ic2FluidStack actualDrain = FluidHandler.drainWorldFluid(state, level, pos, false);
            if (actualDrain == null || actualDrain.isEmpty()) {
                if (state.getBlock() instanceof LiquidBlock
                        && state.getValue(LiquidBlock.LEVEL) == 0) {
                    if (!level.isClientSide()) {
                        world.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
                    }

                    actualDrain = drained;
                } else {
                    if (!(state.getBlock() instanceof SimpleWaterloggedBlock)
                            || !state.getValue(BlockStateProperties.WATERLOGGED)) {
                        return ItemStack.EMPTY;
                    }

                    world.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, false), 3);
                    if (!state.canSurvive(world, pos)) {
                        world.destroyBlock(pos, true);
                    }

                    actualDrain = Ic2FluidStack.create(Fluids.WATER, 1000);
                }
            }

            if (actualDrain.getAmountMb() < 1000) {
                return ItemStack.EMPTY;
            }

            ItemStack result = new ItemStack(this);
            MutableObject<ItemStack> newStack = new MutableObject();
            this.fillMb(result, actualDrain.copyWithAmountMb(1000), false, newStack);
            ItemStack filled =
                    newStack.getValue() != null ? (ItemStack) newStack.getValue() : result;
            return filled.isEmpty() ? ItemStack.EMPTY : filled;
        }
    }

    public boolean useOnCrop(ItemStack stack, TileEntityCrop crop, boolean manual) {
        if (this == Ic2Items.WATER_CELL) {
            if (crop.getStorageWater() < 10) {
                crop.setStorageWater(10);
                return true;
            }
        } else if (this == Ic2Items.WEED_EX_CELL) {
            return crop.applyWeedEx(50, true, manual, false) > 0;
        }

        return false;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            Level world,
            @NotNull List<Component> tooltip,
            @NotNull TooltipFlag advanced) {
        if (this.fluid == Fluids.EMPTY) {
            Ic2FluidStack stored = StandardFluidItem.getFs(stack);
            if (stored != null && !stored.isEmpty()) {
                Ic2Tooltip.add(
                        tooltip,
                        Component.translatable(
                                "ic2.item.fluid_container.with_fluid",
                                stored.getFluidDisplayName(),
                                stored.getAmountMb()));
            }
        }
    }

    @Override
    public Ic2FluidStack getFluidStack(ItemStack stack) {
        if (this.fluid != Fluids.EMPTY) {
            return Ic2FluidStack.create(this.fluid, 1000);
        }

        Ic2FluidStack stored = StandardFluidItem.getFs(stack);
        return stored != null && !stored.isEmpty() ? stored : Ic2FluidStack.EMPTY;
    }

    @Override
    public int getCapacityMb(ItemStack stack) {
        return 1000;
    }

    @Override
    public Ic2FluidStack drainMb(
            ItemStack stack, int amount, boolean simulate, Mutable<ItemStack> newStack) {
        if (newStack != null) {
            newStack.setValue(stack);
        }

        if (stack.getCount() != 1) {
            throw new IllegalArgumentException("invalid stack size");
        }

        if (this.fluid == Fluids.EMPTY) {
            Ic2FluidStack stored = StandardFluidItem.getFs(stack);
            if (stored == null || stored.isEmpty()) {
                return Ic2FluidStack.EMPTY;
            }

            if (amount <= 0) {
                return Ic2FluidStack.EMPTY;
            }

            if (simulate) {
                return stored.copyWithAmountMb(Math.min(amount, 1000));
            }

            StandardFluidItem.setFs(stack, null);
            if (newStack != null) {
                newStack.setValue(stack);
            }

            return stored;
        } else {
            if (amount <= 0) {
                return Ic2FluidStack.EMPTY;
            }

            if (simulate) {
                return Ic2FluidStack.create(this.fluid, Math.min(amount, 1000));
            }

            stack.shrink(1);
            if (newStack != null) {
                newStack.setValue(new ItemStack(Ic2Items.FACADE_CELL));
            }

            return Ic2FluidStack.create(this.fluid, 1000);
        }
    }

    @Override
    public int drainMb(
            ItemStack stack, Ic2FluidStack drainFs, boolean simulate, Mutable<ItemStack> newStack) {
        if (newStack != null) {
            newStack.setValue(stack);
        }

        if (stack.getCount() != 1) {
            throw new IllegalArgumentException("invalid stack size");
        }

        int amount = drainFs.getAmountMb();
        if (amount <= 0) {
            return 0;
        }

        if (this.fluid == Fluids.EMPTY) {
            Ic2FluidStack stored = StandardFluidItem.getFs(stack);
            if (stored == null || stored.isEmpty() || !stored.hasExactFluid(drainFs)) {
                return 0;
            }

            if (simulate) {
                return Math.min(amount, 1000);
            }

            StandardFluidItem.setFs(stack, null);
            if (newStack != null) {
                newStack.setValue(stack);
            }

            return 1000;
        } else if (drainFs.hasExactFluid(this.fluid)) {
            if (simulate) {
                return Math.min(amount, 1000);
            }

            stack.shrink(1);
            if (newStack != null) {
                newStack.setValue(new ItemStack(Ic2Items.FACADE_CELL));
            }

            return 1000;
        } else {
            return 0;
        }
    }

    @Override
    public int fillMb(
            ItemStack stack, Ic2FluidStack fillFs, boolean simulate, Mutable<ItemStack> newStack) {
        if (newStack != null) {
            newStack.setValue(stack);
        }

        if (stack.getCount() != 1) {
            throw new IllegalArgumentException("invalid stack size");
        }

        if (fillFs.getAmountMb() < 1000) {
            return 0;
        }

        if (this.fluid != Fluids.EMPTY) {
            return 0;
        }

        ItemClassicCell newItem = instances.get(fillFs.getFluid());
        if (newItem != null && fillFs.hasExactFluid(newItem.fluid)) {
            if (!simulate) {
                stack.shrink(1);
            }

            if (newStack != null) {
                newStack.setValue(new ItemStack(newItem));
            }

            return 1000;
        } else {
            Ic2FluidStack stored = StandardFluidItem.getFs(stack);
            if (stored != null && !stored.isEmpty()) {
                return 0;
            }

            if (!simulate) {
                StandardFluidItem.setFs(stack, fillFs.copyWithAmountMb(1000));
            }

            if (newStack != null) {
                newStack.setValue(stack);
            }

            return 1000;
        }
    }
}
