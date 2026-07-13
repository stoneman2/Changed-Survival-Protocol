package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.ltxprogrammer.changed.entity.latex.SpreadingLatexType;
import net.ltxprogrammer.changed.init.ChangedLatexTypes;
import net.ltxprogrammer.changed.world.LatexCoverState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.stonenibbler.changed_survive_protocol.common.gamerule.CSPGameRules;

import java.util.ArrayList;
import java.util.List;

final class LatexHeartGrowth {
    private static final Direction[] GROWTH_DIRECTIONS = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN};
    private static final int MAX_CLAIM_CLEANUP_CHECKS = 12;
    private static final int MAX_FRONTIER_CHECKS = 32;
    private static final int MAX_LOW_PRIORITY_FRONTIER_CHECKS = 8;
    private static final int MIN_VERTICAL_STEP_SCAN = -3;
    private static final int MAX_VERTICAL_STEP_SCAN = 4;

    private LatexHeartGrowth() {
    }

    static boolean growHeart(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, boolean lowPriority) {
        int maxClaims = level.getGameRules().getInt(CSPGameRules.LATEX_HEART_MAX_CLAIMS);
        int claimCount = data.claimCount(heart.id());
        boolean grew = false;

        boolean canClaimNewPositions = maxClaims <= 0 || claimCount < maxClaims;
        int attempts = lowPriority ? 2 : claimCount < 48 ? 4 : claimCount < 160 ? 6 : 8;
        for (int i = 0; i < attempts; i++) {
            BlockPos target = nextCoverPos(level, data, heart, canClaimNewPositions, lowPriority);
            if (target == null) {
                break;
            }
            if (tryGrowAt(level, data, heart, target, canClaimNewPositions)) {
                grew = true;
                break;
            }
        }

        if (!lowPriority) {
            LatexHeartDecorations.maybeDecorate(level, data, heart, claimCount);
            LatexHeartNodes.maybePlaceNode(level, data, heart, claimCount);
        }
        return grew;
    }

    static void cleanupDamagedOwnedCover(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart) {
        List<BlockPos> claims = LatexInfestationUtil.randomSample(data.claimPositionList(heart.id()), MAX_CLAIM_CLEANUP_CHECKS, level.random);
        for (BlockPos pos : claims) {
            if (pos.equals(heart.pos()) || !level.isLoaded(pos)) {
                continue;
            }
            if (!LatexCoverRules.isOwnedCover(level, data, heart, pos)) {
                LatexInfestationManager.onLatexCoverRemoved(level, pos);
            }
        }
    }

    private static boolean tryGrowAt(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, BlockPos pos, boolean canClaimNewPositions) {
        if (!LatexCoverRules.isValidCoverTarget(level, data, heart, pos, canClaimNewPositions)) {
            return false;
        }
        return tryPlaceCover(level, data, heart, pos, canClaimNewPositions);
    }

    private static boolean tryPlaceCover(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, BlockPos pos, boolean canClaimNewPositions) {
        if (!LatexCoverRules.isValidCoverTarget(level, data, heart, pos, canClaimNewPositions)) {
            return false;
        }

        BlockState oldState = level.getBlockState(pos);
        LatexCoverState cover = LatexCoverRules.coverStateFor(level, pos, heart.kind());
        if (cover.isAir()) {
            return false;
        }

        if (LatexCoverRules.becomesLatexBlockWhenCovered(oldState)) {
            return tryPlaceLatexBlock(level, data, heart, pos);
        }

        if (LatexCoverRules.isReplaceableFoliage(oldState)) {
            LatexCoverRules.removeFoliage(level, pos, oldState);
        }

        if (!LatexCoverState.setAtAndUpdate(level, pos, cover)) {
            return false;
        }
        LatexCoverRules.normalizeSupport(level, pos);
        data.claim(pos, heart.id());
        LatexCoverRules.wakeNearbyCover(level, pos);
        LatexCoverRules.maybePlaySpreadSound(level, pos);
        return true;
    }

    private static boolean tryPlaceLatexBlock(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, BlockPos pos) {
        if (!level.setBlockAndUpdate(pos, LatexInfestationBlocks.sourceBlock(heart.kind()).defaultBlockState())) {
            return false;
        }

        LatexCoverState.setAtAndUpdate(level, pos, ChangedLatexTypes.NONE.get().defaultCoverState());
        LatexCoverRules.normalizeSupport(level, pos);
        data.claim(pos, heart.id());
        LatexCoverRules.wakeNearbyCover(level, pos);
        LatexCoverRules.maybePlaySpreadSound(level, pos);
        return true;
    }

    private static BlockPos nextCoverPos(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, boolean canClaimNewPositions, boolean lowPriority) {
        int checks = lowPriority ? MAX_LOW_PRIORITY_FRONTIER_CHECKS : MAX_FRONTIER_CHECKS;
        List<BlockPos> claims = LatexInfestationUtil.randomSample(data.activeClaimPositionList(heart.id()), checks, level.random);
        if (claims.isEmpty()) {
            claims = LatexInfestationUtil.randomSample(data.claimPositionList(heart.id()), checks, level.random);
            if (claims.isEmpty()) {
                claims = List.of(heart.pos());
            }
        }

        for (BlockPos origin : claims) {
            if (!level.isLoaded(origin)) {
                continue;
            }
            if (!origin.equals(heart.pos()) && !LatexCoverRules.isOwnedCover(level, data, heart, origin)) {
                LatexInfestationManager.onLatexCoverRemoved(level, origin);
                continue;
            }

            List<BlockPos> candidates = shuffledCoverCandidates(level, origin);
            boolean checkedLoadedCandidate = false;
            boolean hasUnloadedCandidate = false;
            for (BlockPos candidate : candidates) {
                if (level.isOutsideBuildHeight(candidate)) {
                    checkedLoadedCandidate = true;
                    continue;
                }
                if (!level.isLoaded(candidate)) {
                    hasUnloadedCandidate = true;
                    continue;
                }

                checkedLoadedCandidate = true;
                if (LatexCoverRules.isValidCoverTarget(level, data, heart, candidate, canClaimNewPositions)) {
                    return candidate;
                }
            }
            if (checkedLoadedCandidate && !hasUnloadedCandidate && !origin.equals(heart.pos())) {
                data.markCoverExhausted(origin);
            }
        }
        return null;
    }

    private static List<BlockPos> shuffledCoverCandidates(ServerLevel level, BlockPos origin) {
        List<Direction> directions = new ArrayList<>(List.of(GROWTH_DIRECTIONS));
        LatexInfestationUtil.shuffle(directions, level.random);

        List<BlockPos> candidates = new ArrayList<>(directions.size() + 8);
        for (Direction direction : directions) {
            if (direction.getAxis().isHorizontal()) {
                BlockPos side = origin.relative(direction);
                for (int dy = MIN_VERTICAL_STEP_SCAN; dy <= MAX_VERTICAL_STEP_SCAN; dy++) {
                    candidates.add(side.offset(0, dy, 0).immutable());
                }
            } else {
                candidates.add(origin.relative(direction).immutable());
            }
        }
        addWrappedSurfaceCandidates(level, origin, candidates);
        return candidates;
    }

    private static void addWrappedSurfaceCandidates(ServerLevel level, BlockPos origin, List<BlockPos> candidates) {
        if (!level.isLoaded(origin)) {
            return;
        }
        LatexCoverState coverState = LatexCoverState.getAt(level, origin);
        if (coverState.isAir()) {
            return;
        }

        List<Direction> surfaces = new ArrayList<>(List.of(Direction.values()));
        LatexInfestationUtil.shuffle(surfaces, level.random);
        for (Direction surface : surfaces) {
            if (!coverState.getProperties().contains(SpreadingLatexType.FACES.get(surface)) || !coverState.getValue(SpreadingLatexType.FACES.get(surface))) {
                continue;
            }

            BlockPos supportPos = origin.relative(surface);
            for (Direction wrappedSurface : surfaces) {
                if (wrappedSurface == surface || wrappedSurface == surface.getOpposite()) {
                    continue;
                }
                candidates.add(supportPos.relative(wrappedSurface.getOpposite()).immutable());
            }
        }
    }
}
