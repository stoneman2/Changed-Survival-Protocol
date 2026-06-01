package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.ltxprogrammer.changed.init.ChangedParticles;
import net.ltxprogrammer.changed.util.Color3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

final class LatexHeartSignaling {
    private static final int PASSIVE_INTERVAL = 20;
    private static final double PLAYER_SIGNAL_DISTANCE_SQR = 64.0D * 64.0D;
    private static final double LOCATOR_SPACING = 0.45D;

    private LatexHeartSignaling() {
    }

    static void tick(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, long gameTime) {
        if (Math.floorMod(gameTime + heart.id().getLeastSignificantBits(), PASSIVE_INTERVAL) != 0) {
            return;
        }

        List<BlockPos> nodes = LatexHeartNodes.activeNodes(level, data, heart);
        if (nodes.isEmpty()) {
            vulnerableBurst(level, heart, false);
            return;
        }

        if (hasNearbyPlayer(level, heart.pos())) {
            nodeLocator(level, heart, nodes.get(0), false);
        }
    }

    static void protectedBreakFeedback(ServerLevel level, Player player, LatexInfestationSavedData.HeartRecord heart, List<BlockPos> nodes) {
        player.displayClientMessage(Component.translatable("message.changed_survive_protocol.latex_heart.protected", nodes.size()), true);
        if (!nodes.isEmpty()) {
            nodeLocator(level, heart, nearestNodeTo(heart.pos(), nodes), true);
        }
    }

    static void vulnerableFeedback(ServerLevel level, LatexInfestationSavedData.HeartRecord heart) {
        vulnerableBurst(level, heart, true);
        for (ServerPlayer player : level.players()) {
            if (canReceiveSignal(player, heart.pos())) {
                player.displayClientMessage(Component.translatable("message.changed_survive_protocol.latex_heart.vulnerable"), true);
            }
        }
    }

    private static void nodeLocator(ServerLevel level, LatexInfestationSavedData.HeartRecord heart, BlockPos node, boolean strong) {
        Vec3 start = signalStart(level, heart.pos(), node);
        Vec3 target = Vec3.atCenterOf(node).subtract(start);
        if (target.lengthSqr() < 0.0001D) {
            return;
        }

        Vec3 direction = target.normalize();
        ParticleOptions particle = gooParticle(heart.kind());
        int steps = strong ? 18 : 10;
        Vec3 lastOpen = start;
        for (int i = 0; i < steps; i++) {
            Vec3 point = start.add(direction.scale(i * LOCATOR_SPACING));
            if (!isOpen(level, point)) {
                locatorTip(level, particle, lastOpen, direction, strong, true);
                return;
            }

            level.sendParticles(particle, point.x, point.y, point.z, strong ? 2 : 1, 0.025D, 0.025D, 0.025D, 0.004D);
            lastOpen = point;
        }

        locatorTip(level, particle, lastOpen, direction, strong, false);
    }

    private static BlockPos nearestNodeTo(BlockPos origin, List<BlockPos> nodes) {
        BlockPos nearest = nodes.get(0);
        double nearestDistance = nearest.distSqr(origin);
        for (int i = 1; i < nodes.size(); i++) {
            BlockPos node = nodes.get(i);
            double distance = node.distSqr(origin);
            if (distance < nearestDistance) {
                nearest = node;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static Vec3 signalStart(ServerLevel level, BlockPos heart, BlockPos node) {
        Vec3 center = Vec3.atCenterOf(heart);
        Vec3 horizontal = horizontalDirection(center, Vec3.atCenterOf(node));
        Vec3 side = new Vec3(-horizontal.z, 0.0D, horizontal.x);

        Vec3[] candidates = new Vec3[] {
                center.add(0.0D, 1.25D, 0.0D),
                center.add(0.0D, 1.75D, 0.0D),
                center.add(horizontal.scale(-0.55D)).add(0.0D, 1.15D, 0.0D),
                center.add(side.scale(0.55D)).add(0.0D, 1.15D, 0.0D),
                center.add(side.scale(-0.55D)).add(0.0D, 1.15D, 0.0D),
                center.add(0.0D, 0.75D, 0.0D)
        };

        for (Vec3 candidate : candidates) {
            if (isOpen(level, candidate)) {
                return candidate;
            }
        }
        return center.add(0.0D, 1.25D, 0.0D);
    }

    private static Vec3 horizontalDirection(Vec3 from, Vec3 to) {
        Vec3 direction = new Vec3(to.x - from.x, 0.0D, to.z - from.z);
        return direction.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
    }

    private static void locatorTip(ServerLevel level, ParticleOptions particle, Vec3 tip, Vec3 direction, boolean strong, boolean hitWall) {
        Vec3 side = new Vec3(-direction.z, 0.0D, direction.x);
        if (side.lengthSqr() < 0.0001D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }

        int count = hitWall ? (strong ? 14 : 8) : (strong ? 9 : 5);
        double spread = hitWall ? 0.16D : 0.09D;
        level.sendParticles(particle, tip.x, tip.y, tip.z, count, spread, spread, spread, 0.012D);

        Vec3 left = tip.subtract(direction.scale(0.35D)).add(side.scale(0.22D));
        Vec3 right = tip.subtract(direction.scale(0.35D)).subtract(side.scale(0.22D));
        level.sendParticles(particle, left.x, left.y, left.z, strong ? 4 : 2, 0.025D, 0.025D, 0.025D, 0.004D);
        level.sendParticles(particle, right.x, right.y, right.z, strong ? 4 : 2, 0.025D, 0.025D, 0.025D, 0.004D);
    }

    private static boolean isOpen(ServerLevel level, Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos.x, pos.y, pos.z);
        return level.getBlockState(blockPos).getCollisionShape(level, blockPos).isEmpty();
    }

    private static void vulnerableBurst(ServerLevel level, LatexInfestationSavedData.HeartRecord heart, boolean strong) {
        ParticleOptions particle = gooParticle(heart.kind());
        int count = strong ? 18 : 6;
        BlockPos pos = heart.pos();
        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.35D + level.random.nextDouble() * 0.95D;
            double x = pos.getX() + 0.5D + Math.cos(angle) * radius;
            double y = pos.getY() + 0.45D + level.random.nextDouble() * 1.05D;
            double z = pos.getZ() + 0.5D + Math.sin(angle) * radius;
            level.sendParticles(particle, x, y, z, 1, 0.04D, 0.05D, 0.04D, 0.015D);
        }
    }

    private static boolean canReceiveSignal(ServerPlayer player, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        return !player.isSpectator() && player.distanceToSqr(x, y, z) <= PLAYER_SIGNAL_DISTANCE_SQR;
    }

    private static boolean hasNearbyPlayer(ServerLevel level, BlockPos pos) {
        for (ServerPlayer player : level.players()) {
            if (canReceiveSignal(player, pos)) {
                return true;
            }
        }
        return false;
    }

    private static ParticleOptions gooParticle(LatexHeartBlock.Kind kind) {
        return ChangedParticles.drippingLatex(kind == LatexHeartBlock.Kind.DARK ? Color3.BLACK : Color3.WHITE);
    }
}
