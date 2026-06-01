package net.stonenibbler.changed_survive_protocol.common.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.stonenibbler.changed_survive_protocol.ChangedSurviveProtocol;
import net.stonenibbler.changed_survive_protocol.common.block.LatexCentrifugeBlock;
import net.stonenibbler.changed_survive_protocol.common.infestation.LatexHeartBlock;
import net.stonenibbler.changed_survive_protocol.common.infestation.LatexNodeBlock;
import net.stonenibbler.changed_survive_protocol.common.item.TooltipBlockItem;

import java.util.function.Supplier;

public final class CSPBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ChangedSurviveProtocol.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ChangedSurviveProtocol.MODID);

    public static final RegistryObject<LatexHeartBlock> DARK_LATEX_HEART = register("dark_latex_heart", () -> new LatexHeartBlock(LatexHeartBlock.Kind.DARK, heartProperties(1)));
    public static final RegistryObject<LatexHeartBlock> WHITE_LATEX_HEART = register("white_latex_heart", () -> new LatexHeartBlock(LatexHeartBlock.Kind.WHITE, heartProperties(1)));
    public static final RegistryObject<LatexNodeBlock> DARK_LATEX_NODE = register("dark_latex_node", () -> new LatexNodeBlock(LatexHeartBlock.Kind.DARK, nodeProperties()));
    public static final RegistryObject<LatexNodeBlock> WHITE_LATEX_NODE = register("white_latex_node", () -> new LatexNodeBlock(LatexHeartBlock.Kind.WHITE, nodeProperties()));
    public static final RegistryObject<LatexCentrifugeBlock> LATEX_CENTRIFUGE = register("latex_centrifuge", () -> new LatexCentrifugeBlock(machineProperties()));

    private CSPBlocks() {
    }

    private static BlockBehaviour.Properties heartProperties(int light) {
        return BlockBehaviour.Properties.of().sound(SoundType.SLIME_BLOCK).strength(4.0F, 8.0F).lightLevel(state -> light).noOcclusion();
    }

    private static BlockBehaviour.Properties nodeProperties() {
        return BlockBehaviour.Properties.of().sound(SoundType.SLIME_BLOCK).strength(1.6F, 3.0F).noOcclusion();
    }

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(3.0F, 6.0F).noOcclusion();
    }

    private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block) {
        RegistryObject<T> object = BLOCKS.register(name, block);
        ITEMS.register(name, () -> new TooltipBlockItem(object.get(), new Item.Properties()));
        return object;
    }
}
