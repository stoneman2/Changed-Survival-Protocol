package net.stonenibbler.changed_survive_protocol.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.client.gui.TransfurProgressOverlay;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.entity.variant.TransfurVariantInstance;
import net.ltxprogrammer.changed.init.ChangedRegistry;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.ltxprogrammer.changed.util.Color3;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.stonenibbler.changed_survive_protocol.common.config.CSPConfig;
import net.stonenibbler.changed_survive_protocol.common.latex.LatexStrandManager;
import net.stonenibbler.changed_survive_protocol.common.network.SyncCSPPlayerDataPacket;

@OnlyIn(Dist.CLIENT)
public final class CSPOverlays {
    private static final ResourceLocation GOO_OUTLINE = ResourceLocation.fromNamespaceAndPath("changed_survive_protocol", "textures/misc/goo_outline.png");
    private static final ResourceLocation DARK_LATEX_MASK = ResourceLocation.fromNamespaceAndPath("changed_survive_protocol", "textures/misc/latex_mask.png");
    private static final ResourceLocation VIGNETTE = ResourceLocation.parse("textures/misc/vignette.png");
    private static float darkLatexMaskVisibility;

    private CSPOverlays() {
    }

    public static void renderInfectionOverlay(Gui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (Minecraft.getInstance().options.hideGui) {
            return;
        }

        SyncCSPPlayerDataPacket data = CSPClientData.get();
        Player player = Minecraft.getInstance().player;
        if (data == null || player == null) {
            return;
        }
        if (!CSPConfig.CLIENT.infectionOverlayEnabled.get()) {
            return;
        }

        float infection = percent(data.infectionPercent());
        if (infection <= 0.0F || isFullyTransfurred(player, partialTick)) {
            return;
        }

        OverlayColor color = colorForStrain(data.strainId());
        float screenScale = Minecraft.getInstance().options.screenEffectScale().get().floatValue();
        float pulse = pulse(1700L);
        float alpha = infectionAlpha(infection, pulse) * screenScale;

        renderFullscreenTexture(graphics, screenWidth, screenHeight, GOO_OUTLINE, color.red, color.green, color.blue, alpha);
        renderFinalInfectionWash(graphics, screenWidth, screenHeight, player, data.strainId(), infection, pulse, screenScale, color);
    }

    public static void renderCoverageOverlay(Gui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (Minecraft.getInstance().options.hideGui) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        SyncCSPPlayerDataPacket data = CSPClientData.get();
        if (shouldRenderSharedMeter(player, data)) {
            MeterMode mode = meterMode(player, data);
            float meterValue = meterValue(mode, data);
            OverlayColor color = mode == MeterMode.LUCIDITY ? lucidityColor(meterValue) : colorForStrain(data.strainId());
            float screenScale = Minecraft.getInstance().options.screenEffectScale().get().floatValue();
            float danger = dangerFor(mode, meterValue);
            float criticalPulse = mode == MeterMode.LUCIDITY && danger >= 0.95F ? pulse(260L) * 0.05F : 0.0F;
            float maxVignetteAlpha = mode == MeterMode.LUCIDITY ? CSPConfig.CLIENT.lowLucidityVignetteMaxAlpha.get().floatValue() : CSPConfig.CLIENT.coverageVignetteMaxAlpha.get().floatValue();
            float vignetteBase = mode == MeterMode.LUCIDITY ? 0.02F : 0.0F;
            float vignettePower = mode == MeterMode.LUCIDITY ? 1.45F : 2.35F;
            float vignetteAlpha = Mth.clamp((vignetteBase + (float)Math.pow(danger, vignettePower) * maxVignetteAlpha + criticalPulse) * screenScale, 0.0F, maxVignetteAlpha + 0.05F);

            renderFullscreenTexture(graphics, screenWidth, screenHeight, VIGNETTE, color.red, color.green, color.blue, vignetteAlpha);
            renderSharedMeter(gui, graphics, player, screenWidth, screenHeight, meterValue, color, mode);
        }

        renderDarkLatexMaskOverlay(graphics, screenWidth, screenHeight, player, true);
    }

