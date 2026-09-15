package ic2.neoforge.interop.jade;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineBlockEntity;
import ic2.neoforge.machine.PoweredBlockEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

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

import java.util.List;

/**
 * Jade integration (M14-b): EU storage on powered machines and processing progress on all machines,
 * rendered through Jade's universal energy/progress views. Loaded only when Jade is installed (Jade
 * discovers this class through the plugin annotation, and nothing else references it); the classes
 * touch no client-only Minecraft code, so a dedicated server with Jade may also load them for
 * multiplayer data sync.
 */
@WailaPlugin
public final class Ic2JadePlugin implements IWailaPlugin {
    private static final float MIN_VISIBLE_PROGRESS = 1.0E-4F;
    private static final String PROGRESS_CURRENT_KEY = "ic2Current";
    private static final String PROGRESS_MAXIMUM_KEY = "ic2Maximum";

    private static int percent(float ratio) {
        return Math.clamp(Math.round(ratio * 100.0F), 0, 100);
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEnergyStorage(
                MachineEnergyProvider.INSTANCE, PoweredBlockEntity.class);
        registration.registerProgress(MachineProgressProvider.INSTANCE, MachineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEnergyStorageClient(MachineEnergyProvider.INSTANCE);
        registration.registerProgressClient(MachineProgressProvider.INSTANCE);
    }

    public enum MachineEnergyProvider
            implements
                    IServerExtensionProvider<EnergyView.Data>,
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
            return ClientViewGroup.map(
                    groups,
                    data -> {
                        EnergyView view = EnergyView.read(data, " EU");
                        if (view == null) return null;

                        int percent = percent(view.ratio);
                        view.overrideText =
                                accessor.showDetails()
                                        ? Component.translatable(
                                                "ic2.jade.energy.both",
                                                view.current,
                                                view.max,
                                                percent)
                                        : Component.translatable(
                                                "ic2.jade.energy.amount", view.current, view.max);
                        return view;
                    },
                    null);
        }
    }

    public enum MachineProgressProvider
            implements
                    IServerExtensionProvider<ProgressView.Data>,
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
            if (!machine.getBlockState().getValue(MachineBlock.ACTIVE)) return List.of();
            int maximum = machine.progressMaximum();
            if (maximum <= 0) return List.of();
            int current = Math.clamp(machine.progress(), 0, maximum);
            float ratio = (float) current / maximum;
            if (ratio <= MIN_VISIBLE_PROGRESS) return List.of();

            ViewGroup<ProgressView.Data> group =
                    new ViewGroup<>(List.of(new ProgressView.Data(ratio)));
            group.getExtraData().putLong(PROGRESS_CURRENT_KEY, current);
            group.getExtraData().putLong(PROGRESS_MAXIMUM_KEY, maximum);
            return List.of(group);
        }

        @Override
        public List<ClientViewGroup<ProgressView>> getClientGroups(
                Accessor<?> accessor, List<ViewGroup<ProgressView.Data>> groups) {
            return ClientViewGroup.map(
                    groups,
                    ProgressView::read,
                    (group, clientGroup) -> {
                        var extra = group.getExtraData();
                        long current = extra.getLongOr(PROGRESS_CURRENT_KEY, -1L);
                        long maximum = extra.getLongOr(PROGRESS_MAXIMUM_KEY, -1L);
                        for (var view : clientGroup.views) {
                            int percent = percent(view.parts.getFirst().progress());
                            view.text =
                                    accessor.showDetails() && maximum > 0 && current >= 0
                                            ? Component.translatable(
                                                    "ic2.jade.progress.both",
                                                    current,
                                                    maximum,
                                                    percent)
                                            : Component.translatable(
                                                    "ic2.jade.progress.working", percent);
                        }
                    });
        }
    }
}
