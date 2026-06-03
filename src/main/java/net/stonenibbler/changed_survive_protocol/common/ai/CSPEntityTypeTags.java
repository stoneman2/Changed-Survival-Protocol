package net.stonenibbler.changed_survive_protocol.common.ai;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.stonenibbler.changed_survive_protocol.ChangedSurviveProtocol;

public final class CSPEntityTypeTags {
    public static final TagKey<EntityType<?>> HUNTER_AI_LATEX_BEASTS = create("hunter_ai_latex_beasts");

    private CSPEntityTypeTags() {
    }

    private static TagKey<EntityType<?>> create(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, ChangedSurviveProtocol.resource(path));
    }
}
