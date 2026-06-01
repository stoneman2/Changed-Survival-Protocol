package net.stonenibbler.changed_survive_protocol.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.stonenibbler.changed_survive_protocol.common.menu.LatexCentrifugeMenu;

import java.util.List;

public class LatexCentrifugeScreen extends AbstractContainerScreen<LatexCentrifugeMenu> {
    public LatexCentrifugeScreen(LatexCentrifugeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF1B2024);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, 0xFF293038);
        graphics.fill(leftPos + 33, topPos + 33, leftPos + 55, topPos + 55, 0xFF101418);
        graphics.fill(leftPos + 60, topPos + 33, leftPos + 82, topPos + 55, 0xFF101418);
        graphics.fill(leftPos + 87, topPos + 33, leftPos + 109, topPos + 55, 0xFF101418);
        graphics.fill(leftPos + 114, topPos + 33, leftPos + 136, topPos + 55, 0xFF101418);
        graphics.fill(leftPos + 150, topPos + 33, leftPos + 172, topPos + 55, 0xFF101418);
        graphics.fill(leftPos + 139, topPos + 39, leftPos + 148, topPos + 43, 0xFF4D5965);
        int total = Math.max(1, menu.data.get(1));
        int width = menu.data.get(0) * 8 / total;
        int color = menu.data.get(2) == 2 ? 0xFFD7A7FF : 0xFF9BE86D;
        graphics.fill(leftPos + 140, topPos + 40, leftPos + 140 + width, topPos + 42, color);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderEmptySlotHints(graphics, mouseX, mouseY);
    }

    private void renderEmptySlotHints(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.container.getItem(0).isEmpty() && isHovering(33, 33, 22, 22, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("tooltip.changed_survive_protocol.centrifuge.strand_slot")), mouseX, mouseY);
        } else if (menu.container.getItem(1).isEmpty() && isHovering(60, 33, 22, 22, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("tooltip.changed_survive_protocol.centrifuge.additive_slot")), mouseX, mouseY);
        } else if (menu.container.getItem(2).isEmpty() && isHovering(87, 33, 22, 22, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("tooltip.changed_survive_protocol.centrifuge.disinfectant_slot")), mouseX, mouseY);
        } else if (menu.container.getItem(3).isEmpty() && isHovering(114, 33, 22, 22, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("tooltip.changed_survive_protocol.centrifuge.latex_feed_slot")), mouseX, mouseY);
        } else if (menu.container.getItem(4).isEmpty() && isHovering(150, 33, 22, 22, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable("tooltip.changed_survive_protocol.centrifuge.output_slot")), mouseX, mouseY);
        }
    }
}
