package net.stonenibbler.changed_survive_protocol.mixin;

import net.ltxprogrammer.changed.ability.AbstractAbilityInstance;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Predicate;

@Mixin(value = ChangedEntity.class, remap = false)
public interface ChangedEntityAccessor {
    @Invoker("registerAbility")
    <A extends AbstractAbilityInstance> A csp$registerAbility(Predicate<A> predicate, A instance);
}
