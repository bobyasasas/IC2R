package ic2.neoforge.machine;

import net.neoforged.bus.api.Event;

/** Common lifecycle notification; client services subscribe without leaking renderer types here. */
public final class MachineLoadedEvent extends Event {
    private final MachineBlockEntity machine;

    public MachineLoadedEvent(MachineBlockEntity machine) {
        this.machine = machine;
    }

    public MachineBlockEntity machine() {
        return machine;
    }
}
