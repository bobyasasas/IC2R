package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyNode;
import ic2.core.machine.InductionCycle;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModSounds;
import ic2.neoforge.transfer.ResourcePort;
import ic2.neoforge.transfer.UpgradeTransfers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jspecify.annotations.Nullable;

/** Both rows share heat and progress, but each preserves its own input/output pairing. */
public final class InductionFurnaceBlockEntity extends PoweredBlockEntity {
    private record Row(
            int inputSlot,
            int outputSlot,
            ItemResource input,
            ItemStackTemplate output,
            double experience) {}

    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> first =
            RecipeManager.createCheck(RecipeType.SMELTING);
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> second =
            RecipeManager.createCheck(RecipeType.SMELTING);
    private final InductionCycle cycle = new InductionCycle();
    private final MachineJournal<InductionCycle.State> journal =
            new MachineJournal<>(energy, cycle::state, cycle::restore, this::setChanged);
    private double experience;

    public InductionFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(MachineKind.INDUCTION_FURNACE),
                pos,
                state,
                10000,
                MachineKind.INDUCTION_FURNACE.slots());
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        return EnergyNode.Terminal.sink(energy, 128, 1);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> side != Direction.DOWN && (slot == 0 || slot == 3),
                slot -> slot == 1 || slot == 4,
                (slot, resource) ->
                        level instanceof ServerLevel server
                                && first.getRecipeFor(
                                                new SingleRecipeInput(resource.toStack()), server)
                                        .isPresent());
    }

    private @Nullable Row row(
            ServerLevel level,
            int inputSlot,
            int outputSlot,
            RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> recipes) {
        var input = new SingleRecipeInput(inventory.stack(inputSlot));
        var recipe = recipes.getRecipeFor(input, level).orElse(null);
        if (recipe == null) return null;
        var output = ItemStackTemplate.fromNonEmptyStack(recipe.value().assemble(input));
        try (var tx = Transaction.openRoot()) {
            if (inventory.insert(outputSlot, ItemResource.of(output), output.count(), tx)
                    != output.count()) return null;
        }
        return new Row(
                inputSlot,
                outputSlot,
                inventory.getResource(inputSlot),
                output,
                recipe.value().experience());
    }

    private boolean finish(@Nullable Row row, Transaction transaction) {
        return row == null
                || inventory.extract(row.inputSlot(), row.input(), 1, transaction) == 1
                        && inventory.insert(
                                        row.outputSlot(),
                                        ItemResource.of(row.output()),
                                        row.output().count(),
                                        transaction)
                                == row.output().count();
    }

    @Override
    public void serverTick(ServerLevel level) {
        boolean previouslyActive = getBlockState().getValue(MachineBlock.ACTIVE);
        var battery = inventory.stack(2);
        double charge = ElectricItemEnergy.discharge(battery, energy.free(), 2, false, true, false);
        if (charge > 0) {
            inventory.set(2, ItemResource.of(battery), battery.getCount());
            energy.insert(charge);
            setChanged();
        }
        if (cycle.completionReady()) {
            var a = row(level, 0, 1, first);
            var b = row(level, 3, 4, second);
            try (var transaction = Transaction.openRoot()) {
                journal.updateSnapshots(transaction);
                if (!finish(a, transaction) || !finish(b, transaction)) return;
                cycle.completed();
                transaction.commit();
            }
            experience += (a == null ? 0 : a.experience()) + (b == null ? 0 : b.experience());
            setChanged();
        }
        boolean canProcess = row(level, 0, 1, first) != null || row(level, 3, 4, second) != null;
        int before = comparator();
        boolean active;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            active =
                    cycle.tick(
                            canProcess,
                            UpgradeItem.invertedSignal(kind(), inventory, level, worldPosition),
                            energy);
            transaction.commit();
        }
        if (before != comparator())
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        UpgradeTransfers.tick(level, this);
        setActive(active);
        if (this.level == level && previouslyActive != active) {
            var sound =
                    active
                            ? ModSounds.MACHINE_FURNACE_INDUCTION_START.get()
                            : progress() > 0
                                    ? ModSounds.MACHINE_INTERRUPT1.get()
                                    : ModSounds.MACHINE_FURNACE_INDUCTION_STOP.get();
            level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, .5f, 1);
        }
    }

    public int heat() {
        return cycle.state().heat();
    }

    public int comparator() {
        return heat() * 15 / InductionCycle.MAX_HEAT;
    }

    @Override
    public int progress() {
        return cycle.state().progress();
    }

    @Override
    public int progressMaximum() {
        return InductionCycle.WORK;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> heat();
            case 1 -> InductionCycle.MAX_HEAT;
            default -> 0;
        };
    }

    @Override
    public void awardExperience(Player player) {
        if (level instanceof ServerLevel server && experience >= 1) {
            int whole = (int) Math.min(Integer.MAX_VALUE, Math.floor(experience));
            experience -= whole;
            ExperienceOrb.award(server, player.position(), whole);
            setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        cycle.restore(
                new InductionCycle.State(
                        Math.clamp(input.getIntOr("heat", 0), 0, InductionCycle.MAX_HEAT),
                        Math.clamp(
                                input.getIntOr("progress", 0),
                                0,
                                InductionCycle.WORK + InductionCycle.MAX_HEAT / 30 - 1)));
        double xp = input.getDoubleOr("xp", 0);
        experience = Double.isFinite(xp) ? Math.max(0, xp) : 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("heat", heat());
        output.putInt("progress", progress());
        output.putDouble("xp", experience);
    }
}
