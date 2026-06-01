package net.stonenibbler.changed_survive_protocol.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class CSPKeyMappings {
    public static final String CATEGORY = "key.categories.changed_survive_protocol";

    public static final KeyMapping TOGGLE_DARK_LATEX_MASK = new KeyMapping(
            "key.changed_survive_protocol.toggle_dark_latex_mask",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY);

    private CSPKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_DARK_LATEX_MASK);
    }
}
