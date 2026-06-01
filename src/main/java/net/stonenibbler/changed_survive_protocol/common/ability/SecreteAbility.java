package net.stonenibbler.changed_survive_protocol.common.ability;

import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.ability.IAbstractChangedEntity;
import net.ltxprogrammer.changed.ability.SimpleAbility;
import net.ltxprogrammer.changed.entity.latex.SpreadingLatexType;
import net.ltxprogrammer.changed.init.ChangedTags;
import net.ltxprogrammer.changed.world.LatexCoverState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import net.stonenibbler.changed_survive_protocol.common.infestation.LatexInfestationManager;
import net.stonenibbler.changed_survive_protocol.common.latex.LatexStrandManager;

import java.util.Collection;
import java.util.List;

public class SecreteAbility extends SimpleAbility {
    private static final float EXHAUSTION_COST = 2.0F;
    private static final float MINIMUM_FOOD = 2.0F;
    private static final double REACH = 5.0D;
    private static final Collection<Component> DESCRIPTION = List.of(
            Component.translatable("ability.changed_survive_protocol.secrete.desc"),
            Component.translatable("ability.changed_survive_protocol.secrete.desc.resource")
    );

    @Override
    public boolean canUse(IAbstractChangedEntity entity) {
        return entity.getEntity() instanceof Player
                && LatexStrandManager.resolve(entity.getEntity()).map(LatexStrandManager.Strand::canSecrete).orElse(false)
                && (entity.isCreative() || entity.getFoodLevel() > MINIMUM_FOOD);
    }

    @Override
    public void startUsing(IAbstractChangedEntity entity) {
        if (!(entity.getEntity() instanceof Player player)) {
            return;
        }

        Level level = entity.getLevel();
        if (level.isClientSide) {
            return;
        }

        SpreadingLatexType latexType = LatexStrandManager.resolve(player)
                .map(LatexStrandManager.Strand::secretionType)
                .orElse(null);
        if (latexType == null) {
            player.displayClientMessage(Component.translatable("message.changed_survive_protocol.secrete.invalid_latex"), true);
            return;
        }

        BlockHitResult hit = pickTarget(player);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(Component.translatable("message.changed_survive_protocol.secrete.no_target"), true);
            return;
        }

        InteractionResult result = placeLatex(level, player, latexType, hit);
        if (!result.consumesAction()) {
            player.displayClientMessage(Component.translatable("message.changed_survive_protocol.secrete.blocked"), true);
            return;
        }

        if (!entity.isCreative()) {
            entity.causeFoodExhaustion(EXHAUSTION_COST);
        }
    }

    @Override
    public int getCoolDown(IAbstractChangedEntity entity) {
        return 8;
    }

    @Override
    public Collection<Component> getAbilityDescription(IAbstractChangedEntity entity) {
        return DESCRIPTION;
    }

    @Nullable
    private static BlockHitResult pickTarget(Player player) {
        HitResult result = player.pick(REACH, 0.0F, false);
        return result instanceof BlockHitResult blockHit ? blockHit : null;
    }

    private static InteractionResult placeLatex(Level level, Player player, SpreadingLatexType latexType, BlockHitResult hit) {
        BlockPos clickedPos = hit.getBlockPos();
        Direction clickedFace = hit.getDirection();
        BlockState clickedState = level.getBlockState(clickedPos);
        if (clickedState.is(ChangedTags.Blocks.DENY_LATEX_COVER)) {
            return InteractionResult.FAIL;
        }

        BlockPos coverPos = clickedState.isFaceSturdy(level, clickedPos, clickedFace, SupportType.FULL)
                ? clickedPos.relative(clickedFace)
                : clickedPos;
        if (level.isOutsideBuildHeight(coverPos)) {
            return InteractionResult.FAIL;
        }

        BlockState originalState = level.getBlockState(coverPos);
        if (!originalState.getFluidState().isEmpty() || originalState.is(ChangedTags.Blocks.DENY_LATEX_COVER)) {
            return InteractionResult.FAIL;
        }

        LatexCoverState originalCover = LatexCoverState.getAt(level, coverPos);
        LatexCoverState plannedCover = latexType.spreadState(level, coverPos, latexType.sourceCoverState());
        if (!hasVisibleFace(plannedCover)) {
            return InteractionResult.FAIL;
        }

        var event = new SpreadingLatexType.CoveringBlockEvent(latexType, originalState, originalState, plannedCover, coverPos, level);
        latexType.defaultCoverBehavior(event);
        if (Changed.postModEvent(event)) {
            return InteractionResult.FAIL;
        }
        if (event.originalState == event.getPlannedState() && event.plannedCoverState == originalCover) {
            return InteractionResult.FAIL;
        }

        level.setBlockAndUpdate(event.blockPos, event.getPlannedState());
        LatexCoverState.setAtAndUpdate(level, event.blockPos, event.plannedCoverState);

        var soundType = event.plannedCoverState.getSoundType(level, event.blockPos, player);
        if (soundType != null) {
            level.playSound(player, event.blockPos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
        }

        event.getPostProcess().accept(level, coverPos);
        if (level instanceof ServerLevel serverLevel) {
            LatexInfestationManager.markPlayerSecretion(serverLevel, event.blockPos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean hasVisibleFace(LatexCoverState state) {
        for (BooleanProperty face : SpreadingLatexType.FACES.values()) {
            if (state.getValue(face)) {
                return true;
            }
        }
        return false;
    }
}
