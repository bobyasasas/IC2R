package ic2.neoforge.test;

import ic2.neoforge.item.MugItem;
import ic2.neoforge.registration.ModItems;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * Legacy ItemMug: each sip tops speed and haste (amplifier capped per type, duration extended
 * by a per-type bonus, a first sip granting 300 ticks), coffee over-amplification backfires
 * into nausea plus instant damage, and every drink hands back an empty mug.
 */
final class MugTests {
    static void mugDrinkAppliesEffectsAndReturnsEmpty(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        ItemStack first = new ItemStack(ModItems.COLD_COFFEE_MUG.get());
        ItemStack result = first.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(
                result.getItem() == ModItems.EMPTY_MUG.get(),
                "A finished mug must hand back an empty mug");
        helper.assertTrue(
                effectDuration(player, MobEffects.SPEED) == 300 && effectAmplifier(player, MobEffects.SPEED) == 0,
                "A first sip must grant 300 ticks of speed at amplifier 0");
        helper.assertTrue(
                effectDuration(player, MobEffects.HASTE) == 300 && effectAmplifier(player, MobEffects.HASTE) == 0,
                "A first sip must grant 300 ticks of haste at amplifier 0");

        ItemStack second = new ItemStack(ModItems.COLD_COFFEE_MUG.get());
        second.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(
                effectAmplifier(player, MobEffects.SPEED) == 1
                        && effectDuration(player, MobEffects.SPEED) == 900,
                "A second cold sip must cap at amplifier 1 and extend by 600 ticks");
        helper.assertTrue(
                effectDuration(player, MobEffects.HASTE) == 900,
                "Haste must stack durations in step with speed");
        helper.succeed();
    }

    static void mugUseDurationAndAnimation(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var empty = ModItems.EMPTY_MUG.get();
        var coffee = ModItems.COFFEE_MUG.get();

        helper.assertTrue(
                empty.getUseDuration(new ItemStack(empty), player) == 0,
                "An empty mug must not be drinkable");
        helper.assertTrue(
                empty.getUseAnimation(new ItemStack(empty)) == ItemUseAnimation.NONE,
                "An empty mug must show no use animation");
        helper.assertTrue(
                coffee.getUseDuration(new ItemStack(coffee), player) == 32,
                "A filled mug must take 32 ticks to drink");
        helper.assertTrue(
                coffee.getUseAnimation(new ItemStack(coffee)) == ItemUseAnimation.DRINK,
                "A filled mug must use the drink animation");

        var result = empty.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                !result.consumesAction(), "Using an empty mug must pass without starting use");
        ItemStack filled = new ItemStack(coffee);
        player.setItemInHand(InteractionHand.MAIN_HAND, filled);
        coffee.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                player.isUsingItem(), "Using a filled mug must start the drinking flow");
        helper.assertTrue(
                player.getUseItem() == filled, "The drinking flow must use the held mug");
        helper.succeed();
    }

    static void mugOverdrinkBackfire(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var darkCoffee = ModItems.DARK_COFFEE_MUG.get();

        // Sips one to three: amplifier 0..2, durations 300/1500/2700, no backfire below 3.
        for (int sip = 1; sip <= 3; sip++) {
            new ItemStack(darkCoffee).finishUsingItem(helper.getLevel(), player);
        }
        helper.assertTrue(
                effectAmplifier(player, MobEffects.SPEED) == 2
                        && effectDuration(player, MobEffects.SPEED) == 2700,
                "Three dark sips must reach amplifier 2 with stacked durations");
        helper.assertTrue(
                player.getEffect(MobEffects.NAUSEA) == null,
                "Below amplifier 3 there must be no nausea backfire");

        // Sip four: amplifier 3 => nausea (3-2)*200 = 200 ticks, still no instant damage.
        ItemStack fourth = new ItemStack(darkCoffee);
        ItemStack result = fourth.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(
                result.getItem() == ModItems.EMPTY_MUG.get(),
                "Even a backfiring sip must return an empty mug");
        helper.assertTrue(
                player.getEffect(MobEffects.NAUSEA) != null
                        && player.getEffect(MobEffects.NAUSEA).getDuration() == 200,
                "Amplifier 3 must trigger 200 ticks of nausea");

        // Sip five: amplifier 4 => nausea 400 ticks plus an instant damage effect on the
        // player (mock players never tick, so the effect stays queued instead of resolving).
        new ItemStack(darkCoffee).finishUsingItem(helper.getLevel(), player);
        MobEffectInstance nausea = player.getEffect(MobEffects.NAUSEA);
        helper.assertTrue(
                nausea != null && nausea.getDuration() == 400,
                "Amplifier 4 must refresh nausea to 400 ticks");
        helper.assertTrue(
                effectAmplifier(player, MobEffects.SPEED) == 4,
                "Dark coffee caps its amplifier at five");
        helper.assertTrue(
                player.getEffect(MobEffects.INSTANT_DAMAGE) != null,
                "Amplifier 4 must add the instant damage backfire");
        helper.succeed();
    }

    private static int effectDuration(Player player, Holder<MobEffect> effect) {
        MobEffectInstance instance = player.getEffect(effect);
        return instance == null ? -1 : instance.getDuration();
    }

    private static int effectAmplifier(Player player, Holder<MobEffect> effect) {
        MobEffectInstance instance = player.getEffect(effect);
        return instance == null ? -1 : instance.getAmplifier();
    }
}
