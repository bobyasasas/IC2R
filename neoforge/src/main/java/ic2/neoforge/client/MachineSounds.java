package ic2.neoforge.client;

import ic2.neoforge.machine.CannerBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineBlockEntity;
import ic2.neoforge.machine.MachineLoadedEvent;
import ic2.neoforge.registration.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Owns one loop per nearby machine; mode changes compare the required sound identity every tick.
 */
final class MachineSounds {
    private static final Map<MachineBlockEntity, Loop> LOOPS = new IdentityHashMap<>();

    static void loaded(MachineLoadedEvent event) {
        var machine = event.machine();
        if (machine.getLevel() != null && machine.getLevel().isClientSide())
            LOOPS.putIfAbsent(machine, null);
    }

    static void reloaded(SoundEngineLoadEvent event) {
        // The engine discards its channels on reload; recreate loops on the client thread.
        Minecraft.getInstance()
                .execute(
                        () ->
                                LOOPS.replaceAll(
                                        (machine, loop) -> {
                                            if (loop != null) loop.finish();
                                            return null;
                                        }));
    }

    static void tick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        if (client.level == null) {
            LOOPS.values()
                    .forEach(
                            loop -> {
                                if (loop != null) loop.finish();
                            });
            LOOPS.clear();
            return;
        }
        if (client.isPaused()) return;
        var iterator = LOOPS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var machine = entry.getKey();
            var loop = entry.getValue();
            if (machine.isRemoved() || machine.getLevel() != client.level) {
                if (loop != null) loop.finish();
                iterator.remove();
                continue;
            }
            boolean nearby =
                    client.player != null
                            && machine.getBlockPos().distToCenterSqr(client.player.position())
                                    <= 64 * 64;
            SoundEvent required =
                    nearby && machine.getBlockState().getValue(MachineBlock.ACTIVE)
                            ? sound(machine)
                            : null;
            if (loop != null && (loop.event != required || loop.isStopped())) {
                loop.finish();
                entry.setValue(null);
                loop = null;
            }
            if (loop == null && required != null) {
                loop = new Loop(machine, required);
                entry.setValue(loop);
                client.getSoundManager().play(loop);
            }
        }
    }

    private static SoundEvent sound(MachineBlockEntity machine) {
        return switch (machine.kind()) {
            case STEAM_KINETIC_GENERATOR,
                    STEAM_GENERATOR,
                    STEAM_REPRESSURIZER,
                    RT_HEAT_GENERATOR,
                    RT_GENERATOR,
                    MAGNETIZER,
                    TRADE_O_MAT,
                    ITEM_BUFFER,
                    BLOCK_CUTTER,
                    BLAST_FURNACE,
                    MATTER_GENERATOR,
                    NUCLEAR_REACTOR,
                    REACTOR_CHAMBER,
                    REACTOR_FLUID_PORT,
                    REPLICATOR,
                    REACTOR_ACCESS_HATCH,
                    REACTOR_REDSTONE_PORT,
                    RCI_RSH,
                    RCI_LZH,
                    UU_SCANNER,
                    PATTERN_STORAGE,
                    PUMP,
                    MINER,
                    ADV_MINER,
                    TELEPORTER,
                    PERSONAL_CHEST,
                    SORTING_MACHINE,
                    WOODEN_STORAGE_BOX,
                    BRONZE_STORAGE_BOX,
                    IRON_STORAGE_BOX,
                    STEEL_STORAGE_BOX,
                    IRIDIUM_STORAGE_BOX,
                    BATBOX_CHARGEPAD,
                    CESU_CHARGEPAD,
                    MFE_CHARGEPAD,
                    MFSU_CHARGEPAD,
                    CONDENSER,
                    FLUID_REGULATOR,
                    TANK,
                    LIQUID_HEAT_EXCHANGER,
                    FERMENTER,
                    WATER_KINETIC_GENERATOR,
                    WIND_KINETIC_GENERATOR,
                    MANUAL_KINETIC_GENERATOR,
                    SOLID_HEAT_GENERATOR,
                    FLUID_HEAT_GENERATOR,
                    ELECTRIC_HEAT_GENERATOR,
                    ELECTRIC_KINETIC_GENERATOR,
                    STIRLING_GENERATOR,
                    KINETIC_GENERATOR,
                    CENTRIFUGE,
                    ORE_WASHING_PLANT,
                    METAL_FORMER,
                    CROPMATRON,
                    CROP_HARVESTER,
                    SOLAR_GENERATOR ->
                    null;
            case ELECTROLYZER -> ModSounds.MACHINE_ELECTROLYZER_LOOP.get();
            case GEO_GENERATOR, SEMIFLUID_GENERATOR -> ModSounds.GENERATOR_GEOTHERMAL_LOOP.get();
            case WIND_GENERATOR -> ModSounds.GENERATOR_WIND_LOOP.get();
            case WATER_GENERATOR -> ModSounds.GENERATOR_WATER_LOOP.get();
            case GENERATOR -> ModSounds.GENERATOR_GENERATOR_LOOP.get();
            case ELECTRIC_FURNACE -> ModSounds.MACHINE_FURNACE_ELECTRIC_LOOP.get();
            case INDUCTION_FURNACE -> ModSounds.MACHINE_FURNACE_INDUCTION_LOOP.get();
            case RECYCLER -> ModSounds.MACHINE_RECYCLER_OPERATE.get();
            case MACERATOR -> ModSounds.MACHINE_MACERATOR_OPERATE.get();
            case EXTRACTOR -> ModSounds.MACHINE_EXTRACTOR_OPERATE.get();
            case COMPRESSOR -> ModSounds.MACHINE_COMPRESSOR_OPERATE.get();
            case IRON_FURNACE,
                    BATBOX,
                    CESU,
                    MFE,
                    MFSU,
                    LV_TRANSFORMER,
                    MV_TRANSFORMER,
                    HV_TRANSFORMER,
                    EV_TRANSFORMER ->
                    null;
            case CANNER ->
                    switch (((CannerBlockEntity) machine).mode()) {
                        case BOTTLE_SOLID, BOTTLE_LIQUID -> ModSounds.MACHINE_CANNER_OPERATE.get();
                        case EMPTY_LIQUID -> ModSounds.MACHINE_CANNER_REVERSE.get();
                        case ENRICH_LIQUID -> null;
                    };
        };
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        private final MachineBlockEntity machine;
        private final SoundEvent event;

        Loop(MachineBlockEntity machine, SoundEvent event) {
            super(event, SoundSource.BLOCKS, RandomSource.create());
            this.machine = machine;
            this.event = event;
            x = machine.getBlockPos().getX() + .5;
            y = machine.getBlockPos().getY() + .5;
            z = machine.getBlockPos().getZ() + .5;
            looping = true;
            delay = 0;
            volume = .5f;
        }

        void finish() {
            stop();
        }

        @Override
        public void tick() {
            if (machine.isRemoved()
                    || machine.getLevel() != Minecraft.getInstance().level
                    || !machine.getBlockState().getValue(MachineBlock.ACTIVE)
                    || sound(machine) != event) stop();
        }
    }

    private MachineSounds() {}
}
