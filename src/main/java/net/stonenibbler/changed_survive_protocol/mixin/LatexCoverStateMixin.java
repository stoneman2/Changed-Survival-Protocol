package net.stonenibbler.changed_survive_protocol.mixin;

import net.ltxprogrammer.changed.world.LatexCoverState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.stonenibbler.changed_survive_protocol.common.infestation.LatexInfestationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LatexCoverState.class, remap = false)
public abstract class LatexCoverStateMixin {
    @Inject(method = "onRemove", at = @At("TAIL"))
    private void csp$removeManagedClaimWhenCoverStateIsRemoved(Level level, BlockPos pos, LatexCoverState newState, boolean moved, CallbackInfo ci) {
        LatexCoverState oldState = (LatexCoverState)(Object)this;
        if (!oldState.isAir() && level instanceof ServerLevel serverLevel) {
            LatexInfestationManager.onLatexCoverChanged(serverLevel, pos, oldState, newState);
        }
    }
}
