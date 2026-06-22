package net.stonenibbler.changed_survive_protocol.common.gamerule;

import net.minecraft.world.level.GameRules;

public final class CSPGameRules {
    public static final GameRules.Key<GameRules.BooleanValue> DO_LATEX_HEART_INFESTATIONS = GameRules.register("csp:doLatexHeartInfestations", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> LATEX_HEART_NEW_CHUNK_SEEDING = GameRules.register("csp:latexHeartNewChunkSeeding", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> LATEX_HEART_LOADED_CHUNK_SEEDING = GameRules.register("csp:latexHeartLoadedChunkSeeding", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> LATEX_HEART_SPECIAL_BLOCKS = GameRules.register("csp:latexHeartSpecialBlocks", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> LATEX_HEART_MOB_SPAWNING = GameRules.register("csp:latexHeartMobSpawning", GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> LATEX_HEART_SUPPRESS_NATURAL_CHANGED_SPAWNS = GameRules.register("csp:latexHeartSuppressNaturalChangedSpawns", GameRules.Category.MOBS, GameRules.BooleanValue.create(true));

    public static final GameRules.Key<GameRules.IntegerValue> LATEX_HEART_NEW_CHUNK_CHANCE = GameRules.register("csp:latexHeartNewChunkChance", GameRules.Category.UPDATES, GameRules.IntegerValue.create(400));
    public static final GameRules.Key<GameRules.IntegerValue> LATEX_HEART_LOADED_CHUNK_CHANCE = GameRules.register("csp:latexHeartLoadedChunkChance", GameRules.Category.UPDATES, GameRules.IntegerValue.create(72000));
    public static final GameRules.Key<GameRules.IntegerValue> LATEX_HEART_GROWTH_INTERVAL = GameRules.register("csp:latexHeartGrowthInterval", GameRules.Category.UPDATES, GameRules.IntegerValue.create(120));
    public static final GameRules.Key<GameRules.IntegerValue> LATEX_HEART_MAX_CLAIMS = GameRules.register("csp:latexHeartMaxClaims", GameRules.Category.UPDATES, GameRules.IntegerValue.create(12000));
    public static final GameRules.Key<GameRules.IntegerValue> LATEX_HEART_MAX_ACTIVE_PER_DIMENSION = GameRules.register("csp:latexHeartMaxActivePerDimension", GameRules.Category.UPDATES, GameRules.IntegerValue.create(32));
    public static final GameRules.Key<GameRules.IntegerValue> LATEX_HEART_MAX_NODES = GameRules.register("csp:latexHeartMaxNodes", GameRules.Category.UPDATES, GameRules.IntegerValue.create(6));
    public static final GameRules.Key<GameRules.IntegerValue> LATEX_HEART_MAX_NODE_DISTANCE = GameRules.register("csp:latexHeartMaxNodeDistance", GameRules.Category.UPDATES, GameRules.IntegerValue.create(32));
    public static final GameRules.Key<GameRules.IntegerValue> LATEX_HEART_DECAY_INTERVAL = GameRules.register("csp:latexHeartDecayInterval", GameRules.Category.UPDATES, GameRules.IntegerValue.create(60));
    public static final GameRules.Key<GameRules.IntegerValue> LATEX_HEART_MOB_SPAWN_INTERVAL = GameRules.register("csp:latexHeartMobSpawnInterval", GameRules.Category.MOBS, GameRules.IntegerValue.create(1200));
    public static final GameRules.Key<GameRules.IntegerValue> LATEX_HEART_MOB_SPAWN_MIN_CLAIMS = GameRules.register("csp:latexHeartMobSpawnMinClaims", GameRules.Category.MOBS, GameRules.IntegerValue.create(80));
    public static final GameRules.Key<GameRules.IntegerValue> LATEX_HEART_MOB_SPAWN_CAP = GameRules.register("csp:latexHeartMobSpawnCap", GameRules.Category.MOBS, GameRules.IntegerValue.create(8));

    private CSPGameRules() {
    }

    public static void touch() {
    }
}
