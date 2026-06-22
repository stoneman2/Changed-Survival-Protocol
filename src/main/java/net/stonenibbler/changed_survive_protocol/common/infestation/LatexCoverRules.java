package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.ltxprogrammer.changed.entity.latex.SpreadingLatexType;
import net.ltxprogrammer.changed.init.ChangedBlocks;
import net.ltxprogrammer.changed.init.ChangedLatexTypes;
import net.ltxprogrammer.changed.init.ChangedTags;
import net.ltxprogrammer.changed.world.LatexCoverState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class LatexCoverRules {
    private LatexCoverRules() {
    }

    static boolean canPlaceHeart(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos)
                && !level.isOutsideBuildHeight(pos)
                && level.getBlockState(pos).isAir()
                && isValidSupport(level, LatexInfestationSavedData.get(level), pos.below());
    }

    static boolean canPlaceHeartReplacingSoftBlock(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos)
                && !level.isOutsideBuildHeight(pos)
                && isSoftHeartTarget(level.getBlockState(pos))
                && isValidSupport(level, LatexInfestationSavedData.get(level), pos.below());
    }

    static boolean canForceHeartOnExistingSupport(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos) || level.isOutsideBuildHeight(pos) || level.isOutsideBuildHeight(pos.below())) {
            return false;
        }

        return isSoftHeartTarget(level.getBlockState(pos)) && isSolidGround(level, pos.below());
    }

    static void clearSoftHeartTarget(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (isReplaceableFoliage(state)) {
            removeFoliage(level, pos, state);
        }
    }

    private static boolean isSoftHeartTarget(BlockState state) {
        return state.getFluidState().isEmpty() && (state.isAir() || isReplaceableFoliage(state));
    }

    static boolean isValidCoverTarget(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, BlockPos pos, boolean canClaimNewPositions) {
        if (!level.isLoaded(pos) || level.isOutsideBuildHeight(pos) || pos.equals(heart.pos())) {
            return false;
        }

        Optional<UUID> claim = data.claimedBy(pos);
        if (claim.isEmpty() && !canClaimNewPositions) {
            return false;
        }
        if (claim.isPresent() && !canUseExistingClaim(data, heart, claim.get(), canClaimNewPositions)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        LatexCoverState coverState = LatexCoverState.getAt(level, pos);
        LatexCoverState refreshed = coverStateFor(level, pos, heart.kind());
        if (refreshed.isAir()) {
            return false;
        }
        if (claim.filter(heart.id()::equals).isPresent() && !pos.equals(heart.pos()) && !isOwnedCover(level, data, heart, pos)) {
            return false;
        }
        if (claim.filter(heart.id()::equals).isPresent() && coverState.is(LatexInfestationBlocks.latexState(heart.kind())) && refreshed == coverState) {
            return false;
        }
        return isOpenForCover(state)
                && (coverState.isAir() || coverState.is(LatexInfestationBlocks.latexState(heart.kind())))
                && !refreshed.isAir();
    }

    private static boolean canUseExistingClaim(LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, UUID owner, boolean canClaimNewPositions) {
        if (owner.equals(heart.id())) {
            return true;
        }
        return canClaimNewPositions && data.heart(owner).filter(LatexInfestationSavedData.HeartRecord::alive).isEmpty();
    }

    static boolean isValidSupport(ServerLevel level, LatexInfestationSavedData data, BlockPos supportPos) {
        if (!level.isLoaded(supportPos) || level.isOutsideBuildHeight(supportPos) || data.claimedBy(supportPos).isPresent()) {
            return false;
        }
        BlockState support = level.getBlockState(supportPos);
        return isSolidGround(level, supportPos)
                && isSpreadableSupport(support)
                && !isLatexBlock(support)
                && LatexCoverState.getAt(level, supportPos).isAir();
    }

    static LatexCoverState coverStateFor(ServerLevel level, BlockPos pos, LatexHeartBlock.Kind kind) {
        LatexCoverState current = LatexCoverState.getAt(level, pos);
        SpreadingLatexType latex = LatexInfestationBlocks.latexState(kind);
        LatexCoverState state = current.is(latex) ? current : latex.defaultCoverState();
        BlockState sourceState = level.getBlockState(pos);

        for (Direction direction : Direction.values()) {
            BlockPos surfacePos = pos.relative(direction);
            BlockState surfaceState = level.getBlockState(surfacePos);
            boolean canCoverFace = isSpreadableSurface(level, surfacePos, surfaceState, direction.getOpposite())
                    && !sourceState.isFaceSturdy(level, pos, direction, SupportType.FULL);
            state = state.setValue(SpreadingLatexType.FACES.get(direction), canCoverFace);
        }

        return hasAnyFace(state) ? state : ChangedLatexTypes.NONE.get().defaultCoverState();
    }

    private static boolean hasAnyFace(LatexCoverState state) {
        return SpreadingLatexType.FACES.values().stream().anyMatch(state::getValue);
    }

    static boolean isOwnedCover(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, BlockPos pos) {
        if (!data.claimedBy(pos).filter(heart.id()::equals).isPresent()) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (state.is(LatexInfestationBlocks.sourceBlock(heart.kind()))) {
            return true;
        }

        return isOpenForCover(state)
                && isCompleteCover(level, pos, heart.kind());
    }

    static boolean isCompleteCover(ServerLevel level, BlockPos pos, LatexHeartBlock.Kind kind) {
        LatexCoverState current = LatexCoverState.getAt(level, pos);
        return current.is(LatexInfestationBlocks.latexState(kind))
                && sameCoverFaces(current, coverStateFor(level, pos, kind));
    }

    static boolean hasFewerCoverFaces(LatexCoverState oldState, LatexCoverState newState) {
        for (Direction direction : Direction.values()) {
            if (coverFace(oldState, direction) && !coverFace(newState, direction)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameCoverFaces(LatexCoverState first, LatexCoverState second) {
        for (Direction direction : Direction.values()) {
            if (coverFace(first, direction) != coverFace(second, direction)) {
                return false;
            }
        }
        return true;
    }

    private static boolean coverFace(LatexCoverState state, Direction direction) {
        var property = SpreadingLatexType.FACES.get(direction);
        return state.getProperties().contains(property) && state.getValue(property);
    }

    static BlockPos randomOwnedCover(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart) {
        List<BlockPos> claims = LatexInfestationUtil.randomSample(data.claimPositionList(heart.id()), 16, level.random);
        for (BlockPos pos : claims) {
            if (isOwnedCover(level, data, heart, pos)) {
                return pos;
            }
        }
        return null;
    }

    static boolean isOpenForCover(BlockState state) {
        return state.getFluidState().isEmpty() && (state.isAir() || isReplaceableFoliage(state) || becomesLatexBlockWhenCovered(state));
    }

    static boolean becomesLatexBlockWhenCovered(BlockState state) {
        return state.is(BlockTags.LEAVES);
    }

    static boolean isReplaceableFoliage(BlockState state) {
        return state.is(BlockTags.REPLACEABLE)
                || state.is(Blocks.SNOW)
                || state.is(Blocks.GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.DEAD_BUSH)
                || state.is(BlockTags.SMALL_FLOWERS)
                || state.is(BlockTags.TALL_FLOWERS)
                || state.is(BlockTags.SAPLINGS);
    }

    static void removeFoliage(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getProperties().contains(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            BlockPos otherHalf = half == DoubleBlockHalf.UPPER ? pos.below() : pos.above();
            BlockState otherState = level.getBlockState(otherHalf);
            if (otherState.is(state.getBlock())) {
                level.setBlockAndUpdate(otherHalf, Blocks.AIR.defaultBlockState());
            }
        }
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    private static boolean isSpreadableSurface(ServerLevel level, BlockPos pos, BlockState state, Direction face) {
        return level.isLoaded(pos)
                && !level.isOutsideBuildHeight(pos)
                && canSupportCoverFace(level, pos, state, face)
                && isSpreadableSupport(state)
                && !isLatexBlock(state);
    }

    private static boolean isSolidGround(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && (state.isFaceSturdy(level, pos, Direction.UP) || state.is(Blocks.DIRT_PATH));
    }

    private static boolean canSupportCoverFace(ServerLevel level, BlockPos pos, BlockState state, Direction face) {
        return state.isFaceSturdy(level, pos, face, SupportType.FULL)
                || state.is(BlockTags.LEAVES)
                || (face == Direction.UP && state.is(Blocks.DIRT_PATH));
    }

    static boolean isLatexBlock(BlockState state) {
        Block block = state.getBlock();
        return block instanceof LatexHeartBlock
                || block instanceof LatexNodeBlock
                || state.is(ChangedBlocks.DARK_LATEX_BLOCK.get())
                || state.is(ChangedBlocks.WHITE_LATEX_BLOCK.get())
                || state.is(ChangedBlocks.DARK_LATEX_FLUID.get())
                || state.is(ChangedBlocks.WHITE_LATEX_FLUID.get());
    }

    private static boolean isSpreadableSupport(BlockState state) {
        return !state.is(Blocks.BEDROCK)
                && !state.is(Blocks.BARRIER)
                && !state.is(Blocks.COMMAND_BLOCK)
                && !state.is(Blocks.CHAIN_COMMAND_BLOCK)
                && !state.is(Blocks.REPEATING_COMMAND_BLOCK)
                && !state.is(ChangedTags.Blocks.DENY_LATEX_COVER)
                && !state.hasBlockEntity()
                && state.getFluidState().isEmpty();
    }

    static void wakeNearbyCover(ServerLevel level, BlockPos changedPos) {
        LatexInfestationSavedData data = LatexInfestationSavedData.get(level);
        for (BlockPos pos : BlockPos.betweenClosed(changedPos.offset(-2, -1, -2), changedPos.offset(2, 1, 2))) {
            data.claimedBy(pos).ifPresent(heartId -> data.wakeCover(pos.immutable()));
        }
    }

    static void normalizeSupport(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos supportPos = pos.relative(direction);
            if (level.getBlockState(supportPos).is(Blocks.DIRT_PATH)) {
                level.setBlockAndUpdate(supportPos, Blocks.DIRT.defaultBlockState());
            }
        }
    }

    static void maybePlaySpreadSound(ServerLevel level, BlockPos pos) {
        if (level.random.nextInt(8) == 0) {
            level.playSound(null, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 0.18F, 0.75F + level.random.nextFloat() * 0.35F);
        }
    }
}
