package net.stonenibbler.changed_survive_protocol.mixin;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.stonenibbler.changed_survive_protocol.common.config.CSPConfig;
import net.stonenibbler.changed_survive_protocol.common.latex.LatexStrandManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChangedEntity.class, remap = false)
public abstract class ChangedEntityTargetingMixin {
    @Inject(
            method = "targetSelectorTest",
            at = @At(value = "INVOKE", target = "Lnet/ltxprogrammer/changed/entity/ChangedEntity;getTransfurMode()Lnet/ltxprogrammer/changed/entity/TransfurMode;"),
            cancellable = true)
    private void csp$targetDifferentLatexPlayers(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (!CSPConfig.COMMON.latexMobsAttackDifferentLatexPlayers.get()) {
            return;
        }
        if (!(target instanceof Player player) || !ProcessTransfur.isPlayerLatex(player)) {
            return;
        }

        cir.setReturnValue(LatexStrandManager.shouldAttack((ChangedEntity)(Object)this, player));
    }
}
