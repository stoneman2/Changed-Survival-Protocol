package net.stonenibbler.changed_survive_protocol;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.stonenibbler.changed_survive_protocol.client.CSPClientEvents;
import net.stonenibbler.changed_survive_protocol.common.command.CSPCommands;
import net.stonenibbler.changed_survive_protocol.common.collapse.FeralBodySpawner;
import net.stonenibbler.changed_survive_protocol.common.config.CSPConfig;
import net.stonenibbler.changed_survive_protocol.common.data.CSPCapabilities;
import net.stonenibbler.changed_survive_protocol.common.event.CSPLucidityEvents;
import net.stonenibbler.changed_survive_protocol.common.event.CSPMicroscopeEvents;
import net.stonenibbler.changed_survive_protocol.common.event.CSPPlayerEvents;
import net.stonenibbler.changed_survive_protocol.common.event.CSPTransfurEvents;
import net.stonenibbler.changed_survive_protocol.common.gamerule.CSPGameRules;
import net.stonenibbler.changed_survive_protocol.common.infestation.LatexInfestationManager;
import net.stonenibbler.changed_survive_protocol.common.network.CSPNetwork;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPAbilities;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPBlockEntities;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPBlocks;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPCreativeTabs;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPItems;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPMenus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ChangedSurviveProtocol.MODID)
public class ChangedSurviveProtocol {
    public static final String MODID = "changed_survive_protocol";
    public static final Logger LOGGER = LogManager.getLogger(ChangedSurviveProtocol.class);

    public ChangedSurviveProtocol(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        CSPConfig.register(net.minecraftforge.fml.ModLoadingContext.get());
        CSPGameRules.touch();
        CSPAbilities.ABILITIES.register(modBus);
        CSPBlocks.BLOCKS.register(modBus);
        CSPBlocks.ITEMS.register(modBus);
        CSPItems.ITEMS.register(modBus);
        CSPBlockEntities.BLOCK_ENTITIES.register(modBus);
        CSPMenus.MENUS.register(modBus);
        CSPCreativeTabs.TABS.register(modBus);
        modBus.addListener(CSPCapabilities::registerCapabilities);
        modBus.addListener(this::commonSetup);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CSPClientEvents.register(modBus));

        MinecraftForge.EVENT_BUS.addGenericListener(net.minecraft.world.entity.Entity.class, CSPPlayerEvents::attachCapabilities);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, CSPPlayerEvents::onPlayerDeath);
        MinecraftForge.EVENT_BUS.addListener(CSPPlayerEvents::onPlayerClone);
        MinecraftForge.EVENT_BUS.addListener(CSPPlayerEvents::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(CSPPlayerEvents::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, CSPPlayerEvents::onPlayerRespawn);
        MinecraftForge.EVENT_BUS.addListener(CSPPlayerEvents::onPlayerChangedDimension);
        MinecraftForge.EVENT_BUS.addListener(CSPPlayerEvents::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(CSPPlayerEvents::onFoodFinished);
        MinecraftForge.EVENT_BUS.addListener(CSPLucidityEvents::onPlayerWakeUp);
        MinecraftForge.EVENT_BUS.addListener(CSPLucidityEvents::onAssimilatedEntity);
        MinecraftForge.EVENT_BUS.addListener(CSPLucidityEvents::onAbsorbedEntity);
        MinecraftForge.EVENT_BUS.addListener(CSPMicroscopeEvents::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(CSPMicroscopeEvents::onBlockBreak);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, CSPPlayerEvents::onLivingChangeTarget);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, CSPTransfurEvents::onUntransfurPlayer);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, CSPTransfurEvents::onKeepConscious);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, CSPTransfurEvents::onImmediateTransfurDecision);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, CSPTransfurEvents::onRightClickItem);
        MinecraftForge.EVENT_BUS.addListener(CSPTransfurEvents::onEntityVariantAssigned);
        MinecraftForge.EVENT_BUS.addListener(CSPTransfurEvents::onChangedVariant);
        MinecraftForge.EVENT_BUS.addListener(LatexInfestationManager::onLevelTick);
        MinecraftForge.EVENT_BUS.addListener(LatexInfestationManager::onChunkLoad);
        MinecraftForge.EVENT_BUS.addListener(LatexInfestationManager::onEntityLeaveLevel);
        MinecraftForge.EVENT_BUS.addListener(LatexInfestationManager::onEntityJoinLevel);
        MinecraftForge.EVENT_BUS.addListener(FeralBodySpawner::onEntityLeaveLevel);
        MinecraftForge.EVENT_BUS.addListener(FeralBodySpawner::onEntityJoinLevel);
        MinecraftForge.EVENT_BUS.addListener(LatexInfestationManager::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, LatexInfestationManager::onSpawnPlacementCheck);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, LatexInfestationManager::onExplosionDetonate);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, LatexInfestationManager::onBlockBreak);
        MinecraftForge.EVENT_BUS.addListener(LatexInfestationManager::onBlockPlace);
        MinecraftForge.EVENT_BUS.addListener(LatexInfestationManager::onNeighborNotify);
        MinecraftForge.EVENT_BUS.addListener(LatexInfestationManager::onLevelUnload);
        MinecraftForge.EVENT_BUS.addListener(LatexInfestationManager::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(CSPCommands::register);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CSPNetwork::register);
    }

    public static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
