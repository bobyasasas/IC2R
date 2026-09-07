package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyNode;
import ic2.core.machine.WorkBuffer;
import ic2.neoforge.api.WorkSource;
import ic2.neoforge.item.ElectricItem;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Shared electric input, installed parts and sided output for electric heat/kinetic generators. */
public final class ElectricWorkBlockEntity extends PoweredBlockEntity {
    public static final int PARTS = 10, BATTERY = 10;
    private final WorkBuffer work = new WorkBuffer(1000);
    private final MachineJournal<WorkBuffer.State> journal =
            new MachineJournal<>(energy, work::state, work::restore, this::setChanged);

    public ElectricWorkBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                10000,
                11);
    }

    private boolean heat() {
        return kind() == MachineKind.ELECTRIC_HEAT_GENERATOR;
    }

    public static MaterialDefinition part(MachineKind kind) {
        return kind == MachineKind.ELECTRIC_HEAT_GENERATOR
                ? MaterialDefinition.COIL
                : MaterialDefinition.ELECTRIC_MOTOR;
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        return slot == BATTERY
                ? resource.getItem() instanceof ElectricItem
                : resource.is(ModItems.MATERIALS.get(part(kind())).get());
    }

    @Override
    protected int inventorySlotLimit(int slot) {
        return 1;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(inventory, slot -> false, slot -> false);
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        return EnergyNode.Terminal.sink(energy, 2048, 1);
    }

    @Override
    public boolean acceptsFrom(Direction side) {
        return heat() || side != getBlockState().getValue(MachineBlock.FACING);
    }

    private int bandwidth() {
        int installed = 0;
        for (int slot = 0; slot < PARTS; slot++)
            if (inventory.getResource(slot).is(ModItems.MATERIALS.get(part(kind())).get()))
                installed++;
        return installed * (heat() ? 10 : 100);
    }

    public WorkSource output(@org.jspecify.annotations.Nullable Direction side) {
        return new WorkSource() {
            private boolean connected() {
                return !isRemoved()
                        && level != null
                        && !level.isClientSide()
                        && side == getBlockState().getValue(MachineBlock.FACING);
            }

            @Override
            public int bandwidth() {
                return connected() ? ElectricWorkBlockEntity.this.bandwidth() : 0;
            }

            @Override
            public int available() {
                return connected() ? work.available(level.getGameTime(), bandwidth()) : 0;
            }

            @Override
            public int extract(int maximum, TransactionContext transaction) {
                if (maximum < 0) throw new IllegalArgumentException("Negative work request");
                if (!connected()) return 0;
                journal.updateSnapshots(transaction);
                return work.extract(level.getGameTime(), bandwidth(), maximum);
            }
        };
    }

    @Override
    public void serverTick(ServerLevel level) {
        var battery = inventory.stack(BATTERY);
        double charged =
                ElectricItemEnergy.discharge(battery, energy.free(), 4, false, true, false);
        if (charged > 0) {
            inventory.set(BATTERY, ItemResource.of(battery), battery.getCount());
            energy.insert(charged);
            setChanged();
        }
        double generated;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            generated =
                    work.generate(
                            energy,
                            heat()
                                    ? GenerationConfig.ELECTRIC_HEAT.get()
                                    : 4 * GenerationConfig.ELECTRIC_KINETIC.get(),
                            heat() ? bandwidth() : 1000,
                            heat());
            transaction.commit();
        }
        setActive(generated > 0);
    }

    @Override
    public int progress() {
        return (int) work.state().stored();
    }

    @Override
    public int progressMaximum() {
        return heat() ? 100 : 1000;
    }

    @Override
    public int menuValue(int index) {
        return index == 0 ? progress() : index == 1 ? bandwidth() : 0;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        double stored = input.getDoubleOr("work", 0);
        work.restore(
                new WorkBuffer.State(
                        Double.isFinite(stored) ? Math.clamp(stored, 0, heat() ? 100 : 1000) : 0,
                        input.getLongOr("workTick", Long.MIN_VALUE),
                        Math.clamp(input.getIntOr("workExtracted", 0), 0, 1000)));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("work", work.state().stored());
        output.putLong("workTick", work.state().tick());
        output.putInt("workExtracted", work.state().extracted());
    }
}
