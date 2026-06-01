package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.ltxprogrammer.changed.init.ChangedLatexTypes;
import net.ltxprogrammer.changed.world.LatexCoverState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.stonenibbler.changed_survive_protocol.common.gamerule.CSPGameRules;

import java.util.ArrayList;
import java.util.List;

final class LatexInfestationDecay {
    private static final int MAX_DECAY_PER_INTERVAL = 8;
    private static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private LatexInfestationDecay() {
    }

    static void decayDeadClaims(ServerLevel level, LatexInfestationSavedData data, long gameTime) {
        int interval = level.getGameRules().getInt(CSPGameRules.LATEX_HEART_DECAY_INTERVAL);
        if (interval <= 0) {
            return;
        }

        int decayed = 0;
        for (LatexInfestationSavedData.HeartRecord heart : data.deadHearts()) {
            if (decayed >= MAX_DECAY_PER_INTERVAL) {
                break;
            }
            if (gameTime < heart.nextDecayTick()) {
                continue;
            }

            decayed += decayHeartClaims(level, data, heart, MAX_DECAY_PER_INTERVAL - decayed);
            data.heart(heart.id()).ifPresent(current -> data.updateHeart(current.withNextDecayTick(gameTime + interval)));
            data.forgetDeadHeartIfUnclaimed(heart.id());
        }
    }

    private static int decayHeartClaims(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, int limit) {
        int decayed = 0;
        List<BlockPos> claims = data.claimsFor(heart.id());
        LatexInfestationUtil.shuffle(claims, level.random);
        cleanupStaleClaims(level, data, heart, claims);
        decayed += decayClaims(level, data, heart, exposedClaims(level, data, heart, claims), limit);
        if (decayed < limit) {
            decayed += decayClaims(level, data, heart, claims, limit - decayed);
        }
        return decayed;
    }

    private static void cleanupStaleClaims(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, List<BlockPos> claims) {
        for (BlockPos pos : claims) {
            if (!level.isLoaded(pos) || !data.claimedBy(pos).filter(heart.id()::equals).isPresent()) {
                continue;
            }
            if (hasDecayTarget(level, data, pos)) {
                continue;
            }

            data.unclaim(pos);
        }
    }

    private static int decayClaims(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, List<BlockPos> claims, int limit) {
        int decayed = 0;
        for (BlockPos pos : claims) {
            if (decayed >= limit) {
                break;
            }
            if (!level.isLoaded(pos) || !data.claimedBy(pos).filter(heart.id()::equals).isPresent()) {
                continue;
            }

            decayClaim(level, data, pos);
            decayed++;
        }
        return decayed;
    }

    private static List<BlockPos> exposedClaims(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, List<BlockPos> claims) {
        List<BlockPos> exposed = new ArrayList<>();
        for (BlockPos pos : claims) {
            if (level.isLoaded(pos) && isExposed(level, data, heart, pos)) {
                exposed.add(pos);
            }
        }
        return exposed;
    }

    private static boolean isExposed(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, BlockPos pos) {
        if (!hasDecayTarget(level, data, pos)) {
            return true;
        }

        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            if (!hasOwnedNeighborColumn(data, heart, pos, direction)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOwnedNeighborColumn(LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, BlockPos pos, Direction direction) {
        for (int dy = -1; dy <= 1; dy++) {
            BlockPos neighbor = pos.relative(direction).offset(0, dy, 0);
            if (data.claimedBy(neighbor).filter(heart.id()::equals).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDecayTarget(ServerLevel level, LatexInfestationSavedData data, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return isOwnedLatexBlock(state)
                || data.shouldRemoveDecorationBlock(pos, state)
                || !LatexCoverState.getAt(level, pos).isAir();
    }

    private static void decayClaim(ServerLevel level, LatexInfestationSavedData data, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (data.isDecoration(pos)) {
            removeDecorationBlock(level, data, pos, state);
        } else if (isOwnedLatexBlock(state)) {
            removeBlock(level, pos);
        }
        if (!LatexCoverState.getAt(level, pos).isAir()) {
            LatexCoverState.setAtAndUpdate(level, pos, ChangedLatexTypes.NONE.get().defaultCoverState());
        }
        data.unclaim(pos);
    }

    private static void removeDecorationBlock(ServerLevel level, LatexInfestationSavedData data, BlockPos pos, BlockState state) {
        if (data.shouldRemoveDecorationBlock(pos, state)) {
            removeBlock(level, pos);
            removeAttachedDecorationParts(level, data, pos);
        }
    }

    private static void removeAttachedDecorationParts(ServerLevel level, LatexInfestationSavedData data, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (!level.isLoaded(neighbor)) {
                continue;
            }

            BlockState neighborState = level.getBlockState(neighbor);
            if (data.isDecoration(neighbor) && data.shouldRemoveDecorationBlock(neighbor, neighborState)) {
                removeBlock(level, neighbor);
                data.unclaim(neighbor);
            } else if (LatexInfestationSavedData.isAttachedDecorationPart(neighborState)) {
                removeBlock(level, neighbor);
            }
        }
    }

    private static boolean isOwnedLatexBlock(BlockState state) {
        return state.getBlock() instanceof LatexNodeBlock
                || state.getBlock() instanceof LatexHeartBlock
                || state.is(LatexInfestationBlocks.sourceBlock(LatexHeartBlock.Kind.DARK))
                || state.is(LatexInfestationBlocks.sourceBlock(LatexHeartBlock.Kind.WHITE))
                || state.is(LatexInfestationBlocks.fluidBlock(LatexHeartBlock.Kind.DARK))
                || state.is(LatexInfestationBlocks.fluidBlock(LatexHeartBlock.Kind.WHITE));
    }

    private static void removeBlock(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }
}
