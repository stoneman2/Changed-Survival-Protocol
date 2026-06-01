package net.stonenibbler.changed_survive_protocol.common.damage;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import net.stonenibbler.changed_survive_protocol.ChangedSurviveProtocol;

public final class CSPDamageSources {
    public static final ResourceKey<DamageType> LUCIDITY_COLLAPSE = ResourceKey.create(Registries.DAMAGE_TYPE, ChangedSurviveProtocol.resource("lucidity_collapse"));

    private CSPDamageSources() {
    }

    public static DamageSource lucidityCollapse(Level level) {
        Holder<DamageType> holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(LUCIDITY_COLLAPSE);
        return new DamageSource(holder);
    }
}
