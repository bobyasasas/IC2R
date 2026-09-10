package ic2.neoforge.interop.jade;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.machine.MachineBlockEntity;
import ic2.neoforge.machine.PoweredBlockEntity;

import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.EnergyView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ProgressView;
import snownee.jade.api.view.ViewGroup;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Jade integration (M14-b): EU storage on powered machines and processing progress on
 * all machines, rendered through Jade's universal energy/progress views. Loaded only
 * when Jade is installed (Jade discovers this class through the plugin annotation, and
 * nothing else references it); the classes touch no client-only Minecraft code, so a
 * dedicated server with Jade may also load them for multiplayer data sync.
 */
@WailaPlugin
public final class Ic2JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEnergyStorage(MachineEnergyProvider.INSTANCE, PoweredBlockEntity.class);
        registration.registerProgress(MachineProgressProvider.INSTANCE, MachineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEnergyStorageClient(MachineEnergyProvider.INSTANCE);
        registration.registerProgressClient(MachineProgressProvider.INSTANCE);
    }

    public enum MachineEnergyProvider
            implements IServerExtensionProvider<EnergyView.Data>,
                    IClientExtensionProvider<EnergyView.Data, EnergyView> {
        INSTANCE;
        public static final Identifier UID =
                Identifier.fromNamespaceAndPath(IndustrialCraft.MOD_ID, "machine_energy");

        @Override
        public Identifier getUid() {
            return UID;
        }

        @Override
        public List<ViewGroup<EnergyView.Data>> getGroups(Accessor<?> accessor) {
            if (!(accessor instanceof BlockAccessor block)
                    || !(block.getBlockEntity() instanceof PoweredBlockEntity machine)) {
                return List.of();
            }
            double capacity = machine.energy().capacity();
            if (capacity <= 0) return List.of();
            long stored = (long) machine.energy().stored();
            return List.of(new ViewGroup<>(List.of(new EnergyView.Data(stored, (long) capacity))));
        }

        @Override
        public List<ClientViewGroup<EnergyView>> getClientGroups(
                Accessor<?> accessor, List<ViewGroup<EnergyView.Data>> groups) {
            return ClientViewGroup.map(groups, data -> EnergyView.read(data, " EU"), null);
        }
    }

    public enum MachineProgressProvider
            implements IServerExtensionProvider<ProgressView.Data>,
                    IClientExtensionProvider<ProgressView.Data, ProgressView> {
        INSTANCE;
        public static final Identifier UID =
                Identifier.fromNamespaceAndPath(IndustrialCraft.MOD_ID, "machine_progress");

        @Override
        public Identifier getUid() {
            return UID;
        }

        @Override
        public List<ViewGroup<ProgressView.Data>> getGroups(Accessor<?> accessor) {
            if (!(accessor instanceof BlockAccessor block)
                    || !(block.getBlockEntity() instanceof MachineBlockEntity machine)) {
                return List.of();
            }
            int maximum = machine.progressMaximum();
            if (maximum <= 0) return List.of();
            float ratio = (float) machine.progress() / maximum;
            return List.of(new ViewGroup<>(List.of(new ProgressView.Data(ratio))));
        }

        @Override
        public List<ClientViewGroup<ProgressView>> getClientGroups(
                Accessor<?> accessor, List<ViewGroup<ProgressView.Data>> groups) {
            return ClientViewGroup.map(
                    groups,
                    ProgressView::read,
                    (group, clientGroup) -> {
                        for (var view : clientGroup.views) {
                            int percent = Math.round(view.parts.getFirst().progress() * 100);
                            // Existing vocabulary: "ic2.jade.progress": "%s%%"
                            view.text = Component.translatable("ic2.jade.progress", percent);
                        }
                    });
        }
    }
}
