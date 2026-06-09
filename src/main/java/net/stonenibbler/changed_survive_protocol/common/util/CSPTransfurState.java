package net.stonenibbler.changed_survive_protocol.common.util;

import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.world.entity.player.Player;

public final class CSPTransfurState {
    private CSPTransfurState() {
    }

    public static boolean hasNonSuitTransfur(Player player) {
        return player != null && ProcessTransfur.getPlayerTransfurVariantSafe(player)
                .filter(variant -> !variant.isTemporaryFromSuit())
                .isPresent();
    }

    public static boolean hasNonSuitLatex(Player player) {
        return hasNonSuitTransfur(player) && ProcessTransfur.isPlayerLatex(player);
    }
}
