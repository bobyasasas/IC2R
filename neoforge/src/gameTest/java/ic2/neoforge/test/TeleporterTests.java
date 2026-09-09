package ic2.neoforge.test;

import com.mojang.authlib.GameProfile;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.machine.EnergyStorageBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.TeleporterBlockEntity;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.UUID;

final class TeleporterTests {
    private static final BlockPos ORIGIN = new BlockPos(8, 5, 8);
    private static final BlockPos DESTINATION = new BlockPos(8, 5, 4);
    private static final BlockPos BATTERY = new BlockPos(7, 5, 8);

    private static TeleporterBlockEntity teleporter(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachines.block(MachineKind.TELEPORTER).defaultBlockState());
        return helper.getBlockEntity(pos, TeleporterBlockEntity.class);
    }

    private static FakePlayer player(GameTestHelper helper) {
        var player =
                new FakePlayer(
                        helper.getLevel(),
                        new GameProfile(UUID.randomUUID(), "ic2-teleporter-test"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        return player;
    }

    private static InteractionResult use(
            FakePlayer player, GameTestHelper helper, BlockPos pos, ItemStack transmitter) {
        player.setItemInHand(InteractionHand.MAIN_HAND, transmitter);
        var hit =
                new BlockHitResult(
                        Vec3.atCenterOf(helper.absolutePos(pos)),
                        Direction.UP,
                        helper.absolutePos(pos),
                        false);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        return transmitter.getItem().useOn(context);
    }

    private static ItemEntity dropAt(GameTestHelper helper, BlockPos pos) {
        var level = helper.getLevel();
        var loot =
                new ItemEntity(
                        level,
                        helper.absolutePos(pos).getX() + 0.5,
                        helper.absolutePos(pos).getY() + 1.0,
                        helper.absolutePos(pos).getZ() + 0.5,
                        new ItemStack(Items.STONE, 64));
        level.addFreshEntity(loot);
        return loot;
    }

    static void linksAndTeleports(GameTestHelper helper) {
        var level = helper.getLevel();
        var sender = teleporter(helper, ORIGIN);
        teleporter(helper, DESTINATION);
        helper.setBlock(BATTERY, ModMachines.block(MachineKind.MFE).defaultBlockState());
        var battery = helper.getBlockEntity(BATTERY, EnergyStorageBlockEntity.class);
        battery.energy().restore(1000000);
        helper.setBlock(
                ORIGIN.below(),
                net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState());
        var transmitter = ModTools.FREQUENCY_TRANSMITTER.toStack();
        var player = player(helper);
        helper.assertTrue(
                use(player, helper, ORIGIN, transmitter) == InteractionResult.SUCCESS,
                "The transmitter links the first teleporter");
        use(player, helper, DESTINATION, transmitter);
        helper.assertTrue(
                sender.hasTarget() && sender.getTarget().equals(helper.absolutePos(DESTINATION)),
                "The second use connects both teleporters");
        var loot = dropAt(helper, ORIGIN);
        double before = battery.energy().stored();
        for (int tick = 0; tick < 30; tick++) sender.serverTick((ServerLevel) level);
        helper.assertTrue(
                Math.abs(loot.getX() - (helper.absolutePos(DESTINATION).getX() + 0.5)) < 0.1
                        && Math.abs(loot.getZ() - (helper.absolutePos(DESTINATION).getZ() + 0.5))
                                < 0.1,
                "The closest entity crosses to the linked teleporter");
        long cost = Math.round(100 * Math.pow(4.0 + 10.0, 0.7) * 5.0);
        helper.assertTrue(
                Math.abs(before - battery.energy().stored() - cost) <= 1,
                "One teleport pays weight x (distance+10)^0.7 x 5 EU from adjacent storage");
        helper.succeed();
    }

    static void cooldownAndShortage(GameTestHelper helper) {
        var level = helper.getLevel();
        var sender = teleporter(helper, ORIGIN);
        var receiver = teleporter(helper, DESTINATION);
        helper.setBlock(BATTERY, ModMachines.block(MachineKind.MFSU).defaultBlockState());
        var battery = helper.getBlockEntity(BATTERY, EnergyStorageBlockEntity.class);
        battery.energy().restore(100);
        helper.setBlock(ORIGIN.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        var transmitter = ModTools.FREQUENCY_TRANSMITTER.toStack();
        var player = player(helper);
        use(player, helper, ORIGIN, transmitter);
        use(player, helper, DESTINATION, transmitter);
        var loot = dropAt(helper, ORIGIN);
        for (int tick = 0; tick < 30; tick++) sender.serverTick((ServerLevel) level);
        helper.assertTrue(
                loot.getX() == helper.absolutePos(ORIGIN).getX() + 0.5,
                "An unpayable teleport leaves the entity alone");
        receiver.setTarget(helper.absolutePos(ORIGIN));
        receiver.onTeleportTo();
        var second = dropAt(helper, DESTINATION);
        helper.setBlock(DESTINATION.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        for (int tick = 0; tick < 10; tick++) receiver.serverTick((ServerLevel) level);
        helper.assertTrue(
                second.getX() == helper.absolutePos(DESTINATION).getX() + 0.5,
                "The twenty-tick cooldown blocks the return trip");
        helper.succeed();
    }

    static void unlinksInAir(GameTestHelper helper) {
        var sender = teleporter(helper, ORIGIN);
        teleporter(helper, DESTINATION);
        var transmitter = ModTools.FREQUENCY_TRANSMITTER.toStack();
        var player = player(helper);
        use(player, helper, ORIGIN, transmitter);
        use(player, helper, DESTINATION, transmitter);
        helper.assertTrue(sender.hasTarget(), "The link is established");
        player.setItemInHand(InteractionHand.MAIN_HAND, transmitter);
        transmitter.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        transmitter.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                !transmitter.has(ModDataComponents.FREQUENCY_POS),
                "A later air use unlinks the remembered teleporter");
        helper.succeed();
    }

    private TeleporterTests() {}
}
