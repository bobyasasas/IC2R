package ic2.neoforge.test;

import ic2.neoforge.energy.CableBlock;
import ic2.neoforge.item.CableItem;
import ic2.neoforge.item.CutterItem;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

final class CableFamilyTests {
    private static final BlockPos MID = new BlockPos(2, 1, 2);
    private static final BlockPos WEST = new BlockPos(1, 1, 2),
            EAST = new BlockPos(3, 1, 2),
            NORTH = new BlockPos(2, 1, 1),
            ABOVE = new BlockPos(2, 2, 2);

    /** Recovered legacy CableType table: voltage, amps, classic loss, GT loss, insulation ladder. */
    private record Row(
            String id, int voltage, int amps, double classicLoss, int gtLoss, int insulation, int maxInsulation) {}

    private static final List<Row> ROWS =
            List.of(
                    new Row("glass_fibre_cable", 8192, 8, 0.025, 0, 0, 0),
                    new Row("copper_cable", 128, 2, 0.2, 2, 0, 1),
                    new Row("insulated_copper_cable", 128, 2, 0.2, 1, 1, 1),
                    new Row("gold_cable", 512, 3, 0.4, 4, 0, 2),
                    new Row("insulated_gold_cable", 512, 3, 0.4, 2, 1, 2),
                    new Row("double_insulated_gold_cable", 512, 3, 0.4, 2, 2, 2),
                    new Row("iron_cable", 2048, 4, 0.8, 6, 0, 3),
                    new Row("insulated_iron_cable", 2048, 4, 0.8, 3, 1, 3),
                    new Row("double_insulated_iron_cable", 2048, 4, 0.8, 3, 2, 3),
                    new Row("triple_insulated_iron_cable", 2048, 4, 0.8, 3, 3, 3),
                    new Row("tin_cable", 32, 1, 0.2, 2, 0, 1),
                    new Row("insulated_tin_cable", 32, 1, 0.2, 1, 1, 1));

    private static CableBlock cable(String id) {
        return ModMachines.CABLES.get(id).get();
    }

    private static ItemStack rubber() {
        return new ItemStack(ModItems.MATERIALS.get(MaterialDefinition.RUBBER).get());
    }

