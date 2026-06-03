package net.stonenibbler.changed_survive_protocol.common.ai;

import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.ability.GrabEntityAbilityInstance;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.TamableLatexEntity;
import net.ltxprogrammer.changed.init.ChangedAbilities;
import net.ltxprogrammer.changed.init.ChangedSounds;
import net.ltxprogrammer.changed.network.packet.GrabEntityPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.stonenibbler.changed_survive_protocol.ChangedSurviveProtocol;
import net.stonenibbler.changed_survive_protocol.common.config.CSPConfig;

import java.util.EnumSet;
import java.util.UUID;

public class SoloHunterGoal extends Goal {
    private static final double EPSILON = 1.0E-5D;
    private static final int FAKE_RETREAT_RECOMMIT_TICKS = 12;

    private final ChangedEntity mob;
    private HunterState state = HunterState.IDLE_OR_NORMAL;
    private int stateTicks;
    private int stateMaxTicks;
    private UUID currentTargetUuid;
    private int targetLockTicks;
    private Vec3 lastSeenTargetPosition;
    private Vec3 lastSeenTargetVelocity = Vec3.ZERO;
    private int lastSeenTargetTicks = 9999;
    private Vec3 currentMoveTarget;
    private int realRetreatsUsed;
    private int maxRealRetreats;
    private int fakeRetreatCooldownTicks;
    private int fakeRetreatRollCooldownTicks;
    private int realRetreatCooldownTicks;
    private int behindGrabCooldownTicks;
    private int attackCooldownTicks;
    private int recoverTicks;
    private int pathRecalculateCooldownTicks;
    private int lineOfSightCooldownTicks;
    private int watchedTicks;
    private int notWatchedTicks;
    private int behindTicks;
    private int failedPathAttempts;
    private boolean desperate;
    private UUID retreatIgnoredTargetUuid;
    private int retreatIgnoreUntilTick;
    private int recentlyHitTicks;
    private float lastHealthValue;
    private final double circleBias;
    private final double aggressionBias;
    private final double fakeRetreatBias;
    private int interceptCooldownTicks;
    private int circleDirection;
    private int weaveDirection;
    private int weaveDirectionTicks;
    private boolean cachedCanSeeTarget;
    private boolean cachedTargetCanSeeMob;
    private int nextStartCheckTick;
    private int lastHurtReactionTimestamp = -1;
    private Vec3 failedMoveTarget;
    private int failedMoveTargetTicks;
    private String nextStateReason = "initial";

    public SoloHunterGoal(ChangedEntity mob) {
        this.mob = mob;
        this.lastHealthValue = mob.getHealth();
        this.maxRealRetreats = CSPConfig.COMMON.maxRealRetreats.get();
        this.circleBias = 0.75D + mob.getRandom().nextDouble() * 0.7D;
        this.aggressionBias = 0.75D + mob.getRandom().nextDouble() * 0.7D;
        this.fakeRetreatBias = 0.75D + mob.getRandom().nextDouble() * 0.7D;
        this.circleDirection = mob.getRandom().nextBoolean() ? 1 : -1;
        this.weaveDirection = circleDirection;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!isRuntimeEnabled() || mob.tickCount < nextStartCheckTick) {
            return false;
        }

        nextStartCheckTick = mob.tickCount + CSPConfig.COMMON.hunterAITickRate.get();
        Player target = asValidPlayer(mob.getTarget());
        if (target == null) {
            resetTargetMemory();
            return false;
        }
        if (isRetreatIgnoredTarget(target)) {
            mob.setTarget(null);
            resetTargetMemory();
            return false;
        }

        tickDamageMemory();
        if (!isTargetAllowedToActivate(target)) {
            return false;
        }

