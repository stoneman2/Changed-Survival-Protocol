package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.stonenibbler.changed_survive_protocol.ChangedSurviveProtocol;

public final class CSPBiomeTags {
    public static final TagKey<Biome> LATEX_HEART_ALLOWED = create("latex_heart_allowed_biomes");
    public static final TagKey<Biome> LATEX_HEART_DENIED = create("latex_heart_denied_biomes");
    public static final TagKey<Biome> DARK_LATEX_HEART = create("dark_latex_heart_biomes");
    public static final TagKey<Biome> WHITE_LATEX_HEART = create("white_latex_heart_biomes");

    private CSPBiomeTags() {
    }

    private static TagKey<Biome> create(String path) {
        return TagKey.create(Registries.BIOME, ChangedSurviveProtocol.resource(path));
    }
}
