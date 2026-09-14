package ic2.neoforge.machine;

import ic2.core.energy.EnergyStore;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModSounds;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ports the legacy teleporter: redstone power plus a linked target teleports the closest unmounted
 * entity in the surrounding box, paying weight x (distance+10)^0.7 x 5 EU pulled straight from
 * adjacent storage devices instead of the cable grid.
 */
public final class TeleporterBlockEntity extends MachineBlockEntity {
    private static final int COOLDOWN_TICKS = 20;
    // Legacy idle dust: blue while armed, green while cooling down (an int ARGB colour here).
    private static final DustParticleOptions BLUE_DUST = new DustParticleOptions(0xFF0000FF, 1.0F);
    private static final DustParticleOptions GREEN_DUST = new DustParticleOptions(0xFF00FF00, 1.0F);
    private BlockPos target;
    private int cooldown;
    private int targetCheckTicker = ThreadLocalRandom.current().nextInt(1024);

    public TeleporterBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(((MachineBlock) state.getBlock()).kind()), pos, state, 0);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(inventory, slot -> false, slot -> false);
    }

    @Override
    public void serverTick(ServerLevel level) {
        boolean coolingDown = cooldown > 0;
        if (coolingDown) cooldown--;
        if (!level.hasNeighborSignal(worldPosition) || target == null) {
            setActive(false);
            return;
        }
        setActive(true);
        // Legacy renders this dust client-side every active tick; the port broadcasts it.
        level.sendParticles(
                coolingDown ? GREEN_DUST : BLUE_DUST,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 1.5,
                worldPosition.getZ() + 0.5,
                2,
                0.5,
                1.0,
                0.5,
                0.0);
        // Legacy searches no entities while cooling down, yet still runs its target check below.
        Entity closest = null;
        if (!coolingDown) {
            var bounds =
                    new AABB(
                            worldPosition.getX() - 1,
                            worldPosition.getY(),
                            worldPosition.getZ() - 1,
                            worldPosition.getX() + 2,
                            worldPosition.getY() + 3,
                            worldPosition.getZ() + 2);
            double best = Double.MAX_VALUE;
            for (var entity :
                    level.getEntitiesOfClass(
                            Entity.class, bounds, e -> e.isAlive() && e.getVehicle() == null)) {
                double distance =
                        worldPosition.distToLowCornerSqr(entity.getX(), entity.getY(), entity.getZ());
                if (distance < best) {
                    best = distance;
                    closest = entity;
                }
            }
        }
        if (closest != null && verifyTarget(level)) {
            teleport(closest, Math.sqrt(worldPosition.distSqr(target)));
        } else if (++targetCheckTicker % 1024 == 0) {
            // Legacy sweeps for a destroyed target every 1024 ticks even with no entity around.
            verifyTarget(level);
        }
    }

    private boolean verifyTarget(ServerLevel level) {
        if (level.getBlockEntity(target) instanceof TeleporterBlockEntity) return true;
        target = null;
        setChanged();
        updateComparatorSignal();
        return false;
    }

    public void teleport(Entity user, double distance) {
        int weight = weightOf(user);
        if (weight == 0) return;
        int cost = (int) (weight * Math.pow(distance + 10.0, 0.7) * 5.0);
        if (cost > availableEnergy()) return;
        consumeEnergy(cost);
        user.teleportTo(target.getX() + 0.5, target.getY() + 1.5, target.getZ() + 0.5);
        if (level instanceof ServerLevel server
                && server.getBlockEntity(target) instanceof TeleporterBlockEntity other) {
            other.onTeleportTo();
        }
        if (level instanceof ServerLevel server) {
            server.playSound(
                    null,
                    worldPosition,
                    ModSounds.MACHINE_TELEPORTER_USE.get(),
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
            // Legacy event 0: a blue burst over both ends of the link.
            burst(server, worldPosition);
            burst(server, target);
        }
        setChanged();
    }

    private static void burst(ServerLevel level, BlockPos pos) {
        level.sendParticles(
                BLUE_DUST,
                pos.getX() + 0.5,
                pos.getY() + 1.5,
                pos.getZ() + 0.5,
                20,
                0.5,
                1.0,
                0.5,
                0.0);
    }

    /** Legacy linkage hook: the receiving teleporter cools down before teleporting again. */
    public void onTeleportTo() {
        cooldown = COOLDOWN_TICKS;
        setChanged();
    }

    private int availableEnergy() {
        int energy = 0;
        for (var direction : Direction.values()) {
            var store = adjacentStore(direction);
            if (store != null) energy += (int) store.stored();
        }
        return energy;
    }

    /** Legacy teleporters drain adjacent storage devices directly, spread across all of them. */
    private void consumeEnergy(int energy) {
        var sources = new ArrayList<EnergyStore>();
        for (var direction : Direction.values()) {
            var store = adjacentStore(direction);
            if (store != null && store.stored() > 0) sources.add(store);
        }
        while (energy > 0 && !sources.isEmpty()) {
            int share = (energy + sources.size() - 1) / sources.size();
            var iterator = sources.iterator();
            while (iterator.hasNext()) {
                var store = iterator.next();
                int drain = Math.min(share, energy);
                int stored = (int) store.stored();
                if (stored <= drain) {
                    energy -= stored;
                    store.extract(stored);
                    iterator.remove();
                } else {
                    energy -= drain;
                    store.extract(drain);
                }
            }
        }
    }

    private EnergyStore adjacentStore(Direction direction) {
        BlockEntity neighbour = null;
        if (level != null) {
            neighbour = level.getBlockEntity(worldPosition.relative(direction));
        }
        if (neighbour instanceof EnergyStorageBlockEntity storage) return storage.energy();
        if (neighbour instanceof TransformerBlockEntity transformer) return transformer.energy();
        return null;
    }

    /**
     * Legacy weights with the inventory-weight option on (its default). The legacy player path
     * counted the main hand twice and missed nothing else; that quirk is not kept.
     */
    private int weightOf(Entity user) {
        int weight = 0;
        if (user instanceof ItemEntity item) {
            var stack = item.getItem();
            weight += 100 * stack.getCount() / Math.max(1, stack.getMaxStackSize());
        } else if (user instanceof Animal
                || user instanceof AbstractMinecart
                || user instanceof AbstractBoat) {
            weight += 100;
        } else if (user instanceof Player player) {
            weight += 1000;
            for (var stack : player.getInventory().getNonEquipmentItems()) {
                weight += stackCost(stack);
            }
            for (var slot : EquipmentSlot.values()) {
                if (slot != EquipmentSlot.BODY) weight += stackCost(player.getItemBySlot(slot));
            }
        } else if (user instanceof Ghast) {
            weight += 2500;
        } else if (user instanceof LivingEntity living) {
            // legacy weighed the bosses separately (WitherBoss 5000, EnderDragon 10000);
            // plain Monster matching alone would score the wither 500 and the dragon 0
            if (living instanceof EnderDragon) weight += 10000;
            else if (living instanceof WitherBoss) weight += 5000;
            else if (living instanceof Monster) weight += 500;
            for (var slot : EquipmentSlot.values()) {
                if (slot != EquipmentSlot.BODY) weight += stackCost(living.getItemBySlot(slot));
            }
        }
        for (var passenger : user.getPassengers()) weight += weightOf(passenger);
        return weight;
    }

    private static int stackCost(ItemStack stack) {
        return stack.isEmpty() ? 0 : 100 * stack.getCount() / Math.max(1, stack.getMaxStackSize());
    }

    /** Legacy comparator: a full signal while a target is linked, none otherwise. */
    public int comparator() {
        return target != null ? 15 : 0;
    }

    private void updateComparatorSignal() {
        if (level instanceof ServerLevel server)
            server.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Legacy refreshes the comparator level once the tile comes back from disk.
        updateComparatorSignal();
    }

    public boolean hasTarget() {
        return target != null;
    }

    public BlockPos getTarget() {
        return target;
    }

    public void setTarget(BlockPos pos) {
        target = pos;
        setChanged();
        updateComparatorSignal();
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        cooldown = input.getIntOr("cooldown", 0);
        target = input.read("target", BlockPos.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("cooldown", cooldown);
        if (target != null) output.store("target", BlockPos.CODEC, target);
    }
}
