package ic2.neoforge.item;

import net.minecraft.world.item.ItemStack;

/**
 * Legacy IJetpack: the per-item flight parameters consumed by {@link JetpackLogic}. The charge
 * level is a 0..1 ratio, so the electric jetpack reports EU over capacity and the biogas jetpack
 * reports millibuckets over tank size.
 */
public interface JetpackLike {
    float jetpackPower(ItemStack stack);

    /** Fraction of the tank below which the thrust ramps down linearly. */
    float dropPercentage();

    float hoverMultiplier();

    float worldHeightDivisor();

    double chargeLevel(ItemStack stack);

    /** Legacy drainEnergy: one flight tick costs this much of the tank (1–2 units). */
    void drainFlightEnergy(ItemStack stack, int amount);

    boolean isJetpackActive(ItemStack stack);
}
