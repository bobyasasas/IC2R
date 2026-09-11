package ic2.neoforge.machine;

import ic2.core.energy.VoltageTier;
import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.energy.WorldEnergyNetworks;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Personal energy vendor: the owner pins a demanded item and a price; every stack a visitor feeds
 * in — distributed whole into adjacent inventories — buys {@code offer} EU of grid credit. The
 * machine only pulls grid power while credit lasts, buffers it, feeds its charge slot and emits
 * the rest through the marked face. Legacy metered every injected EU at the packet edge; the port
 * gates at grid-connect time and meters arrivals per tick, so intake may overshoot the credit by
 * up to one packet.
 */
public final class EnergyOMatBlockEntity extends PoweredBlockEntity {
    public static final int DEMAND = 0, INPUT = 1, CHARGE = 2;
    public static final int BASE_OFFER = 1000, OFFER_FLOOR = 100, BASE_CAPACITY = 10000;
    private int offer = BASE_OFFER;
    private int paidFor;
    private int tier = 1;
    private double lastSeen;

    public EnergyOMatBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(MachineKind.ENERGY_O_MAT),
                pos,
                state,
                ((MachineBlock) state.getBlock()).kind().capacity(),
                4);
    }

    public int offer() {
        return offer;
    }

    public int paidFor() {
        return paidFor;
    }

    public int voltage() {
        return VoltageTier.fromIcTier(tier).getVoltage();
    }

    @Override
    public boolean emitsTo(Direction side) {
        return side == getBlockState().getValue(MachineBlock.FACING);
    }

    @Override
    public boolean acceptsFrom(Direction side) {
        return !emitsTo(side) && paidFor > 0;
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        // Sink tier 1 accepts every packet voltage, matching the legacy MAX_VALUE sink tier.
        return new EnergyNode.Terminal(
                energy,
                Optional.of(new EnergyNode.Output(voltage(), 1)),
                Optional.of(new EnergyNode.Input(VoltageTier.fromIcTier(1).getVoltage(), 2)));
    }

    @Override
    public void serverTick(ServerLevel level) {
        refreshUpgrades();
        double arrived = energy.stored() - lastSeen;
        if (arrived > 0 && paidFor > 0)
            setPaidFor(paidFor - (int) Math.min(paidFor, Math.ceil(arrived)));
        chargeFromBuffer();
        trade(level);
        lastSeen = energy.stored();
    }

    private void setPaidFor(int value) {
        boolean wasOpen = paidFor > 0;
        paidFor = Math.max(0, value);
        if (wasOpen != paidFor > 0 && level instanceof ServerLevel server)
            WorldEnergyNetworks.invalidate(server);
        setChanged();
    }

    private void chargeFromBuffer() {
        var stack = inventory.stack(CHARGE);
        if (stack.isEmpty() || energy.stored() < 1) return;
        double used = ElectricItemEnergy.charge(stack, energy.stored(), tier, false, false);
        if (used > 0) {
            energy.extract(used);
            inventory.set(CHARGE, ItemResource.of(stack), stack.getCount());
            setChanged();
        }
    }

    private void trade(ServerLevel level) {
        var input = inventory.stack(INPUT);
        if (input.isEmpty()) return;
        var traded = ItemResource.of(input);
        int count = input.getCount();
        try (var transaction = Transaction.openRoot()) {
            if (inventory.extract(INPUT, traded, count, transaction) != count) return;
            if (!distribute(level, traded, count, transaction)) return;
            transaction.commit();
            setPaidFor(paidFor + offer);
        }
    }

    /** Legacy StackUtil.distribute: the trade only counts if the whole stack lands next door. */
    private boolean distribute(
            ServerLevel level, ItemResource traded, int count, Transaction transaction) {
        int remaining = count;
        for (var direction : Direction.values()) {
            if (remaining == 0) break;
            var supply = supply(level, direction);
            if (supply == null) continue;
            for (int slot = 0; slot < supply.size() && remaining > 0; slot++) {
                remaining -= supply.insert(slot, traded, remaining, transaction);
            }
        }
        return remaining == 0;
    }

    private @Nullable ResourceHandler<ItemResource> supply(ServerLevel level, Direction direction) {
        var pos = worldPosition.relative(direction);
        if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return null;
        return level.getCapability(Capabilities.Item.BLOCK, pos, direction.getOpposite());
    }

    /** Legacy per-tick recompute: storage upgrades widen the buffer, transformers raise tiers. */
    private void refreshUpgrades() {
        int storage = 0;
        int transformers = 0;
        for (int slot = kind().upgradeStart(); slot < inventory.size(); slot++) {
            int count = Math.min(64, inventory.getAmountAsInt(slot));
            if (count <= 0) continue;
            if (inventory.getResource(slot).getItem() instanceof UpgradeItem item) {
                switch (item.kind()) {
                    case ENERGY_STORAGE -> storage += count;
                    case TRANSFORMER -> transformers += count;
                    default -> {}
                }
            }
        }
        tier = Math.min(5, 1 + transformers);
        double capacity = BASE_CAPACITY + (double) BASE_CAPACITY * storage;
        if (energy.capacity() != capacity) energy.resize(capacity);
    }

    @Override
    protected int inventorySlotLimit(int slot) {
        return slot == DEMAND ? 1 : super.inventorySlotLimit(slot);
    }

    @Override
    public boolean menuAction(int id) {
        // Legacy network events 0-3/4-7: -/+ 100000, 10000, 1000, 100 with a 100 EU floor.
        if (id < 0 || id > 7) return false;
        int delta =
                switch (id) {
                    case 0 -> -100000;
                    case 1 -> -10000;
                    case 2 -> -1000;
                    case 3 -> -100;
                    case 4 -> 100000;
                    case 5 -> 10000;
                    case 6 -> 1000;
                    default -> 100;
                };
        offer = Math.max(OFFER_FLOOR, offer + delta);
        setChanged();
        return true;
    }

    @Override
    public int menuValue(int index) {
        return index == 0 ? offer : 0;
    }

    @Override
    public int progress() {
        return 0;
    }

    @Override
    public int progressMaximum() {
        return 0;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        // The demand template is owner-editor only; visitors feed the input and batteries
        // rotate through the charge slot.
        return new ResourcePort<>(
                inventory,
                slot -> slot == INPUT,
                slot -> slot == INPUT || slot == CHARGE,
                (slot, resource) -> slot == INPUT || slot == CHARGE);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        offer = Math.max(OFFER_FLOOR, input.getIntOr("euOffer", BASE_OFFER));
        paidFor = Math.max(0, input.getIntOr("paidFor", 0));
        lastSeen = energy.stored();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("euOffer", offer);
        output.putInt("paidFor", paidFor);
    }
}
