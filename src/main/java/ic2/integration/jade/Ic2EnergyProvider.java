package ic2.integration.jade;

import ic2.core.IC2;
import ic2.core.block.comp.Energy;
import ic2.core.block.tileentity.Ic2TileEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

public enum Ic2EnergyProvider
        implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final ResourceLocation UID = IC2.getIdentifier("energy_storage");
    private static final String KEY_HAS = "ic2EHas";
    private static final String KEY_STORED = "ic2ECur";
    private static final String KEY_CAPACITY = "ic2ECap";

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public int getDefaultPriority() {
        return 50;
    }

    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof Ic2TileEntity te
                && te.hasComponent(Energy.class)) {
            Energy energy = te.getComponent(Energy.class);
            long capacity = Math.max(0L, (long) energy.getCapacity());
            if (capacity > 0L) {
                long stored = Math.max(0L, Math.min(capacity, (long) energy.getEnergy()));
                tag.putBoolean(KEY_HAS, true);
                tag.putLong(KEY_STORED, stored);
                tag.putLong(KEY_CAPACITY, capacity);
            }
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.getBoolean(KEY_HAS)) {
            if (JadeConfigHelper.energyMode().isVisible(accessor.showDetails())) {
                long capacity = data.getLong(KEY_CAPACITY);
                if (capacity > 0L) {
                    long stored = Math.max(0L, Math.min(capacity, data.getLong(KEY_STORED)));
                    float ratio = (float) stored / (float) capacity;
                    Component text = JadeConfigHelper.formatEnergyText(stored, capacity, ratio);
                    IElementHelper helper = IElementHelper.get();
                    tooltip.add(
                            helper.progress(
                                    ratio,
                                    text,
                                    JadeConfigHelper.energyStyle(),
                                    BoxStyle.DEFAULT,
                                    true));
                }
            }
        }
    }
}
