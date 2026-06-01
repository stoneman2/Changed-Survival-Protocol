package net.stonenibbler.changed_survive_protocol.mixin;

import net.ltxprogrammer.changed.world.LatexCoverState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.stonenibbler.changed_survive_protocol.common.infestation.LatexInfestationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(value = LatexCoverState.class, remap = false)
public abstract class LatexCoverStateMixin {
    @Unique
    private static final ThreadLocal<Deque<LatexCoverState>> csp$oldCoverStates = ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "onRemove", at = @At("TAIL"))
    private void csp$removeManagedClaimWhenCoverStateIsRemoved(Level level, BlockPos pos, LatexCoverState newState, boolean moved, CallbackInfo ci) {
        LatexCoverState oldState = (LatexCoverState)(Object)this;
        if (!oldState.isAir() && level instanceof ServerLevel serverLevel) {
            LatexInfestationManager.onLatexCoverChanged(serverLevel, pos, oldState, newState);
        }
    }

    @Inject(method = "setAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/ltxprogrammer/changed/world/LatexCoverState;II)Z", at = @At("HEAD"))
    private static void csp$captureOldCoverState(Level level, BlockPos pos, LatexCoverState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        csp$oldCoverStates.get().push(LatexCoverState.getAt(level, pos));
    }

    @Inject(method = "setAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/ltxprogrammer/changed/world/LatexCoverState;II)Z", at = @At("RETURN"))
    private static void csp$removeManagedClaimWhenCoverIsCleared(Level level, BlockPos pos, LatexCoverState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        Deque<LatexCoverState> oldStates = csp$oldCoverStates.get();
        LatexCoverState oldState = oldStates.isEmpty() ? null : oldStates.pop();
        if (oldStates.isEmpty()) {
            csp$oldCoverStates.remove();
        }

        if (cir.getReturnValueZ() && oldState != null && !oldState.isAir() && level instanceof ServerLevel serverLevel) {
            LatexInfestationManager.onLatexCoverChanged(serverLevel, pos, oldState, state);
        }
    }
}
