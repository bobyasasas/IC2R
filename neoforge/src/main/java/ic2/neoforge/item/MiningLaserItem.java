package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.entity.LaserBulletEntity;
import ic2.neoforge.registration.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Legacy ItemToolMiningLaser: eight firing modes with per-shot EU costs. Legacy switched modes
 * with the client mode-switch key; the port uses sneak + use like the other electric tools.
 * Aiming a horizontal/3x3 shot at a block goes through {@link #useOn}, the air-shot through
 * {@link #use}. Legacy long-range mode fell through its switch into the super-heat branch and
 * fires a second smelting ray; that behaviour is ported verbatim.
 */
public class MiningLaserItem extends ElectricItem {
    public static final int MODE_COUNT = 8;

    // Per-shot EU by mode: mining, low-focus, long-range, horizontal, super-heat, scatter,
    // explosive, 3x3. Horizontal aiming bills 3000 EU per volley in useOn instead.
    private static final double[] SHOT_COSTS =
            {1250.0, 100.0, 5000.0, 0.0, 2500.0, 10000.0, 5000.0, 7500.0};
    private static final double AIMED_SHOT_COST = 3000.0;

    public MiningLaserItem(Properties properties, ElectricItemSpec specification) {
        super(properties, specification);
    }

    public static int modeOf(ItemStack stack) {
        Integer mode = stack.get(ModDataComponents.LASER_MODE);
        return mode == null ? 0 : mode;
    }

    private static String modeKey(int mode) {
        return switch (mode) {
            case 0 -> "item.ic2.mining_laser.tooltip.mode.mining";
            case 1 -> "item.ic2.mining_laser.tooltip.mode.lowFocus";
            case 2 -> "item.ic2.mining_laser.tooltip.mode.longRange";
            case 3 -> "item.ic2.mining_laser.tooltip.mode.horizontal";
            case 4 -> "item.ic2.mining_laser.tooltip.mode.superHeat";
            case 5 -> "item.ic2.mining_laser.tooltip.mode.scatter";
            case 6 -> "item.ic2.mining_laser.tooltip.mode.explosive";
            case 7 -> "item.ic2.mining_laser.tooltip.mode.3x3";
            default -> throw new IllegalArgumentException("No such mode: " + mode);
        };
    }

    // Legacy onNetworkEvent mapping: which loopback sound each fired mode plays.
    private static SoundEvent shotSound(int mode) {
        return switch (mode) {
            case 1 -> ModSounds.ITEM_LASER_LOW_FOCUS.get();
            case 2 -> ModSounds.ITEM_LASER_LONG_RANGE.get();
            case 5, 7 -> ModSounds.ITEM_LASER_SCATTER.get();
            case 6 -> ModSounds.ITEM_LASER_EXPLOSIVE.get();
            default -> ModSounds.ITEM_LASER_SHOOT.get();
        };
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            int mode = (modeOf(stack) + 1) % MODE_COUNT;
            stack.set(ModDataComponents.LASER_MODE, mode);
            if (level instanceof ServerLevel) {
                player.sendSystemMessage(
                        Component.translatable(
                                "item.ic2.mining_laser.tooltip.mode",
                                Component.translatable(modeKey(mode))));
            }
            return InteractionResult.SUCCESS;
        }

        if (level instanceof ServerLevel serverLevel) {
            int mode = modeOf(stack);
            double cost = SHOT_COSTS[mode];
            if (!ElectricItemEnergy.use(stack, cost, player)) {
                return InteractionResult.FAIL;
            }

            switch (mode) {
                case 0 ->
                    this.shootLaser(
                            serverLevel,
                            player,
                            Float.POSITIVE_INFINITY,
                            5.0F,
                            Integer.MAX_VALUE,
                            false,
                            false);
                case 1 -> this.shootLaser(serverLevel, player, 4.0F, 5.0F, 1, false, false);
                case 2 -> {
                    // Legacy case 2 fell through into the 3/4/7 branch and also fired the
                    // smelting ray; ported verbatim.
                    this.shootLaser(
                            serverLevel,
                            player,
                            Float.POSITIVE_INFINITY,
                            20.0F,
                            Integer.MAX_VALUE,
                            false,
                            false);
                    this.shootLaser(
                            serverLevel,
                            player,
                            Float.POSITIVE_INFINITY,
                            8.0F,
                            Integer.MAX_VALUE,
                            false,
                            true);
                }
                case 3, 4, 7 ->
                    this.shootLaser(
                            serverLevel,
                            player,
                            Float.POSITIVE_INFINITY,
                            8.0F,
                            Integer.MAX_VALUE,
                            false,
                            true);
                case 5 -> this.shootScatter(serverLevel, player);
                case 6 ->
                    this.shootLaser(
                            serverLevel,
                            player,
                            Float.POSITIVE_INFINITY,
                            12.0F,
                            Integer.MAX_VALUE,
                            true,
                            false);
            }

            this.playShotSound(serverLevel, player, shotSound(mode));
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide() || player == null) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        if (!player.isSecondaryUseActive()) {
            int mode = modeOf(stack);
            if (mode == 3 || mode == 7) {
                this.shootAimed(mode, stack, context, player);
            }
        }

        return InteractionResult.FAIL;
    }

    private void shootAimed(int mode, ItemStack stack, UseOnContext context, Player player) {
        ServerLevel level = (ServerLevel) context.getLevel();
        Vec3 look = player.getLookAngle();
        if (Math.abs(look.y) < 1.0 / Math.sqrt(2.0)) {
            if (!ElectricItemEnergy.use(stack, AIMED_SHOT_COST, player)) {
                return;
            }

            Vec3 dir = new Vec3(look.x, 0.0, look.z).normalize();
            Vec3 start =
                    new Vec3(
                                    player.getEyePosition().x,
                                    context.getClickedPos().getY() + 0.5,
                                    player.getEyePosition().z)
                            .add(dir.scale(0.2));
            this.shootLaser(
                    level,
                    start,
                    dir,
                    player,
                    Float.POSITIVE_INFINITY,
                    5.0F,
                    Integer.MAX_VALUE,
                    false,
                    false);
            if (mode == 7) {
                this.shootLaser(
                        level,
                        start.add(0.0, -1.0, 0.0),
                        dir,
                        player,
                        Float.POSITIVE_INFINITY,
                        5.0F,
                        Integer.MAX_VALUE,
                        false,
                        false);
                this.shootLaser(
                        level,
                        start.add(0.0, 1.0, 0.0),
                        dir,
                        player,
                        Float.POSITIVE_INFINITY,
                        5.0F,
                        Integer.MAX_VALUE,
                        false,
                        false);
                Direction facing = player.getDirection();
                if (facing == Direction.SOUTH || facing == Direction.NORTH) {
                    for (int dy = -1; dy <= 1; dy++) {
                        this.shootLaser(
                                level,
                                start.add(-1.0, dy, 0.0),
                                dir,
                                player,
                                Float.POSITIVE_INFINITY,
                                5.0F,
                                Integer.MAX_VALUE,
                                false,
                                false);
                        this.shootLaser(
                                level,
                                start.add(1.0, dy, 0.0),
                                dir,
                                player,
                                Float.POSITIVE_INFINITY,
                                5.0F,
                                Integer.MAX_VALUE,
                                false,
                                false);
                    }
                }

                if (facing == Direction.EAST || facing == Direction.WEST) {
                    for (int dy = -1; dy <= 1; dy++) {
                        this.shootLaser(
                                level,
                                start.add(0.0, dy, -1.0),
                                dir,
                                player,
                                Float.POSITIVE_INFINITY,
                                5.0F,
                                Integer.MAX_VALUE,
                                false,
                                false);
                        this.shootLaser(
                                level,
                                start.add(0.0, dy, 1.0),
                                dir,
                                player,
                                Float.POSITIVE_INFINITY,
                                5.0F,
                                Integer.MAX_VALUE,
                                false,
                                false);
                    }
                }
            }

            this.playShotSound(
                    level,
                    player,
                    mode == 7
                            ? ModSounds.ITEM_LASER_SCATTER.get()
                            : ModSounds.ITEM_LASER_SHOOT.get());
        } else if (mode == 7) {
            if (!ElectricItemEnergy.use(stack, AIMED_SHOT_COST, player)) {
                return;
            }

            Vec3 dir = new Vec3(0.0, look.y, 0.0).normalize();
            BlockPos clicked = context.getClickedPos();
            Vec3 start =
                    new Vec3(clicked.getX() + 0.5, player.getEyePosition().y, clicked.getZ() + 0.5)
                            .add(dir.scale(0.2));
            this.shootLaser(
                    level,
                    start,
                    dir,
                    player,
                    Float.POSITIVE_INFINITY,
                    5.0F,
                    Integer.MAX_VALUE,
                    false,
                    false);
            for (double[] offset :
                    new double[][] {
                        {1, 0, 0}, {-1, 0, 0}, {1, 0, 1}, {-1, 0, -1},
                        {1, 0, -1}, {-1, 0, 1}, {0, 0, 1}, {0, 0, -1}
                    }) {
                this.shootLaser(
                        level,
                        start.add(offset[0], offset[1], offset[2]),
                        dir,
                        player,
                        Float.POSITIVE_INFINITY,
                        5.0F,
                        Integer.MAX_VALUE,
                        false,
                        false);
            }

            this.playShotSound(level, player, ModSounds.ITEM_LASER_SCATTER.get());
        } else {
            player.sendSystemMessage(Component.literal("Mining laser aiming angle too steep"));
        }
    }

    private void shootScatter(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
        if (right.lengthSqr() < 1.0e-4) {
            double angle = Math.toRadians(player.getYRot()) - (Math.PI / 2);
            right = new Vec3(Math.sin(angle), 0.0, -Math.cos(angle));
        } else {
            right = right.normalize();
        }

        Vec3 up = right.cross(look);
        Vec3 reach = look.scale(8.0);
        for (int r = -2; r <= 2; r++) {
            for (int u = -2; u <= 2; u++) {
                Vec3 dir = reach.add(right.scale(r)).add(up.scale(u)).normalize();
                this.shootLaser(
                        level,
                        dir,
                        player,
                        Float.POSITIVE_INFINITY,
                        12.0F,
                        Integer.MAX_VALUE,
                        false,
                        false);
            }
        }
    }

    public boolean shootLaser(
            ServerLevel level,
            LivingEntity owner,
            float range,
            float power,
            int blockBreaks,
            boolean explosive,
            boolean smelt) {
        return this.shootLaser(
                level, owner.getLookAngle(), owner, range, power, blockBreaks, explosive, smelt);
    }

    public boolean shootLaser(
            ServerLevel level,
            Vec3 dir,
            LivingEntity owner,
            float range,
            float power,
            int blockBreaks,
            boolean explosive,
            boolean smelt) {
        Vec3 start = owner.getEyePosition().add(dir.scale(0.2));
        return this.shootLaser(
                level, start, dir, owner, range, power, blockBreaks, explosive, smelt);
    }

    public boolean shootLaser(
            ServerLevel level,
            Vec3 start,
            Vec3 dir,
            LivingEntity owner,
            float range,
            float power,
            int blockBreaks,
            boolean explosive,
            boolean smelt) {
        LaserBulletEntity laser =
                new LaserBulletEntity(level, start, owner, range, power, blockBreaks, explosive);
        laser.init(owner, range, power, blockBreaks, explosive, smelt, true);
        laser.shoot(dir.x, dir.y, dir.z, 3.0F, 1.0F);
        Vec3 shooterMotion = owner.getDeltaMovement();
        laser.setDeltaMovement(
                laser.getDeltaMovement()
                        .add(
                                shooterMotion.x,
                                owner.onGround() ? 0.0 : shooterMotion.y,
                                shooterMotion.z));
        level.addFreshEntity(laser);
        return true;
    }

    private void playShotSound(ServerLevel level, Player player, SoundEvent sound) {
        level.playSound(
                null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

}
