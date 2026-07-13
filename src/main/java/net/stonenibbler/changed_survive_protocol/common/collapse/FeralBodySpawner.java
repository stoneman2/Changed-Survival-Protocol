package net.stonenibbler.changed_survive_protocol.common.collapse;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.latex.LatexType;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.entity.variant.TransfurVariantInstance;
import net.ltxprogrammer.changed.init.ChangedLatexTypes;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.stonenibbler.changed_survive_protocol.ChangedSurviveProtocol;
import net.stonenibbler.changed_survive_protocol.common.damage.CSPDamageSources;
import net.stonenibbler.changed_survive_protocol.common.data.CSPPlayerData;
import net.stonenibbler.changed_survive_protocol.common.data.CSPCapabilities;
import net.stonenibbler.changed_survive_protocol.common.infestation.LatexHeartBlock;
import net.stonenibbler.changed_survive_protocol.common.infestation.LatexInfestationManager;
import net.stonenibbler.changed_survive_protocol.common.latex.LatexStrandManager;
import net.stonenibbler.changed_survive_protocol.common.network.CSPNetwork;

import java.util.UUID;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;

public final class FeralBodySpawner {
    public static final String ORIGINAL_PLAYER_UUID = ChangedSurviveProtocol.MODID + ":original_player";
    public static final String ORIGINAL_PLAYER_NAME = ChangedSurviveProtocol.MODID + ":original_player_name";
    public static final String STRAIN_ID = ChangedSurviveProtocol.MODID + ":strain_id";
    public static final String COLLAPSE_COUNT = ChangedSurviveProtocol.MODID + ":collapse_count";

    private FeralBodySpawner() {
    }

    public static void forceCollapse(ServerPlayer player, CSPPlayerData data) {
        TransfurVariantInstance<?> instance = ProcessTransfur.getPlayerTransfurVariant(player);
        TransfurVariant<?> variant = instance == null ? null : instance.getParent();

        // how are we collapsing if we don't have a variant?
        if (variant == null) {
            data.setLucidity(1.0D);
            CSPNetwork.sync(player, data);
            return;
        }

        data.incrementCollapseCount();
        ChangedEntity feral = findExistingFeral(player, data);
        if (feral == null && data.getFeralSelfUuid() == null) {
            feral = spawnFeral(player, variant, data);
        } else {
            if (feral != null) {
                strengthenExistingFeral(feral, data);
            }
        }

        if (feral != null) {
            data.setFeralSelfUuid(feral.getUUID());
            data.setFeralSelfLocation(feral.level().dimension().location().toString(), feral.blockPosition());
        }

        if (player.level() instanceof ServerLevel level) {
            LatexInfestationManager.spawnCollapseHeartNear(level, player.blockPosition(), kindFor(instance, variant));
        }


        // reset
        ProcessTransfur.removePlayerTransfurVariant(player);
        data.setCoverage(0.0D);
        data.setInfected(false);
        data.setLucidity(100.0D);
        data.setLucidityActive(false);
        data.setUnstableLatex(false);
        data.setStabilizedLatex(false);
        data.setUnstableLatexTicks(0);
        data.setLucidityDrainMultiplier(1.0D);

        CSPNetwork.sync(player, data);
        killPlayer(player);
    }

    private static void killPlayer(ServerPlayer player) {
        if (player.isDeadOrDying()) {
            return;
        }

        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        player.invulnerableTime = 0;
        if (!player.hurt(CSPDamageSources.lucidityCollapse(player.level()), Float.MAX_VALUE) && !player.isDeadOrDying()) {
            player.setHealth(0.0F);
            player.die(CSPDamageSources.lucidityCollapse(player.level()));
        }
    }

    private static LatexHeartBlock.Kind kindFor(TransfurVariantInstance<?> instance, TransfurVariant<?> variant) {
        LatexType latexType = instance == null ? LatexStrandManager.resolve(variant).latexType() : LatexStrandManager.resolve(instance).latexType();
        if (latexType == ChangedLatexTypes.WHITE_LATEX.get()) {
            return LatexHeartBlock.Kind.WHITE;
        }
        return LatexHeartBlock.Kind.DARK;
    }

    private static ChangedEntity spawnFeral(ServerPlayer player, TransfurVariant<?> variant, CSPPlayerData data) {
        ChangedEntity feral = variant.spawnAtEntity(player);
        ResourceLocation formId = variant.getFormId();
        String formName = feral.getType().getDescription().getString();
        feral.setCustomName(Component.literal(formName + " \"" + player.getGameProfile().getName() + "\""));
        feral.setCustomNameVisible(true);
        feral.setPersistenceRequired();

        CompoundTag persistent = feral.getPersistentData();
        persistent.putUUID(ORIGINAL_PLAYER_UUID, player.getUUID());
        persistent.putString(ORIGINAL_PLAYER_NAME, player.getGameProfile().getName());
        persistent.putString(STRAIN_ID, formId.toString());
        persistent.putInt(COLLAPSE_COUNT, data.getCollapseCount());
        return feral;
    }

    private static ChangedEntity findExistingFeral(ServerPlayer player, CSPPlayerData data) {
        UUID feralUuid = data.getFeralSelfUuid();
        if (feralUuid == null) {
            return null;
        }

        for (ServerLevel candidate : player.getServer().getAllLevels()) {
            Entity loadedEntity = candidate.getEntity(feralUuid);
            if (loadedEntity instanceof ChangedEntity changedEntity && loadedEntity.isAlive()) {
                data.setFeralSelfLocation(candidate.dimension().location().toString(), loadedEntity.blockPosition());
                return changedEntity;
            }
        }

        ServerLevel level = resolveFeralLevel(player, data);
        if (level == null) {
            data.clearFeralSelf();
            return null;
        }

        if (data.getFeralSelfPos() != null && !level.isLoaded(data.getFeralSelfPos())) {
            return null;
        }

        Entity entity = level.getEntity(feralUuid);
        if (entity instanceof ChangedEntity changedEntity && entity.isAlive()) {
            return changedEntity;
        }
        if (data.getFeralSelfPos() == null) {
            return null;
        }
        data.clearFeralSelf();
        return null;
    }

    private static ServerLevel resolveFeralLevel(ServerPlayer player, CSPPlayerData data) {
        ResourceLocation dimensionId = ResourceLocation.tryParse(data.getFeralSelfDimension());
        if (dimensionId == null) {
            return player.serverLevel();
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        return player.getServer().getLevel(dimension);
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        updateTrackedLocation(event.getEntity());
    }

    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        updateTrackedLocation(event.getEntity());
    }

    private static void updateTrackedLocation(Entity entity) {
        if (!(entity instanceof ChangedEntity) || !entity.getPersistentData().hasUUID(ORIGINAL_PLAYER_UUID) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(entity.getPersistentData().getUUID(ORIGINAL_PLAYER_UUID));
        if (owner == null) {
            return;
        }
        CSPCapabilities.get(owner).ifPresent(data -> {
            if (data.getFeralSelfUuid() != null && !data.getFeralSelfUuid().equals(entity.getUUID())) {
                return;
            }
            data.setFeralSelfUuid(entity.getUUID());
            data.setFeralSelfLocation(level.dimension().location().toString(), entity.blockPosition());
        });
    }

    private static void strengthenExistingFeral(ChangedEntity feral, CSPPlayerData data) {
        feral.setHealth(Math.min(feral.getMaxHealth(), feral.getHealth() + 6.0F));
        feral.getPersistentData().putInt(COLLAPSE_COUNT, data.getCollapseCount());
    }
}
