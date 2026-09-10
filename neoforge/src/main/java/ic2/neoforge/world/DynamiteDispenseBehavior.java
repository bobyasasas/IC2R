package ic2.neoforge.world;

import ic2.neoforge.entity.DynamiteEntity;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;

/**
 * Dispenser behaviour for both dynamite items (legacy BehaviorDynamiteDispense): ejects a primed
 * charge along the dispenser facing.
 */
public final class DynamiteDispenseBehavior extends DefaultDispenseItemBehavior {
    private final boolean sticky;

    public DynamiteDispenseBehavior(boolean sticky) {
        this.sticky = sticky;
    }

    @Override
    public ItemStack execute(net.minecraft.core.dispenser.BlockSource source, ItemStack stack) {
        Position position = DispenserBlock.getDispensePosition(source);
        Direction direction = source.state().getValue(DispenserBlock.FACING);
        DynamiteEntity entity =
                DynamiteEntity.at(
                        source.level(), position.x(), position.y(), position.z(), this.sticky);
        entity.shoot(
                direction.getStepX(),
                direction.getStepY() + 0.1F,
                direction.getStepZ(),
                1.1F,
                6.0F);
        source.level().addFreshEntity(entity);
        stack.shrink(1);
        return stack;
    }
}
