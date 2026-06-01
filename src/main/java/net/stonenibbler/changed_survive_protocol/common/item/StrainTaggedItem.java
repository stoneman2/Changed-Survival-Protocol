package net.stonenibbler.changed_survive_protocol.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StrainTaggedItem extends Item {
    private final boolean showAttunement;

    public StrainTaggedItem(Properties properties) {
        this(properties, false);
    }

    public StrainTaggedItem(Properties properties, boolean showAttunement) {
        super(properties);
        this.showAttunement = showAttunement;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        CSPStrainItems.addStrainTooltip(stack, level, tooltip);
        if (showAttunement) {
            CSPStrainItems.addAttunementTooltip(stack, tooltip);
        }
    }
}
