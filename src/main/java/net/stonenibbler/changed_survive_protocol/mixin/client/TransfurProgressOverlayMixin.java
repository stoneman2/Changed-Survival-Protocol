package net.stonenibbler.changed_survive_protocol.mixin.client;

import net.ltxprogrammer.changed.client.gui.TransfurProgressOverlay;
import net.ltxprogrammer.changed.util.EntityUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.stonenibbler.changed_survive_protocol.client.CSPOverlays;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TransfurProgressOverlay.class, remap = false)
public abstract class TransfurProgressOverlayMixin {
    @Inject(method = "renderDangerOverlay", at = @At("HEAD"), cancellable = true)
    private static void csp$suppressChangedDangerOverlay(Gui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight, CallbackInfo ci) {
        Player player = EntityUtil.playerOrNull(Minecraft.getInstance().getCameraEntity());
        if (CSPOverlays.shouldSuppressChangedDangerOverlay(player)) {
            ci.cancel();
        }
    }
}
