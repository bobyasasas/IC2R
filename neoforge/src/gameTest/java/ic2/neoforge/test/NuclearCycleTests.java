package ic2.neoforge.test;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.DepletingRodItem;
import ic2.neoforge.machine.CreativeGeneratorBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.NuclearReactorBlockEntity;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModArmor;
import ic2.neoforge.registration.ModEffects;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModReactorItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

final class NuclearCycleTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    private static NuclearReactorBlockEntity reactor(GameTestHelper helper) {
        helper.setBlock(
                POSITION, ModMachines.block(MachineKind.NUCLEAR_REACTOR).defaultBlockState());
        return helper.getBlockEntity(POSITION, NuclearReactorBlockEntity.class);
    }

    static void nuclearResourcesIrradiateCarriers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var unshielded = helper.makeMockPlayer(GameType.SURVIVAL);
        var stack = ModReactorItems.NEAR_DEPLETED_URANIUM.get().getDefaultInstance();
        ModReactorItems.NEAR_DEPLETED_URANIUM.get().inventoryTick(stack, level, unshielded, null);
        MobEffectInstance radiation = unshielded.getEffect(ModEffects.RADIATION);
        helper.assertTrue(radiation != null, "Carrying near-depleted uranium applies radiation");
        helper.assertTrue(
                radiation.getAmplifier() == 100,
                "Legacy ItemNuclearResource irradiates at amplifier 100");
        helper.assertTrue(
                radiation.getDuration() == 15 * 20,
                "Near-depleted uranium irradiates for fifteen seconds");

        var shielded = helper.makeMockPlayer(GameType.SURVIVAL);
        shielded.setItemSlot(EquipmentSlot.HEAD, ModArmor.HAZMAT_HELMET.get().getDefaultInstance());
        shielded.setItemSlot(
                EquipmentSlot.CHEST, ModArmor.HAZMAT_CHESTPLATE.get().getDefaultInstance());
        shielded.setItemSlot(
                EquipmentSlot.LEGS, ModArmor.HAZMAT_LEGGINGS.get().getDefaultInstance());
        shielded.setItemSlot(EquipmentSlot.FEET, ModArmor.RUBBER_BOOTS.get().getDefaultInstance());
        var reEnriched = ModReactorItems.RE_ENRICHED_URANIUM.get().getDefaultInstance();
        ModReactorItems.RE_ENRICHED_URANIUM.get()
                .inventoryTick(reEnriched, level, shielded, null);
        helper.assertTrue(
                !shielded.hasEffect(ModEffects.RADIATION),
                "A complete hazmat suit blocks nuclear-resource radiation");

        var reactor = reactor(helper);
        var component = (ic2.neoforge.item.ReactorComponent)
                ModReactorItems.NEAR_DEPLETED_URANIUM.get();
        helper.assertTrue(
                !component.canBePlacedIn(stack, reactor),
                "Irradiating resources refuse the reactor grid");
        helper.succeed();
    }

    static void depletingRodsChargeAndSwap(GameTestHelper helper) {
        var reactor = reactor(helper);
        DepletingRodItem lithium = ModReactorItems.LITHIUM_FUEL_ROD.get();
        var lithiumStack = lithium.getDefaultInstance();

        // Non-heat passes never touch the rods; the heat pass charges by reactor heat / 3000.
        lithium.acceptUraniumPulse(lithiumStack, reactor, lithiumStack, 0, 1, 1, 1, false);
        helper.assertTrue(lithium.use(lithiumStack) == 0, "Pulse passes leave the lithium cell");
        reactor.setHeat(15000);
        lithium.acceptUraniumPulse(lithiumStack, reactor, lithiumStack, 0, 1, 1, 1, true);
        helper.assertTrue(
                lithium.use(lithiumStack) == 5,
                "A heat pass at 15000 heat charges the lithium cell by five");

        // Reaching maxUse swaps the cell for a tritium fuel rod.
        lithiumStack.set(ModDataComponents.REACTOR_USE, 9996);
        reactor.setHeat(12000);
        lithium.acceptUraniumPulse(lithiumStack, reactor, lithiumStack, 0, 1, 1, 1, true);
        helper.assertTrue(
                reactor.getItemAt(0, 1)
                        .is(ModItems.MATERIALS.get(MaterialDefinition.TRITIUM_FUEL_ROD).get()),
                "A fully charged lithium cell becomes a tritium fuel rod");

        // The depleted isotope gains +1 per pulse from itself and re-enriches when full.
        DepletingRodItem isotope = ModReactorItems.DEPLETED_ISOTOPE_FUEL_ROD.get();
        var isotopeStack = isotope.getDefaultInstance();
        reactor.setHeat(0);
        isotope.acceptUraniumPulse(isotopeStack, reactor, isotopeStack, 1, 1, 0, 1, true);
        helper.assertTrue(
                isotope.use(isotopeStack) == 1,
                "The depleted isotope charges by one even without reactor heat");
        isotopeStack.set(ModDataComponents.REACTOR_USE, 9999);
        isotope.acceptUraniumPulse(isotopeStack, reactor, isotopeStack, 1, 1, 0, 1, true);
        helper.assertTrue(
                reactor.getItemAt(1, 1).is(ModReactorItems.RE_ENRICHED_URANIUM.get()),
                "A fully charged isotope becomes re-enriched uranium");
        helper.succeed();
    }

    static void creativeGeneratorTopsUpForever(GameTestHelper helper) {
        helper.setBlock(
                POSITION, ModMachines.block(MachineKind.CREATIVE_GENERATOR).defaultBlockState());
        var generator = helper.getBlockEntity(POSITION, CreativeGeneratorBlockEntity.class);
        helper.assertTrue(
                generator.energy().capacity() == 32000,
                "The creative generator carries the legacy capacity");
        generator.serverTick(helper.getLevel());
        helper.assertTrue(
                generator.energy().stored() == 32000, "Ticking tops the store up to full");
        generator.energy().extract(32000);
        helper.assertTrue(generator.energy().stored() == 0, "The store drains like any source");
        generator.serverTick(helper.getLevel());
        helper.assertTrue(
                generator.energy().stored() == 32000, "One tick tops the store back up");
        helper.assertTrue(
                helper.getBlockState(POSITION).getDestroySpeed(helper.getLevel(), POSITION) == -1.0F,
                "The creative generator is unbreakable like its legacy counterpart");
        helper.succeed();
    }

    private NuclearCycleTests() {}
}
