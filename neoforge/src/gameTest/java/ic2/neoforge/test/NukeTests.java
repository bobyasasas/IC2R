package ic2.neoforge.test;

import ic2.neoforge.entity.NukeEntity;
import ic2.neoforge.machine.NukeBlockEntity;
import ic2.neoforge.menu.NukeMenu;
import ic2.neoforge.registration.ModEntities;
import ic2.neoforge.registration.ModExplosives;
import ic2.neoforge.registration.ModNuke;
import ic2.neoforge.registration.ModReactorItems;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;

final class NukeTests {
    private static final BlockPos ORIGIN = new BlockPos(17, 17, 17);

    static void powerFormulaFollowsLegacyScaling(GameTestHelper helper) {
        helper.setBlock(ORIGIN, ModNuke.NUKE.get());
        NukeBlockEntity nuke = helper.getBlockEntity(ORIGIN, NukeBlockEntity.class);
        helper.assertTrue(
                nuke.getNukeExplosivePower() == -1.0F,
                "An unloaded nuke refuses to compute a blast power");
        load(nuke, 1, 0, ModReactorItems.URANIUM_238.get());
        helper.assertTrue(
                nuke.getNukeExplosivePower() == 5.0F,
                "One industrial TNT gives the legacy base power of five");
        load(nuke, 8, 0, ModReactorItems.URANIUM_238.get());
        helper.assertTrue(
                nuke.getNukeExplosivePower() == 10.0F,
                "Eight charges give the cube-root scaled power of ten");
        helper.succeed();
    }

    static void radioactivePayloadBoostsPowerAndRadiation(GameTestHelper helper) {
        helper.setBlock(ORIGIN, ModNuke.NUKE.get());
        NukeBlockEntity nuke = helper.getBlockEntity(ORIGIN, NukeBlockEntity.class);
        load(nuke, 32, 1, ModReactorItems.URANIUM_235.get());
        float expected = (float) (5.0 * Math.pow(32, 0.3333333333333333) + 0.5);
        helper.assertTrue(
                Math.abs(nuke.getNukeExplosivePower() - expected) < 0.001F,
                "Thirty-two charges plus one uranium 235 follow the legacy boost formula");
        helper.assertTrue(
                nuke.getRadiationRange() == 64,
                "Uranium 235 doubles the itnt count into the radiation range");
        helper.succeed();
    }

    static void redstonePrimesLoadedCharge(GameTestHelper helper) {
        helper.setBlock(ORIGIN, ModNuke.NUKE.get());
        NukeBlockEntity nuke = helper.getBlockEntity(ORIGIN, NukeBlockEntity.class);
        load(nuke, 2, 1, ModReactorItems.URANIUM_238.get());
        helper.setBlock(ORIGIN.east(), Blocks.REDSTONE_BLOCK);
        NukeEntity charge = soleCharge(helper);
        helper.assertTrue(charge != null, "A redstone pulse primes the loaded nuke into a charge");
        helper.assertTrue(helper.getBlockState(ORIGIN).isAir(), "Priming removes the charge block");
        if (charge != null) {
            helper.assertTrue(charge.getFuse() == 300, "The primed nuke carries the 300-tick fuse");
            helper.assertTrue(
                    charge.getRadiationRange() == 2,
                    "Uranium 238 maps the itnt count into the radiation range");
        }
        helper.assertTrue(
                nuke.inventory().stack(NukeBlockEntity.OUTSIDE_SLOT).isEmpty()
                        && nuke.inventory().stack(NukeBlockEntity.INSIDE_SLOT).isEmpty(),
                "Priming consumes the loaded payload");
        helper.succeed();
    }

    static void unloadedNukeRefusesToPrime(GameTestHelper helper) {
        helper.setBlock(ORIGIN, ModNuke.NUKE.get());
        NukeBlockEntity nuke = helper.getBlockEntity(ORIGIN, NukeBlockEntity.class);
        helper.assertFalse(
                nuke.explode(null, false), "A nuke without industrial TNT stays put when primed");
        helper.assertTrue(
                helper.getBlockState(ORIGIN).is(ModNuke.NUKE.get()),
                "The refused priming leaves the charge block in place");
        helper.succeed();
    }

