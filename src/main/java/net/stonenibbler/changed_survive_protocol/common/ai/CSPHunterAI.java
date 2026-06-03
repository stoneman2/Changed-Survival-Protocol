package net.stonenibbler.changed_survive_protocol.common.ai;

import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.ability.GrabEntityAbilityInstance;
import net.ltxprogrammer.changed.ability.IAbstractChangedEntity;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.LivingEntityDataExtension;
import net.ltxprogrammer.changed.entity.TamableLatexEntity;
import net.ltxprogrammer.changed.entity.TransfurCause;
import net.ltxprogrammer.changed.entity.ai.LatexAssimilationDecision;
import net.ltxprogrammer.changed.init.ChangedAbilities;
import net.ltxprogrammer.changed.init.ChangedAttributes;
import net.ltxprogrammer.changed.init.ChangedSounds;
import net.ltxprogrammer.changed.init.ChangedTags;
import net.ltxprogrammer.changed.network.packet.GrabEntityPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.network.PacketDistributor;
import net.stonenibbler.changed_survive_protocol.common.config.CSPConfig;
import net.stonenibbler.changed_survive_protocol.common.event.CSPTransfurEvents;
import net.stonenibbler.changed_survive_protocol.mixin.ChangedEntityAccessor;

import java.util.Map;
import java.util.WeakHashMap;

public final class CSPHunterAI {
    private static final Map<ChangedEntity, SoloHunterGoal> GOALS = new WeakHashMap<>();
    private static long budgetGameTime = Long.MIN_VALUE;
    private static int budgetUsed;

