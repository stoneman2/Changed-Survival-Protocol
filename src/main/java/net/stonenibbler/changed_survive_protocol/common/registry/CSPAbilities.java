package net.stonenibbler.changed_survive_protocol.common.registry;

import net.ltxprogrammer.changed.ability.AbstractAbility;
import net.ltxprogrammer.changed.init.ChangedRegistry;
import net.ltxprogrammer.changed.init.ChangedTags;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.stonenibbler.changed_survive_protocol.ChangedSurviveProtocol;
import net.stonenibbler.changed_survive_protocol.common.ability.SecreteAbility;

public final class CSPAbilities {
    public static final DeferredRegister<AbstractAbility<?>> ABILITIES = ChangedRegistry.ABILITY.createDeferred(ChangedSurviveProtocol.MODID);

    public static final RegistryObject<SecreteAbility> SECRETE = ABILITIES.register("secrete", SecreteAbility::new);

    private CSPAbilities() {
    }

    public static AbstractAbility<?> secreteFor(EntityType<?> entityType) {
        return entityType.is(ChangedTags.EntityTypes.LATEX) && !entityType.is(ChangedTags.EntityTypes.PARTIAL_LATEX) ? SECRETE.get() : null;
    }
}
