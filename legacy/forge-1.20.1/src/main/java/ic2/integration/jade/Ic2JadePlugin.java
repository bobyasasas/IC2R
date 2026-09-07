package ic2.integration.jade;

import ic2.core.block.tileentity.Ic2TileEntity;
import ic2.core.block.tileentity.Ic2TileEntityBlock;
import ic2.core.ref.Ic2BlockTags;
import ic2.core.ref.Ic2Items;

import snownee.jade.addon.harvest.HarvestToolProvider;
import snownee.jade.addon.harvest.SimpleToolHandler;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Ic2JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerProgress(Ic2ProgressProvider.INSTANCE, Ic2TileEntity.class);
        registration.registerBlockDataProvider(Ic2EnergyProvider.INSTANCE, Ic2TileEntity.class);
        registration.registerBlockDataProvider(
                Ic2MachineTooltipProvider.INSTANCE, Ic2TileEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        Ic2JadePluginConfigs.register(registration);
        registration.registerProgressClient(Ic2ProgressProvider.INSTANCE);
        registration.registerBlockComponent(Ic2EnergyProvider.INSTANCE, Ic2TileEntityBlock.class);
        registration.registerBlockComponent(
                Ic2MachineTooltipProvider.INSTANCE, Ic2TileEntityBlock.class);
        HarvestToolProvider.registerHandler(
                new SimpleToolHandler(
                        "wrench", Ic2BlockTags.MINEABLE_WITH_WRENCH, Ic2Items.WRENCH));
    }
}
