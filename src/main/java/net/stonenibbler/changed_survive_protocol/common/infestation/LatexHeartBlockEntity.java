package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPBlockEntities;

import java.util.UUID;

public class LatexHeartBlockEntity extends BlockEntity {
    private UUID heartId;

    public LatexHeartBlockEntity(BlockPos pos, BlockState state) {
        super(CSPBlockEntities.LATEX_HEART.get(), pos, state);
    }

    public UUID getHeartId() {
        if (heartId == null) {
            heartId = UUID.randomUUID();
            setChanged();
        }
        return heartId;
    }

    public void setHeartId(UUID heartId) {
        this.heartId = heartId;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (heartId != null) {
            tag.putUUID("heartId", heartId);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        heartId = tag.hasUUID("heartId") ? tag.getUUID("heartId") : null;
    }
}
