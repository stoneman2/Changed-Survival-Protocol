package net.stonenibbler.changed_survive_protocol.common.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.stonenibbler.changed_survive_protocol.ChangedSurviveProtocol;

public final class CSPCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ChangedSurviveProtocol.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.changed_survive_protocol.main"))
            .icon(() -> new ItemStack(CSPBlocks.DARK_LATEX_HEART.get()))
            .displayItems((params, output) -> {
                output.accept(CSPBlocks.DARK_LATEX_HEART.get());
                output.accept(CSPBlocks.WHITE_LATEX_HEART.get());
                output.accept(CSPBlocks.DARK_LATEX_NODE.get());
                output.accept(CSPBlocks.WHITE_LATEX_NODE.get());
                output.accept(CSPBlocks.LATEX_CENTRIFUGE.get());
                output.accept(CSPItems.DISINFECTANT_WIPE.get());
                output.accept(CSPItems.DISINFECTANT_SPRAY.get());
                output.accept(CSPItems.LATEX_INHIBITOR.get());
                output.accept(CSPItems.SAMPLING_SCALPEL.get());
                output.accept(CSPItems.RAW_LATEX_SAMPLE.get());
                output.accept(CSPItems.IDENTIFIED_LATEX_STRAND.get());
                output.accept(CSPItems.STABILIZING_REAGENT.get());
                output.accept(CSPItems.STRAND_CURE_DOSE.get());
                output.accept(CSPItems.CULTURED_LATEX_STRAND.get());
                output.accept(CSPItems.STABILIZATION_DOSE.get());
            })
            .build());

    private CSPCreativeTabs() {
    }
}
