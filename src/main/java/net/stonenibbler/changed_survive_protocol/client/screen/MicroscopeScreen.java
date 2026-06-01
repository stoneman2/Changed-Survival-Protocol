package net.stonenibbler.changed_survive_protocol.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.stonenibbler.changed_survive_protocol.common.menu.MicroscopeMenu;

import java.util.List;

public class MicroscopeScreen extends AbstractContainerScreen<MicroscopeMenu> {
    public MicroscopeScreen(MicroscopeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF1E1E24);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, 0xFF2A2A33);
        graphics.fill(leftPos + 54, topPos + 33, leftPos + 74, topPos + 53, 0xFF111116);
        graphics.fill(leftPos + 114, topPos + 33, leftPos + 134, topPos + 53, 0xFF111116);
        graphics.fill(leftPos + 80, topPos + 39, leftPos + 108, topPos + 43, 0xFF4C4C5A);
        int total = Math.max(1, menu.data.get(1));
        int width = menu.data.get(0) * 24 / total;
        graphics.fill(leftPos + 82, topPos + 40, leftPos + 82 + width, topPos + 42, 0xFF8FD6FF);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderEmptySlotHints(graphics, mouseX, mouseY);
    }

    private void renderEmptySlotHints(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.container.getItem(0).isEmpty() && isHovering(54, 33, 22, 22, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("tooltip.changed_survive_protocol.microscope.input_slot")), mouseX, mouseY);
        } else if (menu.container.getItem(1).isEmpty() && isHovering(114, 33, 22, 22, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("tooltip.changed_survive_protocol.microscope.output_slot")), mouseX, mouseY);
        }
    }
}
