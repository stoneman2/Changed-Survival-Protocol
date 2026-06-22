package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.stonenibbler.changed_survive_protocol.common.gamerule.CSPGameRules;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

final class LatexHeartNodes {
    private static final int NODE_SPAWN_INTERVAL = 2400;
    private static final int NODE_REMOVED_COOLDOWN = 6000;
    private static final int NODE_MAX_Y_DISTANCE = 6;

    private LatexHeartNodes() {
    }

    static void removeNode(ServerLevel level, BlockPos pos) {
        LatexInfestationSavedData data = LatexInfestationSavedData.get(level);
        long gameTime = level.getGameTime();
        Optional<LatexInfestationSavedData.HeartRecord> heart = data.nodeOwner(pos).flatMap(data::heart);
        heart.ifPresent(record -> data.updateHeart(record.withNextNodeTick(Math.max(record.nextNodeTick(), gameTime + NODE_SPAWN_INTERVAL))));
        data.removeNode(pos, gameTime + NODE_REMOVED_COOLDOWN);
        heart.ifPresent(record -> updateHeartProtection(level, data, record));
        heart.ifPresent(record -> {
            if (activeNodes(level, data, record).isEmpty()) {
                LatexHeartSignaling.vulnerableFeedback(level, record);
            }
        });
        LatexCoverRules.wakeNearbyCover(level, pos);
    }

    static void maybePlaceNode(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, int claimCount) {
        int maxNodes = level.getGameRules().getInt(CSPGameRules.LATEX_HEART_MAX_NODES);
        int maxDistance = level.getGameRules().getInt(CSPGameRules.LATEX_HEART_MAX_NODE_DISTANCE);
        double maxDistanceSqr = maxDistance <= 0 ? Double.MAX_VALUE : (double)maxDistance * maxDistance;
        long gameTime = level.getGameTime();
        if (maxNodes <= 0 || data.nodeCount(heart.id()) >= maxNodes || claimCount < 36 || gameTime < heart.nextNodeTick()) {
            return;
        }
        for (int i = 0; i < 12; i++) {
            BlockPos pos = LatexCoverRules.randomOwnedCover(level, data, heart);
            if (pos == null || pos.equals(heart.pos()) || Math.abs(pos.getY() - heart.pos().getY()) > NODE_MAX_Y_DISTANCE || pos.distSqr(heart.pos()) < 9.0D || pos.distSqr(heart.pos()) > maxDistanceSqr || data.isNodeOnCooldown(pos, gameTime) || !LatexHeartDecorations.canPlaceDecorationAt(level, pos)) {
                continue;
            }
            level.setBlockAndUpdate(pos, LatexInfestationBlocks.nodeBlock(heart.kind()).defaultBlockState());
            data.addNode(pos, heart.id());
            data.updateHeart(heart.withNextNodeTick(gameTime + NODE_SPAWN_INTERVAL));
            updateHeartProtection(level, data, heart);
            return;
        }
    }

    static List<BlockPos> activeNodes(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart) {
        return data.nodePositions(heart.id()).stream()
                .filter(pos -> level.getBlockState(pos).getBlock() instanceof LatexNodeBlock)
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(heart.pos())))
                .toList();
    }

    static boolean hasActiveNodes(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart) {
        for (BlockPos pos : data.nodePositions(heart.id())) {
            if (level.getBlockState(pos).getBlock() instanceof LatexNodeBlock) {
                return true;
            }
        }
        return false;
    }

    static void updateHeartProtection(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart) {
        if (!level.isLoaded(heart.pos())) {
            return;
        }

        BlockState state = level.getBlockState(heart.pos());
        if (!(state.getBlock() instanceof LatexHeartBlock) || !state.getProperties().contains(LatexHeartBlock.PROTECTED)) {
            return;
        }

        boolean protectedByNodes = hasActiveNodes(level, data, heart);
        if (state.getValue(LatexHeartBlock.PROTECTED) != protectedByNodes) {
            level.setBlock(heart.pos(), state.setValue(LatexHeartBlock.PROTECTED, protectedByNodes), 3);
        }
    }
}
