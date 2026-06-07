package net.stonenibbler.changed_survive_protocol.common.event;

import net.ltxprogrammer.changed.ability.IAbstractChangedEntity;
import net.ltxprogrammer.changed.entity.latex.LatexType;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.ltxprogrammer.changed.process.TransfurEvents;
import net.ltxprogrammer.changed.world.LatexCoverState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.stonenibbler.changed_survive_protocol.common.config.CSPConfig;
import net.stonenibbler.changed_survive_protocol.common.data.CSPCapabilities;
import net.stonenibbler.changed_survive_protocol.common.data.CSPPlayerData;
import net.stonenibbler.changed_survive_protocol.common.item.CSPStrainItems;
import net.stonenibbler.changed_survive_protocol.common.latex.LatexStrandManager;
import net.stonenibbler.changed_survive_protocol.common.network.CSPNetwork;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPItems;

public final class CSPLucidityEvents {
    private static final int SMALL_LATEX_COUNT = 3;
    private static final int MEDIUM_LATEX_COUNT = 8;
    private static final int LARGE_LATEX_COUNT = 16;
    private static final int NEST_LATEX_COUNT = 12;

    private CSPLucidityEvents() {
    }

    public static boolean tickLatexEnvironment(ServerPlayer player, CSPPlayerData data, double lucidityDrain) {
        if (!ProcessTransfur.isPlayerTransfurred(player) || player.tickCount % CSPConfig.COMMON.latexNeedIntervalTicks.get() != 0) {
            return false;
        }

        boolean dirty = false;
        boolean nearFriendlyLatex = false;
        double recovery = 0.0D;
        LatexStrandManager.Strand strand = LatexStrandManager.resolve(player).orElse(null);
        if (strand != null) {
            int count = countFriendlyLatex(player.level(), player.blockPosition(), strand.latexType(), 2, 1, 2);
            nearFriendlyLatex = count >= SMALL_LATEX_COUNT;
            boolean aquaticUnderwater = strand.family() == LatexStrandManager.Family.AQUATIC && player.isEyeInFluid(FluidTags.WATER);
            if (nearFriendlyLatex) {
                recovery += count >= LARGE_LATEX_COUNT
                        ? CSPConfig.COMMON.lucidityRecoveryNearLatexLarge.get()
                        : count >= MEDIUM_LATEX_COUNT
                        ? CSPConfig.COMMON.lucidityRecoveryNearLatexMedium.get()
                        : CSPConfig.COMMON.lucidityRecoveryNearLatexSmall.get();
            }
            if (aquaticUnderwater) {
                recovery += CSPConfig.COMMON.lucidityRecoveryAquaticUnderwater.get();
            }
        }

        double delta = recovery - Math.max(0.0D, lucidityDrain);
        if (delta != 0.0D) {
            data.addLucidity(delta);
            dirty = true;
        }

        if (nearFriendlyLatex && data.getLucidity() >= 70.0D && player.tickCount % 1200 == 0) {
            dirty |= attuneCarriedCulturedStrands(player, CSPConfig.COMMON.culturedStrandPassiveAttunement.get());
        }
        return dirty;
    }

    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !ProcessTransfur.isPlayerTransfurred(player) || !player.isSleepingLongEnough()) {
            return;
        }
        BlockPos bedPos = player.getSleepingPos().orElse(null);
        if (bedPos == null) {
            return;
        }
        LatexStrandManager.Strand strand = LatexStrandManager.resolve(player).orElse(null);
        if (strand == null || !isLatexNest(player.level(), bedPos, strand.latexType())) {
            return;
        }
        CSPCapabilities.get(player).ifPresent(data -> {
            data.addLucidity(CSPConfig.COMMON.lucidityRecoveryFromLatexNestSleep.get());
            attuneCarriedCulturedStrands(player, CSPConfig.COMMON.culturedStrandNestAttunement.get());
            CSPNetwork.sync(player, data);
        });
    }

    public static void onAssimilatedEntity(TransfurEvents.AssimilatedEntityEvent event) {
        rewardAssimilation(event.entity);
    }

    public static void onAbsorbedEntity(TransfurEvents.AbsorbedEntityEvent event) {
        rewardAssimilation(event.entity);
    }

    private static void rewardAssimilation(IAbstractChangedEntity source) {
        if (source.getEntity() instanceof Player player) {
            rewardAssimilation(player);
        }
    }

    public static void rewardAssimilation(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !ProcessTransfur.isPlayerTransfurred(player)) {
            return;
        }
        CSPCapabilities.get(serverPlayer).ifPresent(data -> {
            data.addLucidity(CSPConfig.COMMON.lucidityRecoveryFromAssimilation.get());
            attuneCarriedCulturedStrands(serverPlayer, CSPConfig.COMMON.culturedStrandAssimilationAttunement.get());
            CSPNetwork.sync(serverPlayer, data);
        });
    }

    public static boolean attuneCarriedCulturedStrands(Player player, double amount) {
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(CSPItems.CULTURED_LATEX_STRAND.get()) || !CSPStrainItems.matchesCurrentLatex(player, stack)) {
                continue;
            }
            double attunement = CSPStrainItems.attunement(stack) + amount;
            if (attunement >= 100.0D) {
                ItemStack dose = CSPStrainItems.withStrain(new ItemStack(CSPItems.STABILIZATION_DOSE.get()), CSPStrainItems.strainId(stack));
                player.getInventory().setItem(slot, dose);
            } else {
                CSPStrainItems.withAttunement(stack, attunement);
            }
            changed = true;
        }
        return changed;
    }

    private static boolean isLatexNest(Level level, BlockPos bedPos, LatexType playerType) {
        BlockPos center = bedPos;
        BlockState state = level.getBlockState(bedPos);
        if (state.getBlock() instanceof BedBlock && state.hasProperty(BedBlock.PART)) {
            Direction facing = state.getValue(BedBlock.FACING);
            center = state.getValue(BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD ? bedPos.relative(facing.getOpposite()) : bedPos;
        }
        return countFriendlyLatex(level, center, playerType, 2, 1, 2) >= NEST_LATEX_COUNT;
    }

    private static int countFriendlyLatex(Level level, BlockPos center, LatexType playerType, int xzRange, int downRange, int upRange) {
        int count = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dx = -xzRange; dx <= xzRange; dx++) {
            for (int dy = -downRange; dy <= upRange; dy++) {
                for (int dz = -xzRange; dz <= xzRange; dz++) {
                    mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (isFriendlyLatex(level, mutable, playerType)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static boolean isFriendlyLatex(Level level, BlockPos pos, LatexType playerType) {
        LatexCoverState coverState = LatexCoverState.getAt(level, pos);
        if (!coverState.isAir() && LatexStrandManager.samePhysicalLatex(playerType, coverState.getType())) {
            return true;
        }
        Block block = level.getBlockState(pos).getBlock();
        LatexType blockType = LatexStrandManager.latexTypeForBlock(block);
        return blockType != null && LatexStrandManager.samePhysicalLatex(playerType, blockType);
    }
}
