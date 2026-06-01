package net.stonenibbler.changed_survive_protocol.mixin;

import net.ltxprogrammer.changed.entity.latex.SpreadingLatexType;
import net.ltxprogrammer.changed.world.LatexCoverState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.stonenibbler.changed_survive_protocol.common.infestation.LatexInfestationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {SpreadingLatexType.DarkLatex.class, SpreadingLatexType.WhiteLatex.class}, remap = false)
public abstract class SpreadingLatexSubtypeMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void csp$stopChangedSubtypeRandomTickForManagedLatex(LatexCoverState state, ServerLevel level, BlockPos blockPos, RandomSource random, CallbackInfo ci) {
        if (LatexInfestationManager.isClaimed(level, blockPos) || LatexInfestationManager.isPlayerSecretion(level, blockPos)) {
            ci.cancel();
        }
    }
}
