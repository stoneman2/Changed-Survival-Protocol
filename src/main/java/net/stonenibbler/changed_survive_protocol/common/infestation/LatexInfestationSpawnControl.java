package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.ltxprogrammer.changed.init.ChangedTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.stonenibbler.changed_survive_protocol.common.config.CSPConfig;
import net.stonenibbler.changed_survive_protocol.common.gamerule.CSPGameRules;

final class LatexInfestationSpawnControl {
    private static final int CHANGED_SPAWN_BLOCK_CHECK_DEPTH = 3;

    private LatexInfestationSpawnControl() {
    }

    static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL || !event.getEntityType().is(ChangedTags.EntityTypes.LATEX) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.getGameRules().getBoolean(CSPGameRules.LATEX_HEART_SUPPRESS_NATURAL_CHANGED_SPAWNS) && isInsideManagedInfestation(level, event.getPos())) {
            event.setResult(Event.Result.DENY);
            return;
        }

        if (CSPConfig.COMMON.changedLatexMobsIgnoreNaturalSpawnLight.get() && event.getResult() != Event.Result.DENY && !event.getDefaultResult() && canSpawnIgnoringLight(event, level)) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    private static boolean isInsideManagedInfestation(ServerLevel level, BlockPos pos) {
        LatexInfestationSavedData data = LatexInfestationSavedData.get(level);
        for (int i = 0; i <= CHANGED_SPAWN_BLOCK_CHECK_DEPTH; i++) {
            if (data.claimedBy(pos.below(i)).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static boolean canSpawnIgnoringLight(MobSpawnEvent.SpawnPlacementCheck event, ServerLevel level) {
        BlockPos pos = event.getPos();
        if (pos.getY() < level.getSeaLevel() - 10 || level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (!canSpawnOnChangedLatexGround(level, pos)) {
            return false;
        }

        return checkMobSpawnRules(event, level);
    }

    private static boolean canSpawnOnChangedLatexGround(ServerLevel level, BlockPos pos) {
        BlockPos checkPos = pos;
        for (int i = 0; i <= CHANGED_SPAWN_BLOCK_CHECK_DEPTH; i++) {
            BlockState state = level.getBlockState(checkPos);
            if (state.is(ChangedTags.Blocks.LATEX_SPAWNABLE_ON)) {
                return true;
            }
            if (!state.isAir() && state.isCollisionShapeFullBlock(level, checkPos)) {
                return false;
            }
            checkPos = checkPos.below();
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean checkMobSpawnRules(MobSpawnEvent.SpawnPlacementCheck event, ServerLevel level) {
        return Mob.checkMobSpawnRules((EntityType)event.getEntityType(), level, event.getSpawnType(), event.getPos(), event.getRandom());
    }
}
