package net.stonenibbler.changed_survive_protocol.common.data;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.stonenibbler.changed_survive_protocol.ChangedSurviveProtocol;

public final class CSPCapabilities {
    public static final Capability<CSPPlayerData> PLAYER_DATA = CapabilityManager.get(new CapabilityToken<>() {});

    private CSPCapabilities() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(CSPPlayerData.class);
    }

    public static void attachPlayerData(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) {
            return;
        }

        CSPPlayerDataProvider provider = new CSPPlayerDataProvider();
        event.addCapability(ChangedSurviveProtocol.resource("player_data"), provider);
        event.addListener(provider::invalidate);
    }

    public static LazyOptional<CSPPlayerData> get(Player player) {
        return player.getCapability(PLAYER_DATA);
    }
}
