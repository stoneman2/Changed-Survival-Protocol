package net.stonenibbler.changed_survive_protocol.common.event;

import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.stonenibbler.changed_survive_protocol.common.collapse.FeralBodySpawner;
import net.stonenibbler.changed_survive_protocol.common.config.CSPConfig;
import net.stonenibbler.changed_survive_protocol.common.data.CSPCapabilities;
import net.stonenibbler.changed_survive_protocol.common.data.CSPPlayerData;
import net.stonenibbler.changed_survive_protocol.common.latex.LatexStrandManager;
import net.stonenibbler.changed_survive_protocol.common.network.CSPNetwork;

public final class CSPPlayerEvents {
    private CSPPlayerEvents() {
    }

    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        CSPCapabilities.attachPlayerData(event);
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        CSPCapabilities.get(event.getOriginal()).ifPresent(oldData ->
                CSPCapabilities.get(event.getEntity()).ifPresent(newData -> newData.copyFrom(oldData)));
        event.getOriginal().invalidateCaps();
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        sync(event.getEntity());
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sync(event.getEntity());
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event.getEntity());
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        CSPCapabilities.get(serverPlayer).ifPresent(data -> {
            boolean dirty = tick(serverPlayer, data);
            if (dirty || serverPlayer.tickCount % 100 == 0) {
                CSPNetwork.sync(serverPlayer, data);
            }
        });
    }

    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            CSPCapabilities.get(serverPlayer).ifPresent(data -> CSPNetwork.sync(serverPlayer, data));
        }
    }

    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewTarget() instanceof Player player) || !ProcessTransfur.isPlayerLatex(player)) {
            return;
        }

        if (LatexStrandManager.isSociallyFriendly(event.getEntity(), player)) {
            event.setNewTarget(null);
        }
    }

    public static void onFoodFinished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !ProcessTransfur.isPlayerTransfurred(player)) {
            return;
        }
        if (event.getItem().getFoodProperties(player) == null) {
            return;
        }

        var food = event.getItem().getFoodProperties(player);
        double recovered = food.getNutrition() * CSPConfig.COMMON.lucidityRecoveryPerFoodNutrition.get()
                + food.getSaturationModifier() * CSPConfig.COMMON.lucidityRecoveryPerFoodSaturation.get();
        if (recovered <= 0.0D) {
            return;
        }

        CSPCapabilities.get(player).ifPresent(data -> {
            data.addLucidity(recovered);
            CSPNetwork.sync(player, data);
        });
    }

    private static boolean tick(ServerPlayer player, CSPPlayerData data) {
        boolean dirty = false;

        if (data.tickSuppressant()) {
            dirty = true;
        }

        boolean transfurred = ProcessTransfur.isPlayerTransfurred(player);
        if (!transfurred && data.hasSettledStrain()) {
            dirty |= CSPTransfurEvents.restoreSettledForm(player, data);
            transfurred = ProcessTransfur.isPlayerTransfurred(player);
        }

        if (data.isLucidityActive() != transfurred) {
            data.setLucidityActive(transfurred);
            dirty = true;
        }

        if (transfurred) {
            if (data.getCoverage() > 0.0D || data.isInfected()) {
                data.setCoverage(0.0D);
                data.setInfected(false);
                dirty = true;
            }
            dirty |= tickLatexNeeds(player, data);
        }

        if (!transfurred) {
            if (!data.isInfected() && data.getCoverage() >= CSPConfig.COMMON.coverageInfectionThreshold.get()) {
                startInfection(data);
                dirty = true;
            }

            if (!data.isInfected() && isBeingWashed(player) && data.getCoverage() > 0.0D) {
                double washAmount = player.isInWater() ? CSPConfig.COMMON.waterCoverageWashPerTick.get() : CSPConfig.COMMON.rainCoverageWashPerTick.get();
                data.setCoverage(data.getCoverage() - washAmount);
                dirty = true;
            } else if (!data.isInfected() && data.getCoverage() > 0.0D && player.tickCount % CSPConfig.COMMON.passiveCoverageDecayIntervalTicks.get() == 0) {
                data.setCoverage(data.getCoverage() - CSPConfig.COMMON.passiveCoverageDecayAmount.get());
                dirty = true;
            }

            if (data.isInfected()) {
                if (data.getCoverage() > 0.0D) {
                    data.setCoverage(0.0D);
                    dirty = true;
                }

                if (data.getInfectionPercent() < 100.0D && data.getSuppressantTicks() <= 0 && player.tickCount % CSPConfig.COMMON.infectionGrowthIntervalTicks.get() == 0) {
                    data.addInfection(CSPConfig.COMMON.infectionGrowthPerInterval.get());
                    dirty = true;
                }

                if (data.getInfectionPercent() >= 100.0D) {
                    CSPTransfurEvents.forceUncontrolledTransfur(player, data);
                    dirty = true;
                }
            }
        }

        if (data.isLucidityActive() && data.getLucidity() <= 0.0D) {
            FeralBodySpawner.forceCollapse(player, data);
            dirty = true;
        }

        return dirty;
    }

    private static boolean tickLatexNeeds(ServerPlayer player, CSPPlayerData data) {
        boolean dirty = false;

        if (!shouldDrainLucidity(player)) {
            dirty |= CSPLucidityEvents.tickNearLatex(player, data);
            return dirty;
        }

        if (!data.isStabilizedLatex()) {
            if (!data.isUnstableLatex()) {
                data.setUnstableLatex(true);
                dirty = true;
            }
            data.addUnstableLatexTick();
            double multiplierRange = CSPConfig.COMMON.maxUnstableLucidityMultiplier.get() - 1.0D;
            double multiplierProgress = Math.min(1.0D, data.getUnstableLatexTicks() / (double)CSPConfig.COMMON.unstableTicksForMaxMultiplier.get());
            data.setLucidityDrainMultiplier(1.0D + multiplierRange * multiplierProgress);
            dirty = true;
        }

        if (player.tickCount % CSPConfig.COMMON.latexNeedIntervalTicks.get() == 0) {
            double lucidityDrain = data.isStabilizedLatex() ? CSPConfig.COMMON.stabilizedLucidityDrain.get() : CSPConfig.COMMON.unstableLucidityDrain.get() * data.getLucidityDrainMultiplier();
            data.addLucidity(-lucidityDrain);
            dirty = true;
        }

        dirty |= CSPLucidityEvents.tickNearLatex(player, data);

        return dirty;
    }

    private static boolean shouldDrainLucidity(ServerPlayer player) {
        return !player.isCreative() || CSPConfig.COMMON.creativeModeLucidityDrain.get();
    }

    private static void startInfection(CSPPlayerData data) {
        data.setCoverage(0.0D);
        data.setInfectionPercent(Math.max(1.0D, CSPConfig.COMMON.infectionStartPercent.get()));
    }

    private static boolean isBeingWashed(Player player) {
        return player.isInWater() || player.isInWaterRainOrBubble();
    }
}
