package net.stonenibbler.changed_survive_protocol.common.event;

import net.ltxprogrammer.changed.block.Microscope;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.network.NetworkHooks;
import net.stonenibbler.changed_survive_protocol.common.blockentity.MicroscopeBlockEntity;
import net.stonenibbler.changed_survive_protocol.common.lab.CSPMicroscopeInteractions;

public final class CSPMicroscopeEvents {
    private CSPMicroscopeEvents() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof Microscope)) {
            return;
        }

        Level level = event.getLevel();
        if (level.isClientSide) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            MicroscopeBlockEntity blockEntity = microscopeBlockEntity(level, event.getPos());
            if (blockEntity != null) {
                NetworkHooks.openScreen(serverPlayer, blockEntity, extra -> extra.writeBlockPos(event.getPos()));
                event.setCancellationResult(InteractionResult.CONSUME);
                event.setCanceled(true);
                return;
            }
        }

        InteractionResult result = CSPMicroscopeInteractions.use(level, event.getEntity(), event.getHand());
        event.setCancellationResult(result);
        event.setCanceled(true);
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getState().getBlock() instanceof Microscope)) {
            return;
        }
        if (level.getBlockEntity(event.getPos()) instanceof MicroscopeBlockEntity blockEntity) {
            Containers.dropContents(level, event.getPos(), blockEntity);
            blockEntity.clearContent();
        }
    }

    private static MicroscopeBlockEntity microscopeBlockEntity(Level level, BlockPos pos) {
        BlockEntity existing = level.getBlockEntity(pos);
        if (existing instanceof MicroscopeBlockEntity microscope) {
            return microscope;
        }
        if (level.isClientSide) {
            return null;
        }
        BlockState state = level.getBlockState(pos);
        MicroscopeBlockEntity microscope = new MicroscopeBlockEntity(pos, state);
        level.setBlockEntity(microscope);
        return microscope;
    }
}
