package net.stonenibbler.changed_survive_protocol.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.stonenibbler.changed_survive_protocol.common.menu.LatexCentrifugeMenu;

import java.util.List;

public class LatexCentrifugeScreen extends AbstractContainerScreen<LatexCentrifugeMenu> {
    private static final ResourceLocation BLANK_UI_TEXTURE = ResourceLocation.fromNamespaceAndPath("changed_survive_protocol", "textures/gui/blank_ui.png");
    private static final Component STRAND_LABEL = Component.literal("STR");
    private static final Component ADDITIVE_LABEL = Component.literal("ADD");
    private static final Component DISINFECT_LABEL = Component.literal("SPR");
    private static final Component FEED_LABEL = Component.literal("LAT");
    private static final Component OUTPUT_LABEL = Component.literal("OUT");
    private static final Component IDLE_LABEL = Component.literal("IDLE");
    private static final Component CURE_LABEL = Component.literal("CURE");
    private static final Component CULTURE_LABEL = Component.literal("CULTURE");
    private static final int DEFAULT_LABEL_COLOR = 4210752;

    public LatexCentrifugeScreen(LatexCentrifugeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.blit(BLANK_UI_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);

        float progress = progress();
        int accent = modeAccent();
        drawMachineDeck(graphics, accent);
        drawSlotWell(graphics, leftPos + 20, topPos + 31, accent);
        drawSlotWell(graphics, leftPos + 50, topPos + 31, accent);
        drawSlotWell(graphics, leftPos + 80, topPos + 31, accent);
        drawSlotWell(graphics, leftPos + 110, topPos + 31, accent);
        drawSlotWell(graphics, leftPos + 140, topPos + 31, outputAccent());
        drawProcessLines(graphics, accent, progress);
        drawStatusLights(graphics, leftPos + 164, topPos + 31, accent, progress);

        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, DEFAULT_LABEL_COLOR, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, DEFAULT_LABEL_COLOR, false);
        drawSmallString(graphics, STRAND_LABEL, 24, 23, 0x86A4B8, 0.65F);
        drawSmallString(graphics, ADDITIVE_LABEL, 54, 23, 0x86A4B8, 0.65F);
        drawSmallString(graphics, DISINFECT_LABEL, 84, 23, 0x86A4B8, 0.65F);
        drawSmallString(graphics, FEED_LABEL, 114, 23, 0x86A4B8, 0.65F);
        drawSmallString(graphics, OUTPUT_LABEL, 144, 23, 0xD7D0A0, 0.65F);
        drawSmallString(graphics, modeLabel(), 138, 8, modeAccent(), 0.7F);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderEmptySlotHints(graphics, mouseX, mouseY);
    }

    private void renderEmptySlotHints(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.container.getItem(0).isEmpty() && isHovering(20, 31, 22, 22, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("tooltip.changed_survive_protocol.centrifuge.strand_slot")), mouseX, mouseY);
        } else if (menu.container.getItem(1).isEmpty() && isHovering(50, 31, 22, 22, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("tooltip.changed_survive_protocol.centrifuge.additive_slot")), mouseX, mouseY);
        } else if (menu.container.getItem(2).isEmpty() && isHovering(80, 31, 22, 22, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("tooltip.changed_survive_protocol.centrifuge.disinfectant_slot")), mouseX, mouseY);
        } else if (menu.container.getItem(3).isEmpty() && isHovering(110, 31, 22, 22, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("tooltip.changed_survive_protocol.centrifuge.latex_feed_slot")), mouseX, mouseY);
        } else if (menu.container.getItem(4).isEmpty() && isHovering(140, 31, 22, 22, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("tooltip.changed_survive_protocol.centrifuge.output_slot")), mouseX, mouseY);
        }
    }

    private void drawMachineDeck(GuiGraphics graphics, int accent) {
        int x = leftPos + 14;
        int y = topPos + 18;
        graphics.fill(x, y, x + 158, y + 53, 0xCC10151D);
        graphics.fill(x + 1, y + 1, x + 157, y + 52, 0xAA202A36);
        drawFrame(graphics, x, y, 158, 53, accent, 0xFF06080C);
        graphics.fill(leftPos + 18, topPos + 69, leftPos + 168, topPos + 70, 0x6644515C);
    }

    private void drawSlotWell(GuiGraphics graphics, int x, int y, int accent) {
        graphics.fill(x, y, x + 22, y + 22, 0xCC07090D);
        drawFrame(graphics, x, y, 22, 22, accent, 0xFF050608);
        graphics.fill(x + 2, y + 2, x + 20, y + 20, 0xFF151A22);
        graphics.fill(x + 3, y + 3, x + 19, y + 19, 0xFF090C11);
    }

    private void drawProcessLines(GuiGraphics graphics, int accent, float progress) {
        int y = topPos + 60;
        graphics.fill(leftPos + 31, y - 1, leftPos + 151, y + 3, 0xFF06080C);
        graphics.fill(leftPos + 33, y, leftPos + 149, y + 2, 0xFF3B4653);
        graphics.fill(leftPos + 33, y, leftPos + 33 + (int)(116.0F * progress), y + 2, accent);

        int pulseX = leftPos + 33 + (int)(112.0F * progress);
        graphics.fill(pulseX, y - 2, pulseX + 4, y + 4, 0x99FFFFFF);
    }

    private void drawStatusLights(GuiGraphics graphics, int x, int y, int accent, float progress) {
        drawStatusLight(graphics, x, y, 0xFF78FFC4);
        drawStatusLight(graphics, x, y + 9, progress > 0.0F ? accent : 0xFF28313D);
        drawStatusLight(graphics, x, y + 18, hasOutput() ? 0xFFFFED86 : 0xFF28313D);
    }

    private void drawStatusLight(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x - 1, y - 1, x + 6, y + 6, 0xFF05070A);
        graphics.fill(x, y, x + 5, y + 5, color);
        graphics.fill(x + 1, y + 1, x + 4, y + 2, 0x99FFFFFF);
    }

    private void drawFrame(GuiGraphics graphics, int x, int y, int width, int height, int topColor, int bottomColor) {
        graphics.fill(x, y, x + width, y + 1, topColor);
        graphics.fill(x, y, x + 1, y + height, topColor);
        graphics.fill(x + width - 1, y, x + width, y + height, bottomColor);
        graphics.fill(x, y + height - 1, x + width, y + height, bottomColor);
    }

    private void drawSmallString(GuiGraphics graphics, Component text, int x, int y, int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, (int)(x / scale), (int)(y / scale), color, false);
        graphics.pose().popPose();
    }

    private int modeAccent() {
        return switch (menu.data.get(2)) {
            case 1 -> 0xFF9BE86D;
            case 2 -> 0xFFD7A7FF;
            default -> 0xFF80D9FF;
        };
    }

    private int outputAccent() {
        return hasOutput() ? 0xFFFFED86 : 0xFFB8B48D;
    }

    private Component modeLabel() {
        return switch (menu.data.get(2)) {
            case 1 -> CURE_LABEL;
            case 2 -> CULTURE_LABEL;
            default -> IDLE_LABEL;
        };
    }

    private float progress() {
        int total = menu.data.get(1);
        if (total <= 0) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (float)menu.data.get(0) / total));
    }

    private boolean hasOutput() {
        return !menu.container.getItem(4).isEmpty();
    }
}
