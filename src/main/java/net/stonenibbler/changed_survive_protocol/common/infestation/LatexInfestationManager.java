package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.ltxprogrammer.changed.init.ChangedLatexTypes;
import net.ltxprogrammer.changed.world.LatexCoverState;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.stonenibbler.changed_survive_protocol.ChangedSurviveProtocol;
import net.stonenibbler.changed_survive_protocol.common.config.CSPConfig;
import net.stonenibbler.changed_survive_protocol.common.gamerule.CSPGameRules;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class LatexInfestationManager {
    private static final int COLLAPSE_HEART_SEARCH_RADIUS = 8;
    private static final int COLLAPSE_HEART_SEARCH_UP = 4;
    private static final int COLLAPSE_HEART_SEARCH_DOWN = 12;
    private static final int PROTECTION_REFRESH_INTERVAL = 100;

    private LatexInfestationManager() {
    }

    public static LatexInfestationSavedData.HeartRecord ensureHeart(ServerLevel level, BlockPos pos, LatexHeartBlock.Kind kind) {
        LatexInfestationSavedData data = LatexInfestationSavedData.get(level);
        LatexInfestationSavedData.HeartRecord heart = data.addHeart(pos, kind);
        if (level.getBlockEntity(pos) instanceof LatexHeartBlockEntity blockEntity) {
            blockEntity.setHeartId(heart.id());
        }
        data.claim(pos, heart.id());
        LatexHeartNodes.updateHeartProtection(level, data, heart);
        return heart;
    }

    public static boolean spawnHeartAt(ServerLevel level, BlockPos pos, LatexHeartBlock.Kind kind) {
        if (!LatexCoverRules.canPlaceHeart(level, pos)) {
            return false;
        }
        return placeHeartBlock(level, pos, kind);
    }

    public static boolean spawnCollapseHeartNear(ServerLevel level, BlockPos origin, LatexHeartBlock.Kind kind) {
        BlockPos softTarget = nearestHeartTarget(level, origin, candidate -> LatexCoverRules.canPlaceHeartReplacingSoftBlock(level, candidate));
        if (softTarget != null && placeHeartReplacingSoftBlock(level, softTarget, kind)) {
            return true;
        }

        BlockPos sturdyTarget = nearestHeartTarget(level, origin, candidate -> LatexCoverRules.canForceHeartOnExistingSupport(level, candidate));
        if (sturdyTarget != null && forcePlaceHeartAt(level, sturdyTarget, kind, false)) {
            return true;
        }

        return forcePlaceHeartAt(level, clampedBuildPos(level, origin), kind, true);
    }

    private static BlockPos nearestHeartTarget(ServerLevel level, BlockPos origin, Predicate<BlockPos> canPlace) {
        BlockPos best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int dx = -COLLAPSE_HEART_SEARCH_RADIUS; dx <= COLLAPSE_HEART_SEARCH_RADIUS; dx++) {
            for (int dz = -COLLAPSE_HEART_SEARCH_RADIUS; dz <= COLLAPSE_HEART_SEARCH_RADIUS; dz++) {
                for (int dy = -COLLAPSE_HEART_SEARCH_DOWN; dy <= COLLAPSE_HEART_SEARCH_UP; dy++) {
                    BlockPos candidate = origin.offset(dx, dy, dz);
                    if (!canPlace.test(candidate)) {
                        continue;
                    }

                    int score = dx * dx + dz * dz + Math.abs(dy) * 3;
                    if (score < bestScore) {
                        best = candidate.immutable();
                        bestScore = score;
                    }
                }
            }
        }
        return best;
    }

    private static boolean placeHeartReplacingSoftBlock(ServerLevel level, BlockPos pos, LatexHeartBlock.Kind kind) {
        if (!LatexCoverRules.canPlaceHeartReplacingSoftBlock(level, pos)) {
            return false;
        }
        LatexCoverRules.clearSoftHeartTarget(level, pos);
        return placeHeartBlock(level, pos, kind);
    }

    private static boolean forcePlaceHeartAt(ServerLevel level, BlockPos pos, LatexHeartBlock.Kind kind, boolean forceSupport) {
        if (!level.isLoaded(pos) || level.isOutsideBuildHeight(pos)) {
            return false;
        }

        BlockPos supportPos = pos.below();
        if (level.isOutsideBuildHeight(supportPos)) {
            return false;
        }

        if (forceSupport && !level.getBlockState(supportPos).isFaceSturdy(level, supportPos, Direction.UP)) {
            level.setBlockAndUpdate(supportPos, LatexInfestationBlocks.sourceBlock(kind).defaultBlockState());
            LatexCoverState.setAtAndUpdate(level, supportPos, ChangedLatexTypes.NONE.get().defaultCoverState());
        }

        LatexCoverRules.clearSoftHeartTarget(level, pos);
        LatexCoverState.setAtAndUpdate(level, pos, ChangedLatexTypes.NONE.get().defaultCoverState());
        return placeHeartBlock(level, pos, kind);
    }

    private static BlockPos clampedBuildPos(ServerLevel level, BlockPos origin) {
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - 1;
        int y = Math.max(minY, Math.min(maxY, origin.getY()));
        return new BlockPos(origin.getX(), y, origin.getZ());
    }

    private static boolean placeHeartBlock(ServerLevel level, BlockPos pos, LatexHeartBlock.Kind kind) {
        boolean spawned = level.setBlockAndUpdate(pos, LatexInfestationBlocks.heartBlock(kind).defaultBlockState());
        if (spawned) {
            sendDebugSpawnMessage(level, pos, kind);
        }
        return spawned;
    }

    private static void sendDebugSpawnMessage(ServerLevel level, BlockPos pos, LatexHeartBlock.Kind kind) {
        if (!CSPConfig.COMMON.debugLatexHeartSpawnMessages.get()) {
            return;
        }

        String dimension = level.dimension().location().toString();
        String command = "/execute in " + dimension + " run tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
        MutableComponent message = Component.literal("[CSP] ").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal(kind.name().toLowerCase(java.util.Locale.ROOT) + " latex heart spawned at ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ()).withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to run: " + command)))))
                .append(Component.literal(" in " + dimension).withStyle(ChatFormatting.DARK_GRAY));

        level.getServer().getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(message));
    }

    public static void removeHeart(ServerLevel level, BlockPos pos) {
        LatexInfestationSavedData.get(level).removeHeart(pos);
    }

    public static void removeNode(ServerLevel level, BlockPos pos) {
        LatexHeartNodes.removeNode(level, pos);
    }

    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LatexInfestationSavedData.get(level).removeMob(event.getEntity().getUUID());
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level) {
            LatexInfestationSavedData.get(level).removeMob(event.getEntity().getUUID());
        }
    }

    public static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        LatexInfestationSpawnControl.onSpawnPlacementCheck(event);
    }

    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        LatexInfestationSavedData data = LatexInfestationSavedData.get(level);
        event.getAffectedBlocks().removeIf(pos -> isExplosionProtectedHeart(level, data, pos));
    }

    private static boolean isExplosionProtectedHeart(ServerLevel level, LatexInfestationSavedData data, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof LatexHeartBlock)) {
            return false;
        }

        Optional<LatexInfestationSavedData.HeartRecord> heart = data.heartAt(pos);
        if (heart.isEmpty()) {
            return state.hasProperty(LatexHeartBlock.PROTECTED) && state.getValue(LatexHeartBlock.PROTECTED);
        }

        List<BlockPos> nodes = LatexHeartNodes.activeNodes(level, data, heart.get());
        if (nodes.isEmpty()) {
            return false;
        }

        LatexHeartNodes.updateHeartProtection(level, data, heart.get());
        return true;
    }

    public static boolean isClaimed(ServerLevel level, BlockPos pos) {
        return LatexInfestationSavedData.get(level).claimedBy(pos).isPresent();
    }

    public static void markPlayerSecretion(ServerLevel level, BlockPos pos) {
        PlayerSecretions.add(level, pos);
    }

    public static boolean isPlayerSecretion(ServerLevel level, BlockPos pos) {
        return PlayerSecretions.contains(level, pos);
    }

    public static void onLatexCoverChanged(ServerLevel level, BlockPos pos, LatexCoverState oldState, LatexCoverState newState) {
        if (newState.isAir() || removedManagedCoverFace(level, pos, oldState, newState)) {
            onLatexCoverRemoved(level, pos);
        }
    }

    private static boolean removedManagedCoverFace(ServerLevel level, BlockPos pos, LatexCoverState oldState, LatexCoverState newState) {
        LatexInfestationSavedData data = LatexInfestationSavedData.get(level);
        Optional<LatexInfestationSavedData.HeartRecord> heart = data.claimedBy(pos).flatMap(data::heart);
        if (heart.isEmpty()) {
            return false;
        }

        if (!oldState.is(LatexInfestationBlocks.latexState(heart.get().kind()))) {
            return false;
        }
        if (!newState.is(LatexInfestationBlocks.latexState(heart.get().kind()))) {
            return true;
        }
        return LatexCoverRules.hasFewerCoverFaces(oldState, newState);
    }

    public static void onLatexCoverRemoved(ServerLevel level, BlockPos pos) {
        LatexInfestationSavedData data = LatexInfestationSavedData.get(level);
        data.removePlayerSecretion(pos);
        Optional<LatexInfestationSavedData.HeartRecord> heart = data.claimedBy(pos).flatMap(data::heart);
        if (heart.isEmpty()) {
            return;
        }

        if (shouldKeepClaim(level, data, heart.get(), pos)) {
            return;
        }

        data.unclaim(pos);
        LatexCoverRules.wakeNearbyCover(level, pos);
    }

    private static void cleanupStaleClaimsNear(ServerLevel level, BlockPos changedPos) {
        LatexInfestationSavedData data = LatexInfestationSavedData.get(level);
        for (BlockPos pos : BlockPos.betweenClosed(changedPos.offset(-1, -1, -1), changedPos.offset(1, 1, 1))) {
            BlockPos immutable = pos.immutable();
            Optional<LatexInfestationSavedData.HeartRecord> heart = data.claimedBy(immutable).flatMap(data::heart);
            if (heart.isPresent() && !shouldKeepClaim(level, data, heart.get(), immutable)) {
                data.unclaim(immutable);
                LatexCoverRules.wakeNearbyCover(level, immutable);
            }
        }
    }

    private static boolean shouldKeepClaim(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, BlockPos pos) {
        if (pos.equals(heart.pos())) {
            return true;
        }

        BlockState state = level.getBlockState(pos);
        return LatexCoverRules.isLatexBlock(state)
                || data.shouldRemoveDecorationBlock(pos, state)
                || LatexCoverRules.isOwnedCover(level, data, heart, pos);
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        LatexCoverRules.wakeNearbyCover(level, event.getPos());
        cleanupStaleClaimsNear(level, event.getPos());
        if (!(event.getState().getBlock() instanceof LatexHeartBlock)) {
            return;
        }

        LatexInfestationSavedData data = LatexInfestationSavedData.get(level);
        Optional<LatexInfestationSavedData.HeartRecord> heart = data.heartAt(event.getPos());
        if (heart.isEmpty()) {
            return;
        }

        List<BlockPos> nodes = LatexHeartNodes.activeNodes(level, data, heart.get());
        LatexHeartNodes.updateHeartProtection(level, data, heart.get());
        if (nodes.isEmpty()) {
            return;
        }

        event.setCanceled(true);
        event.setExpToDrop(0);
        level.setBlock(event.getPos(), level.getBlockState(event.getPos()), 3);
        LatexHeartSignaling.protectedBreakFeedback(level, event.getPlayer(), heart.get(), nodes);
    }

    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LatexCoverRules.wakeNearbyCover(level, event.getPos());
            cleanupStaleClaimsNear(level, event.getPos());
        }
    }

    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LatexCoverRules.wakeNearbyCover(level, event.getPos());
            cleanupStaleClaimsNear(level, event.getPos());
        }
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        LatexHeartSeeder.onChunkLoad(event);
    }

    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        if (level.players().isEmpty()) {
            return;
        }

        try {
            LatexHeartSeeder.processPendingChunks(level);
        } catch (RuntimeException exception) {
            ChangedSurviveProtocol.LOGGER.error("Latex infestation chunk seeding failed in {}", level.dimension().location(), exception);
        }

        LatexInfestationSavedData data = LatexInfestationSavedData.get(level);
        long gameTime = level.getGameTime();
        for (LatexInfestationSavedData.HeartRecord heart : data.activeHearts()) {
            try {
                if (!level.isLoaded(heart.pos())) {
                    continue;
                }
                if (!(level.getBlockState(heart.pos()).getBlock() instanceof LatexHeartBlock)) {
                    data.removeHeart(heart.id());
                    continue;
                }

                if (shouldRefreshHeartProtection(heart, gameTime)) {
                    LatexHeartNodes.updateHeartProtection(level, data, heart);
                }
                LatexHeartSignaling.tick(level, data, heart, gameTime);

                if (gameTime >= heart.nextGrowthTick()) {
                    int interval = Math.max(1, level.getGameRules().getInt(CSPGameRules.LATEX_HEART_GROWTH_INTERVAL));
                    LatexHeartGrowth.growHeart(level, data, heart);
                    heart = data.heart(heart.id()).orElse(heart).withNextGrowthTick(gameTime + interval);
                    data.updateHeart(heart);
                    LatexHeartNodes.updateHeartProtection(level, data, heart);
                }
                if (gameTime >= heart.nextMobTick()) {
                    LatexHeartMobSpawner.spawnMob(level, data, heart);
                    int interval = level.getGameRules().getInt(CSPGameRules.LATEX_HEART_MOB_SPAWN_INTERVAL);
                    if (interval > 0) {
                        data.updateHeart(heart.withNextMobTick(gameTime + interval / 2L + level.random.nextInt(Math.max(1, interval))));
                    }
                }
            } catch (RuntimeException exception) {
                data.updateHeart(heart.withNextGrowthTick(gameTime + 20L).withNextMobTick(gameTime + 20L));
                ChangedSurviveProtocol.LOGGER.error("Latex infestation tick failed for heart {} at {} in {}", heart.id(), heart.pos(), level.dimension().location(), exception);
            }
        }

        try {
            LatexInfestationDecay.decayDeadClaims(level, data, gameTime);
            PlayerSecretions.tick(level, data);
            data.cleanupNodeCooldowns(gameTime);
            LatexHeartSeeder.maybeSeedLoadedChunk(level, data);
        } catch (RuntimeException exception) {
            ChangedSurviveProtocol.LOGGER.error("Latex infestation maintenance failed in {}", level.dimension().location(), exception);
        }
    }

    private static boolean shouldRefreshHeartProtection(LatexInfestationSavedData.HeartRecord heart, long gameTime) {
        return Math.floorMod(gameTime + heart.id().getLeastSignificantBits(), PROTECTION_REFRESH_INTERVAL) == 0;
    }
}
