package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.machine.MachineBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class ElectricWrenchItem extends ElectricItem implements WrenchTool {
    public ElectricWrenchItem(Properties properties) {
        super(properties, new ElectricItemSpec(12000, 250, 1, false));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return WrenchTool.rotate(context);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return ElectricItemEnergy.charge(stack) >= 100
                && (state.getBlock() instanceof MachineBlock
                        || super.isCorrectToolForDrops(stack, state));
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return isCorrectToolForDrops(stack, state) ? 6 : 1;
    }

    @Override
    public boolean mineBlock(
            ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide()) ElectricItemEnergy.discharge(stack, 100, 1, true, false, false);
        return true;
    }
}
