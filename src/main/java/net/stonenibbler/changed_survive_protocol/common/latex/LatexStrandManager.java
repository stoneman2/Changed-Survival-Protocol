package net.stonenibbler.changed_survive_protocol.common.latex;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.beast.AquaticEntity;
import net.ltxprogrammer.changed.entity.latex.LatexType;
import net.ltxprogrammer.changed.entity.latex.SpreadingLatexType;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.entity.variant.TransfurVariantInstance;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.init.ChangedLatexTypes;
import net.ltxprogrammer.changed.init.ChangedTags;
import net.ltxprogrammer.changed.init.ChangedTransfurVariants;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

import java.util.Optional;
import java.util.function.Supplier;

public final class LatexStrandManager {
    private LatexStrandManager() {
    }

    public static Optional<Strand> resolve(Entity entity) {
        if (entity instanceof Player player) {
            return resolve(player);
        }
        if (entity instanceof ChangedEntity changedEntity) {
            return Optional.of(resolve(changedEntity));
        }
        return Optional.empty();
    }

    public static Optional<Strand> resolve(Player player) {
        return ProcessTransfur.getPlayerTransfurVariantSafe(player).map(LatexStrandManager::resolve);
    }

    public static Strand resolve(ChangedEntity entity) {
        return build(entity.getSelfVariant(), usableLatexType(entity.getLatexType()), entity.getType(), entity);
    }

    public static Strand resolve(TransfurVariantInstance<?> instance) {
        ChangedEntity entity = instance.getChangedEntity();
        EntityType<?> entityType = entity == null ? instance.getParent().getEntityType() : entity.getType();
        return build(instance.getParent(), usableLatexType(instance.getLatexType()), entityType, entity);
    }

    public static Strand resolve(TransfurVariant<?> variant) {
        return build(variant, null, variant == null ? null : variant.getEntityType(), null);
    }

    public static boolean isSociallyFriendly(Entity source, Player target) {
        Optional<Strand> sourceStrand = resolve(source);
        Optional<Strand> targetStrand = resolve(target);
        return sourceStrand.isPresent() && targetStrand.isPresent() && isSociallyFriendly(sourceStrand.get(), targetStrand.get());
    }

    public static boolean shouldAttack(Entity source, Player target) {
        Optional<Strand> sourceStrand = resolve(source);
        Optional<Strand> targetStrand = resolve(target);
        return sourceStrand.isPresent() && targetStrand.isPresent() && !isSociallyFriendly(sourceStrand.get(), targetStrand.get());
    }

    public static boolean isSociallyFriendly(Strand first, Strand second) {
        if (first.family() == Family.INDEPENDENT || second.family() == Family.INDEPENDENT || first.family() == Family.UNKNOWN || second.family() == Family.UNKNOWN) {
            return first.formId() != null && first.formId().equals(second.formId());
        }
        return first.family() == second.family();
    }

    public static boolean samePhysicalLatex(LatexType first, LatexType second) {
        LatexType usableFirst = usableLatexType(first);
        LatexType usableSecond = usableLatexType(second);
        return usableFirst != null && usableSecond != null && (usableFirst == usableSecond || usableFirst.isFriendlyTo(usableSecond) || usableSecond.isFriendlyTo(usableFirst));
    }

    public static LatexType latexTypeForBlock(Block block) {
        LatexType dark = ChangedLatexTypes.DARK_LATEX.get();
        if (block == dark.getBlock()) {
            return dark;
        }

        LatexType white = ChangedLatexTypes.WHITE_LATEX.get();
        if (block == white.getBlock()) {
            return white;
        }

        return null;
    }

    public static SpreadingLatexType fallbackSecretionType() {
        return ChangedLatexTypes.DARK_LATEX.get();
    }

    private static Strand build(TransfurVariant<?> variant, LatexType directLatexType, EntityType<?> entityType, ChangedEntity entity) {
        Family family = familyFor(variant, directLatexType, entityType, entity);
        LatexType latexType = physicalLatexTypeFor(family, directLatexType, variant, entityType);
        SpreadingLatexType secretionType = latexType instanceof SpreadingLatexType spreading ? spreading : fallbackSecretionType();
        ResourceLocation formId = variant == null ? null : variant.getFormId();
        boolean hasMask = variant != null && variant.is(ChangedTags.TransfurVariants.MASKED);
        return new Strand(family, latexType, secretionType, variant, formId, hasMask);
    }

