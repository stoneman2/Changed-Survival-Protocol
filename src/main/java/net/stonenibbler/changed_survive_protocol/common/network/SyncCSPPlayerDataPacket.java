package net.stonenibbler.changed_survive_protocol.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.stonenibbler.changed_survive_protocol.client.CSPClientData;
import net.stonenibbler.changed_survive_protocol.common.data.CSPPlayerData;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public record SyncCSPPlayerDataPacket(double infectionPercent,
                                      double coverage,
                                      boolean infected,
                                      String strainId,
                                      int suppressantTicks,
                                      double lucidity,
                                      boolean lucidityActive,
                                      boolean unstableLatex,
                                       boolean stabilizedLatex,
                                       int unstableLatexTicks,
                                       double lucidityDrainMultiplier,
                                       String settledStrainId,
                                       Optional<UUID> feralSelfUuid,
                                       int collapseCount) {
    public static SyncCSPPlayerDataPacket from(CSPPlayerData data) {
        return new SyncCSPPlayerDataPacket(
                data.getInfectionPercent(),
                data.getCoverage(),
                data.isInfected(),
                data.getStrainId(),
                data.getSuppressantTicks(),
                data.getLucidity(),
                data.isLucidityActive(),
                data.isUnstableLatex(),
                data.isStabilizedLatex(),
                data.getUnstableLatexTicks(),
                data.getLucidityDrainMultiplier(),
                data.getSettledStrainId(),
                Optional.ofNullable(data.getFeralSelfUuid()),
                data.getCollapseCount());
    }

    public static void encode(SyncCSPPlayerDataPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.infectionPercent);
        buffer.writeDouble(packet.coverage);
        buffer.writeBoolean(packet.infected);
        buffer.writeUtf(packet.strainId);
        buffer.writeVarInt(packet.suppressantTicks);
        buffer.writeDouble(packet.lucidity);
        buffer.writeBoolean(packet.lucidityActive);
        buffer.writeBoolean(packet.unstableLatex);
        buffer.writeBoolean(packet.stabilizedLatex);
        buffer.writeVarInt(packet.unstableLatexTicks);
        buffer.writeDouble(packet.lucidityDrainMultiplier);
        buffer.writeUtf(packet.settledStrainId);
        buffer.writeOptional(packet.feralSelfUuid, FriendlyByteBuf::writeUUID);
        buffer.writeVarInt(packet.collapseCount);
    }

    public static SyncCSPPlayerDataPacket decode(FriendlyByteBuf buffer) {
        return new SyncCSPPlayerDataPacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readBoolean(),
                buffer.readUtf(),
                buffer.readVarInt(),
                buffer.readDouble(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readDouble(),
                buffer.readUtf(),
                buffer.readOptional(FriendlyByteBuf::readUUID),
                buffer.readVarInt());
    }

    public static void handle(SyncCSPPlayerDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> CSPClientData.set(packet));
        context.setPacketHandled(true);
    }
}
