package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.ltxprogrammer.changed.entity.latex.SpreadingLatexType;
import net.ltxprogrammer.changed.world.LatexCoverState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

final class PlayerSecretions {
    private static final int DECAY_CHECK_LIMIT = 64;

    private PlayerSecretions() {
    }

    static void add(ServerLevel level, BlockPos pos) {
        LatexInfestationSavedData data = LatexInfestationSavedData.get(level);
        if (data.claimedBy(pos).isPresent()) {
            return;
        }
        data.addPlayerSecretion(pos, Long.MAX_VALUE);
    }

    static boolean contains(ServerLevel level, BlockPos pos) {
        return LatexInfestationSavedData.get(level).isPlayerSecretion(pos);
    }

    static void tick(ServerLevel level, LatexInfestationSavedData data) {
        List<LatexInfestationSavedData.PlayerSecretionRecord> secretions = data.playerSecretions(DECAY_CHECK_LIMIT, level.random);
        if (secretions.isEmpty()) {
            return;
        }

        for (LatexInfestationSavedData.PlayerSecretionRecord secretion : secretions) {
            BlockPos pos = secretion.pos();
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (data.claimedBy(pos).isPresent()) {
                data.removePlayerSecretion(pos);
                continue;
            }

            LatexCoverState cover = LatexCoverState.getAt(level, pos);
            if (!isSecretedCover(cover)) {
                data.removePlayerSecretion(pos);
            }
        }
    }

    private static boolean isSecretedCover(LatexCoverState cover) {
        return !cover.isAir() && cover.getType() instanceof SpreadingLatexType;
    }
}
