package net.stonenibbler.changed_survive_protocol.common.item;

import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.init.ChangedRegistry;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class CSPStrainItems {
    public static final String STRAIN_ID = "strainId";
    public static final String LATEX_TYPE_ID = "latexTypeId";
    public static final String ATTUNEMENT = "attunement";

    private CSPStrainItems() {
    }

    public static ItemStack withStrain(ItemStack stack, String strainId) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(STRAIN_ID, strainId == null ? "" : strainId);
        return stack;
    }

    public static ItemStack withAttunement(ItemStack stack, double attunement) {
        stack.getOrCreateTag().putDouble(ATTUNEMENT, clamp(attunement));
        return stack;
    }

    public static String strainId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(STRAIN_ID);
    }

    public static double attunement(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0.0D : clamp(tag.getDouble(ATTUNEMENT));
    }

    public static boolean matchesCurrentLatex(Player player, ItemStack stack) {
        String strainId = strainId(stack);
        if (strainId.isBlank()) {
            return false;
        }
        return ProcessTransfur.getPlayerTransfurVariantSafe(player)
                .map(variant -> strainId.equals(variant.getParent().getFormId().toString()))
                .orElse(false);
    }

    public static boolean hasStrain(ItemStack stack) {
        return !strainId(stack).isBlank();
    }

    public static void addStrainTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip) {
        String strainId = strainId(stack);
        if (strainId.isBlank()) {
            tooltip.add(Component.translatable("tooltip.changed_survive_protocol.no_strain").withStyle(ChatFormatting.GRAY));
            return;
        }

        ResourceLocation id = ResourceLocation.tryParse(strainId);
        TransfurVariant<?> variant = id == null ? null : ChangedRegistry.TRANSFUR_VARIANT.get().getValue(id);
        if (variant != null) {
            tooltip.add(Component.translatable("tooltip.changed_survive_protocol.strain", Component.translatable(variant.getEntityType().getDescriptionId())).withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("tooltip.changed_survive_protocol.strain", strainId).withStyle(ChatFormatting.AQUA));
        }
    }

    public static void addAttunementTooltip(ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.changed_survive_protocol.attunement", String.format(java.util.Locale.ROOT, "%.0f", attunement(stack))).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static double clamp(double value) {
        if (Double.isNaN(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(100.0D, value));
    }
}
