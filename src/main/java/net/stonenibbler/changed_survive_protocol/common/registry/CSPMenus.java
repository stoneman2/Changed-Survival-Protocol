package net.stonenibbler.changed_survive_protocol.common.registry;

import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.stonenibbler.changed_survive_protocol.ChangedSurviveProtocol;
import net.stonenibbler.changed_survive_protocol.common.menu.LatexCentrifugeMenu;
import net.stonenibbler.changed_survive_protocol.common.menu.MicroscopeMenu;

public final class CSPMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ChangedSurviveProtocol.MODID);

    public static final RegistryObject<MenuType<MicroscopeMenu>> MICROSCOPE = register("microscope", MicroscopeMenu::new);
    public static final RegistryObject<MenuType<LatexCentrifugeMenu>> LATEX_CENTRIFUGE = register("latex_centrifuge", LatexCentrifugeMenu::new);

    private CSPMenus() {
    }

    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> register(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> new MenuType<>(factory, FeatureFlagSet.of()));
    }
}
