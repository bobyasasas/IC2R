package ic2.neoforge.machine;

import ic2.core.machine.MachineProcess;
import ic2.core.machine.MetalFormerMode;
import ic2.core.recipe.ProcessingMethod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The mode selects a recipe family; all charging, upgrades and atomic processing are shared. */
public final class MetalFormerBlockEntity extends SingleInputBlockEntity {
    private MetalFormerMode mode = MetalFormerMode.EXTRUDING;

    public MetalFormerBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    public MetalFormerMode mode() {
        return mode;
    }

    @Override
    protected ProcessingMethod method() {
        return mode.method();
    }

    public void setMode(MetalFormerMode mode) {
        java.util.Objects.requireNonNull(mode);
        if (this.mode == mode) return;
        this.mode = mode;
        process.restore(new MachineProcess.State("", 0));
        clearSelection();
        setActive(false);
        setChanged();
    }

    @Override
    public int menuValue(int index) {
        return index == 0 ? mode.id() : 0;
    }

    @Override
    public boolean menuAction(int id) {
        if (id != 0) return false;
        setMode(mode.next());
        return true;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mode = MetalFormerMode.byId(input.getIntOr("mode", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("mode", mode.id());
    }
}