        lockTarget(target);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!isRuntimeEnabled()) {
            return false;
        }

        Player target = getLockedOrCurrentTarget();
        if (target == null) {
            resetTargetMemory();
            return false;
        }

        syncMobTarget(target);
        return true;
    }

    @Override
    public void start() {
        this.stateTicks = 0;
        this.pathRecalculateCooldownTicks = 0;
    }

    @Override
    public void stop() {
        if (lastSeenTargetTicks > CSPConfig.COMMON.hunterMemoryAfterLostSightTicks.get()) {
            mob.setTarget(null);
            resetTargetMemory();
        }
        mob.getNavigation().stop();
        currentMoveTarget = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Player target = getLockedOrCurrentTarget();
        if (target == null) {
            resetTargetMemory();
            mob.setTarget(null);
            mob.getNavigation().stop();
            return;
        }

        syncMobTarget(target);
        tickTimers();
        if (hasActiveGrab(target)) {
            mob.getNavigation().stop();
            return;
        }
        Situation situation = readSituation(target);

        if (!situation.validWithMemory) {
            mob.setTarget(null);
            resetTargetMemory();
            mob.getNavigation().stop();
            return;
        }

        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        HunterState nextState = chooseState(situation);
        setState(nextState, situation);
        runState(situation);
        debugTick(situation);
        stateTicks++;
    }

    private boolean isRuntimeEnabled() {
        return CSPConfig.COMMON.enableHunterAI.get()
                && CSPConfig.COMMON.hunterAITargetsOnlyPlayers.get()
                && !mob.level().isClientSide
                && (!(mob instanceof TamableLatexEntity tamable) || !tamable.isTame())
                && (!CSPConfig.COMMON.hunterAIRequiresEntityTag.get() || mob.getType().is(CSPEntityTypeTags.HUNTER_AI_LATEX_BEASTS));
    }

    private Player asValidPlayer(LivingEntity entity) {
        if (entity instanceof Player player && isBasicValidTarget(player)) {
            return player;
        }
        return null;
    }

    private boolean isBasicValidTarget(Player player) {
        return player.isAlive()
                && !player.isSpectator()
                && !player.isCreative()
                && player.level() == mob.level()
                && mob.distanceToSqr(player) <= Mth.square(CSPConfig.COMMON.hunterImprovedSightRange.get());
    }

    private boolean isTargetAllowedToActivate(Player target) {
        if (currentTargetUuid != null && currentTargetUuid.equals(target.getUUID()) && hasFreshMemoryOrValidTrigger(target)) {
            return true;
        }

        return isRecentlyHurtBy(target) || canSeeTargetNow(target);
    }

    private boolean hasFreshMemoryOrValidTrigger(Player target) {
        return lastSeenTargetTicks <= CSPConfig.COMMON.hunterMemoryAfterLostSightTicks.get() || isRecentlyHurtBy(target) || canSeeTargetNow(target);
    }

    private boolean canSeeTargetNow(Player target) {
        return mob.getSensing().hasLineOfSight(target);
    }

    private boolean isRecentlyHurtBy(Player target) {
        LivingEntity attacker = mob.getLastHurtByMob();
        return attacker == target && mob.tickCount - mob.getLastHurtByMobTimestamp() <= 80;
    }

    private void lockTarget(Player target) {
        if (!target.getUUID().equals(currentTargetUuid)) {
            currentTargetUuid = target.getUUID();
            targetLockTicks = CSPConfig.COMMON.hunterTargetLockTicks.get();
            lastSeenTargetTicks = CSPConfig.COMMON.hunterMemoryAfterLostSightTicks.get() + 1;
            lastSeenTargetPosition = null;
            currentMoveTarget = null;
            setState(HunterState.IDLE_OR_NORMAL, null, "new_target");
        }
    }

    private Player getLockedOrCurrentTarget() {
        Player current = asValidPlayer(mob.getTarget());
        Player locked = findLockedTarget();
        if (current != null && isRetreatIgnoredTarget(current)) {
            mob.setTarget(null);
            current = null;
        }
        if (locked != null && isRetreatIgnoredTarget(locked)) {
            locked = null;
        }

        if (currentTargetUuid == null) {
            if (current == null) {
                return null;
            }
            if (!isTargetAllowedToActivate(current)) {
                return null;
            }
            lockTarget(current);
            return current;
        }

        if (current != null && current.getUUID().equals(currentTargetUuid)) {
            return current;
        }

        if (current == null) {
            if (locked != null && isBasicValidTarget(locked) && hasFreshMemoryOrValidTrigger(locked)) {
                return locked;
            }
            return null;
        }

        if (targetLockTicks > 0 && locked != null && isBasicValidTarget(locked) && hasFreshMemoryOrValidTrigger(locked)) {
            return locked;
        }

        if (!isTargetAllowedToActivate(current)) {
            return locked != null && isBasicValidTarget(locked) && hasFreshMemoryOrValidTrigger(locked) ? locked : null;
        }

        lockTarget(current);
        return current;
    }

    private Player findLockedTarget() {
        if (currentTargetUuid == null || !(mob.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getPlayerByUUID(currentTargetUuid);
    }

    private void tickTimers() {
        tickDamageMemory();
        targetLockTicks = Math.max(0, targetLockTicks - 1);
        fakeRetreatCooldownTicks = Math.max(0, fakeRetreatCooldownTicks - 1);
        fakeRetreatRollCooldownTicks = Math.max(0, fakeRetreatRollCooldownTicks - 1);
        realRetreatCooldownTicks = Math.max(0, realRetreatCooldownTicks - 1);
        behindGrabCooldownTicks = Math.max(0, behindGrabCooldownTicks - 1);
        attackCooldownTicks = Math.max(0, attackCooldownTicks - 1);
        recoverTicks = Math.max(0, recoverTicks - 1);
        pathRecalculateCooldownTicks = Math.max(0, pathRecalculateCooldownTicks - 1);
        lineOfSightCooldownTicks = Math.max(0, lineOfSightCooldownTicks - 1);
        interceptCooldownTicks = Math.max(0, interceptCooldownTicks - 1);
        weaveDirectionTicks = Math.max(0, weaveDirectionTicks - 1);
        recentlyHitTicks = Math.max(0, recentlyHitTicks - 1);
        failedMoveTargetTicks = Math.max(0, failedMoveTargetTicks - 1);
        if (failedMoveTargetTicks <= 0) {
            failedMoveTarget = null;
        }
        if (!cachedCanSeeTarget) {
            lastSeenTargetTicks++;
        }
    }

    private void tickDamageMemory() {
        float health = mob.getHealth();
        if (health + 0.05F < lastHealthValue) {
            recentlyHitTicks = 80;
        }
        lastHealthValue = health;
    }

    private Situation readSituation(Player target) {
        Vec3 targetLook = horizontal(target.getViewVector(1.0F));
        if (targetLook.lengthSqr() < EPSILON) {
            targetLook = horizontal(target.getLookAngle());
        }
        targetLook = normalizeOrZero(targetLook);

        Vec3 toMob = horizontal(mob.position().subtract(target.position()));
        Vec3 toMobNormal = normalizeOrZero(toMob);
        double targetViewDot = targetLook.lengthSqr() < EPSILON || toMobNormal.lengthSqr() < EPSILON ? 0.0D : targetLook.dot(toMobNormal);

        if (lineOfSightCooldownTicks <= 0) {
            cachedCanSeeTarget = mob.getSensing().hasLineOfSight(target);
            boolean likelyWatched = targetViewDot >= CSPConfig.COMMON.watchedDotThreshold.get();
            cachedTargetCanSeeMob = !CSPConfig.COMMON.watchedLineOfSightRequired.get() || (likelyWatched && target.hasLineOfSight(mob));
            lineOfSightCooldownTicks = CSPConfig.COMMON.lineOfSightCheckIntervalTicks.get();
        }

        if (cachedCanSeeTarget) {
            lastSeenTargetPosition = target.position();
            lastSeenTargetTicks = 0;
        } else if (isRecentlyHurtBy(target) && lastHurtReactionTimestamp != mob.getLastHurtByMobTimestamp()) {
            lastSeenTargetPosition = target.position();
            lastSeenTargetTicks = 0;
            lastHurtReactionTimestamp = mob.getLastHurtByMobTimestamp();
        }

        boolean watched = targetViewDot >= CSPConfig.COMMON.watchedDotThreshold.get() && cachedTargetCanSeeMob;
        if (watched) {
            watchedTicks++;
            notWatchedTicks = 0;
        } else {
            notWatchedTicks++;
            watchedTicks = 0;
        }

        double distanceSqr = mob.distanceToSqr(target);
        double horizontalDistance = Math.sqrt(horizontal(mob.position().subtract(target.position())).lengthSqr());
        Vec3 targetVelocity = horizontal(target.getDeltaMovement());
        double targetSpeed = targetVelocity.length();
        if (cachedCanSeeTarget || isRecentlyHurtBy(target)) {
            lastSeenTargetVelocity = targetVelocity;
        }
        double healthPercent = mob.getHealth() / Math.max(1.0F, mob.getMaxHealth());
        boolean lowHealth = healthPercent <= CSPConfig.COMMON.lowHealthRetreatThreshold.get();
        boolean targetPressuring = targetSpeed >= 0.08D
                && targetVelocity.lengthSqr() > EPSILON
                && toMobNormal.lengthSqr() > EPSILON
                && normalizeOrZero(targetVelocity).dot(toMobNormal) > 0.35D;
        boolean frontCone = targetViewDot >= CSPConfig.COMMON.frontConeDotThreshold.get();
        boolean behind = targetViewDot <= CSPConfig.COMMON.behindDotThreshold.get();
        boolean side = !frontCone && !behind;
        boolean attackAngleGood = behind || side || !watched;
        boolean closeEnoughToAttack = distanceSqr <= getAttackReachSqr(target) * 1.12D;
        boolean validWithMemory = cachedCanSeeTarget || lastSeenTargetTicks <= CSPConfig.COMMON.hunterMemoryAfterLostSightTicks.get() || isRecentlyHurtBy(target);

        if (cachedCanSeeTarget
                && behind
                && !watched
                && horizontalDistance <= CSPConfig.COMMON.behindGrabMaxDistance.get() + 0.75D) {
            behindTicks++;
        } else {
            behindTicks = 0;
        }

        return new Situation(target, targetLook, targetVelocity, distanceSqr, horizontalDistance, targetSpeed,
                cachedCanSeeTarget, watched, frontCone, side, behind, attackAngleGood, closeEnoughToAttack,
                validWithMemory, healthPercent, lowHealth, recentlyHitTicks > 0, targetPressuring);
    }

    private HunterState chooseState(Situation situation) {
        nextStateReason = "fallback";

        if (state == HunterState.REAL_RETREAT && stateTicks < CSPConfig.COMMON.realRetreatDurationTicks.get()) {
            nextStateReason = "retreat_min_duration";
            return HunterState.REAL_RETREAT;
        }

        if (state == HunterState.FAKE_RETREAT && stateTicks < CSPConfig.COMMON.fakeRetreatDurationTicks.get()) {
            if (!situation.canSeeTarget && lastSeenTargetPosition != null) {
                nextStateReason = "fake_retreat_lost_sight";
                return HunterState.SEARCH_LAST_KNOWN;
            }
            nextStateReason = "fake_retreat_active";
            return HunterState.FAKE_RETREAT;
        }

        maxRealRetreats = CSPConfig.COMMON.maxRealRetreats.get();
        if (CSPConfig.COMMON.enableDesperationRush.get()
                && situation.lowHealth
                && realRetreatsUsed >= CSPConfig.COMMON.desperationAfterRetreats.get()) {
            desperate = true;
        }

        if (CSPConfig.COMMON.enableRealRetreat.get()
                && !desperate
                && situation.lowHealth
                && situation.recentlyHit
                && realRetreatsUsed < maxRealRetreats
                && realRetreatCooldownTicks <= 0) {
            nextStateReason = "low_health_recent_damage";
            return HunterState.REAL_RETREAT;
        }

        if (recoverTicks > 0) {
            nextStateReason = "recover_cooldown";
            return HunterState.RECOVER;
        }

        if (!situation.canSeeTarget && lastSeenTargetPosition != null) {
            nextStateReason = "lost_sight_search_last_known";
            return HunterState.SEARCH_LAST_KNOWN;
        }

        if (desperate) {
            if (situation.canSeeTarget && situation.closeEnoughToAttack && attackCooldownTicks <= 0) {
                nextStateReason = "desperate_in_reach";
                return HunterState.ATTACK;
            }
            nextStateReason = "desperate_rush";
            return HunterState.DESPERATION_RUSH;
        }

        if (situation.canSeeTarget && situation.closeEnoughToAttack && attackCooldownTicks <= 0 && canAttackFromSituation(situation)) {
            nextStateReason = "angle_attack_ready";
            return HunterState.ATTACK;
        }

        if (situation.canSeeTarget && situation.behind && !situation.watched && situation.horizontalDistance <= 3.2D) {
            nextStateReason = "rear_angle_commit";
            return HunterState.REAR_COMMIT;
        }

        if (shouldFakeRetreat(situation)) {
            nextStateReason = "bait_fake_retreat";
            return HunterState.FAKE_RETREAT;
        }

        if (state == HunterState.CIRCLE_APPROACH && stateTicks < CSPConfig.COMMON.circleMinTicks.get()) {
            nextStateReason = "circle_min_duration";
            return HunterState.CIRCLE_APPROACH;
        }

        if (state == HunterState.CIRCLE_APPROACH && stateTicks < stateMaxTicks && situation.watched) {
            nextStateReason = "circle_target_still_watching";
            return HunterState.CIRCLE_APPROACH;
        }

        if (state == HunterState.INTERCEPT && stateTicks < stateMaxTicks && situation.canSeeTarget && situation.targetSpeed >= CSPConfig.COMMON.playerRunningSpeedThreshold.get()) {
            nextStateReason = "intercept_min_duration";
            return HunterState.INTERCEPT;
        }

        if (CSPConfig.COMMON.enableIntercept.get()
                && interceptCooldownTicks <= 0
                && situation.targetSpeed >= CSPConfig.COMMON.playerRunningSpeedThreshold.get()
                && situation.horizontalDistance >= 6.0D
                && situation.horizontalDistance <= 18.0D
                && !situation.watched) {
            nextStateReason = "running_target_intercept";
            return HunterState.INTERCEPT;
        }

        if (situation.watched) {
            nextStateReason = "watched_response";
            return CSPConfig.COMMON.enableCircleApproach.get() ? HunterState.CIRCLE_APPROACH : HunterState.FLANK;
        }

        if (CSPConfig.COMMON.enableFlank.get()) {
            nextStateReason = "default_flank";
            return HunterState.FLANK;
        }

        nextStateReason = "normal_ai_fallback";
        return HunterState.IDLE_OR_NORMAL;
    }

    private boolean shouldFakeRetreat(Situation situation) {
        if (!CSPConfig.COMMON.enableFakeRetreat.get()
                || fakeRetreatCooldownTicks > 0
                || fakeRetreatRollCooldownTicks > 0
                || situation.healthPercent < CSPConfig.COMMON.fakeRetreatMinHealthPercent.get()
                || situation.recentlyHit
                || situation.horizontalDistance < 3.5D
                || situation.horizontalDistance > 7.5D
                || !situation.canSeeTarget) {
            return false;
        }

        if (!situation.targetPressuring || !situation.frontCone) {
            return false;
        }

        fakeRetreatRollCooldownTicks = 20;
        double chance = CSPConfig.COMMON.fakeRetreatChance.get() * fakeRetreatBias;
        if (situation.horizontalDistance < 5.0D) {
            chance += 0.02D;
        }
        if (watchedTicks > 15) {
            chance += 0.02D;
        }
        return mob.getRandom().nextDouble() < Mth.clamp(chance, 0.0D, 0.20D);
    }

    private boolean canAttackFromSituation(Situation situation) {
        return !CSPConfig.COMMON.attackAngleRequirementEnabled.get() || situation.attackAngleGood;
    }

    private void setState(HunterState nextState, Situation situation) {
        setState(nextState, situation, nextStateReason);
    }

    private void setState(HunterState nextState, Situation situation, String reason) {
        if (state == nextState) {
            return;
        }

        HunterState oldState = state;
        state = nextState;
        stateTicks = 0;
        currentMoveTarget = null;
        pathRecalculateCooldownTicks = 0;

        switch (nextState) {
            case CIRCLE_APPROACH -> {
                int min = CSPConfig.COMMON.circleMinTicks.get();
                int max = Math.max(min, CSPConfig.COMMON.circleMaxTicks.get());
                stateMaxTicks = Mth.nextInt(mob.getRandom(), min, max);
                if (mob.getRandom().nextFloat() < 0.25F) {
                    circleDirection *= -1;
                }
                weaveDirection = circleDirection;
                weaveDirectionTicks = Mth.nextInt(mob.getRandom(), 30, 60);
            }
            case FLANK -> {
                weaveDirection = circleDirection;
                weaveDirectionTicks = Mth.nextInt(mob.getRandom(), 18, 42);
            }
            case REAL_RETREAT -> {
                realRetreatsUsed++;
                realRetreatCooldownTicks = CSPConfig.COMMON.realRetreatCooldownTicks.get();
                if (realRetreatsUsed >= maxRealRetreats) {
                    desperate = CSPConfig.COMMON.enableDesperationRush.get();
                }
            }
            case FAKE_RETREAT -> fakeRetreatCooldownTicks = CSPConfig.COMMON.fakeRetreatCooldownTicks.get();
            case ATTACK -> {
                attackCooldownTicks = CSPConfig.COMMON.attackCooldownTicks.get();
                recoverTicks = CSPConfig.COMMON.recoverTicks.get();
            }
            case INTERCEPT -> {
                interceptCooldownTicks = CSPConfig.COMMON.interceptCooldownTicks.get();
                stateMaxTicks = Math.max(12, CSPConfig.COMMON.pathCheckIntervalTicks.get() * 2);
            }
            default -> {
            }
        }

        debugStateChange(oldState, nextState, situation, reason);
    }

    private void runState(Situation situation) {
        switch (state) {
            case CIRCLE_APPROACH -> runCircle(situation);
            case FLANK -> runFlank(situation);
            case INTERCEPT -> runIntercept(situation);
            case FAKE_RETREAT -> runFakeRetreat(situation);
            case REAL_RETREAT -> runRealRetreat(situation);
            case DESPERATION_RUSH -> runDirectApproach(situation, CSPConfig.COMMON.desperationSpeedMultiplier.get());
            case ATTACK -> runAttack(situation);
            case RECOVER -> runRecover(situation);
            case SEARCH_LAST_KNOWN -> runSearchLastKnown(situation);
            case REAR_COMMIT -> runRearCommit(situation);
            case IDLE_OR_NORMAL -> runNormalFallback(situation);
        }
    }

    private void runNormalFallback(Situation situation) {
        if (CSPConfig.COMMON.enableFlank.get() && !desperate) {
            runFlank(situation);
            return;
        }

        runDirectApproach(situation, CSPConfig.COMMON.flankSpeedMultiplier.get() * aggressionBias);
    }

    private void runCircle(Situation situation) {
        if (tryStrafeGapClose(situation, CSPConfig.COMMON.circleSpeedMultiplier.get() * circleBias, true)) {
            return;
        }

        Vec3 target = situation.watched || situation.frontCone ? selectBlindsidePoint(situation, true) : selectCirclePoint(situation);
        moveTo(target, CSPConfig.COMMON.circleSpeedMultiplier.get() * circleBias);
    }

    private void runFlank(Situation situation) {
        if (tryStrafeGapClose(situation, CSPConfig.COMMON.flankSpeedMultiplier.get() * aggressionBias, false)) {
            return;
        }

        Vec3 target = situation.behind ? selectFlankPoint(situation) : selectBlindsidePoint(situation, false);
        moveTo(target, CSPConfig.COMMON.flankSpeedMultiplier.get() * aggressionBias);
    }

    private void runIntercept(Situation situation) {
        Vec3 target = selectInterceptPoint(situation);
        moveTo(target, CSPConfig.COMMON.interceptSpeedMultiplier.get() * aggressionBias);
    }

    private void runFakeRetreat(Situation situation) {
        if (stateTicks >= CSPConfig.COMMON.fakeRetreatDurationTicks.get() - FAKE_RETREAT_RECOMMIT_TICKS
                && (!situation.watched || situation.targetPressuring)) {
            Vec3 target = selectFlankPoint(situation);
            moveTo(target, CSPConfig.COMMON.flankSpeedMultiplier.get() * aggressionBias);
            return;
        }

        Vec3 target = selectRetreatPoint(situation, 4.0D, 6.0D);
        moveTo(target, CSPConfig.COMMON.fakeRetreatSpeedMultiplier.get());
    }

    private void runRealRetreat(Situation situation) {
        Vec3 target = selectRetreatPoint(situation, CSPConfig.COMMON.realRetreatDistanceMin.get(), CSPConfig.COMMON.realRetreatDistanceMax.get());
        moveTo(target, CSPConfig.COMMON.realRetreatSpeedMultiplier.get());
        if (situation.horizontalDistance >= CSPConfig.COMMON.realRetreatDropAggroDistance.get()
                || stateTicks >= CSPConfig.COMMON.realRetreatDurationTicks.get() - 1) {
            dropAggroAfterRealRetreat(situation.target);
        }
    }

    private void runRearCommit(Situation situation) {
        if (!situation.canSeeTarget || !situation.behind || situation.watched) {
            runFlank(situation);
            return;
        }

        Vec3 rearPoint = situation.target.position().subtract(situation.targetLook.scale(0.85D));
        moveTo(findValidPosition(rearPoint), CSPConfig.COMMON.flankSpeedMultiplier.get() * aggressionBias);
    }

    private void runDirectApproach(Situation situation, double speed) {
        moveTo(situation.target.position(), speed);
    }

    private void runAttack(Situation situation) {
        mob.getNavigation().stop();
        if (situation.distanceSqr <= getAttackReachSqr(situation.target) * 1.12D) {
            mob.swing(InteractionHand.MAIN_HAND);
            if (!tryBehindGrab(situation)) {
                mob.doHurtTarget(situation.target);
            }
        } else {
            failedPathAttempts++;
        }
        setState(HunterState.RECOVER, situation);
    }

    private boolean tryBehindGrab(Situation situation) {
        if (!CSPConfig.COMMON.enableHunterBehindGrab.get()
                || behindGrabCooldownTicks > 0
                || behindTicks < CSPConfig.COMMON.behindGrabRequiredTicks.get()
                || !situation.canSeeTarget
                || !situation.behind
                || situation.watched
                || situation.horizontalDistance > CSPConfig.COMMON.behindGrabMaxDistance.get()
                || !mob.getBoundingBox().inflate(0.55D, 0.15D, 0.55D).intersects(situation.target.getBoundingBox())) {
            return false;
        }

        if (situation.target instanceof Player && !Changed.config.server.isGrabEnabled.get()) {
            return false;
        }

        GrabEntityAbilityInstance ability = CSPHunterAI.ensureHunterGrabAbility(mob);
        if (ability.grabbedEntity == situation.target) {
            return true;
        }
        if (ability.grabbedEntity != null) {
            return false;
        }
        if (!ability.grabEntity(situation.target)) {
            return false;
        }

        behindGrabCooldownTicks = CSPConfig.COMMON.behindGrabCooldownTicks.get();
        Changed.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY.with(() -> mob),
                new GrabEntityPacket(mob, situation.target, GrabEntityPacket.GrabType.ARMS));
        ChangedSounds.broadcastSound(mob, ChangedSounds.LATEX_GRAB_ENTITY, 1.0f, 1.0f);
        return true;
    }

    private boolean hasActiveGrab(Player target) {
        GrabEntityAbilityInstance ability = getExistingGrabAbility();
        return ability != null && ability.grabbedEntity == target;
    }

    private GrabEntityAbilityInstance getExistingGrabAbility() {
        return mob.getAbilityInstance(ChangedAbilities.GRAB_ENTITY_ABILITY.get());
    }

    private void dropAggroAfterRealRetreat(Player target) {
        retreatIgnoredTargetUuid = target.getUUID();
        retreatIgnoreUntilTick = mob.tickCount + CSPConfig.COMMON.realRetreatForgetTargetTicks.get();
        mob.setTarget(null);
        resetTargetMemory();
        mob.getNavigation().stop();
    }

    private boolean isRetreatIgnoredTarget(Player target) {
        return retreatIgnoredTargetUuid != null
                && retreatIgnoredTargetUuid.equals(target.getUUID())
                && mob.tickCount < retreatIgnoreUntilTick;
    }

    private void runRecover(Situation situation) {
        if (situation.watched && situation.horizontalDistance < 4.0D) {
            Vec3 target = selectCirclePoint(situation);
            moveTo(target, CSPConfig.COMMON.circleSpeedMultiplier.get() * 0.75D);
        } else {
            mob.getNavigation().stop();
        }
    }

    private void runSearchLastKnown(Situation situation) {
        if (lastSeenTargetPosition == null) {
            mob.setTarget(null);
            resetTargetMemory();
            return;
        }

        if (lastSeenTargetTicks > CSPConfig.COMMON.hunterMemoryAfterLostSightTicks.get()) {
            mob.setTarget(null);
            resetTargetMemory();
            return;
        }

        moveTo(selectLostSightSearchPoint(), CSPConfig.COMMON.flankSpeedMultiplier.get());
    }

    private boolean tryStrafeGapClose(Situation situation, double speed, boolean widerCircle) {
        if (!situation.canSeeTarget
                || situation.behind
                || situation.horizontalDistance < 2.4D
                || situation.horizontalDistance > (widerCircle ? 6.5D : 5.4D)) {
            return false;
        }

        if (weaveDirectionTicks <= 0) {
            if (mob.getRandom().nextFloat() < 0.35F) {
                weaveDirection *= -1;
            }
            weaveDirectionTicks = Mth.nextInt(mob.getRandom(), widerCircle ? 28 : 18, widerCircle ? 55 : 38);
        }

        Vec3 toTarget = normalizeOrZero(horizontal(situation.target.position().subtract(mob.position())));
        if (toTarget.lengthSqr() < EPSILON) {
            return false;
        }

        Vec3 side = sideVector(toTarget).scale(weaveDirection);
        double desiredDistance = widerCircle ? 4.8D : 3.6D;
        double closeBias = Mth.clamp((situation.horizontalDistance - desiredDistance) * 0.28D, -0.35D, 0.35D);
        Vec3 step = side.scale(widerCircle ? 1.45D : 1.15D).add(toTarget.scale(closeBias));
        Vec3 target = findValidPosition(mob.position().add(normalizeOrZero(step).scale(widerCircle ? 1.7D : 1.35D)));
        if (!isPositionSafe(target)) {
            return false;
        }

        mob.getNavigation().stop();
        currentMoveTarget = null;
        mob.getMoveControl().setWantedPosition(target.x, target.y, target.z, speed);
        return true;
    }

    private Vec3 selectCirclePoint(Situation situation) {
        double distance = Mth.clamp(situation.horizontalDistance * 0.75D, CSPConfig.COMMON.circleRadiusMin.get(), CSPConfig.COMMON.circleRadiusMax.get());
        Vec3 side = sideVector(situation.targetLook).scale(circleDirection);
        Vec3 behind = situation.targetLook.scale(situation.horizontalDistance < 7.0D ? -0.35D : -0.65D);
        Vec3 preferred = situation.target.position().add(side.add(behind).normalize().scale(distance));
        return bestCandidate(situation, preferred, distance, true);
    }

    private Vec3 selectFlankPoint(Situation situation) {
        double distance = Mth.clamp(situation.horizontalDistance * 0.65D, CSPConfig.COMMON.flankRadiusMin.get(), CSPConfig.COMMON.flankRadiusMax.get());
        Vec3 side = sideVector(situation.targetLook).scale(circleDirection);
        Vec3 behind = situation.targetLook.scale(-1.0D);
        double sideWeight = situation.behind || situation.horizontalDistance > 9.0D ? 0.45D : 1.05D;
        Vec3 preferred = situation.target.position().add(behind.add(side.scale(sideWeight)).normalize().scale(distance));
        return bestCandidate(situation, preferred, distance, false);
    }

    private Vec3 selectBlindsidePoint(Situation situation, boolean circle) {
        double minRadius = circle ? CSPConfig.COMMON.circleRadiusMin.get() : CSPConfig.COMMON.flankRadiusMin.get();
        double maxRadius = circle ? CSPConfig.COMMON.circleRadiusMax.get() + 2.5D : CSPConfig.COMMON.flankRadiusMax.get() + 3.0D;
        double radius = Mth.clamp(situation.horizontalDistance * (circle ? 0.95D : 0.85D), minRadius, maxRadius);
        Vec3 side = sideVector(situation.targetLook);
        Vec3 velocity = normalizeOrZero(situation.targetVelocity);
        Vec3 anchor = situation.target.position();
        if (velocity.lengthSqr() > EPSILON && situation.targetSpeed >= CSPConfig.COMMON.playerRunningSpeedThreshold.get()) {
            anchor = anchor.add(velocity.scale(Math.min(situation.targetSpeed * 16.0D, 4.0D)));
        }

        Vec3[] candidates = new Vec3[]{
                anchor.add(side.scale(radius * circleDirection)).add(situation.targetLook.scale(-radius * 0.45D)),
                anchor.add(side.scale(-radius * circleDirection)).add(situation.targetLook.scale(-radius * 0.45D)),
                anchor.add(situation.targetLook.scale(-radius * 1.15D)).add(side.scale(radius * 0.75D * circleDirection)),
                anchor.add(situation.targetLook.scale(-radius * 1.15D)).add(side.scale(-radius * 0.75D * circleDirection)),
                anchor.add(side.scale(radius * 1.35D * circleDirection)),
                anchor.add(side.scale(-radius * 1.35D * circleDirection)),
                anchor.add(situation.targetLook.scale(-radius * 1.55D)),
                anchor.add(situation.targetLook.scale(-radius * 0.8D)).add(side.scale(radius * 1.25D * circleDirection))
        };

        Vec3 best = candidates[0];
        double bestScore = Double.NEGATIVE_INFINITY;
        int limit = Math.min(candidates.length, CSPConfig.COMMON.candidatePointLimit.get());
        for (int i = 0; i < limit; i++) {
            Vec3 candidate = findValidPosition(candidates[i]);
            double score = scoreBlindsideCandidate(situation, candidate, radius, circle);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private Vec3 selectLostSightSearchPoint() {
        Vec3 velocity = normalizeOrZero(lastSeenTargetVelocity);
        Vec3 anchor = lastSeenTargetPosition;
        if (velocity.lengthSqr() > EPSILON) {
            double lead = Math.min(lastSeenTargetVelocity.length() * Math.min(lastSeenTargetTicks, 45), 10.0D);
            anchor = anchor.add(velocity.scale(lead));
        }

        Vec3 fromMob = normalizeOrZero(horizontal(anchor.subtract(mob.position())));
        Vec3 side = sideVector(velocity.lengthSqr() > EPSILON ? velocity : fromMob);
        double radius = Mth.clamp(3.0D + lastSeenTargetTicks * 0.035D, 3.0D, 10.0D);
        int route = (lastSeenTargetTicks / Math.max(1, CSPConfig.COMMON.flankRepathIntervalTicks.get())) % 6;
        Vec3 preferred = switch (route) {
            case 0 -> anchor;
            case 1 -> anchor.add(side.scale(radius));
            case 2 -> anchor.add(side.scale(-radius));
            case 3 -> anchor.add(velocity.scale(radius));
            case 4 -> anchor.add(velocity.scale(radius * 0.6D)).add(side.scale(radius));
            default -> anchor.add(velocity.scale(radius * 0.6D)).add(side.scale(-radius));
        };
        return findValidPosition(preferred);
    }

    private Vec3 selectInterceptPoint(Situation situation) {
        Vec3 velocity = normalizeOrZero(situation.targetVelocity);
        double predictedDistance = Math.min(situation.targetSpeed * CSPConfig.COMMON.interceptPredictionTicks.get(), CSPConfig.COMMON.interceptMaxDistance.get());
        Vec3 predicted = situation.target.position().add(velocity.scale(predictedDistance));
        Vec3 side = sideVector(velocity.lengthSqr() > EPSILON ? velocity : situation.targetLook).scale(circleDirection * 1.6D);
        return bestCandidateNear(situation, predicted.add(side), 2.8D);
    }

    private Vec3 selectRetreatPoint(Situation situation, double minDistance, double maxDistance) {
        Vec3 away = normalizeOrZero(horizontal(mob.position().subtract(situation.target.position())));
        if (away.lengthSqr() < EPSILON) {
            away = situation.targetLook.scale(-1.0D);
        }
        Vec3 side = sideVector(situation.targetLook).scale(circleDirection * 2.0D);
        double distance = minDistance + mob.getRandom().nextDouble() * Math.max(0.1D, maxDistance - minDistance);
        Vec3 preferred = mob.position().add(away.scale(distance)).add(side);
        return bestCandidateNear(situation, preferred, distance);
    }

    private Vec3 bestCandidateNear(Situation situation, Vec3 preferred, double radius) {
        Vec3 away = normalizeOrZero(horizontal(preferred.subtract(mob.position())));
        if (away.lengthSqr() < EPSILON) {
            away = normalizeOrZero(horizontal(mob.position().subtract(situation.target.position())));
        }
        Vec3 side = sideVector(away);
        Vec3[] candidates = new Vec3[]{
                preferred,
                preferred.add(side.scale(1.5D)),
                preferred.add(side.scale(-1.5D)),
                preferred.add(away.scale(1.5D)),
                preferred.add(away.scale(-1.0D))
        };

        Vec3 best = preferred;
        double bestScore = Double.NEGATIVE_INFINITY;
        int limit = Math.min(candidates.length, CSPConfig.COMMON.candidatePointLimit.get());
        for (int i = 0; i < limit; i++) {
            Vec3 candidate = findValidPosition(candidates[i]);
            double score = isPositionSafe(candidate) ? 5.0D : -100.0D;
            score -= Math.abs(Math.sqrt(mob.distanceToSqr(candidate)) - radius) * 0.25D;
            score -= failedMoveTarget != null && failedMoveTarget.distanceToSqr(candidate) < 2.0D ? 4.0D : 0.0D;
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private Vec3 bestCandidate(Situation situation, Vec3 preferred, double radius, boolean circle) {
        Vec3 side = sideVector(situation.targetLook);
        Vec3[] candidates = new Vec3[]{
                preferred,
                situation.target.position().add(situation.targetLook.scale(-radius)),
                situation.target.position().add(situation.targetLook.scale(-radius * 0.75D)).add(side.scale(radius * 0.7D)),
                situation.target.position().add(situation.targetLook.scale(-radius * 0.75D)).add(side.scale(-radius * 0.7D)),
                situation.target.position().add(side.scale(radius)),
                situation.target.position().add(side.scale(-radius)),
                situation.target.position().add(side.scale(radius * 1.35D)),
                situation.target.position().add(side.scale(-radius * 1.35D))
        };

        Vec3 best = preferred;
        double bestScore = Double.NEGATIVE_INFINITY;
        int limit = Math.min(candidates.length, CSPConfig.COMMON.candidatePointLimit.get());

        for (int i = 0; i < limit; i++) {
            Vec3 candidate = findValidPosition(candidates[i]);
            double score = scoreCandidate(situation, candidate, radius, circle);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best;
    }

    private double scoreCandidate(Situation situation, Vec3 candidate, double radius, boolean circle) {
        Vec3 fromTarget = normalizeOrZero(horizontal(candidate.subtract(situation.target.position())));
        double dot = situation.targetLook.lengthSqr() < EPSILON ? 0.0D : situation.targetLook.dot(fromTarget);
        double distanceToTarget = Math.sqrt(horizontal(candidate.subtract(situation.target.position())).lengthSqr());
        double score = mob.getRandom().nextDouble() * 0.15D;

        if (dot <= CSPConfig.COMMON.behindDotThreshold.get()) {
            score += circle ? 5.0D : 7.0D;
        } else if (dot < CSPConfig.COMMON.frontConeDotThreshold.get()) {
            score += circle ? 6.0D : 4.5D;
        } else {
            score -= 8.0D;
        }

        score -= Math.abs(distanceToTarget - radius) * 0.45D;
        score -= Math.sqrt(mob.distanceToSqr(candidate)) * (circle ? 0.04D : 0.035D);
        if (situation.horizontalDistance < 10.0D) {
            Vec3 toCandidate = normalizeOrZero(horizontal(candidate.subtract(mob.position())));
            Vec3 toTarget = normalizeOrZero(horizontal(situation.target.position().subtract(mob.position())));
            if (toCandidate.lengthSqr() > EPSILON && toTarget.lengthSqr() > EPSILON) {
                double directness = toCandidate.dot(toTarget);
                double lateral = Math.abs(toCandidate.dot(sideVector(toTarget)));
                score += lateral * (circle ? 2.2D : 1.4D);
                score -= Math.max(0.0D, directness - 0.72D) * (circle ? 2.0D : 1.0D);
            }
        }
        if (currentMoveTarget != null && currentMoveTarget.distanceToSqr(candidate) < 2.0D) {
            score += 0.5D;
        }
        if (failedMoveTarget != null && failedMoveTarget.distanceToSqr(candidate) < 2.0D) {
            score -= 4.0D;
        }
        if (!isPositionSafe(candidate)) {
            score -= 100.0D;
        }
        return score;
    }

    private double scoreBlindsideCandidate(Situation situation, Vec3 candidate, double radius, boolean circle) {
        Vec3 fromTarget = normalizeOrZero(horizontal(candidate.subtract(situation.target.position())));
        double dot = situation.targetLook.lengthSqr() < EPSILON ? 0.0D : situation.targetLook.dot(fromTarget);
        double distanceToTarget = Math.sqrt(horizontal(candidate.subtract(situation.target.position())).lengthSqr());
        double score = mob.getRandom().nextDouble() * 0.1D;

        if (!isPositionSafe(candidate)) {
            return -100.0D;
        }
        if (targetCanSeePosition(situation, candidate)) {
            score -= situation.watched ? 12.0D : 7.0D;
        } else {
            score += situation.watched ? 12.0D : 7.0D;
        }
        if (dot <= CSPConfig.COMMON.behindDotThreshold.get()) {
            score += circle ? 4.0D : 6.0D;
        } else if (dot < CSPConfig.COMMON.frontConeDotThreshold.get()) {
            score += circle ? 6.5D : 5.5D;
        } else {
            score -= 9.0D;
        }

        Vec3 toCandidate = normalizeOrZero(horizontal(candidate.subtract(mob.position())));
        Vec3 toTarget = normalizeOrZero(horizontal(situation.target.position().subtract(mob.position())));
        if (toCandidate.lengthSqr() > EPSILON && toTarget.lengthSqr() > EPSILON) {
            double directness = toCandidate.dot(toTarget);
            double lateral = Math.abs(toCandidate.dot(sideVector(toTarget)));
            score += lateral * (circle ? 2.5D : 2.0D);
            score -= Math.max(0.0D, directness - 0.55D) * (circle ? 4.0D : 3.0D);
        }

        score -= Math.abs(distanceToTarget - radius) * 0.35D;
        score -= failedMoveTarget != null && failedMoveTarget.distanceToSqr(candidate) < 2.0D ? 5.0D : 0.0D;
        if (currentMoveTarget != null && currentMoveTarget.distanceToSqr(candidate) < 3.0D) {
            score += 0.8D;
        }
        return score;
    }

    private boolean targetCanSeePosition(Situation situation, Vec3 position) {
        Vec3 toPosition = normalizeOrZero(horizontal(position.subtract(situation.target.position())));
        if (toPosition.lengthSqr() < EPSILON || situation.targetLook.dot(toPosition) < CSPConfig.COMMON.frontConeDotThreshold.get()) {
            return false;
        }

        Vec3 start = situation.target.getEyePosition();
        Vec3 end = new Vec3(position.x, position.y + Math.max(0.6D, mob.getBbHeight() * 0.5D), position.z);
        HitResult hit = mob.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, situation.target));
        return hit.getType() == HitResult.Type.MISS;
    }

    private Vec3 findValidPosition(Vec3 base) {
        Vec3 best = base;
        for (int yOffset : new int[]{0, 1, -1, 2, -2}) {
            Vec3 candidate = new Vec3(base.x, base.y + yOffset, base.z);
            if (isPositionSafe(candidate)) {
                return candidate;
            }
            if (best == base && mob.level().isLoaded(BlockPos.containing(candidate))) {
                best = candidate;
            }
        }
        return best;
    }

    private boolean isPositionSafe(Vec3 position) {
        BlockPos blockPos = BlockPos.containing(position);
        if (!mob.level().isLoaded(blockPos)) {
            return false;
        }

        AABB movedBox = mob.getBoundingBox().move(position.subtract(mob.position()));
        if (!mob.level().noCollision(mob, movedBox)) {
            return false;
        }

        BlockPos below = BlockPos.containing(position.x, position.y - 0.2D, position.z);
        return !mob.level().getBlockState(below).getCollisionShape(mob.level(), below).isEmpty();
    }

    private void moveTo(Vec3 target, double speed) {
        if (target == null) {
            return;
        }

        boolean needsPath = currentMoveTarget == null
                || currentMoveTarget.distanceToSqr(target) > 2.0D
                || mob.distanceToSqr(currentMoveTarget) < 2.0D
                || pathRecalculateCooldownTicks <= 0;
        if (!needsPath) {
            return;
        }

        if (!CSPHunterAI.tryConsumeMovementBudget(mob.level().getGameTime())) {
            pathRecalculateCooldownTicks = Math.max(pathRecalculateCooldownTicks, 2);
            return;
        }

        currentMoveTarget = target;
        int baseCooldown = Math.max(CSPConfig.COMMON.pathCheckIntervalTicks.get(), CSPConfig.COMMON.flankRepathIntervalTicks.get());
        pathRecalculateCooldownTicks = state == HunterState.CIRCLE_APPROACH || state == HunterState.FLANK
                ? CSPConfig.COMMON.flankRepathIntervalTicks.get()
                : baseCooldown;

        if (!mob.getNavigation().moveTo(target.x, target.y, target.z, speed)) {
            debugMoveTarget(target, false);
            failedPathAttempts++;
            failedMoveTarget = target;
            failedMoveTargetTicks = 80;
            pathRecalculateCooldownTicks += 8;
            if (failedPathAttempts >= 3 && (state == HunterState.CIRCLE_APPROACH || state == HunterState.INTERCEPT)) {
                setState(HunterState.FLANK, null, "path_failed_fallback");
            }
        } else if (failedPathAttempts > 0) {
            debugMoveTarget(target, true);
            failedPathAttempts--;
        } else {
            debugMoveTarget(target, true);
        }
    }

    private double getAttackReachSqr(LivingEntity target) {
        double reach = Math.max(1.4D, mob.getBbWidth() * 1.8D + target.getBbWidth() * 0.5D);
        return reach * reach;
    }

    private Vec3 horizontal(Vec3 vector) {
        return new Vec3(vector.x, 0.0D, vector.z);
    }

    private Vec3 normalizeOrZero(Vec3 vector) {
        return vector.lengthSqr() < EPSILON ? Vec3.ZERO : vector.normalize();
    }

    private Vec3 sideVector(Vec3 forward) {
        Vec3 normalized = normalizeOrZero(forward);
        if (normalized.lengthSqr() < EPSILON) {
            return new Vec3(circleDirection, 0.0D, 0.0D);
        }
        return new Vec3(-normalized.z, 0.0D, normalized.x);
    }

    private void resetTargetMemory() {
        currentTargetUuid = null;
        targetLockTicks = 0;
        lastSeenTargetPosition = null;
        lastSeenTargetVelocity = Vec3.ZERO;
        lastSeenTargetTicks = 9999;
        currentMoveTarget = null;
        realRetreatsUsed = 0;
        desperate = false;
        fakeRetreatCooldownTicks = 0;
        fakeRetreatRollCooldownTicks = 0;
        realRetreatCooldownTicks = 0;
        behindGrabCooldownTicks = 0;
        attackCooldownTicks = 0;
        recoverTicks = 0;
        failedPathAttempts = 0;
        interceptCooldownTicks = 0;
        lastHurtReactionTimestamp = -1;
        failedMoveTarget = null;
        failedMoveTargetTicks = 0;
        pathRecalculateCooldownTicks = 0;
        lineOfSightCooldownTicks = 0;
        watchedTicks = 0;
        notWatchedTicks = 0;
        behindTicks = 0;
        cachedCanSeeTarget = false;
        cachedTargetCanSeeMob = false;
        weaveDirection = circleDirection;
        weaveDirectionTicks = 0;
        setState(HunterState.IDLE_OR_NORMAL, null, "reset_target_memory");
    }

    private void syncMobTarget(Player target) {
        LivingEntity rawTarget = mob.getTarget();
        if (rawTarget == target) {
            return;
        }

        if (rawTarget instanceof Player otherPlayer && !otherPlayer.getUUID().equals(target.getUUID())) {
            mob.setTarget(target);
            return;
        }

        if (rawTarget == null && (canSeeTargetNow(target) || isRecentlyHurtBy(target))) {
            mob.setTarget(target);
        }
    }

    private void debugStateChange(HunterState oldState, HunterState nextState, Situation situation, String reason) {
        if (!CSPConfig.COMMON.hunterAIDebug.get()) {
            return;
        }

        String targetName = situation == null ? "none" : situation.target.getGameProfile().getName();
        ChangedSurviveProtocol.LOGGER.debug("hunter_ai {} {} -> {} reason={} target={} visible={} watched={} retreats={} failedPaths={} moveTarget={}",
                mob.getStringUUID(), oldState, nextState, reason, targetName,
                situation != null && situation.canSeeTarget,
                situation != null && situation.watched,
                realRetreatsUsed, failedPathAttempts, currentMoveTarget);

        if (currentMoveTarget != null && mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, currentMoveTarget.x, currentMoveTarget.y + 0.15D, currentMoveTarget.z, 2, 0.08D, 0.04D, 0.08D, 0.0D);
        }
        if (lastSeenTargetPosition != null && nextState == HunterState.SEARCH_LAST_KNOWN && mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, lastSeenTargetPosition.x, lastSeenTargetPosition.y + 0.15D, lastSeenTargetPosition.z, 3, 0.12D, 0.05D, 0.12D, 0.0D);
        }
    }

    private void debugMoveTarget(Vec3 target, boolean accepted) {
        if (!CSPConfig.COMMON.hunterAIDebug.get() || !(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (mob.tickCount % 10 == 0) {
            serverLevel.sendParticles(accepted ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMOKE,
                    target.x, target.y + 0.15D, target.z, accepted ? 1 : 2, 0.06D, 0.03D, 0.06D, 0.0D);
        }
    }

    private void debugTick(Situation situation) {
        if (!CSPConfig.COMMON.hunterAIDebug.get() || mob.tickCount % 40 != 0) {
            return;
        }

        ChangedSurviveProtocol.LOGGER.debug("hunter_ai_tick {} state={} target={} visible={} watched={} lastSeen={} health={} retreats={} moveTarget={}",
                mob.getStringUUID(), state, situation.target.getGameProfile().getName(), situation.canSeeTarget,
                situation.watched, lastSeenTargetTicks, situation.healthPercent, realRetreatsUsed, currentMoveTarget);
    }

    private enum HunterState {
        IDLE_OR_NORMAL,
        SEARCH_LAST_KNOWN,
        CIRCLE_APPROACH,
        FLANK,
        INTERCEPT,
        FAKE_RETREAT,
        REAL_RETREAT,
        DESPERATION_RUSH,
        REAR_COMMIT,
        ATTACK,
        RECOVER
    }

    private record Situation(Player target, Vec3 targetLook, Vec3 targetVelocity, double distanceSqr,
                             double horizontalDistance, double targetSpeed, boolean canSeeTarget,
                             boolean watched, boolean frontCone, boolean side, boolean behind,
                             boolean attackAngleGood, boolean closeEnoughToAttack,
                             boolean validWithMemory, double healthPercent, boolean lowHealth,
                             boolean recentlyHit, boolean targetPressuring) {
    }
}
