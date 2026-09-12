package ic2.neoforge.test;

import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.machine.LuminatorBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModMaterialBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Legacy TileEntityLuminator: 5 EU tier-1 sink paying 0.25 EU/t while lit, redstone XOR invert
 * gating, the 10000 EU hand-discharge quirk that outlives a save/load round trip, monster
 * ignition (undead burn twice as long), and the support checks — including mounting on cables
 * (emitters) but never on reinforced glass.
 */
final class LuminatorTests {
    private static final BlockPos LAMP = new BlockPos(2, 1, 2);
    private static final BlockPos POWER = LAMP.west();

    private static LuminatorBlockEntity lamp(
            GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(
                pos,
                ModMachines.block(MachineKind.LUMINATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, facing));
        return helper.getBlockEntity(pos, LuminatorBlockEntity.class);
    }

    private static boolean active(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(MachineBlock.ACTIVE);
    }

    static void redstoneLedger(GameTestHelper helper) {
        var lamp = lamp(helper, LAMP, Direction.UP);
        lamp.energy().insert(LuminatorBlockEntity.CAPACITY);
        helper.startSequence().thenIdle(20).thenExecute(() -> {
            helper.assertTrue(
                    Math.abs(lamp.energy().stored() - LuminatorBlockEntity.CAPACITY) < 1e-9,
                    "Unlit lamp must not drain (" + lamp.energy().stored() + ")");
            helper.assertFalse(active(helper, LAMP), "Unlit lamp must be inactive");
            helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
        }).thenIdle(20).thenExecute(() -> {
            helper.assertTrue(
                    Math.abs(lamp.energy().stored()) < 1e-9,
                    "20 lit ticks must burn exactly 20x0.25 EU, got " + lamp.energy().stored());
            helper.assertTrue(active(helper, LAMP), "Lit lamp must be active");
        }).thenIdle(1).thenExecute(() -> {
            helper.assertFalse(
                    active(helper, LAMP), "An empty lamp must go dark on the next tick");
        }).thenSucceed();
    }

    static void invertToggle(GameTestHelper helper) {
        var lamp = lamp(helper, LAMP, Direction.UP);
        lamp.energy().insert(LuminatorBlockEntity.CAPACITY);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
        helper.startSequence().thenIdle(1).thenExecute(() -> {
            helper.assertTrue(active(helper, LAMP), "Powered lamp must light before inversion");
            lamp.use(player, InteractionHand.MAIN_HAND);
        }).thenIdle(5).thenExecute(() -> {
            helper.assertFalse(active(helper, LAMP), "Inverted lamp must stay dark while powered");
            helper.assertTrue(
                    Math.abs(lamp.energy().stored() - 4.75) < 1e-9,
                    "Only the first tick may have burned (got " + lamp.energy().stored() + ")");
            lamp.use(player, InteractionHand.MAIN_HAND);
        }).thenIdle(5).thenExecute(() -> {
            helper.assertTrue(
                    Math.abs(lamp.energy().stored() - 3.5) < 1e-9,
                    "Five relit ticks must cost another 1.25 EU (got "
                            + lamp.energy().stored() + ")");
            helper.assertTrue(active(helper, LAMP), "Second click must restore normal mode");
        }).thenSucceed();
    }

    static void handDischargeQuirk(GameTestHelper helper) {
        var lamp = lamp(helper, LAMP, Direction.UP);
        var battery = new ItemStack(ModItems.RE_BATTERY.get());
        ElectricItemEnergy.charge(battery, 3000, 1, true, false);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, battery);
        lamp.use(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                Math.abs(lamp.energy().stored() - 3000) < 1e-9,
                "Hand discharge must top the lamp past its 5 EU cap (got "
                        + lamp.energy().stored() + ")");
        helper.assertTrue(
                ElectricItemEnergy.charge(battery) == 0,
                "The battery must give up everything the lamp took ("
                        + ElectricItemEnergy.charge(battery) + ")");

