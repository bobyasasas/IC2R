package ic2.neoforge.machine;

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

/**
 * Player trade terminal: the owner pins a demand and an offer template, visitors drop the demanded
 * stack into the input and receive the offer. With the infinite flag the offer is conjured;
 * otherwise it is drawn from adjacent supply inventories and the traded-in stack is distributed
 * back into them.
 */
public final class TradeOMatBlockEntity extends MachineBlockEntity {
    public static final int DEMAND = 0, OFFER = 1, INPUT = 2, OUTPUT = 3;
    private boolean infinite;
    private int stock = -1, totalTradeCount;

    public TradeOMatBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.TRADE_O_MAT), pos, state, 4);
    }

    public boolean infinite() {
        return infinite;
    }

    public void toggleInfinite() {
        infinite = !infinite;
        stock = infinite ? -1 : 0;
        setChanged();
    }

    public int stock() {
        return stock;
    }

    public int totalTradeCount() {
        return totalTradeCount;
    }

    private static boolean templateMatches(
            ResourceHandler<ItemResource> handler, int slot, ItemResource input) {
        var template = handler.getResource(slot);
        return !template.isEmpty() && template.equals(input);
    }

    @Override
    public void serverTick(ServerLevel level) {
        trade(level);
        if (!infinite && stock >= 0) stock = countSupply(level, offerResource());
        else if (!infinite) stock = 0;
    }

    private @Nullable ItemResource offerResource() {
        var offer = inventory.getResource(OFFER);
        return offer.isEmpty() ? null : offer;
    }

    private void trade(ServerLevel level) {
        var input = inventory.stack(INPUT);
        if (input.isEmpty()) return;
        var inputResource = ItemResource.of(input);
        var demand = inventory.getResource(DEMAND);
        var offer = inventory.getResource(OFFER);
        if (demand.isEmpty() || offer.isEmpty() || !demand.equals(inputResource)) return;
        int offerCount = inventory.getAmountAsInt(OFFER);
        try (var transaction = Transaction.openRoot()) {
            if (!infinite && !drawOffer(level, offer, offerCount, transaction)) return;
            if (inventory.insert(OUTPUT, offer, offerCount, transaction) != offerCount) return;
            if (inventory.extract(INPUT, inputResource, input.getCount(), transaction)
                    != input.getCount()) return;
            if (!infinite) {
                giveBack(level, inputResource, input.getCount(), transaction);
                stock--;
            }
            totalTradeCount++;
            transaction.commit();
            setChanged();
        }
    }

    private boolean drawOffer(
            ServerLevel level, ItemResource offer, int count, Transaction transaction) {
        int remaining = count;
        for (var direction : Direction.values()) {
            if (remaining == 0) break;
            var supply = supply(level, direction);
            if (supply == null) continue;
            for (int slot = 0; slot < supply.size() && remaining > 0; slot++) {
                if (!supply.getResource(slot).equals(offer)) continue;
                int took =
                        supply.extract(
                                slot,
                                offer,
                                Math.min(remaining, supply.getAmountAsInt(slot)),
                                transaction);
                remaining -= took;
            }
        }
        return remaining == 0;
    }

    private void giveBack(
            ServerLevel level, ItemResource traded, int count, Transaction transaction) {
        for (var direction : Direction.values()) {
            if (count == 0) break;
            var supply = supply(level, direction);
            if (supply == null) continue;
            for (int slot = 0; slot < supply.size() && count > 0; slot++) {
                int given = supply.insert(slot, traded, count, transaction);
                count -= given;
            }
        }
    }

    private int countSupply(ServerLevel level, @Nullable ItemResource offer) {
        if (offer == null) return 0;
        int count = 0;
        for (var direction : Direction.values()) {
            var supply = supply(level, direction);
            if (supply == null) continue;
            for (int slot = 0; slot < supply.size(); slot++) {
                if (supply.getResource(slot).equals(offer)) count += supply.getAmountAsInt(slot);
            }
        }
        return count;
    }

    private @Nullable ResourceHandler<ItemResource> supply(ServerLevel level, Direction direction) {
        var pos = worldPosition.relative(direction);
        if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return null;
        return level.getCapability(Capabilities.Item.BLOCK, pos, direction.getOpposite());
    }

    @Override
    public boolean menuAction(int id) {
        if (id == 0) {
            toggleInfinite();
            return true;
        }
        return false;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> infinite ? 1 : 0;
            case 1 -> stock;
            case 2 -> totalTradeCount;
            default -> 0;
        };
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
        // Demand and offer templates are owner-editors only; input and output are automated.
        return new ResourcePort<>(
                inventory,
                slot -> slot == INPUT,
                slot -> slot == OUTPUT,
                (slot, resource) -> slot == INPUT || slot == OUTPUT);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        infinite = input.getBooleanOr("infinite", false);
        stock = input.getIntOr("stock", -1);
        totalTradeCount = input.getIntOr("trades", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("infinite", infinite);
        output.putInt("stock", stock);
        output.putInt("trades", totalTradeCount);
    }
}
