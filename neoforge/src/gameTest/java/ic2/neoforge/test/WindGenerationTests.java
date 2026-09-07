package ic2.neoforge.test;

import com.mojang.serialization.JsonOps;

import ic2.neoforge.machine.EnergyStorageBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.WindGeneratorBlockEntity;
import ic2.neoforge.machine.WorldWind;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

final class WindGenerationTests {
    static void persistence(GameTestHelper helper) {
        var wind = WorldWind.create(helper.getLevel());
        for (int i = 0; i < 171; i++) wind.advance();
        var encoded = WorldWind.CODEC.encodeStart(JsonOps.INSTANCE, wind).getOrThrow();
        var restored = WorldWind.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        for (int i = 0; i < 4096; i++) {
            wind.advance();
            restored.advance();
        }
        helper.assertTrue(
                WorldWind.CODEC
                        .encodeStart(JsonOps.INSTANCE, wind)
                        .getOrThrow()
                        .equals(
                                WorldWind.CODEC
                                        .encodeStart(JsonOps.INSTANCE, restored)
                                        .getOrThrow()),
                "Wind strength, direction, update phase and RNG must continue identically after"
                    + " reload");
        helper.assertTrue(
                WorldWind.get(helper.getLevel()) == WorldWind.get(helper.getLevel()),
                "Native storage owns one wind field per dimension");
        helper.succeed();
    }

    static void obstructions(GameTestHelper helper) {
        var pos = new BlockPos(8, 180, 8);
        helper.setBlock(pos, ModMachines.block(MachineKind.WIND_GENERATOR));
        var machine = helper.getBlockEntity(pos, WindGeneratorBlockEntity.class);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.menuValue(0) == 0 && machine.menuValue(1) > 0 && machine.rotorSpeed() == 1,
                "An unobstructed elevated windmill generates power and rotates immediately");
        for (var neighbor : BlockPos.betweenClosed(pos.offset(-4, -2, -4), pos.offset(4, 4, 4)))
            if (!neighbor.equals(pos)) helper.setBlock(neighbor, Blocks.STONE);
        machine.sampleObstructions(helper.getLevel());
        helper.assertTrue(
                machine.menuValue(0) == 566, "The 567-cell volume excludes the windmill itself");
        helper.succeed();
    }

    static void networkSupply(GameTestHelper helper) {
        var pos = new BlockPos(8, 180, 8);
        helper.setBlock(pos, ModMachines.block(MachineKind.WIND_GENERATOR));
        helper.setBlock(pos.east(), ModMachines.block(MachineKind.BATBOX));
        var battery = helper.getBlockEntity(pos.east(), EnergyStorageBlockEntity.class);
        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        battery.energy().stored() >= 64,
                                        "Fractional wind production must supply repeated LV packets"
                                            + " through the real network"))
                .thenSucceed();
    }

    private WindGenerationTests() {}
}
