package net.stonenibbler.changed_survive_protocol.common.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.stonenibbler.changed_survive_protocol.ChangedSurviveProtocol;
import net.stonenibbler.changed_survive_protocol.common.data.CSPPlayerData;

public final class CSPNetwork {
    private static final String PROTOCOL_VERSION = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ChangedSurviveProtocol.MODID, "network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int nextId;

    private CSPNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(nextId++, SyncCSPPlayerDataPacket.class,
                SyncCSPPlayerDataPacket::encode,
                SyncCSPPlayerDataPacket::decode,
                SyncCSPPlayerDataPacket::handle);
    }

    public static void sync(ServerPlayer player, CSPPlayerData data) {
        if (player.connection == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), SyncCSPPlayerDataPacket.from(data));
    }
}
