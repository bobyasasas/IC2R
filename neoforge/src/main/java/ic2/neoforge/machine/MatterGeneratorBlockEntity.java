package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.recipe.MatterFabricatorRecipe;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModProcessingRecipes;
import ic2.neoforge.transfer.FluidContainerPort;
import ic2.neoforge.transfer.MachineFluidTank;
import ic2.neoforge.transfer.ResourcePort;
import ic2.neoforge.transfer.UpgradeTransfers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * UU-matter generator: fills a 1,000,000 EU buffer and converts the whole batch into 1 mB of
 * UU-matter. Scrap amplification refunds 5 EU per EU gained while scrap lasts, so amplified runs
 * finish about six times faster.
 */
public final class MatterGeneratorBlockEntity extends PoweredBlockEntity {
    public static final int TANK_CAPACITY = 8000;
    public static final int AMP_SLOT = 0, OUTPUT = 1, CELL_IN = 2;
    private static final int AMPLIFIER_STORAGE_LIMIT = 10000;

    private final RecipeManager.CachedCheck<SingleRecipeInput, MatterFabricatorRecipe> recipes =
            RecipeManager.createCheck(ModProcessingRecipes.MATTER_FABRICATOR_TYPE.get());
    private final MachineFluidTank tank =
            new MachineFluidTank(
                    TANK_CAPACITY,
                    this::setChanged,
                    resource ->
                            resource.is(
                                    ModFluids.FAMILIES
                                            .get(FluidDefinition.UU_MATTER)
                                            .source()
                                            .get()));
    private int scrap;
    private double lastStored;

    public MatterGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                1000000,
                20);
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        // Legacy sink tier comes from matterFabricatorTier (default 3, 512 EU packets).
        return EnergyNode.Terminal.sink(energy, 512, 3);
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        if (slot == CELL_IN) return FluidContainerPort.accepts(resource);
        // The machine writes filled cells into the output slot itself.
        if (slot == OUTPUT) return FluidContainerPort.accepts(resource);
        return true;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> slot == AMP_SLOT || slot == CELL_IN,
                slot -> slot == OUTPUT,
                (slot, resource) -> slot != CELL_IN || FluidContainerPort.accepts(resource));
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (level.hasNeighborSignal(worldPosition)) {
            lastStored = energy.stored();
            setActive(false);
            return;
        }
        double gained = energy.stored() - lastStored;
        if (gained > 0 && scrap > 0) amplify(gained);
        if (scrap < AMPLIFIER_STORAGE_LIMIT) consumeAmplifier(level);
        if (readyToGenerate(gained)) generate();
        fillCells();
        UpgradeTransfers.tick(level, this);
        lastStored = energy.stored();
        setActive(gained > 0);
    }

    private void amplify(double gained) {
        double bonus = Math.min(scrap, gained);
        if (bonus <= 0) return;
        energy.insert(5 * bonus);
        scrap -= (int) bonus;
        setChanged();
    }

    private void consumeAmplifier(ServerLevel level) {
        var stack = inventory.stack(AMP_SLOT);
        if (stack.isEmpty()) return;
        var recipe =
                recipes.getRecipeFor(new SingleRecipeInput(stack), level)
                        .map(holder -> holder.value())
                        .orElse(null);
        if (recipe == null) return;
        try (var transaction = Transaction.openRoot()) {
            if (inventory.extract(AMP_SLOT, ItemResource.of(stack), 1, transaction) != 1) return;
            transaction.commit();
        }
        scrap = (int) Math.min(AMPLIFIER_STORAGE_LIMIT * 2L, scrap + (long) recipe.result());
        setChanged();
    }

    /**
     * Legacy generates once the buffer stops filling (or is full); the port uses the same
     * stall-or-full rule via the per-tick gain.
     */
    private boolean readyToGenerate(double gained) {
        return energy.stored() >= energy.capacity() || gained <= 0 && energy.stored() > 0;
    }

    private void generate() {
        if (tank.getAmountAsInt(0) >= TANK_CAPACITY) return;
        try (var transaction = Transaction.openRoot()) {
            if (tank.insert(0, uuMatter(), 1, transaction) != 1) return;
            energy.extract(energy.capacity());
            transaction.commit();
            setChanged();
        }
    }

    private void fillCells() {
        if (tank.getAmountAsInt(0) <= 0) return;
        ResourceHandlerUtil.move(
                tank,
                FluidContainerPort.of(inventory, CELL_IN, OUTPUT),
                resource -> resource.is(uuMatter().getFluid()),
                1000,
                null);
    }

    private FluidResource uuMatter() {
        return FluidResource.of(ModFluids.FAMILIES.get(FluidDefinition.UU_MATTER).source().get());
    }

    public int scrap() {
        return scrap;
    }

    public int tankAmount() {
        return tank.getAmountAsInt(0);
    }

    @Override
    public int menuValue(int index) {
        return index == 0
                ? Float.floatToIntBits((float) tank.getAmountAsInt(0) / TANK_CAPACITY)
                : 0;
    }

    @Override
    public int progress() {
        return (int) Math.min(1000, energy.stored() / 1000);
    }

    @Override
    public int progressMaximum() {
        return 1000;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        scrap = Math.max(0, input.getIntOr("scrap", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("scrap", scrap);
    }
}
