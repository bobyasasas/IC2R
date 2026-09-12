package ic2.neoforge.entity;

import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.vehicle.boat.Boat;

/**
 * Legacy RubberBoatEntity: drops the broken rubber boat, 1.05× block speed factor and lava is
 * instantly lethal. Legacy brokenByFalling() = true (the vanilla 1.20.1 fall break); 26.1.2
 * removed boat fall breaking, so the check lives here.
 */
public class RubberBoatEntity extends Boat {
    public RubberBoatEntity(EntityType<? extends Boat> type, Level level) {
        super(type, level, () -> ModItems.MATERIALS.get(MaterialDefinition.BROKEN_RUBBER_BOAT).get());
    }

    @Override
    protected float getBlockSpeedFactor() {
        return super.getBlockSpeedFactor() * 1.05F;
    }

    @Override
    protected void checkFallDamage(
            double heightDifference, boolean onGround, BlockState state, BlockPos pos) {
        super.checkFallDamage(heightDifference, onGround, state, pos);
        if (onGround
                && this.fallDistance > 3.0F
                && this.level() instanceof ServerLevel serverLevel) {
            this.destroy(
                    serverLevel,
                    ModItems.MATERIALS.get(MaterialDefinition.BROKEN_RUBBER_BOAT).get());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel && this.isInLava()) {
            this.hurtServer(serverLevel, this.damageSources().lava(), Float.MAX_VALUE);
        }
    }
}
