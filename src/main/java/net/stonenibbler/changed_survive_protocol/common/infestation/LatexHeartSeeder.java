package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.stonenibbler.changed_survive_protocol.common.gamerule.CSPGameRules;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

final class LatexHeartSeeder {
    private static final Map<ServerLevel, Queue<ChunkPos>> PENDING_NEW_CHUNKS = new HashMap<>();

    private LatexHeartSeeder() {
    }

    static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk) || !event.isNewChunk()) {
            return;
        }
        if (!level.getGameRules().getBoolean(CSPGameRules.DO_LATEX_HEART_INFESTATIONS) || !level.getGameRules().getBoolean(CSPGameRules.LATEX_HEART_NEW_CHUNK_SEEDING)) {
            return;
        }
        PENDING_NEW_CHUNKS.computeIfAbsent(level, ignored -> new ArrayDeque<>()).add(chunk.getPos());
    }

    static void processPendingChunks(ServerLevel level) {
        Queue<ChunkPos> queue = PENDING_NEW_CHUNKS.get(level);
        if (queue == null) {
            return;
        }
        int processed = 0;
        while (!queue.isEmpty() && processed++ < 4) {
            ChunkPos pos = queue.poll();
            trySeedChunk(level, LatexInfestationSavedData.get(level), pos, true);
        }
        if (queue.isEmpty()) {
            PENDING_NEW_CHUNKS.remove(level);
        }
    }

    static void maybeSeedLoadedChunk(ServerLevel level, LatexInfestationSavedData data) {
        if (!level.getGameRules().getBoolean(CSPGameRules.DO_LATEX_HEART_INFESTATIONS) || !level.getGameRules().getBoolean(CSPGameRules.LATEX_HEART_LOADED_CHUNK_SEEDING)) {
            return;
        }
        int chance = level.getGameRules().getInt(CSPGameRules.LATEX_HEART_LOADED_CHUNK_CHANCE);
        if (chance <= 0 || level.random.nextInt(chance) != 0 || level.players().isEmpty()) {
            return;
        }
        if (!hasActiveHeartCapacity(level, data)) {
            return;
        }
        ChunkPos chunk = new ChunkPos(level.players().get(level.random.nextInt(level.players().size())).blockPosition());
        int dx = level.random.nextInt(17) - 8;
        int dz = level.random.nextInt(17) - 8;
        trySeedChunk(level, data, new ChunkPos(chunk.x + dx, chunk.z + dz), false);
    }

    private static void trySeedChunk(ServerLevel level, LatexInfestationSavedData data, ChunkPos chunk, boolean newChunk) {
        if (!level.getGameRules().getBoolean(CSPGameRules.DO_LATEX_HEART_INFESTATIONS)) {
            return;
        }
        if (!hasActiveHeartCapacity(level, data)) {
            return;
        }
        if (newChunk && !data.markChunkGenerated(chunk)) {
            return;
        }
        int chance = newChunk ? level.getGameRules().getInt(CSPGameRules.LATEX_HEART_NEW_CHUNK_CHANCE) : level.getGameRules().getInt(CSPGameRules.LATEX_HEART_LOADED_CHUNK_CHANCE);
        if (chance <= 0 || level.random.nextInt(chance) != 0) {
            return;
        }

        int x = chunk.getMinBlockX() + level.random.nextInt(16);
        int z = chunk.getMinBlockZ() + level.random.nextInt(16);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos center = new BlockPos(x, y, z);
        if (!level.getBiome(center).is(CSPBiomeTags.LATEX_HEART_ALLOWED) || level.getBiome(center).is(CSPBiomeTags.LATEX_HEART_DENIED)) {
            return;
        }
        LatexHeartBlock.Kind kind = kindForBiome(level, center);
        LatexInfestationManager.spawnHeartAt(level, center, kind);
    }

    private static boolean hasActiveHeartCapacity(ServerLevel level, LatexInfestationSavedData data) {
        int maxActive = level.getGameRules().getInt(CSPGameRules.LATEX_HEART_MAX_ACTIVE_PER_DIMENSION);
        return maxActive <= 0 || data.activeHeartCount() < maxActive;
    }

    private static LatexHeartBlock.Kind kindForBiome(ServerLevel level, BlockPos pos) {
        boolean dark = level.getBiome(pos).is(CSPBiomeTags.DARK_LATEX_HEART);
        boolean white = level.getBiome(pos).is(CSPBiomeTags.WHITE_LATEX_HEART);
        if (dark && !white) {
            return LatexHeartBlock.Kind.DARK;
        }
        if (white && !dark) {
            return LatexHeartBlock.Kind.WHITE;
        }
        return randomHeartKind(level.random);
    }

    private static LatexHeartBlock.Kind randomHeartKind(RandomSource random) {
        return random.nextBoolean() ? LatexHeartBlock.Kind.DARK : LatexHeartBlock.Kind.WHITE;
    }
}
