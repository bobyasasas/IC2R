package ic2.neoforge.entity;

import ic2.neoforge.registration.ModEntities;
import ic2.neoforge.registration.ModNuke;
import ic2.neoforge.world.Ic2Explosion;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Primed nuke charge (legacy ExplosiveEntity/NukeEntity): a three-hundred-tick fuse around the
 * configured blast power. With a radioactive payload it detonates as a Nuclear type explosion and
 * carries the radiation range; a wrench can still defuse it.
 */
public class NukeEntity extends ItntEntity {
    static final int DEFAULT_FUSE = 300;
    // Legacy NukeEntity blast profile: almost nothing drops, entities take the full blow.
    static final float DROP_RATE = 0.05F;
    static final float DAMAGE_VS_ENTITIES = 1.5F;
    private static final String TAG_RADIATION_RANGE = "radiation_range";

    private int radiationRange = 1;

    public NukeEntity(EntityType<? extends NukeEntity> type, Level level) {
        super(type, level);
        this.applyBlastProfile();
    }

    public NukeEntity(
            Level level,
            double x,
            double y,
            double z,
            float power,
            int radiationRange,
            @Nullable LivingEntity owner) {
        this(ModEntities.NUKE.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.explosionPower = power;
        this.radiationRange = radiationRange;
        if (owner != null) this.rememberOwner(owner);
    }

    private void applyBlastProfile() {
        this.setFuse(DEFAULT_FUSE);
        this.dropRate = DROP_RATE;
        this.damageVsEntities = DAMAGE_VS_ENTITIES;
        this.setBlockState(ModNuke.NUKE.get().defaultBlockState());
    }

    @Override
    protected void explode() {
        if (this.level() instanceof ServerLevel level
                && level.getGameRules().get(GameRules.TNT_EXPLODES)) {
            // Legacy ExplosiveEntity explodes as Nuclear only with a radioactive payload.
            Ic2Explosion.Type type =
                    this.radiationRange > 0 ? Ic2Explosion.Type.Nuclear : Ic2Explosion.Type.Normal;
            new Ic2Explosion(
                            level,
                            this,
                            this.getOwner(),
                            this.getX(),
                            this.getY(0.0625),
                            this.getZ(),
                            this.explosionPower,
                            this.dropRate,
                            this.damageVsEntities,
                            type,
                            this.radiationRange)
                    .doExplosion();
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof ic2.neoforge.item.WrenchTool) {
            if (this.level().isClientSide()) {
                return InteractionResult.PASS;
            }
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            this.discard();
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt(TAG_RADIATION_RANGE, this.radiationRange);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.radiationRange = input.getIntOr(TAG_RADIATION_RANGE, 1);
        this.setBlockState(ModNuke.NUKE.get().defaultBlockState());
    }

    @Override
    protected float maxSavedPower() {
        // The config power limit caps the computed blast; never clamp it again here.
        return Float.MAX_VALUE;
    }

    /** The radiation range decides Nuclear versus Normal and feeds the radiation pass. */
    public int getRadiationRange() {
        return this.radiationRange;
    }
}
