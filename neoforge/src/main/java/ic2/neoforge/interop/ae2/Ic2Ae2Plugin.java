package ic2.neoforge.interop.ae2;

import ic2.core.energy.EnergyStore;
import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.energy.WorldEnergyNetworks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Port of the legacy Ic2Ae2Plugin: AE2 energy acceptors become IC2 grid sinks so EU
 * generators can feed an Applied Energistics network, at two AE per EU. The AE2 grid
 * API is reached through the same reflection as legacy (never a compile dependency);
 * without it the bridge falls back to the block energy handler on any face. Acceptors
 * join the grid as external sink terminals (tier four, 512 EU packets) and drain their
 * one-tick buffer into AE after each distribution pass.
 */
public final class Ic2Ae2Plugin implements WorldEnergyNetworks.ExternalTerminals {
    public static final double EU_TO_AE_RATIO = 2.0;

    private static final TagKey<Block> ACCEPTORS =
            TagKey.create(
                    Registries.BLOCK,
                    Identifier.fromNamespaceAndPath("ic2", "ae2_energy_acceptor"));
    /** Legacy sink tier four: 512 EU packets, generous per-tick packet count. */
    private static final int SINK_VOLTAGE = 512;
    private static final int SINK_AMPS = 30;
    private static final double BUFFER = SINK_VOLTAGE * (double) SINK_AMPS;

    private static boolean ae2GridApiAvailable;
    private static Method ae2InjectMethod;
    private static Method ae2GetExposedNodeMethod;
    private static Method ae2GetEnergyServiceMethod;
    private static Object ae2ActionableModulate;

    private final Map<ServerLevel, Map<BlockPos, EnergyStore>> bridges = new HashMap<>();

    static {
        try {
            Class<?> gridHelperClass = Class.forName("appeng.api.networking.GridHelper");
            Class<?> energyServiceClass =
                    Class.forName("appeng.api.networking.energy.IEnergyService");
            Class<?> actionableClass = Class.forName("appeng.api.config.Actionable");
            Class<?> iGridClass = Class.forName("appeng.api.networking.IGrid");
            ae2GetExposedNodeMethod =
                    gridHelperClass.getMethod("getExposedNode", Level.class, BlockPos.class, Direction.class);
            ae2InjectMethod = energyServiceClass.getMethod("injectPower", double.class, actionableClass);
            ae2GetEnergyServiceMethod = iGridClass.getMethod("getEnergyService");
            ae2ActionableModulate = actionableClass.getField("MODULATE").get(null);
            ae2GridApiAvailable = true;
        } catch (Exception ignored) {
            ae2GridApiAvailable = false;
        }
    }

    public static void init() {
        WorldEnergyNetworks.setExternalTerminals(new Ic2Ae2Plugin());
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getState().is(ACCEPTORS))
            WorldEnergyNetworks.invalidate(level);
    }

    public static void onBlockBroken(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getState().is(ACCEPTORS))
            WorldEnergyNetworks.invalidate(level);
    }

    private static boolean isAcceptor(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ACCEPTORS);
    }

    @Override
    public EnergyNode.Terminal terminal(ServerLevel level, BlockPos pos) {
        if (!isAcceptor(level, pos)) return null;
        EnergyStore store =
                bridges.computeIfAbsent(level, ignored -> new HashMap<>())
                        .computeIfAbsent(pos, ignored -> new EnergyStore(BUFFER));
        return EnergyNode.Terminal.sink(store, SINK_VOLTAGE, SINK_AMPS);
    }

    @Override
    public void afterDistribution(ServerLevel level) {
        Map<BlockPos, EnergyStore> levelBridges = bridges.get(level);
        if (levelBridges == null || levelBridges.isEmpty()) return;
        Iterator<Map.Entry<BlockPos, EnergyStore>> iterator = levelBridges.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!isAcceptor(level, entry.getKey())) {
                iterator.remove();
                continue;
            }
            double stored = entry.getValue().stored();
            if (stored <= 0) continue;
            double accepted = forward(level, entry.getKey(), stored);
            if (accepted > 0) entry.getValue().extract(accepted);
        }
    }

    /** Returns the EU equivalent that landed in the AE network or fallback handler. */
    private double forward(ServerLevel level, BlockPos pos, double eu) {
        if (ae2GridApiAvailable) {
            Double acceptedAe = injectViaGrid(level, pos, eu * EU_TO_AE_RATIO);
            if (acceptedAe != null && acceptedAe > 0) return acceptedAe / EU_TO_AE_RATIO;
        }
        for (Direction side : Direction.values()) {
            EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, pos, side);
            if (handler == null
                    || handler.getAmountAsLong() >= handler.getCapacityAsLong()) continue;
            int request = (int) Math.min(Integer.MAX_VALUE, Math.ceil(eu * EU_TO_AE_RATIO));
            try (Transaction transaction = Transaction.openRoot()) {
                int acceptedAe = handler.insert(request, transaction);
                transaction.commit();
                if (acceptedAe > 0) return Math.min(acceptedAe / EU_TO_AE_RATIO, eu);
            }
        }
        return 0;
    }

    @javax.annotation.Nullable
    private Double injectViaGrid(ServerLevel level, BlockPos pos, double aeAmount) {
        try {
            Object node = null;
            for (Direction side : Direction.values()) {
                node = ae2GetExposedNodeMethod.invoke(null, level, pos, side);
                if (node != null) break;
            }
            if (node == null) return null;
            Object grid = node.getClass().getMethod("getGrid").invoke(node);
            if (grid == null) return null;
            Object service = ae2GetEnergyServiceMethod.invoke(grid);
            if (service == null) return null;
            return (Double) ae2InjectMethod.invoke(service, aeAmount, ae2ActionableModulate);
        } catch (Exception ignored) {
            return null;
        }
    }
}
