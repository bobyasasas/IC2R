package ic2.neoforge.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Legacy JetpackLogic: the shared flight physics for the classic and electric jetpacks. The legacy
 * engaged thrust while the client jump key was held (or in hover mode); a server-side port cannot
 * read keys, so the worn-piece {@code jetpack_active} flag (sneak + use on the held stack, the
 * quantum suit precedent) arms flight and sneaking stands in for the hover descent.
 */
public final class JetpackLogic {
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ItemStack chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        JetpackLike jetpack = null;
        if (chest.getItem() instanceof JetpackLike like && like.isJetpackActive(chest)) {
            jetpack = like;
        } else if (JetpackAttachmentHelper.hasAttached(chest)
                && JetpackAttachmentHelper.jetpackView().isJetpackActive(chest)) {
            jetpack = JetpackAttachmentHelper.jetpackView();
        }
        if (jetpack != null) {
            useJetpack(player, jetpack, chest);
        }
    }

    /** Legacy JetpackLogic.useJetpack with the per-item parameters from {@link JetpackLike}. */
    public static boolean useJetpack(Player player, JetpackLike jetpack, ItemStack stack) {
        double chargeLevel = jetpack.chargeLevel(stack);
        if (chargeLevel <= 0.0) {
            return false;
        }

        float power = jetpack.jetpackPower(stack);
        float dropPercentage = jetpack.dropPercentage();
        if (chargeLevel <= dropPercentage) {
            power = (float) (power * (chargeLevel / dropPercentage));
        }

        Vec3 motion = player.getDeltaMovement();
        double motionX = motion.x;
        double motionY = motion.y;
        double motionZ = motion.z;
        // Legacy thrusted forward while the forward key was down; sprinting is the stand-in.
        if (player.isSprinting()) {
            float forwarder = power * 0.15F * 2.0F;
            float yaw = player.getYRot() * (float) Math.PI / 180.0F;
            float thrust = 0.4F * forwarder * 0.02F;
            motionX += -Math.sin(yaw) * thrust;
            motionZ += Math.cos(yaw) * thrust;
        }

        int maxFlightHeight = (int) (player.level().getMaxY() / jetpack.worldHeightDivisor());
        double y = player.getY();
        if (y > maxFlightHeight - 25) {
            if (y > maxFlightHeight) {
                y = maxFlightHeight;
            }

            power = (float) (power * ((maxFlightHeight - y) / 25.0));
        }

        motionY = Math.min(motionY + power * 0.2F, 0.6F);
        // Legacy hover mode clamped the descent to the hover multiplier while sneaking.
        if (player.isShiftKeyDown() && motionY > -jetpack.hoverMultiplier()) {
            motionY = -jetpack.hoverMultiplier();
        }

        player.setDeltaMovement(motionX, motionY, motionZ);
        int consume = player.isShiftKeyDown() ? 1 : 2;
        if (!player.onGround()) {
            jetpack.drainFlightEnergy(stack, consume);
        }

        player.resetFallDistance();
        return true;
    }

    private JetpackLogic() {}
}
