package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.ltxprogrammer.changed.entity.latex.SpreadingLatexType;
import net.ltxprogrammer.changed.init.ChangedBlocks;
import net.ltxprogrammer.changed.init.ChangedLatexTypes;
import net.ltxprogrammer.changed.world.LatexCoverState;
import net.minecraft.world.level.block.Block;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPBlocks;

final class LatexInfestationBlocks {
    private LatexInfestationBlocks() {
    }

    static LatexCoverState floorCover(LatexHeartBlock.Kind kind) {
        return latexState(kind).defaultCoverState().setValue(SpreadingLatexType.DOWN, true);
    }

    static Block heartBlock(LatexHeartBlock.Kind kind) {
        return kind == LatexHeartBlock.Kind.DARK ? CSPBlocks.DARK_LATEX_HEART.get() : CSPBlocks.WHITE_LATEX_HEART.get();
    }

    static Block nodeBlock(LatexHeartBlock.Kind kind) {
        return kind == LatexHeartBlock.Kind.DARK ? CSPBlocks.DARK_LATEX_NODE.get() : CSPBlocks.WHITE_LATEX_NODE.get();
    }

    static Block sourceBlock(LatexHeartBlock.Kind kind) {
        return kind == LatexHeartBlock.Kind.DARK ? ChangedBlocks.DARK_LATEX_BLOCK.get() : ChangedBlocks.WHITE_LATEX_BLOCK.get();
    }

    static Block fluidBlock(LatexHeartBlock.Kind kind) {
        return kind == LatexHeartBlock.Kind.DARK ? ChangedBlocks.DARK_LATEX_FLUID.get() : ChangedBlocks.WHITE_LATEX_FLUID.get();
    }

    static SpreadingLatexType latexState(LatexHeartBlock.Kind kind) {
        return kind == LatexHeartBlock.Kind.DARK ? ChangedLatexTypes.DARK_LATEX.get() : ChangedLatexTypes.WHITE_LATEX.get();
    }
}