        // The over-capacity charge must survive a save/load round trip.
        var tag = lamp.saveWithFullMetadata(helper.getLevel().registryAccess());
        var restored =
                (LuminatorBlockEntity) BlockEntity.loadStatic(
                        lamp.getBlockPos(),
                        lamp.getBlockState(),
                        tag,
                        helper.getLevel().registryAccess());
        helper.assertTrue(
                restored != null && Math.abs(restored.energy().stored() - 3000) < 1e-9,
                "Over-capacity charge must survive a save/load round trip");
        helper.assertTrue(
                restored.comparator() == 15,
                "A full over-charged lamp must saturate the comparator");
        lamp.energy().extract(2997.5);
        helper.assertTrue(
                lamp.comparator() == 7,
                "Comparator must read floor(2.5/5*15)=7 at half capacity");
        helper.succeed();
    }

    static void igniteMonsters(GameTestHelper helper) {
        var lamp = lamp(helper, LAMP, Direction.UP);
        lamp.energy().insert(LuminatorBlockEntity.CAPACITY);
        var zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, LAMP);
        var creeper = helper.spawnWithNoFreeWill(EntityType.CREEPER, LAMP);
        helper.startSequence().thenIdle(3).thenExecute(() -> {
            helper.assertTrue(
                    zombie.getRemainingFireTicks() == 0,
                    "An unlit lamp must not ignite anything");
            helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
        }).thenIdle(3).thenExecute(() -> {
            helper.assertTrue(
                    zombie.getRemainingFireTicks() > 0, "A lit lamp must ignite the undead");
            helper.assertTrue(
                    creeper.getRemainingFireTicks() > 0, "A lit lamp must ignite monsters");
            helper.assertTrue(
                    zombie.getRemainingFireTicks() > creeper.getRemainingFireTicks(),
                    "The undead must burn twice as long as other monsters");
        }).thenSucceed();
    }

    static void supportCheck(GameTestHelper helper) {
        BlockPos floating = new BlockPos(2, 1, 2);
        BlockPos onStone = new BlockPos(4, 1, 2);
        BlockPos onCable = new BlockPos(6, 1, 2);
        BlockPos onGlass = new BlockPos(8, 1, 2);
        helper.setBlock(onStone.below(), Blocks.STONE);
        helper.setBlock(onCable.below(), ModMachines.CABLES.get("insulated_copper_cable").get());
        helper.setBlock(onGlass.below(), ModMaterialBlocks.REINFORCED_GLASS.get());
        for (BlockPos pos : List.of(floating, onStone, onCable, onGlass))
            helper.setBlock(
                    pos,
                    ModMachines.block(MachineKind.LUMINATOR)
                            .defaultBlockState()
                            .setValue(MachineBlock.FACING, Direction.UP));
        helper.startSequence().thenIdle(5).thenExecute(() -> {
            helper.assertTrue(
                    helper.getBlockState(floating).isAir(),
                    "A lamp without support must pop off");
            // assertEntityPresent only searches the 1x1 structure box, so scan around the
            // lamp instead.
            var dropArea = new AABB(helper.absolutePos(floating)).inflate(2.0);
            helper.assertTrue(
                    !helper.getLevel().getEntitiesOfClass(ItemEntity.class, dropArea).isEmpty(),
                    "A popped-off lamp must drop itself");
            helper.assertTrue(
                    helper.getBlockState(onStone).is(ModMachines.block(MachineKind.LUMINATOR)),
                    "A sturdy wall must hold the lamp");
            helper.assertTrue(
                    helper.getBlockState(onCable).is(ModMachines.block(MachineKind.LUMINATOR)),
                    "A cable is a legacy energy emitter and must hold the lamp");
            helper.assertTrue(
                    helper.getBlockState(onGlass).isAir(),
                    "Reinforced glass supports nothing (legacy Ic2GlassBlock)");
        }).thenSucceed();
    }

    static void crafting(GameTestHelper helper) {
        var glass = new ItemStack(Items.GLASS);
        var alloy = new ItemStack(ModItems.MATERIALS.get(MaterialDefinition.ALLOY).get());
        var reinforced = (CraftingRecipe) helper.getLevel().recipeAccess()
                .byKey(ResourceKey.create(Registries.RECIPE,
                        Identifier.parse("ic2:shaped/reinforced_glass")))
                .orElseThrow().value();
        var input = CraftingInput.of(3, 3, List.of(
                glass.copy(), alloy.copy(), glass.copy(),
                glass.copy(), glass.copy(), glass.copy(),
                glass.copy(), alloy.copy(), glass.copy()));
        helper.assertTrue(
                reinforced.matches(input, helper.getLevel()),
                "Reinforced glass recipe must match its legacy pattern");
        var result = reinforced.assemble(input);
        helper.assertTrue(
                result.is(ModMaterialBlocks.REINFORCED_GLASS.get().asItem())
                        && result.getCount() == 7,
                "Reinforced glass must craft 7 per batch (got " + result.getCount() + ")");

        var casing = new ItemStack(ModItems.MATERIALS.get(MaterialDefinition.IRON_CASING).get());
        var insulated = new ItemStack(
                ModMachines.CABLES.get("insulated_copper_cable").get());
        var tin = new ItemStack(ModMachines.CABLES.get("tin_cable").get());
        var luminator = (CraftingRecipe) helper.getLevel().recipeAccess()
                .byKey(ResourceKey.create(Registries.RECIPE,
                        Identifier.parse("ic2:shaped/luminator")))
                .orElseThrow().value();
        var lampInput = CraftingInput.of(3, 3, List.of(
                casing.copy(), insulated.copy(), casing.copy(),
                glass.copy(), tin.copy(), glass.copy(),
                glass.copy(), glass.copy(), glass.copy()));
        helper.assertTrue(
                luminator.matches(lampInput, helper.getLevel()),
                "Luminator recipe must match its legacy pattern");
        var lamps = luminator.assemble(lampInput);
        helper.assertTrue(
                lamps.is(ModMachines.block(MachineKind.LUMINATOR).asItem()) && lamps.getCount() == 8,
                "Luminator must craft 8 per batch (got " + lamps.getCount() + ")");
        helper.succeed();
    }

    private LuminatorTests() {}
}
