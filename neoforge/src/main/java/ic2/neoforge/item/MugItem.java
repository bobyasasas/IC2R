package ic2.neoforge.item;

import ic2.neoforge.registration.ModItems;

import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Legacy ItemMug: stone mugs of coffee drunk via the drink use flow. Each sip tops both speed
 * and haste up to a per-type amplifier cap while extending their duration, and overdrinking
 * (amplifier total reaching 3+) backfires with nausea plus instant damage; every drink hands
 * back an empty mug. The mug is not vanilla food — the whole flow is hand rolled.
 */
public class MugItem extends Item {
    public enum MugType {
        EMPTY(0, 0),
        COLD_COFFEE(1, 600),
        DARK_COFFEE(5, 1200),
        COFFEE(6, 1200);

        /** Legacy switch: per-type amplifier cap and per-sip duration bonus (empty never drinks). */
        public final int maxAmplifier;
        public final int extraDuration;

        MugType(int maxAmplifier, int extraDuration) {
            this.maxAmplifier = maxAmplifier;
            this.extraDuration = extraDuration;
        }

        public boolean drinkable() {
            return this != EMPTY;
        }
    }

    private final MugType type;

    public MugItem(Properties properties, MugType type) {
        super(properties);
        this.type = type;
    }

    public MugType mugType() {
        return type;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player) || !type.drinkable()) {
            return stack;
        }
        int highest = amplifyEffect(player, MobEffects.SPEED, type.maxAmplifier, type.extraDuration);
        highest = Math.max(
                highest, amplifyEffect(player, MobEffects.HASTE, type.maxAmplifier, type.extraDuration));
        if (type == MugType.COFFEE) {
            highest -= 2;
        }
        if (highest >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, (highest - 2) * 200, 0));
            if (highest >= 4) {
                player.addEffect(new MobEffectInstance(MobEffects.INSTANT_DAMAGE, 1, highest - 3));
            }
        }
        return new ItemStack(ModItems.EMPTY_MUG.get());
    }

    private int amplifyEffect(Player player, Holder<MobEffect> effect, int maxAmplifier, int extraDuration) {
        MobEffectInstance existing = player.getEffect(effect);
        if (existing != null) {
            int amplifier = existing.getAmplifier();
            int duration = existing.getDuration();
            if (amplifier < maxAmplifier) {
                amplifier++;
            }
            duration += extraDuration;
            player.addEffect(new MobEffectInstance(effect, duration, amplifier));
            return amplifier;
        }
        player.addEffect(new MobEffectInstance(effect, 300, 0));
        return 1;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return type.drinkable() ? 32 : 0;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return type.drinkable() ? ItemUseAnimation.DRINK : ItemUseAnimation.NONE;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (type.drinkable()) {
            player.startUsingItem(hand);
        }
        return super.use(level, player, hand);
    }
}
