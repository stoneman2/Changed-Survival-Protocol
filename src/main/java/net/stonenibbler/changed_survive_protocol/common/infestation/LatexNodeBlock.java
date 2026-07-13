package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LatexNodeBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 10.0D, 13.0D);
    private final LatexHeartBlock.Kind kind;

    public LatexNodeBlock(LatexHeartBlock.Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public LatexHeartBlock.Kind getKind() {
        return kind;
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float progress = super.getDestroyProgress(state, player, level, pos);
        return player.getMainHandItem().is(ItemTags.SHOVELS) ? progress * 4.0F : progress;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            LatexInfestationManager.removeNode((ServerLevel)level, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) != 0) {
            return;
        }
        level.addParticle(kind == LatexHeartBlock.Kind.DARK ? net.minecraft.core.particles.ParticleTypes.SMOKE : net.minecraft.core.particles.ParticleTypes.GLOW,
                pos.getX() + 0.5D, pos.getY() + 0.75D, pos.getZ() + 0.5D, 0.0D, 0.015D, 0.0D);
    }
}
