package net.stonenibbler.changed_survive_protocol.common.data;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class CSPPlayerDataProvider implements ICapabilitySerializable<CompoundTag> {
    private final CSPPlayerData data = new CSPPlayerData();
    private LazyOptional<CSPPlayerData> optional = LazyOptional.of(() -> data);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == CSPCapabilities.PLAYER_DATA) {
            return optional().cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.save();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.load(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }

    private LazyOptional<CSPPlayerData> optional() {
        if (!optional.isPresent()) {
            optional = LazyOptional.of(() -> data);
        }
        return optional;
    }
}
