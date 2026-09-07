package ic2.neoforge.test;

import com.mojang.authlib.GameProfile;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.energy.CableBlock;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.WrenchTool;
import ic2.neoforge.machine.EnergyStorageBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

final class ToolTests {
    static void crafting(GameTestHelper helper) {
        var key =
                ResourceKey.<Recipe<?>>create(
                        Registries.RECIPE, Identifier.parse("ic2:shapeless/iron_plate"));
        var recipe =
                (CraftingRecipe) helper.getLevel().recipeAccess().byKey(key).orElseThrow().value();
        var hammer = ModTools.FORGE_HAMMER.toStack();
        hammer.setDamageValue(78);
        var input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.IRON_INGOT), hammer));
        helper.assertTrue(
                recipe.matches(input, helper.getLevel()),
                "Native hammer tag must enable the legacy plate recipe");
        var remainder = recipe.getRemainingItems(input).get(1);
        helper.assertTrue(
                remainder.is(ModTools.FORGE_HAMMER.get())
                        && remainder.getDamageValue() == 79
                        && hammer.getDamageValue() == 78,
                "Crafting preview must damage only the returned tool");
        hammer.setDamageValue(79);
        helper.assertTrue(
                recipe.getRemainingItems(input).get(1).isEmpty(),
                "Final use must consume the hammer");
        helper.succeed();
    }

    static void rotationAndInsulation(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var level = helper.getLevel();
        level.setBlockAndUpdate(pos, ModMachines.block(MachineKind.BATBOX).defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, ModTools.WRENCH.toStack());
        var center = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        WrenchTool.rotate(new UseOnContext(player, InteractionHand.MAIN_HAND, center));
        helper.assertTrue(
                level.getBlockState(pos).getValue(MachineBlock.FACING) == Direction.UP,
                "Storage must rotate to a vertical face");
        level.setBlockAndUpdate(pos, ModMachines.block(MachineKind.CANNER).defaultBlockState());
        WrenchTool.rotate(new UseOnContext(player, InteractionHand.MAIN_HAND, center));
        helper.assertTrue(
                level.getBlockState(pos).getValue(MachineBlock.FACING) == Direction.NORTH,
                "Horizontal machines must reject vertical wrench rotation");
        level.setBlockAndUpdate(
                pos, ModMachines.CABLES.get("copper_cable").get().defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, ModTools.CUTTER.toStack());
        player.getInventory()
                .setItem(
                        1,
                        new ItemStack(ModItems.MATERIALS.get(MaterialDefinition.RUBBER).get(), 2));
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, center);
        ModTools.CUTTER.get().useOn(context);
        helper.assertTrue(
                ((CableBlock) level.getBlockState(pos).getBlock()).specification().insulation() == 1
                        && player.getInventory().getItem(1).getCount() == 1
                        && player.getMainHandItem().getDamageValue() == 1,
                "Insulating consumes one rubber and one tool use");
        ModTools.CUTTER.get().useOn(context);
        helper.assertTrue(
                player.getInventory().getItem(1).getCount() == 1
                        && player.getMainHandItem().getDamageValue() == 1,
                "Maximum insulation must not consume rubber or durability");
        level.getBlockState(pos).attack(level, pos, player);
        helper.assertTrue(
                ((CableBlock) level.getBlockState(pos).getBlock()).specification().insulation() == 0
                        && player.getMainHandItem().getDamageValue() == 4,
                "Stripping costs three uses and changes cable identity");
        helper.assertTrue(
                helper.getEntities(EntityType.ITEM).stream()
                                .filter(
                                        e ->
                                                e.getItem()
                                                        .is(
                                                                ModItems.MATERIALS
                                                                        .get(
                                                                                MaterialDefinition
                                                                                        .RUBBER)
                                                                        .get()))
                                .mapToInt(e -> e.getItem().getCount())
                                .sum()
                        == 1,
                "Stripping must return exactly one rubber");
        helper.succeed();
    }

    static void miningAndRetention(GameTestHelper helper) {
        var level = helper.getLevel();
        var relative = new BlockPos(2, 2, 2);
        var pos = helper.absolutePos(relative);
        var block = ModMachines.block(MachineKind.MFSU);
        var state = block.defaultBlockState();
        helper.setBlock(relative, state);
        var storage = helper.getBlockEntity(relative, EnergyStorageBlockEntity.class);
        storage.energy().restore(40000000);
        storage.inventory().set(0, ItemResource.of(Items.DIAMOND), 1);
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "ic2-tool-test"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        var pickDrops =
                Block.getDrops(
                        state, level, pos, storage, player, new ItemStack(Items.DIAMOND_PICKAXE));
        helper.assertTrue(
                pickDrops.size() == 1
                        && pickDrops
                                .getFirst()
                                .is(
                                        ModMaterialBlocks.MATERIALS
                                                .get("advanced_machine")
                                                .get()
                                                .asItem()),
                "A pickaxe must recover the MFSU advanced casing");
        var wrench = ModTools.ELECTRIC_WRENCH.toStack();
        ElectricItemEnergy.charge(wrench, 100, 1, true, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        helper.assertTrue(
                player.gameMode.destroyBlock(pos),
                "Native survival mining must remove the machine");
        var drops = helper.getEntities(EntityType.ITEM);
        var machineDrop =
                drops.stream()
                        .map(ItemEntity::getItem)
                        .filter(item -> item.is(block.asItem()))
                        .findFirst()
                        .orElseThrow();
        helper.assertTrue(
                machineDrop.getOrDefault(ModDataComponents.STORED_ENERGY, 0.0) == 32000000
                        && ElectricItemEnergy.charge(wrench) == 0,
                "Electric wrench must retain configured storage energy and consume exactly 100 EU");
        helper.assertTrue(
                drops.stream()
                                .filter(e -> e.getItem().is(Items.DIAMOND))
                                .mapToInt(e -> e.getItem().getCount())
                                .sum()
                        == 1,
                "Machine inventory must drop exactly once");
        level.setBlockAndUpdate(pos, state);
        block.setPlacedBy(level, pos, state, player, machineDrop);
        var restored = (EnergyStorageBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(
                restored.energy().stored() == 32000000, "Placement must restore retained EU");
        // Destruction protection is handled by Minecraft/NeoForge's standard mining pipeline.
        Consumer<BreakBlockEvent> guard =
                event -> {
                    if (event.getPos().equals(pos)) event.setCanceled(true);
                };
        NeoForge.EVENT_BUS.addListener(guard);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, ModTools.WRENCH.toStack());
            helper.assertTrue(
                    !player.gameMode.destroyBlock(pos)
                            && level.getBlockEntity(pos) == restored
                            && player.getMainHandItem().getDamageValue() == 0,
                    "Cancelled break must preserve the block and tool");
        } finally {
            NeoForge.EVENT_BUS.unregister(guard);
        }
        helper.succeed();
    }

    private ToolTests() {}
}
