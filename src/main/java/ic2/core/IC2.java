package ic2.core;

import ic2.core.proxy.EnvProxy;
import ic2.core.proxy.SideProxy;
import ic2.core.proxy.SideProxyClient;
import ic2.core.proxy.SideProxyServer;
import ic2.core.sound.SoundManager;
import ic2.core.util.Keyboard;
import ic2.core.util.Log;
import ic2.core.util.PriorityExecutor;
import ic2.core.util.SideGateway;
import ic2.forge.EnvProxyForge;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;

import org.apache.logging.log4j.LogManager;

public class IC2 {
    public static final EnvProxy envProxy = createEnvProxy();
    public static final SideProxy sideProxy = createSideProxy();
    public static final Log log = new Log(LogManager.getLogger("ic2"));
    public static final SideGateway network = new SideGateway();
    public static final Keyboard keyboard = sideProxy.getKeyboard();
    public static final SoundManager soundManager = sideProxy.getSoundManager();
    public static final CreativeModeTab tabIc2General =
            envProxy.createItemGroup(
                    getIdentifier("general"),
                    new ItemGroupIconSupplier(Ic2ItemGroupType.GENERAL),
                    Ic2ItemGroupType.GENERAL);
    public static final CreativeModeTab tabIc2GeneratorsAndWiring =
            envProxy.createItemGroup(
                    getIdentifier("generators_and_wiring"),
                    new ItemGroupIconSupplier(Ic2ItemGroupType.GENERATORS_AND_WIRING),
                    Ic2ItemGroupType.GENERATORS_AND_WIRING);
    public static final CreativeModeTab tabIc2Reactor =
            envProxy.createItemGroup(
                    getIdentifier("reactor"),
                    new ItemGroupIconSupplier(Ic2ItemGroupType.REACTOR),
                    Ic2ItemGroupType.REACTOR);
    public static final CreativeModeTab tabIc2Machines =
            envProxy.createItemGroup(
                    getIdentifier("machines"),
                    new ItemGroupIconSupplier(Ic2ItemGroupType.MACHINES),
                    Ic2ItemGroupType.MACHINES);
    public static final CreativeModeTab tabIc2ToolsAndUtilities =
            envProxy.createItemGroup(
                    getIdentifier("tools_and_utilities"),
                    new ItemGroupIconSupplier(Ic2ItemGroupType.TOOLS_AND_UTILITIES),
                    Ic2ItemGroupType.TOOLS_AND_UTILITIES);
    public static final CreativeModeTab tabIc2FluidCells =
            envProxy.createItemGroup(
                    getIdentifier("fluid_cells"),
                    new ItemGroupIconSupplier(Ic2ItemGroupType.FLUID_CELLS),
                    Ic2ItemGroupType.FLUID_CELLS);
    public static final CreativeModeTab tabIc2Combat =
            envProxy.createItemGroup(
                    getIdentifier("combat"),
                    new ItemGroupIconSupplier(Ic2ItemGroupType.COMBAT),
                    Ic2ItemGroupType.COMBAT);
    public static final CreativeModeTab tabIc2Farming =
            envProxy.createItemGroup(
                    getIdentifier("farming"),
                    new ItemGroupIconSupplier(Ic2ItemGroupType.FARMING),
                    Ic2ItemGroupType.FARMING);
    public static final CreativeModeTab tabIc2Materials =
            envProxy.createItemGroup(
                    getIdentifier("materials"),
                    new ItemGroupIconSupplier(Ic2ItemGroupType.MATERIALS),
                    Ic2ItemGroupType.MATERIALS);
    public static final PriorityExecutor threadPool =
            new PriorityExecutor(Math.max(Runtime.getRuntime().availableProcessors(), 2));
    public static final RandomSource random = RandomSource.createNewThreadLocalInstance();
    public static boolean initialized = false;
    public static boolean suddenlyHoes = false;
    public static boolean seasonal = false;

    public static int getSeaLevel(Level world) {
        return world.getSeaLevel();
    }

    public static int getWorldMaxHeight(Level world) {
        return world.getHeight();
    }

    public static int getWorldMinHeight(Level world) {
        return world.getMinBuildHeight();
    }

    public static ResourceLocation getIdentifier(String name) {
        return ResourceLocation.fromNamespaceAndPath("ic2", name);
    }

    public static void grantAdvancement(Player player, String path) {
        if (player instanceof ServerPlayer sp) {
            Advancement adv =
                    sp.server
                            .getAdvancements()
                            .getAdvancement(ResourceLocation.fromNamespaceAndPath("ic2", path));
            if (adv != null) {
                sp.getAdvancements().award(adv, "impossible");
            }
        }
    }

    private static EnvProxy createEnvProxy() {
        return new EnvProxyForge();
    }

    private static SideProxy createSideProxy() {
        return envProxy.isClientEnv() ? new SideProxyClient() : new SideProxyServer();
    }
}
