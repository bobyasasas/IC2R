package ic2.neoforge.machine;

import com.mojang.serialization.Codec;

import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.energy.WorldEnergyNetworks;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.List;

/**
 * Legacy TileEntityChunkLoader: keeps its loaded chunks ticketed while powered, one EU per tick
 * per chunk. The GUI chunk picking stays with the (deferred) legacy container port; without it
 * the set holds the machine's own chunk, exactly what placement seeds in legacy onPlaced.
 */
public final class ChunkLoaderBlockEntity extends PoweredBlockEntity {
    public static final int DISCHARGE = 0;

    /** Legacy IC2Config.balance.euPerChunk default; the port has no balance config family yet. */
    public static final double EU_PER_CHUNK = 1.0;

    private final LongSet loadedChunks = new LongOpenHashSet();

    private int tier = 1;

    public ChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(MachineKind.CHUNK_LOADER),
                pos,
                state,
                MachineKind.CHUNK_LOADER.capacity(),
                MachineKind.CHUNK_LOADER.slots());
        refreshUpgrades();
    }

    /** Legacy setOverclockRates: transformer upgrades raise the sink tier, storage the buffer. */
    private void refreshUpgrades() {
        int transformers = 0, storage = 0;
        for (int slot = MachineKind.CHUNK_LOADER.upgradeStart();
                slot < inventory.size();
                slot++) {
            var resource = inventory.getResource(slot);
            int count = Math.min(64, inventory.getAmountAsInt(slot));
            if (!(resource.getItem() instanceof UpgradeItem item)) continue;
            switch (item.kind()) {
                case TRANSFORMER -> transformers += count;
                case ENERGY_STORAGE -> storage += count;
                default -> {}
            }
        }
        tier = Math.min(5, 1 + transformers);
        double capacity = 2500.0 + 10000.0 * storage;
        if (capacity != energy.capacity()) {
            energy.resize(capacity);
            if (level instanceof ServerLevel server) WorldEnergyNetworks.invalidate(server);
        }
    }

    public LongSet loadedChunks() {
        return loadedChunks;
    }

    /** Legacy isChunkInRange: the pickable nine-by-nine window around the machine. */
    public boolean isChunkInRange(ChunkPos chunk) {
        ChunkPos main = ownChunk();
        return Math.abs(chunk.x() - main.x()) <= 4 && Math.abs(chunk.z() - main.z()) <= 4;
    }

    public boolean addChunk(ChunkPos chunk) {
        if (!(level instanceof ServerLevel server)) return false;
        if (loadedChunks.size() >= 9 || !isChunkInRange(chunk)) return false;
        boolean added = loadedChunks.add(chunk.pack());
        if (added) {
            if (getBlockState().getValue(MachineBlock.ACTIVE)) ChunkLoaderTickets.add(server, chunk);
            setChanged();
        }
        return added;
    }

    public boolean removeChunk(ChunkPos chunk) {
        if (!(level instanceof ServerLevel server)) return false;
        if (chunk.equals(ownChunk())) return false;
        boolean removed = loadedChunks.remove(chunk.pack());
        if (removed) {
            if (getBlockState().getValue(MachineBlock.ACTIVE)) ChunkLoaderTickets.remove(server, chunk);
            setChanged();
        }
        return removed;
    }

    private ChunkPos ownChunk() {
        return ChunkPos.containing(worldPosition);
    }

    private void ensureOwnChunk() {
        loadedChunks.add(ownChunk().pack());
    }

    @Override
    public void serverTick(ServerLevel level) {
        ensureOwnChunk();

        var battery = inventory.stack(DISCHARGE);
        double charged = ElectricItemEnergy.discharge(battery, energy.free(), 1, false, true, false);
        if (charged > 0) {
            inventory.set(DISCHARGE, ItemResource.of(battery), battery.getCount());
            energy.insert(charged);
        }

        boolean active = energy.consume(loadedChunks.size() * EU_PER_CHUNK);
        boolean wasActive = getBlockState().getValue(MachineBlock.ACTIVE);
        if (active != wasActive) {
            if (active) ticketAll(level, true);
            else ticketAll(level, false);
            setActive(active);
        }
    }

    private void ticketAll(ServerLevel level, boolean add) {
        for (long chunk : loadedChunks) {
            var pos = ChunkPos.unpack(chunk);
            if (add) ChunkLoaderTickets.add(level, pos);
            else ChunkLoaderTickets.remove(level, pos);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Legacy onLoaded re-registered tickets for an active loader; the vanilla storage also
        // replays persist-flagged tickets, so only fill the gap when none survived.
        if (level instanceof ServerLevel server
                && getBlockState().getValue(MachineBlock.ACTIVE)
                && !ChunkLoaderTickets.holds(server, ownChunk())) {
            ensureOwnChunk();
            ticketAll(server, true);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server && state.getValue(MachineBlock.ACTIVE))
            ticketAll(server, false);
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        return EnergyNode.Terminal.sink(
                energy,
                ic2.core.energy.VoltageTier.fromIcTier(tier).getVoltage(),
                tier);
    }

    /** Chunk loading has no progress bar; the menu keeps the empty defaults. */
    @Override
    public int progress() {
        return 0;
    }

    @Override
    public int progressMaximum() {
        return 1;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory, slot -> slot == DISCHARGE, slot -> false);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        loadedChunks.clear();
        List<Long> chunks = input.read("loadedChunks", Codec.LONG.listOf()).orElse(List.of());
        loadedChunks.addAll(chunks);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        ensureOwnChunk();
        super.saveAdditional(output);
        var chunks = new java.util.ArrayList<Long>(loadedChunks.size());
        for (long value : loadedChunks) chunks.add(value);
        output.store("loadedChunks", Codec.LONG.listOf(), chunks);
    }
}
