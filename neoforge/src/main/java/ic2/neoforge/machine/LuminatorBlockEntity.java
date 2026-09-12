package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.energy.CableBlock;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Legacy TileEntityLuminator: a wall lamp whose grid sink holds at most 5 EU and pays 0.25 EU/t
 * while lit. "Lit" is the redstone input XOR the invert flag, so a right click or a wrench flips
 * between night-light and normally-lit modes. Right-clicking with a charged electric item instead
 * discharges it straight into the lamp — legacy forceAddEnergy bypasses the 5 EU cap up to
 * 10000 EU, letting the lamp run unpowered for hours. Lit lamps set fire to monsters standing
 * inside them; the undead burn twice as long.
 */
public final class LuminatorBlockEntity extends PoweredBlockEntity {
    public static final double CAPACITY = 5.0;
    private static final double DRAIN_PER_TICK = 0.25;
    /** Hand discharge tops the store up to this legacy ceiling regardless of the 5 EU cap. */
    private static final double DISCHARGE_LIMIT = 10000.0;

    /** Ignition time in ticks: undead take 20, everything else 10 (legacy values). */
    private static final int UNDEAD_FIRE_TICKS = 20;
    private static final int FIRE_TICKS = 10;

    private boolean invertRedstone;
    private boolean placementChecked;

    public LuminatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.LUMINATOR), pos, state, CAPACITY, 0);
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        return EnergyNode.Terminal.sink(
                energy, ic2.core.energy.VoltageTier.fromIcTier(kind().electricalTier()).getVoltage(),
                1);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!placementChecked) {
            placementChecked = true;
            checkPlacement();
        }
        boolean lit = level.hasNeighborSignal(worldPosition) != invertRedstone
                && energy.consume(DRAIN_PER_TICK);
        setActive(lit);
        if (lit) igniteTouchingMonsters(level);
    }

    private void igniteTouchingMonsters(ServerLevel level) {
        for (Monster monster : level.getEntitiesOfClass(
                Monster.class, igniteBounds(getBlockState().getValue(MachineBlock.FACING))
                        .move(worldPosition),
                Entity::isAlive)) {
            monster.setRemainingFireTicks(
                    monster.isInvertedHealAndHarm() ? UNDEAD_FIRE_TICKS : FIRE_TICKS);
        }
    }

    /**
     * Legacy aabbMap, transcribed verbatim: a ~0.53³ box anchored at the support face and — a
     * legacy quirk kept faithful — offset toward the +x/+y corner, which also lets it protrude
     * into the support block for some facings.
     */
    private static AABB igniteBounds(Direction facing) {
        Direction side = facing.getOpposite();
        int dx = side.getStepX(), dy = side.getStepY(), dz = side.getStepZ();
        double xS = (dx + 1) / 2.0 * 0.9375, yS = (dy + 1) / 2.0 * 0.9375,
                zS = (dz + 1) / 2.0 * 0.9375;
        double xE = 0.0625 + (dx + 2) / 2.0 * 0.9375,
                yE = 0.0625 + (dy + 2) / 2.0 * 0.9375,
                zE = 0.0625 + (dz + 2) / 2.0 * 0.9375;
        return new AABB(Math.min(xS, xE), Math.min(yS, yE), Math.min(zS, zE),
                Math.max(xS, xE), Math.max(yS, yE), Math.max(zS, zE));
    }

    /** Legacy isValidPosition: sturdy support face, or an energy emitter to mount on. */
    static boolean supported(ServerLevel level, BlockPos pos, Direction facing) {
        BlockPos supportPos = pos.relative(facing.getOpposite());
        var state = level.getBlockState(supportPos);
        if (state.isFaceSturdy(level, supportPos, facing)) return true;
        // Cables are emitters in the legacy grid, so a lamp may hang off one.
        if (state.getBlock() instanceof CableBlock) return true;
        return level.getBlockEntity(supportPos) instanceof PoweredBlockEntity powered
                && powered.energyNode().output().isPresent();
    }

    /** Legacy checkPlacement: a lamp that lost its support pops off with a drop. */
    void checkPlacement() {
        if (level instanceof ServerLevel server && !supported(
                server, worldPosition, getBlockState().getValue(MachineBlock.FACING))) {
            server.destroyBlock(worldPosition, true);
        }
    }

    /**
     * Legacy onActivated: a charged electric item in the hand is drained into the lamp (up to the
     * 10000 EU quirk ceiling); anything else toggles the redstone inversion.
     */
    public void use(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        double amount = DISCHARGE_LIMIT - energy.stored();
        if (!stack.isEmpty() && amount > 0.0
                && (amount = ElectricItemEnergy.discharge(
                        stack, amount, kind().electricalTier(), true, true, true)) > 0.0) {
            ElectricItemEnergy.discharge(
                    stack, amount, kind().electricalTier(), true, true, false);
            energy.forceAdd(amount);
        } else {
            toggleInvert();
        }
    }

    /** Legacy setFacingWrench: the wrench flips the inversion instead of rotating. */
    public void toggleInvert() {
        invertRedstone = !invertRedstone;
    }

    boolean inverted() {
        return invertRedstone;
    }

    /** Legacy ComparatorEmitter over energy.getComparatorValue. */
    public int comparator() {
        return Math.min((int) (energy.stored() * 15.0 / energy.capacity()), 15);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        // The store is clamped to the 5 EU capacity on load; re-add what a hand discharge had
        // pushed past the cap so the quirk survives a save/load round trip.
        double raw = input.getDoubleOr("energy", 0);
        super.loadAdditional(input);
        if (Double.isFinite(raw) && raw > energy.capacity()) {
            energy.forceAdd(raw - energy.capacity());
        }
        invertRedstone = input.getBooleanOr("invert", false);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("invert", invertRedstone);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory, slot -> false, slot -> false, (slot, resource) -> false);
    }

    @Override
    public int progress() {
        return 0;
    }

    @Override
    public int progressMaximum() {
        return 0;
    }
}
