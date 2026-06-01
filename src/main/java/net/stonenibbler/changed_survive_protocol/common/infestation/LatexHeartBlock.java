package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class LatexHeartBlock extends Block implements EntityBlock {
    public static final BooleanProperty PROTECTED = BooleanProperty.create("protected");
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 15.0D, 14.0D);
    private final Kind kind;

    public LatexHeartBlock(Kind kind, Properties properties) {
        super(properties.randomTicks());
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(PROTECTED, false));
    }

    public Kind getKind() {
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
        if (state.getValue(PROTECTED)) {
            return 0.0F;
        }

        float progress = super.getDestroyProgress(state, player, level, pos);
        return player.getMainHandItem().is(ItemTags.SHOVELS) ? progress * 4.0F : progress;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PROTECTED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LatexHeartBlockEntity(pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!level.isClientSide && oldState.getBlock() != state.getBlock()) {
            LatexInfestationManager.ensureHeart((ServerLevel)level, pos, kind);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            LatexInfestationManager.removeHeart((ServerLevel)level, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) != 0) {
            return;
        }
        double x = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.8D;
        double y = pos.getY() + 0.8D + random.nextDouble() * 0.7D;
        double z = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.8D;
        level.addParticle(kind == Kind.DARK ? net.minecraft.core.particles.ParticleTypes.SQUID_INK : net.minecraft.core.particles.ParticleTypes.END_ROD,
                x, y, z, 0.0D, 0.025D, 0.0D);
    }

    public enum Kind {
        DARK,
        WHITE
    }
}
