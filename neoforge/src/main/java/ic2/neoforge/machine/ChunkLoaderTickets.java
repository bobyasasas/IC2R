package ic2.neoforge.machine;

import ic2.neoforge.IndustrialCraft;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Chunk-loader tickets. Legacy drove {@code addRegionTicket} through its own SavedData recovery
 * chain; 26.1.2 persists tickets of a persist-flagged type inside the vanilla TicketStorage, so
 * the port only has to add and remove them around the machine's active state.
 */
public final class ChunkLoaderTickets {
    private static final DeferredRegister<TicketType> TYPES =
            DeferredRegister.create(BuiltInRegistries.TICKET_TYPE, IndustrialCraft.MOD_ID);

    // Persist + load + simulate: the legacy region ticket kept chunks fully loaded and ticking.
    private static final DeferredHolder<TicketType, TicketType> CHUNK_LOADER =
            TYPES.register("chunk_loader", () -> new TicketType(
                    TicketType.NO_TIMEOUT,
                    TicketType.FLAG_PERSIST | TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION));

    /** Legacy ChunkLoaderLogic radius: the loaded chunk plus two rings around it. */
    private static final int RADIUS = 2;

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }

    public static TicketType type() {
        return CHUNK_LOADER.value();
    }

    public static void add(ServerLevel level, ChunkPos pos) {
        level.getChunkSource().addTicketWithRadius(type(), pos, RADIUS);
    }

    public static void remove(ServerLevel level, ChunkPos pos) {
        level.getChunkSource().removeTicketWithRadius(type(), pos, RADIUS);
    }

    /** True when this chunk currently carries at least one chunk-loader ticket. */
    public static boolean holds(ServerLevel level, ChunkPos pos) {
        TicketStorage storage = level.getDataStorage().get(TicketStorage.TYPE);
        if (storage == null) return false;
        return storage.getTickets(pos.pack()).stream().anyMatch(ticket -> ticket.getType() == type());
    }

    private ChunkLoaderTickets() {}
}
