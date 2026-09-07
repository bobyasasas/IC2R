package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyNode;
import ic2.core.machine.FuelGenerator;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class GeneratorBlockEntity extends PoweredBlockEntity {
    public static final int FUEL = 0, BATTERY = 1;
    private final FuelGenerator generator = new FuelGenerator();
    private final MachineJournal<FuelGenerator.State> journal =
            new MachineJournal<>(energy, generator::state, generator::restore, this::setChanged);

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.GENERATOR_ENTITY.get(), pos, state, 4000, 2);
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        return EnergyNode.Terminal.source(energy, 32, 1);
    }

    @Override
    public int progress() {
        return generator.state().remaining();
    }

    @Override
    public int progressMaximum() {
        return generator.state().total();
    }

    @Override
    public void serverTick(ServerLevel level) {
        boolean active;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            if (generator.needsFuel(energy, 10)) {
                var fuel = inventory.stack(FUEL);
                int burnTime = fuel.getBurnTime(RecipeType.SMELTING, level.fuelValues());
                if (burnTime >= 4
                        && inventory.extract(FUEL, ItemResource.of(fuel), 1, transaction) == 1) {
                    generator.acceptFuel(burnTime);
                    if (fuel.getCraftingRemainder() != null) {
                        var remainder = fuel.getCraftingRemainder();
                        if (inventory.insert(
                                        FUEL,
                                        ItemResource.of(remainder),
                                        remainder.count(),
                                        transaction)
                                != remainder.count()) return;
                    }
                }
            }
            active = generator.tick(energy, 10);
            transaction.commit();
        }
        var battery = inventory.stack(BATTERY);
        double accepted = ElectricItemEnergy.charge(battery, energy.stored(), 1, false, false);
        if (accepted > 0) {
            inventory.set(BATTERY, ItemResource.of(battery), battery.getCount());
            energy.extract(accepted);
            setChanged();
        }
        setActive(active);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int total = Math.max(0, input.getIntOr("totalFuel", 0));
        generator.restore(
                new FuelGenerator.State(Math.clamp(input.getIntOr("fuel", 0), 0, total), total));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("fuel", generator.state().remaining());
        output.putInt("totalFuel", generator.state().total());
    }
}
