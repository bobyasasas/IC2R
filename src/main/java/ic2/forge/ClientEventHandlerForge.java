package ic2.forge;

import com.mojang.blaze3d.shaders.FogShape;

import ic2.core.command.CommandIc2c;
import ic2.core.event.EventHandlerClient;
import ic2.core.event.TickHandler;
import ic2.core.proxy.SideProxyClient;
import ic2.core.sound.DeferredSoundOps;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderHighlightEvent.Block;
import net.minecraftforge.client.event.RenderLivingEvent.Post;
import net.minecraftforge.client.event.RenderLivingEvent.Pre;
import net.minecraftforge.client.event.ScreenEvent.Init;
import net.minecraftforge.client.event.ViewportEvent.ComputeFogColor;
import net.minecraftforge.client.event.ViewportEvent.RenderFog;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.client.event.sound.SoundEngineLoadEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.level.LevelEvent.Load;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ClientEventHandlerForge {
    @SubscribeEvent
    public void registerClientCommands(RegisterClientCommandsEvent event) {
        CommandIc2c.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase == Phase.START) {
            TickHandler.onClientTick();
        } else if (event.phase == Phase.END) {
            DeferredSoundOps.flush();
        }
    }

    @SubscribeEvent
    public void onSoundSetup(SoundEngineLoadEvent event) {
        EventHandlerClient.onSoundSetup();
    }

    @SubscribeEvent
    public void livingEntityPreRender(Pre<LivingEntity, EntityModel<LivingEntity>> event) {
        EventHandlerClient.livingEntityPreRender(event.getEntity(), event.getRenderer());
    }

    @SubscribeEvent
    public void livingEntityPostRender(Post<LivingEntity, EntityModel<LivingEntity>> event) {
        EventHandlerClient.livingEntityPostRender(event.getEntity(), event.getRenderer());
    }

    @SubscribeEvent
    public void onSetupFogDensity(RenderFog event) {
        float newDensity =
                EventHandlerClient.onSetupFogDensity(event.getCamera().getBlockAtCamera());
        if (newDensity >= 0.0F) {
            event.setCanceled(true);
            event.setNearPlaneDistance(-8.0F);
            event.setFarPlaneDistance(newDensity * 0.5F);
            event.setFogShape(FogShape.SPHERE);
        }
    }

    @SubscribeEvent
    public void onRenderFogColor(ComputeFogColor event) {
        int color = EventHandlerClient.onRenderFogColor(event.getCamera().getBlockAtCamera());
        if (color >= 0) {
            event.setRed((color >>> 16 & 0xFF) / 255.0F);
            event.setGreen((color >>> 8 & 0xFF) / 255.0F);
            event.setBlue((color & 0xFF) / 255.0F);
        }
    }

    @SubscribeEvent
    public void onDrawBlockHighlight(Block event) {
        if (EventHandlerClient.onDrawBlockHighlight(
                SideProxyClient.mc.player,
                event.getTarget(),
                event.getPartialTick(),
                event.getPoseStack(),
                event.getMultiBufferSource())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDrawBlockHighlightLast(Block event) {
        if (EventHandlerClient.onDrawBlockHighlightLast(
                SideProxyClient.mc.player,
                event.getTarget(),
                event.getPartialTick(),
                event.getPoseStack(),
                event.getMultiBufferSource())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onGuiCreate(Init event) {
        EventHandlerClient.onGuiCreate(
                event.getScreen(), event.getListenersList(), event::addListener);
    }

    @SubscribeEvent
    public void onRenderHotBar(net.minecraftforge.client.event.RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) {
            EventHandlerClient.onRenderHotBar(event.getGuiGraphics());
        }
    }

    @SubscribeEvent
    public void onSoundPlayed(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        SoundInstance newSound = EventHandlerClient.onSoundPlayed(sound);
        if (newSound != sound) {
            event.setSound(newSound);
        }
    }

    @SubscribeEvent
    public void onDisconnect(PlayerLoggedOutEvent event) {
        EventHandlerClient.onDisconnect();
    }

    @SubscribeEvent
    public void onClientLoggingOut(LoggingOut event) {
        EventHandlerClient.onDisconnect();
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) {
            EventHandlerClient.onClientPlayerJoin(event.getEntity());
        }
    }

    @SubscribeEvent
    public void onWorldLoad(Load event) {
        Level world = (Level) event.getLevel();
        if (world.isClientSide) {
            TickHandler.requestSingleWorldTick(
                    world,
                    loadedWorld -> {
                        if (SideProxyClient.mc.player != null
                                && SideProxyClient.mc.player.level() == loadedWorld) {
                            EventHandlerClient.onClientPlayerJoin(SideProxyClient.mc.player);
                        }
                    });
        }
    }
}
