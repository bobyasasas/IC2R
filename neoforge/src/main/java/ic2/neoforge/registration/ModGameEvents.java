package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameEvents {
    private static final DeferredRegister<GameEvent> EVENTS =
            DeferredRegister.create(Registries.GAME_EVENT, IndustrialCraft.MOD_ID);
    public static final DeferredHolder<GameEvent, GameEvent> TOOL_USE = create("tool_use"),
            GENERATOR_ACTIVATE = create("generator_activate"),
            GENERATOR_DEACTIVATE = create("generator_deactivate"),
            MACHINE_ACTIVATE = create("machine_activate"),
            MACHINE_DEACTIVATE = create("machine_deactivate");

    private static DeferredHolder<GameEvent, GameEvent> create(String id) {
        return EVENTS.register(id, () -> new GameEvent(16));
    }

    public static void register(IEventBus bus) {
        EVENTS.register(bus);
    }

    private ModGameEvents() {}
}