    public static boolean shouldSuppressChangedDangerOverlay(Player player) {
        return player != null && CSPClientData.get() != null && CSPConfig.CLIENT.sharedStatusMeterEnabled.get();
    }

    private static boolean shouldRenderSharedMeter(Player player, SyncCSPPlayerDataPacket data) {
        if (player == null || data == null || !CSPConfig.CLIENT.sharedStatusMeterEnabled.get()) {
            return false;
        }

        MeterMode mode = meterMode(player, data);
        return meterValue(mode, data) > 0.0F || mode != MeterMode.COVERAGE;
    }

    private static void renderSharedMeter(Gui gui, GuiGraphics graphics, Player player, int screenWidth, int screenHeight, float value, OverlayColor color, MeterMode mode) {
        TransfurProgressOverlay.Position position = Changed.config.client.transfurMeterPosition.get();
        int x = position.getX(screenWidth, player);
        int y = position.getY(screenHeight);
        int innerHeight = 30;
        int filledPixels = Mth.ceil(value * innerHeight);
        int fillTop = y + 31 - filledPixels;
        float danger = dangerFor(mode, value);
        float alpha = Mth.clamp(0.42F + danger * 0.58F, 0.0F, 1.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        graphics.fill(x, y, x + 8, y + 32, 0xCC050505);
        graphics.fill(x + 1, y + 1, x + 7, y + 31, 0xAA151515);

        if (filledPixels > 0) {
            graphics.fill(x + 1, fillTop, x + 7, y + 31, toArgb(color, alpha));
            graphics.fill(x + 2, fillTop, x + 3, y + 31, toArgb(new OverlayColor(1.0F, 1.0F, 1.0F), alpha * 0.22F));
        }

        if (danger >= 0.95F && (System.currentTimeMillis() / 120L) % 2L == 0L) {
            graphics.fill(x - 1, y - 1, x + 9, y + 33, 0x55FFFFFF);
        }

        int textColor = danger >= 0.95F ? 0xFFFF5555 : 0xFFE6E6E6;
        graphics.drawString(gui.getFont(), mode.label + " " + Mth.floor(value * 100.0F) + "%", x + 12, y + 12, textColor);

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static float infectionAlpha(float infection, float pulse) {
        float lowEnd = percent(CSPConfig.CLIENT.infectionOverlayLowEndPercent.get());
        float criticalStart = percent(CSPConfig.CLIENT.infectionOverlayCriticalStartPercent.get());
        float lowMax = CSPConfig.CLIENT.infectionOverlayLowMaxAlpha.get().floatValue();
        float midMax = CSPConfig.CLIENT.infectionOverlayMidMaxAlpha.get().floatValue();
        float critical = CSPConfig.CLIENT.infectionOverlayCriticalAlpha.get().floatValue();
        float pulseAlpha = CSPConfig.CLIENT.infectionOverlayPulseAlpha.get().floatValue();

        if (infection < lowEnd) {
            return Mth.lerp(Mth.clamp(infection / Math.max(0.001F, lowEnd), 0.0F, 1.0F), 0.0F, lowMax);
        }
        if (infection < criticalStart) {
            float progress = Mth.clamp((infection - lowEnd) / Math.max(0.001F, criticalStart - lowEnd), 0.0F, 1.0F);
            return Mth.lerp(progress, lowMax, midMax);
        }

        return Mth.clamp(critical + pulse * pulseAlpha, 0.0F, 1.0F);
    }

    private static void renderFinalInfectionWash(GuiGraphics graphics, int screenWidth, int screenHeight, Player player, String strainId, float infection, float pulse, float screenScale, OverlayColor fallbackColor) {
        float criticalStart = percent(CSPConfig.CLIENT.infectionOverlayCriticalStartPercent.get());
        if (infection <= criticalStart) {
            return;
        }

        float progress = Mth.clamp((infection - criticalStart) / Math.max(0.001F, 1.0F - criticalStart), 0.0F, 1.0F);
        float spreading = smootherStep(progress);
        float pulseBoost = pulse * 0.055F * spreading;
        float alpha = Mth.clamp((spreading * CSPConfig.CLIENT.infectionOverlayFinalWashAlpha.get().floatValue() + pulseBoost) * screenScale, 0.0F, 1.0F);
        if (alpha <= 0.005F) {
            return;
        }

        graphics.fill(0, 0, screenWidth, screenHeight, toArgb(infectionWashColor(player, strainId, fallbackColor), alpha));
    }

    private static OverlayColor lucidityColor(float lucidity) {
        if (lucidity <= 0.2F) {
            return new OverlayColor(1.0F, 0.16F, 0.18F);
        }
        if (lucidity <= 0.5F) {
            return new OverlayColor(1.0F, 0.72F, 0.24F);
        }
        return new OverlayColor(0.32F, 0.95F, 1.0F);
    }

    private static void renderDarkLatexMaskOverlay(GuiGraphics graphics, int screenWidth, int screenHeight, Player player, boolean respectHiddenGui) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean shouldShow = !(respectHiddenGui && minecraft.options.hideGui)
                && CSPConfig.CLIENT.darkLatexMaskOverlayEnabled.get()
                && CSPClientData.isDarkLatexMaskOverlayVisible()
                && minecraft.options.getCameraType() == CameraType.FIRST_PERSON
                && isMaskedLatexForm(player);

        darkLatexMaskVisibility = Mth.clamp(darkLatexMaskVisibility + (shouldShow ? 0.08F : -0.12F), 0.0F, 1.0F);
        if (darkLatexMaskVisibility <= 0.005F) {
            return;
        }

        float alpha = CSPConfig.CLIENT.darkLatexMaskOverlayAlpha.get().floatValue() * minecraft.options.screenEffectScale().get().floatValue() * smootherStep(darkLatexMaskVisibility);
        renderFullscreenTexture(graphics, screenWidth, screenHeight, DARK_LATEX_MASK, 1.0F, 1.0F, 1.0F, alpha);
    }

    public static void releaseReloadableTextures() {
        darkLatexMaskVisibility = 0.0F;
        Minecraft.getInstance().getTextureManager().release(DARK_LATEX_MASK);
    }

    private static boolean isMaskedLatexForm(Player player) {
        return LatexStrandManager.resolve(player)
                .map(LatexStrandManager.Strand::hasMask)
                .orElse(false);
    }

    private static boolean isFullyTransfurred(Player player, float partialTick) {
        var variant = ProcessTransfur.getPlayerTransfurVariant(player);
        return variant != null && !variant.isTemporaryFromSuit() && variant.getTransfurProgression(partialTick) >= 1.0F;
    }

    private static float percent(double value) {
        if (Double.isNaN(value)) {
            return 0.0F;
        }
        return Mth.clamp((float)value / 100.0F, 0.0F, 1.0F);
    }

    private static float pulse(long periodMs) {
        double angle = (System.currentTimeMillis() % periodMs) / (double)periodMs * Math.PI * 2.0D;
        return (float)((Math.sin(angle) + 1.0D) * 0.5D);
    }

    private static float smootherStep(float value) {
        float progress = Mth.clamp(value, 0.0F, 1.0F);
        return progress * progress * progress * (progress * (progress * 6.0F - 15.0F) + 10.0F);
    }

    private static MeterMode meterMode(Player player, SyncCSPPlayerDataPacket data) {
        if (ProcessTransfur.isPlayerLatex(player)) {
            return MeterMode.LUCIDITY;
        }
        if (data.infected() || data.infectionPercent() > 0.0D) {
            return MeterMode.INFECTION;
        }
        return MeterMode.COVERAGE;
    }

    private static float meterValue(MeterMode mode, SyncCSPPlayerDataPacket data) {
        return switch (mode) {
            case COVERAGE -> percent(data.coverage());
            case INFECTION -> percent(data.infectionPercent());
            case LUCIDITY -> percent(data.lucidity());
        };
    }

    private static float dangerFor(MeterMode mode, float value) {
        return mode == MeterMode.LUCIDITY ? 1.0F - value : value;
    }

    private static OverlayColor colorForStrain(String strainId) {
        String id = strainId == null ? "" : strainId.toLowerCase(java.util.Locale.ROOT);
        OverlayColor registeredColor = colorFromVariantId(id);
        if (registeredColor != null) {
            return registeredColor;
        }
        if (id.contains("dark") || id.contains("black")) {
            return new OverlayColor(0.22F, 0.12F, 0.34F);
        }
        if (id.contains("white") || id.contains("pale")) {
            return new OverlayColor(0.86F, 0.94F, 1.0F);
        }
        if (id.contains("shark") || id.contains("orca") || id.contains("squid") || id.contains("manta") || id.contains("eel") || id.contains("mermaid")) {
            return new OverlayColor(0.18F, 0.62F, 0.95F);
        }
        if (id.contains("dragon")) {
            return new OverlayColor(0.58F, 0.36F, 0.95F);
        }
        return new OverlayColor(0.72F, 0.74F, 0.78F);
    }

    private static OverlayColor infectionWashColor(Player player, String strainId, OverlayColor fallbackColor) {
        TransfurVariantInstance<?> liveVariant = ProcessTransfur.getPlayerTransfurVariant(player);
        if (liveVariant != null) {
            return fromChangedColor(liveVariant.getColors().getFirst());
        }

        OverlayColor registeredColor = colorFromVariantId(strainId);
        return registeredColor != null ? registeredColor : fallbackColor;
    }

    private static OverlayColor colorFromVariantId(String strainId) {
        if (strainId == null || strainId.isBlank()) {
            return null;
        }

        ResourceLocation id = ResourceLocation.tryParse(strainId);
        if (id == null) {
            return null;
        }

        TransfurVariant<?> variant = ChangedRegistry.TRANSFUR_VARIANT.get().getValue(id);
        if (variant == null) {
            return null;
        }

        return fromChangedColor(variant.getColors().getFirst());
    }

    private static OverlayColor fromChangedColor(Color3 color) {
        Color3 clamped = color.clamp();
        return new OverlayColor(clamped.red(), clamped.green(), clamped.blue());
    }

    private static int toArgb(OverlayColor color, float alpha) {
        int a = Mth.clamp((int)(alpha * 255.0F), 0, 255);
        int r = Mth.clamp((int)(color.red * 255.0F), 0, 255);
        int g = Mth.clamp((int)(color.green * 255.0F), 0, 255);
        int b = Mth.clamp((int)(color.blue * 255.0F), 0, 255);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static void renderFullscreenTexture(GuiGraphics graphics, int screenWidth, int screenHeight, ResourceLocation texture, float red, float green, float blue, float alpha) {
        if (alpha <= 0.005F) {
            return;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder.vertex(0.0D, screenHeight, -90.0D).uv(0.0F, 1.0F).color(red, green, blue, alpha).endVertex();
        bufferBuilder.vertex(screenWidth, screenHeight, -90.0D).uv(1.0F, 1.0F).color(red, green, blue, alpha).endVertex();
        bufferBuilder.vertex(screenWidth, 0.0D, -90.0D).uv(1.0F, 0.0F).color(red, green, blue, alpha).endVertex();
        bufferBuilder.vertex(0.0D, 0.0D, -90.0D).uv(0.0F, 0.0F).color(red, green, blue, alpha).endVertex();
        tesselator.end();

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private record OverlayColor(float red, float green, float blue) {
    }

    private enum MeterMode {
        COVERAGE("Coverage"),
        INFECTION("Infection"),
        LUCIDITY("Lucidity");

        private final String label;

        MeterMode(String label) {
            this.label = label;
        }
    }
}
