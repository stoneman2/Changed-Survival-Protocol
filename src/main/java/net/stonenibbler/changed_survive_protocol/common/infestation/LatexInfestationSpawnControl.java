package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.ltxprogrammer.changed.init.ChangedTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.stonenibbler.changed_survive_protocol.common.gamerule.CSPGameRules;

final class LatexInfestationSpawnControl {
    private static final int CHANGED_SPAWN_BLOCK_CHECK_DEPTH = 3;

    private LatexInfestationSpawnControl() {
    }

    static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL || !event.getEntityType().is(ChangedTags.EntityTypes.LATEX) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!level.getGameRules().getBoolean(CSPGameRules.LATEX_HEART_SUPPRESS_NATURAL_CHANGED_SPAWNS)) {
            return;
        }

        LatexInfestationSavedData data = LatexInfestationSavedData.get(level);
        BlockPos pos = event.getPos();
        for (int i = 0; i <= CHANGED_SPAWN_BLOCK_CHECK_DEPTH; i++) {
            if (data.claimedBy(pos.below(i)).isPresent()) {
                event.setResult(Event.Result.DENY);
                return;
            }
        }
    }
}