    private CSPHunterAI() {
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ChangedEntity changedEntity)) {
            return;
        }

        if (!isHunterCapable(changedEntity)) {
            return;
        }

        ensureHunterGrabAbility(changedEntity);

        if (event.getLevel().isClientSide()) {
            return;
        }

        if (GOALS.containsKey(changedEntity)) {
            return;
        }

        SoloHunterGoal goal = new SoloHunterGoal(changedEntity);
        GOALS.put(changedEntity, goal);
        changedEntity.goalSelector.addGoal(0, goal);
    }

    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        clearStaleHunterGrab(entity);

        if (entity instanceof ChangedEntity changedEntity && isHunterCapable(changedEntity)) {
            tickHunterGrabAbility(changedEntity);
        }
    }

    public static GrabEntityAbilityInstance ensureHunterGrabAbility(ChangedEntity entity) {
        GrabEntityAbilityInstance ability = entity.getAbilityInstance(ChangedAbilities.GRAB_ENTITY_ABILITY.get());
        if (ability != null) {
            return ability;
        }

        return ((ChangedEntityAccessor)entity).csp$registerAbility(instance -> false,
                new GrabEntityAbilityInstance(ChangedAbilities.GRAB_ENTITY_ABILITY.get(), IAbstractChangedEntity.forEntity(entity)));
    }

    private static void tickHunterGrabAbility(ChangedEntity entity) {
        GrabEntityAbilityInstance ability = entity.getAbilityInstance(ChangedAbilities.GRAB_ENTITY_ABILITY.get());
        if (ability == null || ability.grabbedEntity == null) {
            return;
        }

        LivingEntity grabbed = ability.grabbedEntity;
        ability.tickIdle();
        tickHunterGrabPressure(entity, ability, grabbed);

        if (!entity.level().isClientSide && ability.grabbedEntity == null) {
            Changed.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity),
                    new GrabEntityPacket(entity, grabbed, GrabEntityPacket.GrabType.RELEASE));
            ChangedSounds.broadcastSound(entity, ChangedSounds.LATEX_UNGRAB_ENTITY, 1.0f, 1.0f);
        }
    }

    private static void tickHunterGrabPressure(ChangedEntity entity, GrabEntityAbilityInstance ability, LivingEntity grabbed) {
        if (entity.level().isClientSide || ability.grabbedEntity == null || grabbed != ability.grabbedEntity) {
            return;
        }

        int maxHoldTicks = CSPConfig.COMMON.hunterGrabMaxHoldTicks.get();
        if (maxHoldTicks > 0 && ability.ticksGrabbed >= maxHoldTicks) {
            ability.releaseEntity(true);
            return;
        }

        if (!(grabbed instanceof ServerPlayer player)) {
            return;
        }

        int interval = CSPConfig.COMMON.hunterGrabExposureIntervalTicks.get();
        if (interval <= 0 || ability.ticksGrabbed < 10 || ability.ticksGrabbed % interval != 0) {
            return;
        }

        LatexAssimilationDecision<?> decision = makeHunterGrabDecision(entity, player);
        if (decision == null || decision.transfurVariant() == null) {
            return;
        }

        var toleranceAttribute = player.getAttribute(ChangedAttributes.TRANSFUR_TOLERANCE.get());
        double tolerance = Math.max(1.0D, toleranceAttribute == null ? 100.0D : toleranceAttribute.getValue());
        double baseCoverage = decision.transfurProgress() / tolerance * CSPConfig.COMMON.coverageInfectionThreshold.get();
        double amount = Math.max(CSPConfig.COMMON.minimumGrabCoverage.get(), baseCoverage * CSPConfig.COMMON.grabCoverageMultiplier.get());
        CSPTransfurEvents.addHunterGrabExposure(player, entity, decision.transfurVariant().getFormId().toString(), amount);
    }

    private static LatexAssimilationDecision<?> makeHunterGrabDecision(ChangedEntity entity, LivingEntity target) {
        return switch (entity.getTransfurMode()) {
            case REPLICATION -> entity.makeLatexAssimilationDecision(TransfurCause.GRAB_REPLICATE, target);
            case ABSORPTION -> entity.makeLatexAssimilationDecision(TransfurCause.GRAB_ABSORB, target);
            case NONE -> null;
        };
    }

    private static void clearStaleHunterGrab(LivingEntity entity) {
        if (!(entity instanceof LivingEntityDataExtension ext) || !(ext.getGrabbedBy() instanceof ChangedEntity grabber) || !isHunterGrabberType(grabber)) {
            return;
        }

        GrabEntityAbilityInstance ability = grabber.getAbilityInstance(ChangedAbilities.GRAB_ENTITY_ABILITY.get());
        if (!grabber.isRemoved() && grabber.isAlive() && ability != null && ability.grabbedEntity == entity) {
            return;
        }

        ext.setGrabbedBy(null);
        entity.noPhysics = false;
    }

    private static boolean isHunterCapable(ChangedEntity entity) {
        return isHunterGrabberType(entity)
                && entity.getUnderlyingPlayer() == null
                && (!(entity instanceof TamableLatexEntity tamable) || !tamable.isTame())
                && entity.getNavigation() instanceof GroundPathNavigation;
    }

    private static boolean isHunterGrabberType(ChangedEntity entity) {
        return entity.getType().is(ChangedTags.EntityTypes.LATEX)
                && !entity.getType().is(ChangedTags.EntityTypes.PARTIAL_LATEX)
                && !entity.getType().is(ChangedTags.EntityTypes.ARMLESS)
                && !entity.getType().is(ChangedTags.EntityTypes.BENIGN_LATEXES)
                && (!CSPConfig.COMMON.hunterAIRequiresEntityTag.get() || entity.getType().is(CSPEntityTypeTags.HUNTER_AI_LATEX_BEASTS));
    }

    public static boolean tryConsumeMovementBudget(long gameTime) {
        int max = CSPConfig.COMMON.maxHunterAIMobsPerTick.get();
        if (max <= 0) {
            return true;
        }

        synchronized (CSPHunterAI.class) {
            if (budgetGameTime != gameTime) {
                budgetGameTime = gameTime;
                budgetUsed = 0;
            }

            if (budgetUsed >= max) {
                return false;
            }

            budgetUsed++;
            return true;
        }
    }
}
