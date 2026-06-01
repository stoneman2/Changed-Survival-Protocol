package net.stonenibbler.changed_survive_protocol.common.registry;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.stonenibbler.changed_survive_protocol.ChangedSurviveProtocol;
import net.stonenibbler.changed_survive_protocol.common.config.CSPConfig;
import net.stonenibbler.changed_survive_protocol.common.item.CoverageCleanerItem;
import net.stonenibbler.changed_survive_protocol.common.item.LatexInhibitorItem;
import net.stonenibbler.changed_survive_protocol.common.item.SamplingScalpelItem;
import net.stonenibbler.changed_survive_protocol.common.item.StabilizationDoseItem;
import net.stonenibbler.changed_survive_protocol.common.item.StrandCureDoseItem;
import net.stonenibbler.changed_survive_protocol.common.item.StrainTaggedItem;
import net.stonenibbler.changed_survive_protocol.common.item.TooltipItem;

public final class CSPItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ChangedSurviveProtocol.MODID);

    public static final RegistryObject<Item> DISINFECTANT_WIPE = ITEMS.register("disinfectant_wipe", () -> new CoverageCleanerItem(new Item.Properties().stacksTo(16), () -> CSPConfig.COMMON.disinfectantWipeCoverageRemoval.get()));
    public static final RegistryObject<Item> DISINFECTANT_SPRAY = ITEMS.register("disinfectant_spray", () -> new CoverageCleanerItem(new Item.Properties().stacksTo(8), () -> CSPConfig.COMMON.disinfectantSprayCoverageRemoval.get()));
    public static final RegistryObject<Item> LATEX_INHIBITOR = ITEMS.register("latex_inhibitor", () -> new LatexInhibitorItem(new Item.Properties().stacksTo(8)));
    public static final RegistryObject<Item> SAMPLING_SCALPEL = ITEMS.register("sampling_scalpel", () -> new SamplingScalpelItem(new Item.Properties().durability(12)));
    public static final RegistryObject<Item> RAW_LATEX_SAMPLE = ITEMS.register("raw_latex_sample", () -> new StrainTaggedItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> IDENTIFIED_LATEX_STRAND = ITEMS.register("identified_latex_strand", () -> new StrainTaggedItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> STABILIZING_REAGENT = ITEMS.register("stabilizing_reagent", () -> new TooltipItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> STRAND_CURE_DOSE = ITEMS.register("strand_cure_dose", () -> new StrandCureDoseItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> CULTURED_LATEX_STRAND = ITEMS.register("cultured_latex_strand", () -> new StrainTaggedItem(new Item.Properties().stacksTo(1), true));
    public static final RegistryObject<Item> STABILIZATION_DOSE = ITEMS.register("stabilization_dose", () -> new StabilizationDoseItem(new Item.Properties().stacksTo(1)));

    private CSPItems() {
    }
}
