package net.stonenibbler.changed_survive_protocol.mixin;

import net.ltxprogrammer.changed.init.ChangedGameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = ChangedGameRules.class, remap = false)
public abstract class ChangedGameRulesMixin {
    @ModifyArg(
            method = "<clinit>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules$BooleanValue;create(Z)Lnet/minecraft/world/level/GameRules$Type;", ordinal = 2, remap = true),
            index = 0)
    private static boolean csp$keepFormDefaultsToTrue(boolean original) {
        return true;
    }

    @ModifyArg(
            method = "<clinit>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules$BooleanValue;create(Z)Lnet/minecraft/world/level/GameRules$Type;", ordinal = 0, remap = true),
            index = 0)
    private static boolean csp$keepConsciousnessDefaultsToTrue(boolean original) {
        return true;
    }
}
