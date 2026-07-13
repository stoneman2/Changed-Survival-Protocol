package net.stonenibbler.changed_survive_protocol.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.stonenibbler.changed_survive_protocol.ChangedSurviveProtocol;
import net.stonenibbler.changed_survive_protocol.client.screen.LatexCentrifugeScreen;
import net.stonenibbler.changed_survive_protocol.client.screen.MicroscopeScreen;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPMenus;

@OnlyIn(Dist.CLIENT)
public final class CSPClientEvents {
    private static final ResourceLocation INFECTION_OVERLAY = ChangedSurviveProtocol.resource("infection_overlay");
    private static final ResourceLocation COVERAGE_OVERLAY = ChangedSurviveProtocol.resource("coverage_overlay");

    private CSPClientEvents() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(CSPClientEvents::registerOverlays);
        modBus.addListener(CSPClientEvents::registerKeyMappings);
        modBus.addListener(CSPClientEvents::registerReloadListeners);
        modBus.addListener(CSPClientEvents::clientSetup);
        MinecraftForge.EVENT_BUS.addListener(CSPClientEvents::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(CSPClientEvents::onClientLogout);
        MinecraftForge.EVENT_BUS.addListener(CSPClientEvents::onLevelUnload);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(CSPMenus.MICROSCOPE.get(), MicroscopeScreen::new);
            MenuScreens.register(CSPMenus.LATEX_CENTRIFUGE.get(), LatexCentrifugeScreen::new);
        });
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        CSPKeyMappings.register(event);
    }

    private static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.VIGNETTE.id(), INFECTION_OVERLAY.getPath(), CSPOverlays::renderInfectionOverlay);
        event.registerAbove(INFECTION_OVERLAY, COVERAGE_OVERLAY.getPath(), CSPOverlays::renderCoverageOverlay);
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((barrier, resourceManager, preparationProfiler, reloadProfiler, backgroundExecutor, gameExecutor) ->
                barrier.wait((Void)null).thenRunAsync(CSPOverlays::releaseReloadableTextures, gameExecutor));
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        while (CSPKeyMappings.TOGGLE_DARK_LATEX_MASK.consumeClick()) {
            CSPClientData.toggleDarkLatexMaskOverlay();
        }
    }

    private static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        CSPClientData.clear();
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            CSPClientData.clear();
        }
    }
}