    private static Family familyFor(TransfurVariant<?> variant, LatexType directLatexType, EntityType<?> entityType, ChangedEntity entity) {
        if (entity instanceof AquaticEntity || isAquatic(variant) || isAquatic(entityType)) {
            return Family.AQUATIC;
        }
        if (directLatexType == ChangedLatexTypes.DARK_LATEX.get() || isDark(variant) || isDark(entityType)) {
            return Family.DARK;
        }
        if (directLatexType == ChangedLatexTypes.WHITE_LATEX.get() || isWhite(variant) || isWhite(entityType)) {
            return Family.WHITE;
        }
        if (variant != null || entityType != null) {
            return Family.INDEPENDENT;
        }
        return Family.UNKNOWN;
    }

    private static LatexType physicalLatexTypeFor(Family family, LatexType directLatexType, TransfurVariant<?> variant, EntityType<?> entityType) {
        if (directLatexType == ChangedLatexTypes.DARK_LATEX.get() || directLatexType == ChangedLatexTypes.WHITE_LATEX.get()) {
            return directLatexType;
        }
        if (family == Family.WHITE || isWhite(variant) || isWhite(entityType)) {
            return ChangedLatexTypes.WHITE_LATEX.get();
        }
        return ChangedLatexTypes.DARK_LATEX.get();
    }

    private static LatexType usableLatexType(LatexType type) {
        if (type == null || type == ChangedLatexTypes.NONE.get()) {
            return null;
        }
        return type;
    }

    private static boolean isAquatic(TransfurVariant<?> variant) {
        return variantIs(variant,
                ChangedTransfurVariants.FERAL_LATEX_SHARK,
                ChangedTransfurVariants.LATEX_BENIGN_ORCA,
                ChangedTransfurVariants.LATEX_EEL,
                ChangedTransfurVariants.LATEX_ORCA,
                ChangedTransfurVariants.LATEX_SHARK,
                ChangedTransfurVariants.LATEX_TIGER_SHARK,
                ChangedTransfurVariants.LATEX_MANTA_RAY_FEMALE,
                ChangedTransfurVariants.LATEX_MANTA_RAY_MALE,
                ChangedTransfurVariants.LATEX_SIREN,
                ChangedTransfurVariants.LATEX_MERMAID_SHARK,
                ChangedTransfurVariants.LATEX_SHARK_FUSION_FEMALE,
                ChangedTransfurVariants.LATEX_SHARK_FUSION_MALE,
                ChangedTransfurVariants.LATEX_SQUID_DOG_FEMALE,
                ChangedTransfurVariants.LATEX_SQUID_DOG_MALE);
    }

    private static boolean isAquatic(EntityType<?> type) {
        return entityTypeIs(type,
                ChangedEntities.FERAL_LATEX_SHARK,
                ChangedEntities.BENIGN_LATEX_ORCA,
                ChangedEntities.LATEX_EEL,
                ChangedEntities.LATEX_ORCA,
                ChangedEntities.LATEX_SHARK,
                ChangedEntities.LATEX_TIGER_SHARK,
                ChangedEntities.LATEX_MANTA_RAY_FEMALE,
                ChangedEntities.LATEX_MANTA_RAY_MALE,
                ChangedEntities.LATEX_SIREN,
                ChangedEntities.LATEX_MERMAID_SHARK,
                ChangedEntities.LATEX_SHARK_FEMALE,
                ChangedEntities.LATEX_SHARK_MALE,
                ChangedEntities.LATEX_SQUID_DOG_FEMALE,
                ChangedEntities.LATEX_SQUID_DOG_MALE);
    }

    private static boolean isDark(TransfurVariant<?> variant) {
        return variantIs(variant,
                ChangedTransfurVariants.DARK_DRAGON,
                ChangedTransfurVariants.DARK_LATEX_WOLF_FEMALE,
                ChangedTransfurVariants.DARK_LATEX_WOLF_MALE,
                ChangedTransfurVariants.DARK_LATEX_WOLF_PUP,
                ChangedTransfurVariants.DARK_LATEX_WOLF_PARTIAL,
                ChangedTransfurVariants.DARK_LATEX_YUFENG,
                ChangedTransfurVariants.DARK_LATEX_DOUBLE_YUFENG,
                ChangedTransfurVariants.PHAGE_LATEX_WOLF_FEMALE,
                ChangedTransfurVariants.PHAGE_LATEX_WOLF_MALE,
                ChangedTransfurVariants.CRYSTAL_WOLF,
                ChangedTransfurVariants.CRYSTAL_WOLF_HORNED);
    }

