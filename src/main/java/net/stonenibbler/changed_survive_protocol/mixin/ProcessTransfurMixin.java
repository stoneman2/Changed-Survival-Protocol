package net.stonenibbler.changed_survive_protocol.mixin;

import net.ltxprogrammer.changed.entity.ai.AssimilationBehavior;
import net.ltxprogrammer.changed.entity.ai.LatexAssimilationDecision;
import net.ltxprogrammer.changed.entity.ai.NonLatexAssimilationDecision;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.stonenibbler.changed_survive_protocol.common.event.CSPTransfurEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ProcessTransfur.class, remap = false)
public abstract class ProcessTransfurMixin {
    @Inject(method = "computeAssimilationBehavior(Lnet/minecraft/world/entity/LivingEntity;Lnet/ltxprogrammer/changed/entity/ai/LatexAssimilationDecision;)Lnet/ltxprogrammer/changed/entity/ai/AssimilationBehavior;", at = @At("HEAD"), cancellable = true)
    private static void csp$handleSuitedLatexExposure(LivingEntity entity, LatexAssimilationDecision<?> decision, CallbackInfoReturnable<AssimilationBehavior> cir) {
        if (entity instanceof ServerPlayer player && CSPTransfurEvents.hasTemporarySuit(player)) {
            cir.setReturnValue(CSPTransfurEvents.suitLatexExposureBehavior(player, decision));
        }
    }

    @Inject(method = "computeAssimilationBehavior(Lnet/minecraft/world/entity/LivingEntity;Lnet/ltxprogrammer/changed/entity/ai/LatexAssimilationDecision;)Lnet/ltxprogrammer/changed/entity/ai/AssimilationBehavior;", at = @At("RETURN"), cancellable = true)
    private static void csp$wrapLatexProgressBehavior(LivingEntity entity, LatexAssimilationDecision<?> decision, CallbackInfoReturnable<AssimilationBehavior> cir) {
        AssimilationBehavior behavior = cir.getReturnValue();
        if (behavior != null && entity instanceof ServerPlayer player) {
            cir.setReturnValue(CSPTransfurEvents.wrapLatexProgressBehavior(player, decision, behavior));
        }
    }

    @Inject(method = "computeAssimilationBehavior(Lnet/minecraft/world/entity/LivingEntity;Lnet/ltxprogrammer/changed/entity/ai/NonLatexAssimilationDecision;)Lnet/ltxprogrammer/changed/entity/ai/AssimilationBehavior;", at = @At("RETURN"), cancellable = true)
    private static void csp$wrapNonLatexProgressBehavior(LivingEntity entity, NonLatexAssimilationDecision<?> decision, CallbackInfoReturnable<AssimilationBehavior> cir) {
        AssimilationBehavior behavior = cir.getReturnValue();
        if (behavior != null && entity instanceof ServerPlayer player) {
            cir.setReturnValue(CSPTransfurEvents.wrapNonLatexProgressBehavior(player, decision, behavior));
        }
    }

    @Inject(method = "computeAssimilationBehavior(Lnet/minecraft/world/entity/LivingEntity;Lnet/ltxprogrammer/changed/entity/ai/NonLatexAssimilationDecision;)Lnet/ltxprogrammer/changed/entity/ai/AssimilationBehavior;", at = @At("HEAD"), cancellable = true)
    private static void csp$handleSuitedNonLatexExposure(LivingEntity entity, NonLatexAssimilationDecision<?> decision, CallbackInfoReturnable<AssimilationBehavior> cir) {
        if (entity instanceof ServerPlayer player && CSPTransfurEvents.hasTemporarySuit(player)) {
            cir.setReturnValue(CSPTransfurEvents.suitNonLatexExposureBehavior(player, decision));
        }
    }

    @Inject(method = "setPlayerTransfurProgress", at = @At("HEAD"), cancellable = true)
    private static void csp$redirectDirectProgress(Player player, float progress, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer && CSPTransfurEvents.tryHandleDirectProgress(serverPlayer, progress)) {
            ci.cancel();
        }
    }
}
