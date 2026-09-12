package ic2.neoforge.entity;

import ic2.neoforge.registration.ModItems;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;

/**
 * Legacy CarbonBoatEntity: drops itself, 1.15× block speed factor, survives falls (legacy
 * brokenByFalling() = false, which is 26.1.2's default behaviour) and lava is instantly lethal.
 */
public class CarbonBoatEntity extends Boat {
    public CarbonBoatEntity(EntityType<? extends Boat> type, Level level) {
        super(type, level, () -> ModItems.CARBON_BOAT.get());
    }

    @Override
    protected float getBlockSpeedFactor() {
        return super.getBlockSpeedFactor() * 1.15F;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel && this.isInLava()) {
            this.hurtServer(serverLevel, this.damageSources().lava(), Float.MAX_VALUE);
        }
    }
}