    private static boolean isDark(EntityType<?> type) {
        return entityTypeIs(type,
                ChangedEntities.DARK_DRAGON,
                ChangedEntities.DARK_LATEX_WOLF_FEMALE,
                ChangedEntities.DARK_LATEX_WOLF_MALE,
                ChangedEntities.DARK_LATEX_WOLF_PUP,
                ChangedEntities.DARK_LATEX_WOLF_PARTIAL,
                ChangedEntities.DARK_LATEX_YUFENG,
                ChangedEntities.DARK_LATEX_DOUBLE_YUFENG,
                ChangedEntities.PHAGE_LATEX_WOLF_FEMALE,
                ChangedEntities.PHAGE_LATEX_WOLF_MALE,
                ChangedEntities.CRYSTAL_WOLF,
                ChangedEntities.CRYSTAL_WOLF_HORNED);
    }

    private static boolean isWhite(TransfurVariant<?> variant) {
        return variantIs(variant,
                ChangedTransfurVariants.WHITE_LATEX_WOLF_FEMALE,
                ChangedTransfurVariants.WHITE_LATEX_WOLF_MALE,
                ChangedTransfurVariants.WHITE_LATEX_CENTAUR,
                ChangedTransfurVariants.WHITE_LATEX_KNIGHT,
                ChangedTransfurVariants.WHITE_LATEX_KNIGHT_FUSION,
                ChangedTransfurVariants.LATEX_WHITE_TIGER,
                ChangedTransfurVariants.LATEX_SNOW_LEOPARD_FEMALE,
                ChangedTransfurVariants.LATEX_SNOW_LEOPARD_MALE,
                ChangedTransfurVariants.WHITE_WOLF_FEMALE,
                ChangedTransfurVariants.WHITE_WOLF_MALE,
                ChangedTransfurVariants.PURE_WHITE_LATEX_WOLF,
                ChangedTransfurVariants.PURE_WHITE_LATEX_WOLF_PUP);
    }

    private static boolean isWhite(EntityType<?> type) {
        return entityTypeIs(type,
                ChangedEntities.WHITE_LATEX_WOLF_FEMALE,
                ChangedEntities.WHITE_LATEX_WOLF_MALE,
                ChangedEntities.WHITE_LATEX_CENTAUR,
                ChangedEntities.WHITE_LATEX_KNIGHT,
                ChangedEntities.WHITE_LATEX_KNIGHT_FUSION,
                ChangedEntities.LATEX_WHITE_TIGER,
                ChangedEntities.LATEX_SNOW_LEOPARD_FEMALE,
                ChangedEntities.LATEX_SNOW_LEOPARD_MALE,
                ChangedEntities.WHITE_WOLF_FEMALE,
                ChangedEntities.WHITE_WOLF_MALE,
                ChangedEntities.PURE_WHITE_LATEX_WOLF,
                ChangedEntities.PURE_WHITE_LATEX_WOLF_PUP,
                ChangedEntities.HEADLESS_KNIGHT,
                ChangedEntities.MILK_PUDDING);
    }

    @SafeVarargs
    private static boolean variantIs(TransfurVariant<?> variant, Supplier<? extends TransfurVariant<?>>... variants) {
        if (variant == null) {
            return false;
        }
        for (Supplier<? extends TransfurVariant<?>> candidate : variants) {
            if (variant.is(candidate)) {
                return true;
            }
        }
        return false;
    }

    @SafeVarargs
    private static boolean entityTypeIs(EntityType<?> type, Supplier<? extends EntityType<?>>... types) {
        if (type == null) {
            return false;
        }
        for (Supplier<? extends EntityType<?>> candidate : types) {
            if (type == candidate.get()) {
                return true;
            }
        }
        return false;
    }

    public enum Family {
        DARK,
        WHITE,
        AQUATIC,
        INDEPENDENT,
        UNKNOWN
    }

    public record Strand(Family family, LatexType latexType, SpreadingLatexType secretionType, TransfurVariant<?> variant, ResourceLocation formId, boolean hasMask) {
        public boolean canSecrete() {
            return secretionType != null;
        }
    }
}