    static void chainReactionShortFuse(GameTestHelper helper) {
        helper.setBlock(ORIGIN, ModNuke.NUKE.get());
        NukeBlockEntity nuke = helper.getBlockEntity(ORIGIN, NukeBlockEntity.class);
        load(nuke, 1, 0, ModReactorItems.URANIUM_238.get());
        var center = helper.absolutePos(ORIGIN);
        helper.getLevel()
                .explode(
                        null,
                        center.getX() + 1.5,
                        center.getY() + 0.5,
                        center.getZ() + 0.5,
                        3.0F,
                        Level.ExplosionInteraction.TNT);
        NukeEntity charge = soleCharge(helper);
        helper.assertTrue(charge != null, "A neighbouring explosion primes the loaded nuke");
        if (charge != null) {
            helper.assertTrue(
                    charge.getFuse() < 300, "Chain-primed nukes use the legacy short fuse");
        }
        helper.succeed();
    }

    static void wrenchDefusesPrimedCharge(GameTestHelper helper) {
        var world = Vec3.atCenterOf(helper.absolutePos(ORIGIN));
        NukeEntity charge =
                new NukeEntity(helper.getLevel(), world.x, world.y, world.z, 5.0F, 0, null);
        helper.getLevel().addFreshEntity(charge);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModTools.WRENCH.get()));
        var result = charge.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(
                result == net.minecraft.world.InteractionResult.CONSUME,
                "The wrench defuses the primed charge");
        helper.assertTrue(charge.isRemoved(), "The defused charge disappears from the world");
        helper.succeed();
    }

    static void menuAcceptsOnlyPayloadItems(GameTestHelper helper) {
        helper.setBlock(ORIGIN, ModNuke.NUKE.get());
        NukeBlockEntity nuke = helper.getBlockEntity(ORIGIN, NukeBlockEntity.class);
        Inventory inventory = helper.makeMockPlayer(GameType.SURVIVAL).getInventory();
        NukeMenu menu = new NukeMenu(0, inventory, nuke);
        helper.assertTrue(
                menu.slots.size() == NukeMenu.MACHINE_SLOTS + 36,
                "The nuke menu carries the payload slots plus the player inventory");
        var inside = menu.slots.get(0);
        var outside = menu.slots.get(1);
        helper.assertTrue(
                inside.mayPlace(new ItemStack(ModReactorItems.URANIUM_238.get())),
                "The payload slot accepts uranium 238");
        helper.assertFalse(
                inside.mayPlace(new ItemStack(Items.STONE)),
                "The payload slot rejects arbitrary blocks");
        helper.assertTrue(
                outside.mayPlace(new ItemStack(ModExplosives.ITNT.get())),
                "The eight charge views accept industrial TNT");
        helper.assertFalse(
                outside.mayPlace(new ItemStack(ModReactorItems.URANIUM_238.get())),
                "The charge views reject radioactive payloads");
        helper.succeed();
    }

    private static void load(
            NukeBlockEntity nuke,
            int itntCount,
            int insideCount,
            net.minecraft.world.item.Item fuel) {
        nuke.inventory()
                .set(
                        NukeBlockEntity.OUTSIDE_SLOT,
                        ItemResource.of(ModExplosives.ITNT.get()),
                        itntCount);
        if (insideCount > 0) {
            nuke.inventory().set(NukeBlockEntity.INSIDE_SLOT, ItemResource.of(fuel), insideCount);
        }
    }

    private static NukeEntity soleCharge(GameTestHelper helper) {
        List<NukeEntity> found =
                helper.getLevel()
                        .getEntities(
                                ModEntities.NUKE.get(),
                                new AABB(helper.absolutePos(ORIGIN)).inflate(4.0),
                                entity -> entity.isAlive());
        return found.isEmpty() ? null : found.getFirst();
    }

    private NukeTests() {}
}
