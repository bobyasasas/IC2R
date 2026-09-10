package ic2.neoforge.test;

import ic2.core.energy.grid.EnergyMode;
import ic2.neoforge.energy.EnergyConfig;
import ic2.neoforge.machine.EnergyStorageBlockEntity;
import ic2.neoforge.machine.GeneratorBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.pig.Pig;

final class CableShockTests {
    // The floor sits at structure y=0; entities and blocks that need support stay on y=1.
    private static final BlockPos SOURCE = new BlockPos(1, 1, 2),
            CABLE = new BlockPos(2, 1, 2),
            SINK = new BlockPos(3, 1, 2),
            PIG_POS = new BlockPos(2, 1, 3);

    private static EnergyStorageBlockEntity buildLine(
            GameTestHelper helper, String cableId, MachineKind storageKind) {
        helper.setBlock(
                SOURCE,
                ModMachines.block(MachineKind.GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(CABLE, ModMachines.CABLES.get(cableId).get());
        helper.setBlock(
                SINK,
                ModMachines.block(storageKind)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        var generator = helper.getBlockEntity(SOURCE, GeneratorBlockEntity.class);
        var sink = helper.getBlockEntity(SINK, EnergyStorageBlockEntity.class);
        generator.energy().restore(1000);
        return sink;
    }

    private static Pig parkedPig(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityType.PIG, PIG_POS);
        pig.setNoAi(true);
        return pig;
    }

    static void uninsulatedCableShocksNearbyEntities(GameTestHelper helper) {
        EnergyStorageBlockEntity sink = buildLine(helper, "tin_cable", MachineKind.MFE);
        Pig pig = parkedPig(helper);
        helper.runAtTickTime(
                20,
                () -> {
                    // A 32 EU packet exceeds the tier-zero absorption of eight, both energy modes.
                    helper.assertTrue(
                            pig.getHealth() < pig.getMaxHealth(),
                            "The bare tin cable shocks the pig standing next to it");
                    helper.assertTrue(
                            sink.energy().stored() > 0,
                            "The shocking link still delivers its packet");
                    helper.assertTrue(
                            helper.getBlockState(CABLE)
                                    .is(ModMachines.CABLES.get("tin_cable").get()),
                            "A 32 EU packet stays below the tin meltdown threshold");
                    helper.succeed();
                });
    }

    static void insulatedCableShieldsEntities(GameTestHelper helper) {
        EnergyStorageBlockEntity sink = buildLine(helper, "insulated_tin_cable", MachineKind.MFE);
        Pig pig = parkedPig(helper);
        helper.runAtTickTime(
                20,
                () -> {
                    helper.assertTrue(
                            pig.getHealth() == pig.getMaxHealth(),
                            "One insulation tier absorbs a 32 EU packet without shocking");
                    helper.assertTrue(
                            sink.energy().stored() > 0,
                            "The insulated link still delivers its packet");
                    helper.succeed();
                });
    }

    static void glassFibreCableNeverShocks(GameTestHelper helper) {
        EnergyStorageBlockEntity sink = buildLine(helper, "glass_fibre_cable", MachineKind.MFE);
        Pig pig = parkedPig(helper);
        helper.runAtTickTime(
                20,
                () -> {
                    helper.assertTrue(
                            pig.getHealth() == pig.getMaxHealth(),
                            "The unshieldable glass family never shocks entities");
                    helper.assertTrue(
                            sink.energy().stored() > 0,
                            "The glass fibre link still delivers its packet");
                    helper.succeed();
                });
    }

    static void overloadedGoldCableMeltsDownAndShocks(GameTestHelper helper) {
        helper.setBlock(
                SOURCE,
                ModMachines.block(MachineKind.MFSU)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(CABLE, ModMachines.CABLES.get("gold_cable").get());
        helper.setBlock(
                SINK,
                ModMachines.block(MachineKind.MFSU)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        var source = helper.getBlockEntity(SOURCE, EnergyStorageBlockEntity.class);
        source.energy().restore(40000);
        Pig pig = parkedPig(helper);
        helper.runAtTickTime(
                10,
                () -> {
                    if (EnergyConfig.MODE.get() == EnergyMode.IC2) {
                        helper.assertTrue(
                                !pig.isAlive(),
                                "A 2048 EU packet over bare gold cable electrocutes with thirty-two"
                                    + " damage");
                    } else {
                        helper.assertTrue(
                                pig.isAlive() && pig.getHealth() == pig.getMaxHealth(),
                                "GT mode rejects the packet before it ever crosses the cable");
                    }
                    helper.assertTrue(
                            helper.getBlockState(CABLE).isAir(),
                            "The overloaded gold cable melts down in both energy modes");
                    helper.succeed();
                });
    }
}
