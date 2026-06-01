package net.stonenibbler.changed_survive_protocol.client;

import net.stonenibbler.changed_survive_protocol.common.network.SyncCSPPlayerDataPacket;

public final class CSPClientData {
    private static SyncCSPPlayerDataPacket current;
    private static boolean darkLatexMaskOverlayVisible = true;

    private CSPClientData() {
    }

    // Gets sets
    public static void set(SyncCSPPlayerDataPacket packet) {
        current = packet;
    }

    public static SyncCSPPlayerDataPacket get() {
        return current;
    }

    public static boolean isDarkLatexMaskOverlayVisible() {
        return darkLatexMaskOverlayVisible;
    }

    public static void toggleDarkLatexMaskOverlay() {
        darkLatexMaskOverlayVisible = !darkLatexMaskOverlayVisible;
    }
}
