package ic2.neoforge.machine;

import ic2.core.energy.VoltageTier;
import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.item.tfbp.TerraformingBlueprintItem;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import org.jspecify.annotations.Nullable;

/**
 * Legacy TileEntityTerra: hand-loaded with a terraforming blueprint it pays {@code consume} EU
 * per successful terrain edit — the walk targets near the last success, or spreads wider after
 * failures — and a tenth of that per failed attempt. There is no GUI: right-click ejects the
 * cartridge or inserts one from the hand. Legacy metered its draw tier dynamically; the port
 * pins a static tier-4 sink, which every blueprint's draw fits.
 */
public final class TerraformerBlockEntity extends PoweredBlockEntity {
    public static final int CAPACITY = 100000;

    private ItemStack tfbp = ItemStack.EMPTY;
    private int failedAttempts;
    private int inactiveTicks;
    private @Nullable BlockPos lastPos;

    public TerraformerBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.TERRAFORMER), pos, state, CAPACITY, 0);
    }

    public ItemStack blueprint() {
        return tfbp;
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        return EnergyNode.Terminal.sink(
                energy, VoltageTier.fromIcTier(kind().electricalTier()).getVoltage(), 1);
    }

    @Override
    public void serverTick(ServerLevel level) {
        boolean newActive = false;
        if (tfbp.getItem() instanceof TerraformingBlueprintItem blueprint) {
            double consume = blueprint.consume();
            if (energy.stored() >= consume) {
                newActive = true;
                BlockPos nextPos;
                var random = level.getRandom();
                if (lastPos != null) {
                    int range = blueprint.range() / 10;
                    nextPos = new BlockPos(
                            lastPos.getX() - random.nextInt(range + 1) + random.nextInt(range + 1),
                            worldPosition.getY(),
                            lastPos.getZ() - random.nextInt(range + 1) + random.nextInt(range + 1));
                } else {
                    failedAttempts = Math.min(failedAttempts, 4);
                    int range = blueprint.range() * (failedAttempts + 1) / 5;
                    nextPos = new BlockPos(
                            worldPosition.getX() - random.nextInt(range + 1)
                                    + random.nextInt(range + 1),
                            worldPosition.getY(),
                            worldPosition.getZ() - random.nextInt(range + 1)
                                    + random.nextInt(range + 1));
                }
                if (blueprint.terraform(level, nextPos)) {
                    energy.extract(consume);
                    failedAttempts = 0;
                    lastPos = nextPos;
                } else {
                    energy.extract(consume / 10.0);
                    failedAttempts++;
                    lastPos = null;
                }
            }
        }
        if (newActive) {
            inactiveTicks = 0;
            setActive(true);
        } else if (getBlockState().getValue(MachineBlock.ACTIVE) && inactiveTicks++ > 30) {
            setActive(false);
        }
    }

    /** Legacy right-click: eject the loaded blueprint first, else insert one from the hand. */
    public void useFromHand(Player player) {
        if (ejectBlueprint()) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && held.getItem() instanceof TerraformingBlueprintItem) {
            insertBlueprint(held.split(1));
        }
    }

    private boolean ejectBlueprint() {
        if (tfbp.isEmpty()) {
            return false;
        }
        if (level != null) {
            Containers.dropItemStack(
                    level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), tfbp);
        }
        tfbp = ItemStack.EMPTY;
        setChanged();
        return true;
    }

    private void insertBlueprint(ItemStack stack) {
        tfbp = stack;
        setChanged();
    }

    public static @Nullable BlockPos getFirstSolidBlockFrom(
            Level world, BlockPos pos, int yOffset) {
        MutableBlockPos ret = new MutableBlockPos(pos.getX(), pos.getY() + yOffset, pos.getZ());
        int floor = world.getMinY();
        while (ret.getY() >= floor) {
            BlockState state = world.getBlockState(ret);
            if (state.isSolidRender() && !isTechnicalBlock(state)) {
                return ret.immutable();
            }
            ret.move(Direction.DOWN);
        }
        return null;
    }

    public static @Nullable BlockPos getFirstBlockFrom(Level world, BlockPos pos, int yOffset) {
        MutableBlockPos ret = new MutableBlockPos(pos.getX(), pos.getY() + yOffset, pos.getZ());
        int floor = world.getMinY();
        while (ret.getY() >= floor) {
            if (!world.isEmptyBlock(ret) && !isTechnicalBlock(world.getBlockState(ret))) {
                return new BlockPos(ret);
            }
            ret.move(Direction.DOWN);
        }
        return null;
    }

    /**
     * Barrier and structure void are unobtainable technical blocks; the vanilla GameTest
     * harness surrounds every test box with barriers, so the downward scans must see
     * through them instead of stalling on harness scaffolding that can never be a
     * legitimate terraforming target.
     */
    private static boolean isTechnicalBlock(BlockState state) {
        return state.is(Blocks.BARRIER) || state.is(Blocks.STRUCTURE_VOID);
    }

    public static boolean switchGround(
            Level world, BlockPos pos, Block from, BlockState to, boolean upwards) {
        MutableBlockPos cPos = new MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
        int floor = world.getMinY();
        while (cPos.getY() >= floor) {
            BlockState state = world.getBlockState(cPos);
            // A downwards walk must not burrow through harness scaffolding into the world
            // ground far below the actual surface.
            if (upwards && state.getBlock() != from
                    || !upwards && (state.getBlock() == from || isTechnicalBlock(state))) {
                break;
            }
            cPos.move(Direction.DOWN);
        }
        if (!upwards && isTechnicalBlock(world.getBlockState(cPos))) {
            return false;
        }
        if ((!upwards || cPos.getY() != pos.getY()) && (upwards || cPos.getY() >= floor)) {
            world.setBlockAndUpdate(upwards ? cPos.above() : new BlockPos(cPos), to);
            return true;
        }
        return false;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        // Blueprints move by hand only.
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

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tfbp = input.read("tfbp", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!tfbp.isEmpty()) output.store("tfbp", ItemStack.CODEC, tfbp);
    }
}
