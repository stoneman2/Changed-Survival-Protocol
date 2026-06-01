package net.stonenibbler.changed_survive_protocol.common.block;

import net.ltxprogrammer.changed.init.ChangedItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.stonenibbler.changed_survive_protocol.common.blockentity.LatexCentrifugeBlockEntity;
import net.stonenibbler.changed_survive_protocol.common.item.CSPStrainItems;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPBlockEntities;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPItems;
import net.stonenibbler.changed_survive_protocol.common.util.CSPInventoryUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LatexCentrifugeBlock extends BaseEntityBlock {
    public LatexCentrifugeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof LatexCentrifugeBlockEntity blockEntity) {
                NetworkHooks.openScreen(serverPlayer, blockEntity, extra -> extra.writeBlockPos(pos));
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.is(CSPItems.STABILIZING_REAGENT.get())) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            return craftCulturedStrandFromInventory(player);
        }

        if (!held.is(CSPItems.IDENTIFIED_LATEX_STRAND.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        String strainId = CSPStrainItems.strainId(held);
        if (strainId.isBlank()) {
            player.displayClientMessage(Component.translatable("message.changed_survive_protocol.centrifuge.invalid_strand"), true);
            return InteractionResult.CONSUME;
        }

        return craftCulturedStrand(player, held, strainId);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LatexCentrifugeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, CSPBlockEntities.LATEX_CENTRIFUGE.get(), LatexCentrifugeBlockEntity::serverTick);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof LatexCentrifugeBlockEntity centrifuge) {
                Containers.dropContents(level, pos, centrifuge);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    private InteractionResult craftCulturedStrandFromInventory(Player player) {
        ItemStack strand = findIdentifiedStrand(player);
        if (strand.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.changed_survive_protocol.centrifuge.need_identified_strand"), true);
            return InteractionResult.CONSUME;
        }

        String strainId = CSPStrainItems.strainId(strand);
        if (strainId.isBlank()) {
            player.displayClientMessage(Component.translatable("message.changed_survive_protocol.centrifuge.invalid_strand"), true);
            return InteractionResult.CONSUME;
        }

        return craftCulturedStrand(player, strand, strainId);
    }

    private InteractionResult craftCulturedStrand(Player player, ItemStack strand, String strainId) {
        if (!hasCultureIngredients(player)) {
            player.displayClientMessage(Component.translatable("message.changed_survive_protocol.centrifuge.need_culture_ingredients"), true);
            return InteractionResult.CONSUME;
        }
        consumeLatexFeed(player);
        CSPInventoryUtil.consume(player, CSPItems.STABILIZING_REAGENT.get(), 1);
        if (!player.getAbilities().instabuild) {
            strand.shrink(1);
        }
        CSPInventoryUtil.giveOrDrop(player, CSPStrainItems.withAttunement(CSPStrainItems.withStrain(new ItemStack(CSPItems.CULTURED_LATEX_STRAND.get()), strainId), 0.0D));
        player.displayClientMessage(Component.translatable("message.changed_survive_protocol.centrifuge.cultured_created"), true);
        return InteractionResult.CONSUME;
    }

    private static boolean hasCultureIngredients(Player player) {
        return hasLatexFeed(player) && CSPInventoryUtil.has(player, CSPItems.STABILIZING_REAGENT.get(), 1);
    }

    private static boolean hasLatexFeed(Player player) {
        return CSPInventoryUtil.has(player, ChangedItems.LATEX_BASE.get(), 1)
                || CSPInventoryUtil.has(player, ChangedItems.DARK_LATEX_GOO.get(), 1)
                || CSPInventoryUtil.has(player, ChangedItems.WHITE_LATEX_GOO.get(), 1);
    }

    private static ItemStack findIdentifiedStrand(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(CSPItems.IDENTIFIED_LATEX_STRAND.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void consumeLatexFeed(Player player) {
        if (player.getAbilities().instabuild) {
            return;
        }
        Item[] feedPriority = new Item[]{ChangedItems.LATEX_BASE.get(), ChangedItems.DARK_LATEX_GOO.get(), ChangedItems.WHITE_LATEX_GOO.get()};
        for (Item item : feedPriority) {
            if (CSPInventoryUtil.consume(player, item, 1)) {
                return;
            }
        }
    }
}
