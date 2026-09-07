package ic2.neoforge.world;

import ic2.neoforge.registration.ModRubberBuilding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Retains ic2:sign while using native double-sided text, edit locks and wax persistence. */
public final class RubberSignBlockEntity extends SignBlockEntity {
    public RubberSignBlockEntity(BlockPos pos, BlockState state) {
        super(ModRubberBuilding.SIGN_ENTITY.get(), pos, state);
    }
}
