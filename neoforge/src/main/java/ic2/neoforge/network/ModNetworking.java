package ic2.neoforge.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Payload registration for the migration; one registrar per protocol version. */
public final class ModNetworking {
    public static void register(IEventBus bus) {
        bus.addListener(
                (RegisterPayloadHandlersEvent event) -> {
                    PayloadRegistrar registrar = event.registrar("1");
                    registrar.playToServer(
                            ObscuratorScanPayload.TYPE,
                            ObscuratorScanPayload.STREAM_CODEC,
                            ObscuratorScanPayload::handle);
                });
    }

    private ModNetworking() {}
}
