package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, IndustrialCraft.MOD_ID);
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_TREETAP_USE =
            sound("item.treetap.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_WRENCH_USE =
            sound("item.wrench.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_CUTTER_USE =
            sound("item.cutter.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_PAINTER_USE =
            sound("item.painter.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_CROWBAR_USE =
            sound("item.crowbar.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_ELECTRIC_SHUTDOWN =
            sound("item.electric.shutdown");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_BATTERY_USE =
            sound("item.battery.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_JETPACK_LOOP =
            sound("item.jetpack.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_JETPACK_FIRE =
            sound("item.jetpack.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_TREETAP_ELECTRIC_USE =
            sound("item.treetap.electric.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_CHAINSAW_IDLE =
            sound("item.chainsaw.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_CHAINSAW_STOP =
            sound("item.chainsaw.stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_CHAINSAW_USE1 =
            sound("item.chainsaw.use1");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_CHAINSAW_USE2 =
            sound("item.chainsaw.use2");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_DRILL_IDLE =
            sound("item.drill.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_DRILL_HARD =
            sound("item.drill.hard");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_DRILL_SOFT =
            sound("item.drill.soft");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_LASER_SHOOT =
            sound("item.laser.shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_LASER_EXPLOSIVE =
            sound("item.laser.explosive");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_LASER_LONG_RANGE =
            sound("item.laser.long_range");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_LASER_LOW_FOCUS =
            sound("item.laser.low_focus");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_LASER_SCATTER =
            sound("item.laser.scatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_NANOSABER_IDLE =
            sound("item.nanosaber.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_NANOSABER_POWER_UP =
            sound("item.nanosaber.power_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_NANOSABER_SWING1 =
            sound("item.nanosaber.swing1");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_NANOSABER_SWING2 =
            sound("item.nanosaber.swing2");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_NANOSABER_SWING3 =
            sound("item.nanosaber.swing3");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_SCANNER_USE =
            sound("item.scanner.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_REMOTE_USE =
            sound("item.remote.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> GENERATOR_GENERATOR_LOOP =
            sound("generator.generator.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> GENERATOR_GEOTHERMAL_LOOP =
            sound("generator.geothermal.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> GENERATOR_WATER_LOOP =
            sound("generator.water.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> GENERATOR_WIND_LOOP =
            sound("generator.wind.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> GENERATOR_NUCLEAR_LOOP =
            sound("generator.nuclear.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> GENERATOR_NUCLEAR_LOW_POWER =
            sound("generator.nuclear.power.low");
    public static final DeferredHolder<SoundEvent, SoundEvent> GENERATOR_NUCLEAR_MEDIUM_POWER =
            sound("generator.nuclear.power.medium");
    public static final DeferredHolder<SoundEvent, SoundEvent> GENERATOR_NUCLEAR_HIGH_POWER =
            sound("generator.nuclear.power.high");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_OVERLOAD =
            sound("machine.overload");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_INTERRUPT1 =
            sound("machine.interrupt1");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_CANNER_OPERATE =
            sound("machine.canner.operate");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_CANNER_REVERSE =
            sound("machine.canner.reverse");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_COMPRESSOR_OPERATE =
            sound("machine.compressor.operate");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_ELECTROLYZER_LOOP =
            sound("machine.electrolyzer.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_EXTRACTOR_OPERATE =
            sound("machine.extractor.operate");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_MATTER_GENERATOR_LOOP =
            sound("machine.matter_generator.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_MATTER_GENERATOR_SCRAP =
            sound("machine.matter_generator.scrap");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_FURNACE_ELECTRIC_START =
            sound("machine.furnace.electric.start");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_FURNACE_ELECTRIC_STOP =
            sound("machine.furnace.electric.stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_FURNACE_ELECTRIC_LOOP =
            sound("machine.furnace.electric.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_FURNACE_INDUCTION_START =
            sound("machine.furnace.induction.start");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_FURNACE_INDUCTION_STOP =
            sound("machine.furnace.induction.stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_FURNACE_INDUCTION_LOOP =
            sound("machine.furnace.induction.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_FURNACE_IRON_OPERATE =
            sound("machine.furnace.iron.operate");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_MACERATOR_OPERATE =
            sound("machine.macerator.operate");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_MINER_OPERATE =
            sound("machine.miner.operate");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_OMAT_OPERATE =
            sound("machine.o_mat.operate");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_PUMP_OPERATE =
            sound("machine.pump.operate");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_RECYCLER_OPERATE =
            sound("machine.recycler.operate");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_TELEPORTER_USE =
            sound("machine.teleporter.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_TELEPORTER_CHARGE =
            sound("machine.teleporter.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_TERRAFORMER_LOOP =
            sound("machine.terraformer.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_NUKE_EXPLODE =
            sound("block.nuke.explode");

    private ModSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(
                name,
                () ->
                        SoundEvent.createVariableRangeEvent(
                                Identifier.fromNamespaceAndPath(IndustrialCraft.MOD_ID, name)));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
