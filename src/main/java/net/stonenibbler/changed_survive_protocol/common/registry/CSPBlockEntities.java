package net.stonenibbler.changed_survive_protocol.common.registry;

import net.ltxprogrammer.changed.init.ChangedBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.stonenibbler.changed_survive_protocol.ChangedSurviveProtocol;
import net.stonenibbler.changed_survive_protocol.common.blockentity.LatexCentrifugeBlockEntity;
import net.stonenibbler.changed_survive_protocol.common.blockentity.MicroscopeBlockEntity;
import net.stonenibbler.changed_survive_protocol.common.infestation.LatexHeartBlockEntity;

public final class CSPBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ChangedSurviveProtocol.MODID);

    public static final RegistryObject<BlockEntityType<LatexHeartBlockEntity>> LATEX_HEART = BLOCK_ENTITIES.register("latex_heart", () -> BlockEntityType.Builder.of(
            LatexHeartBlockEntity::new,
            CSPBlocks.DARK_LATEX_HEART.get(),
            CSPBlocks.WHITE_LATEX_HEART.get()).build(null));

    public static final RegistryObject<BlockEntityType<MicroscopeBlockEntity>> MICROSCOPE = BLOCK_ENTITIES.register("microscope", () -> BlockEntityType.Builder.of(
            MicroscopeBlockEntity::new,
            ChangedBlocks.MICROSCOPE.get()).build(null));

    public static final RegistryObject<BlockEntityType<LatexCentrifugeBlockEntity>> LATEX_CENTRIFUGE = BLOCK_ENTITIES.register("latex_centrifuge", () -> BlockEntityType.Builder.of(
            LatexCentrifugeBlockEntity::new,
            CSPBlocks.LATEX_CENTRIFUGE.get()).build(null));

    private CSPBlockEntities() {
    }
}
