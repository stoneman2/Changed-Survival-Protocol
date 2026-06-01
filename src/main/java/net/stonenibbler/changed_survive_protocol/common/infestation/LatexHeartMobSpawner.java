package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.ltxprogrammer.changed.entity.latex.SpreadingLatexType;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.world.LatexCoverState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.stonenibbler.changed_survive_protocol.ChangedSurviveProtocol;
import net.stonenibbler.changed_survive_protocol.common.gamerule.CSPGameRules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

final class LatexHeartMobSpawner {
    private static final int MOB_SPAWN_POSITION_ATTEMPTS = 48;
    private static final double MOB_SPAWN_PLAYER_MIN_DISTANCE_SQR = 36.0D;
    private static final double MOB_SPAWN_PLAYER_MAX_DISTANCE_SQR = 48.0D * 48.0D;
    private static final double MOB_SPAWN_HEART_MAX_DISTANCE_SQR = 48.0D * 48.0D;
    private static final double MOB_TRACKING_MAX_DISTANCE_SQR = 128.0D * 128.0D;
    private static final double FLOOR_COVER_SPAWN_Y_OFFSET = 0.125D;

    private LatexHeartMobSpawner() {
    }

    static void spawnMob(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart) {
        int claimCount = data.claimCount(heart.id());
        int minClaims = level.getGameRules().getInt(CSPGameRules.LATEX_HEART_MOB_SPAWN_MIN_CLAIMS);
        if (!level.getGameRules().getBoolean(CSPGameRules.LATEX_HEART_MOB_SPAWNING) || level.getGameRules().getInt(CSPGameRules.LATEX_HEART_MOB_SPAWN_INTERVAL) <= 0 || minClaims <= 0 || claimCount < minClaims) {
            return;
        }
        int mobCap = mobCap(level, claimCount, minClaims);
        if (mobCap <= 0) {
            return;
        }
        cleanupTrackedMobs(level, data, heart);
        if (data.mobCount(heart.id()) >= mobCap) {
            return;
        }
        EntityType<? extends Mob> type = chooseMobType(level, heart.kind(), claimCount);
        Mob mob = type.create(level);
        if (mob == null) {
            return;
        }
        MobSpawnSpot spawnSpot = findMobSpawnSpot(level, data, heart, mob);
        if (spawnSpot == null) {
            mob.discard();
            ChangedSurviveProtocol.LOGGER.debug("Latex heart {} at {} could not find a clear mob spawn spot from {} claims", heart.id(), heart.pos(), claimCount);
            return;
        }
        mob.moveTo(spawnSpot.x(), spawnSpot.y(), spawnSpot.z(), level.random.nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnSpot.blockPos()), MobSpawnType.MOB_SUMMONED, null, null);
        if (level.addFreshEntity(mob)) {
            data.addMob(mob.getUUID(), heart.id());
            ChangedSurviveProtocol.LOGGER.debug("Latex heart {} spawned {} at {}", heart.id(), type, spawnSpot.blockPos());
        } else {
            mob.discard();
        }
    }

    private static int mobCap(ServerLevel level, int claimCount, int minClaims) {
        int maxCap = level.getGameRules().getInt(CSPGameRules.LATEX_HEART_MOB_SPAWN_CAP);
        if (maxCap <= 0 || claimCount < minClaims) {
            return 0;
        }
        return Math.min(maxCap, 2 + (claimCount - minClaims) / 600);
    }

    private static MobSpawnSpot findMobSpawnSpot(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, Mob mob) {
        List<BlockPos> claims = data.claimsFor(heart.id());
        MobSpawnSpot nearPlayer = findMobSpawnSpotNearPlayers(level, claims, heart, mob);
        if (nearPlayer != null) {
            return nearPlayer;
        }
        if (hasPlayerNear(level, heart.pos(), MOB_SPAWN_HEART_MAX_DISTANCE_SQR)) {
            MobSpawnSpot nearHeart = findMobSpawnSpotNear(level, claims, heart, mob, heart.pos(), 0.0D, MOB_SPAWN_HEART_MAX_DISTANCE_SQR, MOB_SPAWN_POSITION_ATTEMPTS);
            if (nearHeart != null) {
                return nearHeart;
            }
        }
        return findMobSpawnSpotFromClaims(level, claims, heart, mob);
    }

    private static MobSpawnSpot findMobSpawnSpotNearPlayers(ServerLevel level, List<BlockPos> claims, LatexInfestationSavedData.HeartRecord heart, Mob mob) {
        List<ServerPlayer> players = level.players().stream()
                .filter(player -> !player.isSpectator())
                .sorted(Comparator.comparingDouble(player -> player.distanceToSqr(heart.pos().getX() + 0.5D, heart.pos().getY() + 0.5D, heart.pos().getZ() + 0.5D)))
                .toList();
        for (ServerPlayer player : players) {
            MobSpawnSpot spot = findMobSpawnSpotNear(level, claims, heart, mob, player.blockPosition(), MOB_SPAWN_PLAYER_MIN_DISTANCE_SQR, MOB_SPAWN_PLAYER_MAX_DISTANCE_SQR, MOB_SPAWN_POSITION_ATTEMPTS);
            if (spot != null) {
                return spot;
            }
        }
        return null;
    }

    private static MobSpawnSpot findMobSpawnSpotNear(ServerLevel level, List<BlockPos> claims, LatexInfestationSavedData.HeartRecord heart, Mob mob, BlockPos center, double minDistanceSqr, double maxDistanceSqr, int attempts) {
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos claim : claims) {
            double distance = claim.distSqr(center);
            if (distance >= minDistanceSqr && distance <= maxDistanceSqr && level.isLoaded(claim)) {
                candidates.add(claim);
            }
        }
        LatexInfestationUtil.shuffle(candidates, level.random);
        for (int i = 0; i < attempts && i < candidates.size(); i++) {
            MobSpawnSpot spot = spawnSpotFromClaim(level, heart, candidates.get(i));
            if (spot != null && canFitMob(level, mob, spot)) {
                return spot;
            }
        }
        return null;
    }

    private static MobSpawnSpot findMobSpawnSpotFromClaims(ServerLevel level, List<BlockPos> claims, LatexInfestationSavedData.HeartRecord heart, Mob mob) {
        for (int i = 0; i < MOB_SPAWN_POSITION_ATTEMPTS && !claims.isEmpty(); i++) {
            BlockPos claim = claims.get(level.random.nextInt(claims.size()));
            MobSpawnSpot spot = spawnSpotFromClaim(level, heart, claim);
            if (spot != null && canFitMob(level, mob, spot)) {
                return spot;
            }
        }
        return null;
    }

    private static boolean hasPlayerNear(ServerLevel level, BlockPos pos, double maxDistanceSqr) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && player.distanceToSqr(x, y, z) <= maxDistanceSqr) {
                return true;
            }
        }
        return false;
    }

    private static MobSpawnSpot spawnSpotFromClaim(ServerLevel level, LatexInfestationSavedData.HeartRecord heart, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(LatexInfestationBlocks.sourceBlock(heart.kind())) || state.getBlock() instanceof LatexHeartBlock || state.getBlock() instanceof LatexNodeBlock) {
            return spawnSpotAbove(level, pos);
        }
        if (!state.isAir()) {
            return null;
        }

        LatexCoverState coverState = LatexCoverState.getAt(level, pos);
        if (!coverState.is(LatexInfestationBlocks.latexState(heart.kind())) || !coverState.getProperties().contains(SpreadingLatexType.DOWN) || !coverState.getValue(SpreadingLatexType.DOWN)) {
            return null;
        }
        if (!isClearSpawnColumn(level, pos)) {
            return null;
        }
        return new MobSpawnSpot(pos.immutable(), pos.getX() + 0.5D, pos.getY() + FLOOR_COVER_SPAWN_Y_OFFSET, pos.getZ() + 0.5D);
    }

    private static MobSpawnSpot spawnSpotAbove(ServerLevel level, BlockPos supportPos) {
        BlockPos pos = supportPos.above();
        if (!isClearSpawnColumn(level, pos)) {
            return null;
        }
        return new MobSpawnSpot(pos.immutable(), pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
    }

    private static boolean isClearSpawnColumn(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos)
                && !level.isOutsideBuildHeight(pos)
                && !level.isOutsideBuildHeight(pos.above())
                && level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && level.getFluidState(pos).isEmpty()
                && level.getFluidState(pos.above()).isEmpty();
    }

    private static boolean canFitMob(ServerLevel level, Mob mob, MobSpawnSpot spot) {
        mob.moveTo(spot.x(), spot.y(), spot.z(), level.random.nextFloat() * 360.0F, 0.0F);
        return level.noCollision(mob);
    }

    private static EntityType<? extends Mob> chooseMobType(ServerLevel level, LatexHeartBlock.Kind kind, int claimCount) {
        if (kind == LatexHeartBlock.Kind.DARK) {
            int poolSize = claimCount >= 2400 ? 8 : claimCount >= 900 ? 6 : 4;
            return switch (level.random.nextInt(poolSize)) {
                case 0 -> ChangedEntities.DARK_LATEX_WOLF_MALE.get();
                case 1 -> ChangedEntities.DARK_LATEX_WOLF_FEMALE.get();
                case 2 -> ChangedEntities.DARK_LATEX_WOLF_PUP.get();
                case 3 -> ChangedEntities.DARK_LATEX_YUFENG.get();
                case 4 -> ChangedEntities.PHAGE_LATEX_WOLF_MALE.get();
                case 5 -> ChangedEntities.PHAGE_LATEX_WOLF_FEMALE.get();
                case 6 -> ChangedEntities.CRYSTAL_WOLF.get();
                default -> ChangedEntities.DARK_LATEX_DOUBLE_YUFENG.get();
            };
        }

        int poolSize = claimCount >= 2400 ? 8 : claimCount >= 900 ? 6 : 4;
        return switch (level.random.nextInt(poolSize)) {
            case 0 -> ChangedEntities.WHITE_LATEX_WOLF_MALE.get();
            case 1 -> ChangedEntities.WHITE_LATEX_WOLF_FEMALE.get();
            case 2 -> ChangedEntities.PURE_WHITE_LATEX_WOLF.get();
            case 3 -> ChangedEntities.PURE_WHITE_LATEX_WOLF_PUP.get();
            case 4 -> ChangedEntities.WHITE_LATEX_KNIGHT.get();
            case 5 -> ChangedEntities.LATEX_WHITE_TIGER.get();
            case 6 -> level.random.nextBoolean() ? ChangedEntities.LATEX_SNOW_LEOPARD_MALE.get() : ChangedEntities.LATEX_SNOW_LEOPARD_FEMALE.get();
            default -> ChangedEntities.WHITE_LATEX_CENTAUR.get();
        };
    }

    private static void cleanupTrackedMobs(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart) {
        for (UUID mobId : data.mobsFor(heart.id())) {
            Entity entity = level.getEntity(mobId);
            if (!(entity instanceof Mob mob) || !mob.isAlive() || !isInfestationMobType(mob.getType(), heart.kind()) || mob.distanceToSqr(heart.pos().getX() + 0.5D, heart.pos().getY() + 0.5D, heart.pos().getZ() + 0.5D) > MOB_TRACKING_MAX_DISTANCE_SQR) {
                data.removeMob(mobId);
            }
        }
    }

    private static boolean isInfestationMobType(EntityType<?> type, LatexHeartBlock.Kind kind) {
        if (kind == LatexHeartBlock.Kind.DARK) {
            return type == ChangedEntities.DARK_LATEX_WOLF_MALE.get()
                    || type == ChangedEntities.DARK_LATEX_WOLF_FEMALE.get()
                    || type == ChangedEntities.DARK_LATEX_WOLF_PUP.get()
                    || type == ChangedEntities.DARK_LATEX_YUFENG.get()
                    || type == ChangedEntities.DARK_LATEX_DOUBLE_YUFENG.get()
                    || type == ChangedEntities.PHAGE_LATEX_WOLF_MALE.get()
                    || type == ChangedEntities.PHAGE_LATEX_WOLF_FEMALE.get()
                    || type == ChangedEntities.CRYSTAL_WOLF.get();
        }

        return type == ChangedEntities.WHITE_LATEX_WOLF_MALE.get()
                || type == ChangedEntities.WHITE_LATEX_WOLF_FEMALE.get()
                || type == ChangedEntities.PURE_WHITE_LATEX_WOLF.get()
                || type == ChangedEntities.PURE_WHITE_LATEX_WOLF_PUP.get()
                || type == ChangedEntities.WHITE_LATEX_KNIGHT.get()
                || type == ChangedEntities.WHITE_LATEX_CENTAUR.get()
                || type == ChangedEntities.LATEX_WHITE_TIGER.get()
                || type == ChangedEntities.LATEX_SNOW_LEOPARD_MALE.get()
                || type == ChangedEntities.LATEX_SNOW_LEOPARD_FEMALE.get();
    }

    private record MobSpawnSpot(BlockPos blockPos, double x, double y, double z) {
    }
}
