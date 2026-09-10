package ic2.neoforge.test;

import ic2.neoforge.effect.RadiationEffect;
import ic2.neoforge.item.HazmatLike;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.NuclearReactorBlockEntity;
import ic2.neoforge.registration.ModArmor;
import ic2.neoforge.registration.ModEffects;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.Vec3;

final class RadiationTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    private static void dress(Mob mob) {
        mob.setItemSlot(
                EquipmentSlot.HEAD,
                new net.minecraft.world.item.ItemStack(ModArmor.HAZMAT_HELMET.get()));
        mob.setItemSlot(
                EquipmentSlot.CHEST,
                new net.minecraft.world.item.ItemStack(ModArmor.HAZMAT_CHESTPLATE.get()));
        mob.setItemSlot(
                EquipmentSlot.LEGS,
                new net.minecraft.world.item.ItemStack(ModArmor.HAZMAT_LEGGINGS.get()));
        mob.setItemSlot(
                EquipmentSlot.FEET,
                new net.minecraft.world.item.ItemStack(ModArmor.RUBBER_BOOTS.get()));
    }

    static void hazmatCompleteSetDetection(GameTestHelper helper) {
        Zombie dressed = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        dressed.setNoAi(true);
        dress(dressed);
        helper.assertTrue(
                HazmatLike.hasCompleteHazmat(dressed),
                "The four hazmat pieces count as a complete suit");
        dressed.setItemSlot(EquipmentSlot.FEET, net.minecraft.world.item.ItemStack.EMPTY);
        helper.assertTrue(
                !HazmatLike.hasCompleteHazmat(dressed),
                "The suit is incomplete without the rubber boots");
        Pig naked = helper.spawn(EntityType.PIG, new BlockPos(4, 1, 2));
        naked.setNoAi(true);
        helper.assertTrue(
                !HazmatLike.hasCompleteHazmat(naked),
                "An unbelted animal is never hazmat protected");
        helper.succeed();
    }

    static void radiationEffectDamagesHost(GameTestHelper helper) {
        helper.assertTrue(
                ModEffects.RADIATION.get().shouldApplyEffectTickThisTick(25, 0),
                "Amplifier zero ticks every 25 ticks");
        helper.assertTrue(
                !ModEffects.RADIATION.get().shouldApplyEffectTickThisTick(24, 0),
                "Amplifier zero skips the ticks in between");
        Pig victim = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 2));
        victim.setNoAi(true);
        RadiationEffect.applyTo(victim, 60, 0);
        helper.assertTrue(
                victim.hasEffect(ModEffects.RADIATION),
                "The radiation sickness effect applies to the host");
        float before = victim.getHealth();
        helper.startSequence()
                .thenIdle(30)
                .thenExecute(
                        () -> {
                            helper.assertTrue(
                                    victim.getHealth() < before,
                                    "The radiation effect damages the host over time, health="
                                            + victim.getHealth());
                        })
                .thenSucceed();
    }

    static void explosionRadiationAffectsUnprotectedMobs(GameTestHelper helper) {
        Zombie close = helper.spawn(EntityType.ZOMBIE, new BlockPos(7, 1, 8));
        Zombie far = helper.spawn(EntityType.ZOMBIE, new BlockPos(11, 1, 8));
        Zombie dressed = helper.spawn(EntityType.ZOMBIE, new BlockPos(9, 1, 6));
        close.setNoAi(true);
        far.setNoAi(true);
        dressed.setNoAi(true);
        dress(dressed);
        ServerLevel level = helper.getLevel();
        Vec3 center = Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(8, 1, 8)))
                .add(0.5, 0.5, 0.5);
        ic2.neoforge.world.Ic2Explosion.applyNuclearRadiation(level, center, 5);
        helper.assertTrue(
                close.hasEffect(MobEffects.HUNGER) && close.hasEffect(ModEffects.RADIATION),
                "A close unprotected mob gets hunger ("
                        + close.hasEffect(MobEffects.HUNGER)
                        + ") and radiation ("
                        + close.hasEffect(ModEffects.RADIATION)
                        + ") at dist "
                        + close.position().distanceTo(center));
        helper.assertTrue(
                far.hasEffect(MobEffects.HUNGER) && !far.hasEffect(ModEffects.RADIATION),
                "A distant mob only gets hunger past a third of the range");
        helper.assertTrue(
                !dressed.hasEffect(MobEffects.HUNGER) && !dressed.hasEffect(ModEffects.RADIATION),
                "A mob in a complete hazmat suit stays unaffected");
        helper.succeed();
    }

    static void reactorHeatRadiationDamages(GameTestHelper helper) {
        var reactor = ModMachines.block(MachineKind.NUCLEAR_REACTOR).defaultBlockState();
        helper.setBlock(POSITION, reactor);
        var core = helper.getBlockEntity(POSITION, NuclearReactorBlockEntity.class);
        core.setHeat(7000);
        Pig victim = helper.spawn(EntityType.PIG, new BlockPos(8, 8, 11));
        victim.setNoAi(true);
        float before = victim.getHealth();
        for (int tick = 0; tick < NuclearReactorBlockEntity.CYCLE_TICKS; tick++)
            core.serverTick(helper.getLevel());
        helper.assertTrue(
                victim.getHealth() < before || victim.invulnerableTime > 0,
                "The 70% heat threshold radiates nearby living entities, health="
                        + victim.getHealth()
                        + " invulnerable="
                        + victim.invulnerableTime);
        helper.succeed();
    }

    private RadiationTests() {}
}
