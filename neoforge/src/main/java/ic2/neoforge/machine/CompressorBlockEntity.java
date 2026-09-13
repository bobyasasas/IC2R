package ic2.neoforge.machine;

import ic2.neoforge.transfer.MachineFluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Compressor on top of the shared single-input machine, plus the legacy pump snowball recipe: an
 * empty input with pumps adjacent pulls 1000 mB of water from them and freezes one snowball.
 */
public class CompressorBlockEntity extends SingleInputBlockEntity {
    public static final String PUMP_RECIPE = "ic2:compressor/pump_snowball";
    private static final ItemStackTemplate SNOWBALL =
            ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.SNOWBALL));
    private static final int WATER_PER_SNOWBALL = 1000;

    public CompressorBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    protected Job findJob(ServerLevel level) {
        Job standard = super.findJob(level);
        if (standard != null) return standard;
        return findPumpJob(level);
    }

    private Job findPumpJob(ServerLevel level) {
        if (!inventory.stack(INPUT).isEmpty()) return null;
        List<PumpBlockEntity> pumps = adjacentPumps();
        if (pumps.isEmpty()) return null;
        // Legacy simulates the drain in getRecipeResult and commits it in operateOnce; both here
        // run through consumeInputs, so this pass only proves the water is available.
        try (Transaction simulation = Transaction.openRoot()) {
            if (drainPumps(pumps, simulation) < WATER_PER_SNOWBALL) return null;
        }
        return new Job(PUMP_RECIPE, 0, SNOWBALL, 0);
    }

    private List<PumpBlockEntity> adjacentPumps() {
        var pumps = new ArrayList<PumpBlockEntity>();
        for (Direction side : Direction.values()) {
            BlockEntity entity =
                    level.getBlockEntity(worldPosition.relative(side));
            if (entity instanceof PumpBlockEntity pump) pumps.add(pump);
        }
        return pumps;
    }

    @Override
    protected boolean consumeInputs(Job job, ItemResource input, Transaction transaction) {
        if (!job.recipe().equals(PUMP_RECIPE)) return super.consumeInputs(job, input, transaction);
        return drainPumps(adjacentPumps(), transaction) >= WATER_PER_SNOWBALL;
    }

    private int drainPumps(List<PumpBlockEntity> pumps, Transaction transaction) {
        int drawn = 0;
        for (PumpBlockEntity pump : pumps) {
            if (drawn >= WATER_PER_SNOWBALL) break;
            MachineFluidTank tank = pump.tank();
            drawn +=
                    tank.extract(
                            0,
                            FluidResource.of(Fluids.WATER),
                            WATER_PER_SNOWBALL - drawn,
                            transaction);
        }
        return drawn;
    }
}
