package net.stonenibbler.changed_survive_protocol.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.stonenibbler.changed_survive_protocol.common.menu.MicroscopeMenu;

import java.util.List;

public class MicroscopeScreen extends AbstractContainerScreen<MicroscopeMenu> {
    private static final ResourceLocation BLANK_UI_TEXTURE = ResourceLocation.fromNamespaceAndPath("changed_survive_protocol", "textures/gui/blank_ui.png");
    private static final Component SCAN_LABEL = Component.literal("SCAN");
    private static final int DEFAULT_LABEL_COLOR = 4210752;

    public MicroscopeScreen(MicroscopeMenu menu, Inventory inventory, Component title) {
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

        int panelX = leftPos + 24;
        int panelY = topPos + 18;
        graphics.fill(panelX, panelY, panelX + 128, panelY + 52, 0xCC11151D);
        graphics.fill(panelX + 1, panelY + 1, panelX + 127, panelY + 51, 0xAA202633);
        drawFrame(graphics, panelX, panelY, 128, 52, 0xFF7EB6D8, 0xFF07090E);
        graphics.fill(panelX + 5, panelY + 5, panelX + 123, panelY + 6, 0x99BDEBFF);

        float progress = progress();
        drawSlotWell(graphics, leftPos + 38, topPos + 33);
        drawSlotWell(graphics, leftPos + 118, topPos + 33);
        drawStatusLights(graphics, leftPos + 144, topPos + 31, progress);
        drawScanRail(graphics, progress);

        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, DEFAULT_LABEL_COLOR, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, DEFAULT_LABEL_COLOR, false);
        drawSmallString(graphics, SCAN_LABEL, 136, 8, 0x8DDCFF, 0.75F);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderEmptySlotHints(graphics, mouseX, mouseY);
    }

    private void renderEmptySlotHints(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.container.getItem(0).isEmpty() && isHovering(38, 33, 22, 22, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("tooltip.changed_survive_protocol.microscope.input_slot")), mouseX, mouseY);
        } else if (menu.container.getItem(1).isEmpty() && isHovering(118, 33, 22, 22, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("tooltip.changed_survive_protocol.microscope.output_slot")), mouseX, mouseY);
        }
    }

    private void drawSlotWell(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 22, y + 22, 0xCC07090D);
        drawFrame(graphics, x, y, 22, 22, 0xFF9BC9E5, 0xFF050608);
        graphics.fill(x + 2, y + 2, x + 20, y + 20, 0xFF151821);
        graphics.fill(x + 3, y + 3, x + 19, y + 19, 0xFF090B11);
    }

    private void drawScanRail(GuiGraphics graphics, float progress) {
        int railX = leftPos + 64;
        int railY = topPos + 41;
        graphics.fill(railX, railY, railX + 52, railY + 5, 0xFF070A10);
        graphics.fill(railX + 1, railY + 1, railX + 51, railY + 4, 0xFF354354);
        graphics.fill(railX + 2, railY + 2, railX + 2 + (int)(48.0F * progress), railY + 3, 0xFF81D7FF);

        int scannerX = railX + 2 + (int)(46.0F * progress);
        graphics.fill(scannerX, railY - 2, scannerX + 2, railY + 7, 0xFFE5FAFF);
        graphics.fill(scannerX - 1, railY, scannerX + 3, railY + 5, 0x668EE8FF);
    }

    private void drawStatusLights(GuiGraphics graphics, int x, int y, float progress) {
        drawStatusLight(graphics, x, y, 0xFF78FFC4);
        drawStatusLight(graphics, x, y + 8, progress > 0.0F ? 0xFF80D9FF : 0xFF28313D);
        drawStatusLight(graphics, x, y + 16, hasOutput() ? 0xFFE9F97C : 0xFF28313D);
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

    private float progress() {
        int total = menu.data.get(1);
        if (total <= 0) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (float)menu.data.get(0) / total));
    }

    private boolean hasOutput() {
        return !menu.container.getItem(1).isEmpty();
    }
}