    private static int rubberCount(Player player) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            if (player.getInventory().getItem(slot).is(rubber().getItem()))
                total += player.getInventory().getItem(slot).getCount();
        return total;
    }

    private static void insulate(GameTestHelper helper, Player player, ItemStack cutter) {
        ModTools.CUTTER.get()
                .useOn(
                        new UseOnContext(
                                helper.getLevel(),
                                player,
                                InteractionHand.MAIN_HAND,
                                cutter,
                                new BlockHitResult(
                                        Vec3.atCenterOf(helper.absolutePos(MID)),
                                        Direction.UP,
                                        helper.absolutePos(MID),
                                        false)));
    }

    static void familySpecs(GameTestHelper helper) {
        for (Row row : ROWS) {
            var block = cable(row.id());
            var spec = block.specification();
            helper.assertTrue(
                    spec.voltageLimit() == row.voltage() && spec.ampLimit() == row.amps(),
                    row.id() + " must keep the recovered voltage and amperage limits");
            helper.assertTrue(
                    spec.classicLoss() == row.classicLoss() && spec.gtLoss() == row.gtLoss(),
                    row.id() + " must keep the loss table with GT doubling the bare run");
            helper.assertTrue(
                    spec.insulation() == row.insulation()
                            && spec.maxInsulation() == row.maxInsulation(),
                    row.id() + " must keep the recovered insulation ladder");
            helper.assertTrue(
                    block.asItem() instanceof CableItem,
                    row.id() + " must carry the legacy cable tooltip item");
        }
        helper.succeed();
    }

    static void insulationLadder(GameTestHelper helper) {
        ItemStack cutter = new ItemStack(ModTools.CUTTER.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, cutter);
        player.getInventory().add(new ItemStack(rubber().getItem(), 16));
        helper.setBlock(MID, cable("iron_cable"));

        insulate(helper, player, cutter);
        helper.assertTrue(
                helper.getBlockState(MID).is(cable("insulated_iron_cable")),
                "The cutter plus rubber must raise an iron cable by one insulation layer");
        insulate(helper, player, cutter);
        helper.assertTrue(
                helper.getBlockState(MID).is(cable("double_insulated_iron_cable")),
                "Insulation layers must stack on iron cables");
        insulate(helper, player, cutter);
        helper.assertTrue(
                helper.getBlockState(MID).is(cable("triple_insulated_iron_cable")),
                "The third layer completes the iron ladder");
        int rubberBefore = rubberCount(player);
        int damageBefore = cutter.getDamageValue();
        insulate(helper, player, cutter);
        helper.assertTrue(
                rubberCount(player) == rubberBefore && cutter.getDamageValue() == damageBefore,
                "A fully insulated cable must refuse rubber without tool wear");

        for (int layer = 0; layer < 3; layer++)
            CutterItem.strip(player, helper.getLevel(), helper.absolutePos(MID), helper.getBlockState(MID));
        helper.assertTrue(
                helper.getBlockState(MID).is(cable("iron_cable")),
                "Stripping three times must bare the iron cable");
        int rubberAtBare = rubberCount(player);
        int damageAtBare = cutter.getDamageValue();
        CutterItem.strip(player, helper.getLevel(), helper.absolutePos(MID), helper.getBlockState(MID));
        helper.assertTrue(
                helper.getBlockState(MID).is(cable("iron_cable"))
                        && rubberCount(player) == rubberAtBare
                        && cutter.getDamageValue() == damageAtBare,
                "A bare cable has no insulation left to strip");
        helper.succeed();
    }

    static void waterloggedCable(GameTestHelper helper) {
        var level = helper.getLevel();
        var insulated = cable("insulated_copper_cable");
        helper.setBlock(MID, Blocks.WATER);
        var context =
                new BlockPlaceContext(
                        level,
                        null,
                        InteractionHand.MAIN_HAND,
                        new ItemStack(insulated.asItem()),
                        new BlockHitResult(
                                Vec3.atCenterOf(helper.absolutePos(MID)),
                                Direction.UP,
                                helper.absolutePos(MID),
                                false));
        BlockState placed = insulated.getStateForPlacement(context);
        helper.assertTrue(
                placed.getValue(BlockStateProperties.WATERLOGGED),
                "Placing a cable into water must waterlog it");
        helper.setBlock(MID, placed);
        BlockPos absolute = helper.absolutePos(MID);
        helper.assertTrue(
                level.getFluidState(absolute).is(Fluids.WATER),
                "A waterlogged cable must read as a water source");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModTools.CUTTER.get()));
        CutterItem.strip(player, level, absolute, helper.getBlockState(MID));
        BlockState stripped = helper.getBlockState(MID);
        helper.assertTrue(
                stripped.is(cable("copper_cable"))
                        && stripped.getValue(BlockStateProperties.WATERLOGGED),
                "Stripping insulation must keep the water");

        var drained = ((CableBlock) stripped.getBlock()).pickupBlock(null, level, absolute, stripped);
        helper.assertTrue(
                drained.is(Items.WATER_BUCKET), "A bucket must recover the waterlogged state");
        helper.assertTrue(
                !helper.getBlockState(MID).getValue(BlockStateProperties.WATERLOGGED),
                "Draining must clear the waterlogged property");
        helper.succeed();
    }

    static void connectionMask(GameTestHelper helper) {
        helper.setBlock(WEST, ModMachines.GENERATOR.get());
        helper.setBlock(EAST, Blocks.STONE);
        helper.setBlock(NORTH, cable("gold_cable"));
        helper.setBlock(ABOVE, ModMachines.FOAM_CABLES.get("tin_foam_cable").get());
        BlockState connected =
                CableBlock.connectedState(cable("copper_cable"), helper.getLevel(), helper.absolutePos(MID));
        helper.assertTrue(
                connected.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST)),
                "Cables must visually join powered machines");
        helper.assertTrue(
                !connected.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST)),
                "Cables must not join inert blocks");
        helper.assertTrue(
                connected.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(Direction.NORTH)),
                "Cables must join other cables");
        helper.assertTrue(
                connected.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(Direction.UP)),
                "Cables must join foam-covered cables");
        helper.assertTrue(
                !connected.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(Direction.DOWN)),
                "Cables must not reach into air");
        helper.succeed();
    }

    private CableFamilyTests() {}
}
