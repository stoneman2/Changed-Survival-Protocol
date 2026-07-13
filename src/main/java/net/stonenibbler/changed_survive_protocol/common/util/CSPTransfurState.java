package net.stonenibbler.changed_survive_protocol.common.util;

import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.world.entity.player.Player;
import net.stonenibbler.changed_survive_protocol.common.config.CSPConfig;
import net.stonenibbler.changed_survive_protocol.common.data.CSPPlayerData;

public final class CSPTransfurState {
    private CSPTransfurState() {
    }

    public static boolean isSurvivalProtocolActive(Player player) {
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    public static boolean hasNonSuitTransfur(Player player) {
        return player != null && ProcessTransfur.getPlayerTransfurVariantSafe(player)
                .filter(variant -> !variant.isTemporaryFromSuit())
                .isPresent();
    }

    public static boolean hasNonSuitLatex(Player player) {
        return hasNonSuitTransfur(player) && ProcessTransfur.isPlayerLatex(player);
    }

    public static boolean usesLucidity(Player player) {
        return isSurvivalProtocolActive(player)
                && CSPConfig.COMMON.lucidityMechanicsEnabled.get()
                && hasNonSuitTransfur(player)
                && !isLucidityBlacklisted(player);
    }

    public static boolean usesLucidity(Player player, CSPPlayerData data) {
        return data != null
                && !data.isStabilizedLatex()
                && !isTotemFormActive(player, data)
                && usesLucidity(player);
    }

    private static boolean isTotemFormActive(Player player, CSPPlayerData data) {
        if (!data.hasTotemForm()) {
            return false;
        }

        return ProcessTransfur.getPlayerTransfurVariantSafe(player)
                .map(variant -> data.getTotemFormId().equals(variant.getFormId().toString()))
                .orElse(false);
    }

    public static boolean isLucidityBlacklisted(Player player) {
        if (player == null) {
            return false;
        }

        var variant = ProcessTransfur.getPlayerTransfurVariantSafe(player).orElse(null);
        return variant != null && isLucidityBlacklisted(variant.getParent().getEntityType());
    }

    public static boolean isLucidityBlacklisted(net.minecraft.world.entity.EntityType<?> entityType) {
        if (entityType == null) {
            return false;
        }

        return CSPConfig.COMMON.getLucidityBlacklistedEntityTypes().anyMatch(pred -> pred.test(entityType));
    }
}
