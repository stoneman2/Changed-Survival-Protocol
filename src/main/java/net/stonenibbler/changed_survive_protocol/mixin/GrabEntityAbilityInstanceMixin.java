package net.stonenibbler.changed_survive_protocol.mixin;

import net.ltxprogrammer.changed.ability.AbstractAbilityInstance.KeyReference;
import net.ltxprogrammer.changed.ability.GrabEntityAbilityInstance;
import net.ltxprogrammer.changed.entity.ai.LatexAssimilationDecision;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.stonenibbler.changed_survive_protocol.common.config.CSPConfig;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GrabEntityAbilityInstance.class, remap = false)
public abstract class GrabEntityAbilityInstanceMixin {
    @Shadow
    @Nullable
    public LivingEntity grabbedEntity;

    @Shadow
    public boolean suited;

    @Shadow
    public float suitTransition;

    @Shadow
    public boolean attackDown;

    @Shadow
    public boolean useDown;

    @Shadow
    int instructionTicks;

    @Shadow
    protected abstract @Nullable LatexAssimilationDecision<?> makeAssimilationDecision();

    @Shadow
    public abstract void releaseEntity(boolean applyDebuffs);

    @Inject(method = "handleInstructions", at = @At("HEAD"), cancellable = true)
    private void csp$replaceGrabInstructions(Level level, CallbackInfo ci) {
        if (!CSPConfig.COMMON.reworkGrabEntityControls.get() || !level.isClientSide) {
            return;
        }

        GrabEntityAbilityInstance self = (GrabEntityAbilityInstance)(Object)this;
        if (instructionTicks == 180 || instructionTicks == -180) {
            self.entity.displayClientMessage(Component.translatable("ability.changed_survive_protocol.grab_rework.how_to_release", KeyReference.ABILITY.getName(level)), true);
        } else if (instructionTicks == 120 || instructionTicks == -120) {
            self.entity.displayClientMessage(Component.translatable("ability.changed_survive_protocol.grab_rework.how_to_controls", KeyReference.ATTACK.getName(level), KeyReference.USE.getName(level)), true);
        }

        if (instructionTicks > 0) {
            instructionTicks--;
        } else if (instructionTicks < 0) {
            instructionTicks++;
        }
        ci.cancel();
    }

    @Inject(
            method = "tickIdle",
            at = @At(value = "INVOKE", target = "Lnet/ltxprogrammer/changed/ability/GrabEntityAbilityInstance;makeAssimilationDecision()Lnet/ltxprogrammer/changed/entity/ai/LatexAssimilationDecision;"),
            cancellable = true)
    private void csp$reworkGrabControls(CallbackInfo ci) {
        if (!CSPConfig.COMMON.reworkGrabEntityControls.get() || grabbedEntity == null) {
            return;
        }

        if (!suited && suitTransition > 0.0F) {
            suitTransition = Math.max(0.0F, suitTransition - 0.025F);
        }

        if (attackDown) {
            progressGrabDecision(true);
        } else if (useDown) {
            progressGrabDecision(false);
        }

        ci.cancel();
    }

    private void progressGrabDecision(boolean absorb) {
        LivingEntity target = grabbedEntity;
        if (target == null) {
            return;
        }

        boolean previousSuited = suited;
        suited = absorb;
        try {
            LatexAssimilationDecision<?> decision = makeAssimilationDecision();
            if (decision == null) {
                return;
            }
            if (absorb) {
                decision = decision.withTransfurProgress(decision.transfurProgress() * 1.5F);
            }
            if (ProcessTransfur.progressTransfur(target, decision)) {
                releaseEntity(false);
            }
        } finally {
            if (grabbedEntity != null) {
                suited = previousSuited;
            }
        }
    }
}
