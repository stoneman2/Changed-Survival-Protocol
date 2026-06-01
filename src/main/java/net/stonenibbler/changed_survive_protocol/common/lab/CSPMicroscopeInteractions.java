package net.stonenibbler.changed_survive_protocol.common.lab;

import net.ltxprogrammer.changed.item.LatexSyringe;
import net.ltxprogrammer.changed.item.Syringe;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.stonenibbler.changed_survive_protocol.common.item.CSPStrainItems;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPItems;
import net.stonenibbler.changed_survive_protocol.common.util.CSPInventoryUtil;

public final class CSPMicroscopeInteractions {
    private CSPMicroscopeInteractions() {
    }

    public static InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() || !isSupportedInput(held)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        String strainId = strainIdFrom(held);
        if (strainId.isBlank()) {
            player.displayClientMessage(Component.translatable("message.changed_survive_protocol.microscope.invalid_sample"), true);
            return InteractionResult.CONSUME;
        }

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        CSPInventoryUtil.giveOrDrop(player, CSPStrainItems.withStrain(new ItemStack(CSPItems.IDENTIFIED_LATEX_STRAND.get()), strainId));
        player.displayClientMessage(Component.translatable("message.changed_survive_protocol.microscope.identified"), true);
        return InteractionResult.CONSUME;
    }

    public static boolean isSupportedInput(ItemStack stack) {
        return stack.is(CSPItems.RAW_LATEX_SAMPLE.get()) || stack.getItem() instanceof LatexSyringe;
    }

    public static String strainIdFrom(ItemStack stack) {
        if (stack.is(CSPItems.RAW_LATEX_SAMPLE.get())) {
            return CSPStrainItems.strainId(stack);
        }
        if (stack.getItem() instanceof LatexSyringe) {
            var variant = Syringe.getVariant(stack);
            if (variant != null) {
                return variant.getFormId().toString();
            }
        }
        return "";
    }
}
