package net.stonenibbler.changed_survive_protocol.mixin;

import net.ltxprogrammer.changed.ability.AbstractAbility;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.minecraft.world.entity.EntityType;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPAbilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@Mixin(value = TransfurVariant.Builder.class, remap = false)
public abstract class TransfurVariantBuilderMixin {
    @Shadow
    private List<Function<EntityType<?>, ? extends AbstractAbility<?>>> abilities;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void csp$addSecreteAbility(Supplier<?> entityType, CallbackInfo ci) {
        abilities.add(CSPAbilities::secreteFor);
    }
}
