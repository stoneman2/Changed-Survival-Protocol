package net.stonenibbler.changed_survive_protocol.mixin;

import net.ltxprogrammer.changed.block.Microscope;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.stonenibbler.changed_survive_protocol.common.blockentity.MicroscopeBlockEntity;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPBlockEntities;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Microscope.class, remap = false)
public abstract class ChangedMicroscopeBlockEntityMixin implements EntityBlock {
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MicroscopeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == CSPBlockEntities.MICROSCOPE.get()
                ? (tickerLevel, pos, tickerState, blockEntity) -> MicroscopeBlockEntity.serverTick(tickerLevel, pos, tickerState, (MicroscopeBlockEntity)blockEntity)
                : null;
    }
}
