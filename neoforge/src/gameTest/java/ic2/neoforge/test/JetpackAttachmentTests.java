package ic2.neoforge.test;

import com.mojang.authlib.GameProfile;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.JetpackAttachmentHelper;
import ic2.neoforge.item.JetpackItem;
import ic2.neoforge.item.JetpackLogic;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.TankBlockEntity;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModArmor;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.UUID;

/**
 * The jetpack attachment chain: the shapeless three-part recipe with legacy blacklist and charge
 * transfer, flight on the shared JetpackLogic physics through the virtual jetpack view, the armor
 * break pop-back, and the classic jetpack's world-side biogas tank.
 */
final class JetpackAttachmentTests {
    private static RecipeHolder<Recipe<CraftingInput>> recipe(GameTestHelper helper, String path) {
        var key = ResourceKey.create(Registries.RECIPE, Identifier.parse(path));
        return (RecipeHolder<Recipe<CraftingInput>>)
                (RecipeHolder<?>) helper.getLevel().recipeAccess().byKey(key).orElseThrow();
    }

    private static Player fakePlayer(GameTestHelper helper, String name) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setInvulnerable(false);
        try {
            var respawn =
                    net.minecraft.server.network.ServerGamePacketListenerImpl.class
                            .getDeclaredField("waitingForRespawn");
            respawn.setAccessible(true);
            respawn.setBoolean(player.connection, false);
            player.connection.markClientLoaded();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot enable damage on the fake player", e);
        }
        return player;
    }

    private static ItemStack chargedJetpack(int charge) {
        var jetpack = ModArmor.JETPACK_ELECTRIC.toStack();
        ElectricItemEnergy.charge(jetpack, charge, 1, true, false);
        return jetpack;
    }

    private static ItemStack plate() {
        return ModItems.JETPACK_ATTACHMENT_PLATE.toStack();
    }

    private static CraftingInput grid(ItemStack... stacks) {
        var contents = new java.util.ArrayList<ItemStack>();
        for (var stack : stacks) contents.add(stack);
        while (contents.size() < 9) contents.add(ItemStack.EMPTY);
        return CraftingInput.of(3, 3, contents);
    }

    static void attachmentRecipe(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipe = recipe(helper, "ic2:jetpack_attachment");
        helper.assertTrue(recipe.value() != null, "The jetpack attachment recipe must load");

        // Electric armor draws the charge into its own battery.
        var electric = recipe.value().assemble(
                grid(ModArmor.NANO_CHESTPLATE.toStack(), chargedJetpack(12345), plate()));
        helper.assertTrue(
                electric.is(ModArmor.NANO_CHESTPLATE.get())
                        && JetpackAttachmentHelper.hasAttached(electric),
                "The attachment must mark the armor as carrying a jetpack");
        helper.assertTrue(
                ElectricItemEnergy.charge(electric) == 12345,
                "Electric armor must absorb the jetpack charge in its own battery (got "
                        + ElectricItemEnergy.charge(electric) + ")");

        // Plain armor stores the charge in the virtual component battery.
        var plain = recipe.value().assemble(
                grid(new ItemStack(Items.IRON_CHESTPLATE), chargedJetpack(12345), plate()));
        helper.assertTrue(
                plain.is(Items.IRON_CHESTPLATE) && JetpackAttachmentHelper.hasAttached(plain),
                "Plain armor must carry the attached flag too");
        helper.assertTrue(
                plain.getOrDefault(ModDataComponents.JETPACK_CHARGE.get(), 0.0) == 12345.0,
                "Plain armor must keep the charge in the component battery (got "
                        + plain.getOrDefault(ModDataComponents.JETPACK_CHARGE.get(), 0.0) + ")");

        // The legacy blacklist rejects the jetpacks, the quantum chestplate and the elytra.
        helper.assertTrue(
                recipe.value()
                        .assemble(
                                grid(ModArmor.QUANTUM_CHESTPLATE.toStack(), chargedJetpack(1), plate()))
                        .isEmpty(),
                "The quantum chestplate is blacklisted");
        helper.assertTrue(
                recipe.value()
                        .assemble(grid(ModArmor.JETPACK.toStack(), chargedJetpack(1), plate()))
                        .isEmpty(),
                "The classic jetpack is blacklisted as armor");
        helper.assertTrue(
                recipe.value()
                        .assemble(grid(new ItemStack(Items.ELYTRA), chargedJetpack(1), plate()))
                        .isEmpty(),
                "The elytra is blacklisted");

        // Duplicates, a missing plate and an already attached piece are all rejected.
        helper.assertTrue(
                recipe.value()
                        .assemble(
                                grid(
                                        ModArmor.NANO_CHESTPLATE.toStack(),
                                        chargedJetpack(1),
                                        chargedJetpack(1),
                                        plate()))
                        .isEmpty(),
                "Two jetpacks cannot attach");
        helper.assertTrue(
                recipe.value()
                        .assemble(
                                grid(
                                        ModArmor.NANO_CHESTPLATE.toStack(),
                                        new ItemStack(Items.DIAMOND_CHESTPLATE),
                                        chargedJetpack(1),
                                        plate()))
                        .isEmpty(),
                "Two chest pieces cannot attach");
        helper.assertTrue(
                recipe.value()
                        .assemble(grid(ModArmor.NANO_CHESTPLATE.toStack(), chargedJetpack(1)))
                        .isEmpty(),
                "The plate is mandatory");
        var already = plain.copy();
        helper.assertTrue(
                recipe.value()
                        .assemble(grid(already, chargedJetpack(1), plate()))
                        .isEmpty(),
                "An attached piece cannot attach twice");

        // The attachment plate itself assembles from its legacy pattern.
        var plateRecipe = recipe(helper, "ic2:shaped/jetpack_attachment_plate");
        var materials = ModItems.MATERIALS;
        var plateResult =
                plateRecipe.value()
                        .assemble(
                                CraftingInput.of(
                                        3,
                                        3,
                                        List.of(
                                                materials.get(MaterialDefinition.IRIDIUM_SHARD).get().getDefaultInstance(),
                                                materials.get(MaterialDefinition.ALLOY).get().getDefaultInstance(),
                                                materials.get(MaterialDefinition.IRIDIUM_SHARD).get().getDefaultInstance(),
                                                materials.get(MaterialDefinition.CARBON_PLATE).get().getDefaultInstance(),
                                                materials.get(MaterialDefinition.STEEL_PLATE).get().getDefaultInstance(),
                                                materials.get(MaterialDefinition.CARBON_PLATE).get().getDefaultInstance(),
                                                materials.get(MaterialDefinition.IRIDIUM_SHARD).get().getDefaultInstance(),
                                                materials.get(MaterialDefinition.ALLOY).get().getDefaultInstance(),
                                                materials.get(MaterialDefinition.IRIDIUM_SHARD).get().getDefaultInstance())));
        helper.assertTrue(
                plateResult.is(ModItems.JETPACK_ATTACHMENT_PLATE.get()),
                "The legacy pattern must craft the attachment plate");
        helper.succeed();
    }

    static void attachedFlight(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = fakePlayer(helper, "ic2-jet-att");
        var inside = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);

        var armor = new ItemStack(Items.IRON_CHESTPLATE);
        armor.set(ModDataComponents.JETPACK_ATTACHED.get(), true);
        armor.set(ModDataComponents.JETPACK_ACTIVE.get(), true);
        armor.set(ModDataComponents.JETPACK_CHARGE.get(), 20000.0);
        player.setItemSlot(EquipmentSlot.CHEST, armor);

        // Airborne thrust uses the electric jetpack's parameters through the virtual view.
        player.setOnGround(false);
        player.setDeltaMovement(0, 0, 0);
        JetpackLogic.onPlayerTick(new PlayerTickEvent.Post(player));
        float power = 0.7F;
        int maxFlightHeight = (int) (level.getMaxY() / 1.28F);
        double py = player.getY();
        if (py > maxFlightHeight - 25) {
            power = (float) (power * ((maxFlightHeight - Math.min(py, maxFlightHeight)) / 25.0));
        }
        double expectedVy = Math.min(power * 0.2F, 0.6F);
        double vy = player.getDeltaMovement().y;
        helper.assertTrue(
                Math.abs(vy - expectedVy) < 1e-6,
                "Attached thrust must match the electric jetpack (" + expectedVy + ") (got " + vy
                        + ")");
        helper.assertTrue(
                JetpackAttachmentHelper.charge(armor) == 19992.0,
                "Airborne thrust must pay 2 units plus the legacy +6 quirk (got "
                        + JetpackAttachmentHelper.charge(armor) + ")");

        // Sneaking hovers down at the hover multiplier for 1 unit.
        player.setShiftKeyDown(true);
        JetpackLogic.onPlayerTick(new PlayerTickEvent.Post(player));
        double hoverVy = player.getDeltaMovement().y;
        helper.assertTrue(
                Math.abs(hoverVy - -0.1) < 1e-6,
                "Sneaking must clamp the descent to -0.1 (got " + hoverVy + ")");
        helper.assertTrue(
                JetpackAttachmentHelper.charge(armor) == 19985.0,
                "Hovering must pay 1 unit plus the legacy +6 quirk (got "
                        + JetpackAttachmentHelper.charge(armor) + ")");

        // A grounded flight tick still thrusts but pays nothing.
        player.setShiftKeyDown(false);
        player.setOnGround(true);
        JetpackLogic.onPlayerTick(new PlayerTickEvent.Post(player));
        helper.assertTrue(
                JetpackAttachmentHelper.charge(armor) == 19985.0,
                "Grounded thrust must not drain");

        // Electric armor drains through its own battery with the same physics.
        var nano = ModArmor.NANO_CHESTPLATE.toStack();
        ElectricItemEnergy.charge(nano, 20000, Integer.MAX_VALUE, true, false);
        nano.set(ModDataComponents.JETPACK_ATTACHED.get(), true);
        nano.set(ModDataComponents.JETPACK_ACTIVE.get(), true);
        player.setItemSlot(EquipmentSlot.CHEST, nano);
        player.setOnGround(false);
        player.setDeltaMovement(0, 0, 0);
        JetpackLogic.onPlayerTick(new PlayerTickEvent.Post(player));
        helper.assertTrue(
                Math.abs(player.getDeltaMovement().y - expectedVy) < 1e-6,
                "Electric armor must thrust identically (got " + player.getDeltaMovement().y + ")");
        helper.assertTrue(
                ElectricItemEnergy.charge(nano) == 19992,
                "Electric armor must drain its own battery (got "
                        + ElectricItemEnergy.charge(nano) + ")");
        helper.succeed();
    }

    static void popBack(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = fakePlayer(helper, "ic2-jet-pop");

        var armor = new ItemStack(Items.IRON_CHESTPLATE);
        armor.set(ModDataComponents.JETPACK_ATTACHED.get(), true);
        armor.set(ModDataComponents.JETPACK_CHARGE.get(), 12345.0);
        player.setItemSlot(EquipmentSlot.CHEST, armor);

        // Damage records the piece; the next tick with an empty chest pops the jetpack back.
        JetpackAttachmentHelper.onIncomingDamage(
                new LivingIncomingDamageEvent(
                        player, new net.neoforged.neoforge.common.damagesource.DamageContainer(
                                level.damageSources().generic(), 5.0F)));
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        JetpackAttachmentHelper.onPlayerTick(new PlayerTickEvent.Post(player));
        var chest = player.getItemBySlot(EquipmentSlot.CHEST);
        helper.assertTrue(
                chest.is(ModArmor.JETPACK_ELECTRIC.get()),
                "A broken attached armor must pop the jetpack back (got " + chest + ")");
        helper.assertTrue(
                ElectricItemEnergy.charge(chest) == 12345,
                "The popped-back jetpack keeps the virtual charge (got "
                        + ElectricItemEnergy.charge(chest) + ")");
        helper.assertTrue(
                !JetpackAttachmentHelper.hasAttached(chest),
                "The popped-back stack is a plain jetpack");

        // Damage with the chest surviving must not replace the armor on the next tick.
        var survivor = new ItemStack(Items.IRON_CHESTPLATE);
        survivor.set(ModDataComponents.JETPACK_ATTACHED.get(), true);
        survivor.set(ModDataComponents.JETPACK_CHARGE.get(), 100.0);
        player.setItemSlot(EquipmentSlot.CHEST, survivor);
        JetpackAttachmentHelper.onIncomingDamage(
                new LivingIncomingDamageEvent(
                        player, new net.neoforged.neoforge.common.damagesource.DamageContainer(
                                level.damageSources().generic(), 1.0F)));
        JetpackAttachmentHelper.onPlayerTick(new PlayerTickEvent.Post(player));
        helper.assertTrue(
                player.getItemBySlot(EquipmentSlot.CHEST) == survivor,
                "Intact armor must stay equipped");

        // An unattached piece never arms the pop-back.
        var plain = new ItemStack(Items.IRON_CHESTPLATE);
        player.setItemSlot(EquipmentSlot.CHEST, plain);
        JetpackAttachmentHelper.onIncomingDamage(
                new LivingIncomingDamageEvent(
                        player, new net.neoforged.neoforge.common.damagesource.DamageContainer(
                                level.damageSources().generic(), 1.0F)));
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        JetpackAttachmentHelper.onPlayerTick(new PlayerTickEvent.Post(player));
        helper.assertTrue(
                player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(),
                "Unattached armor must not pop anything back");
        helper.succeed();
    }

    static void worldFill(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var biogas = FluidResource.of(JetpackItem.biogas());

        // The capability mirrors legacy StandardFluidItem: biogas only, 30,000 mB.
        var jetpack = ModArmor.JETPACK.toStack();
        var handler = ItemAccess.forStack(jetpack).getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.ITEM);
        helper.assertTrue(handler != null, "The classic jetpack must expose a fluid handler");
        helper.assertTrue(
                !handler.isValid(0, FluidResource.of(Fluids.WATER)),
                "The tank must reject everything but biogas");
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    handler.insert(0, biogas, 5000, transaction) == 5000,
                    "The handler must accept biogas");
            transaction.commit();
        }
        helper.assertTrue(
                JetpackItem.getContentsMb(jetpack) == 5000,
                "Committed fills must write the tank component (got "
                        + JetpackItem.getContentsMb(jetpack) + ")");
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    handler.extract(0, biogas, 2000, transaction) == 2000,
                    "The handler must drain biogas");
            transaction.commit();
        }
        helper.assertTrue(
                JetpackItem.getContentsMb(jetpack) == 3000,
                "Committed drains must update the tank component (got "
                        + JetpackItem.getContentsMb(jetpack) + ")");

        // End to end: right-clicking a tank with the jetpack fills it world-side.
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(pos, ModMachines.block(MachineKind.TANK).defaultBlockState());
        var tank = (TankBlockEntity) level.getBlockEntity(pos);
        try (var transaction = Transaction.openRoot()) {
            tank.tank().insert(0, biogas, 2000, transaction);
            transaction.commit();
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, ModArmor.JETPACK.toStack());
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var result = level.getBlockState(pos)
                .useItemOn(player.getItemInHand(InteractionHand.MAIN_HAND), level, player,
                        InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(
                result == InteractionResult.SUCCESS || result.consumesAction(),
                "The tank must accept the jetpack for filling (got " + result + ")");
        helper.assertTrue(
                JetpackItem.getContentsMb(player.getItemInHand(InteractionHand.MAIN_HAND)) == 2000,
                "Right-clicking the tank must move its biogas into the jetpack (got "
                        + JetpackItem.getContentsMb(player.getItemInHand(InteractionHand.MAIN_HAND))
                        + ")");
        helper.assertTrue(
                tank.tank().getAmountAsInt(0) == 0,
                "The tank must be drained by the fill (got " + tank.tank().getAmountAsInt(0) + ")");
        helper.succeed();
    }

    private JetpackAttachmentTests() {}
}
