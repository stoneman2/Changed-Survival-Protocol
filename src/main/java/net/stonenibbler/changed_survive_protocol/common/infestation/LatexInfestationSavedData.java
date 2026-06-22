package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.stonenibbler.changed_survive_protocol.ChangedSurviveProtocol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class LatexInfestationSavedData extends SavedData {
    private static final String NAME = ChangedSurviveProtocol.MODID + "_latex_infestations";

    private final Map<UUID, HeartRecord> hearts = new HashMap<>();
    private final Map<BlockPos, UUID> claims = new HashMap<>();
    private final Map<UUID, Set<BlockPos>> claimsByHeart = new HashMap<>();
    private final Map<UUID, List<BlockPos>> claimListsByHeart = new HashMap<>();
    private final Map<UUID, Set<BlockPos>> activeClaimsByHeart = new HashMap<>();
    private final Map<UUID, List<BlockPos>> activeClaimListsByHeart = new HashMap<>();
    private final Map<BlockPos, UUID> nodes = new HashMap<>();
    private final Map<UUID, Set<BlockPos>> nodesByHeart = new HashMap<>();
    private final Map<BlockPos, Long> nodeCooldowns = new HashMap<>();
    private final Map<BlockPos, DecorationRecord> decorations = new HashMap<>();
    private final Map<UUID, Set<BlockPos>> decorationsByHeart = new HashMap<>();
    private final Map<UUID, UUID> mobs = new HashMap<>();
    private final Map<UUID, Set<UUID>> mobsByHeart = new HashMap<>();
    private final Set<BlockPos> exhaustedCover = new HashSet<>();
    private final Set<ChunkPos> generatedChunks = new HashSet<>();
    private final Map<BlockPos, Long> playerSecretions = new HashMap<>();

    public static LatexInfestationSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(LatexInfestationSavedData::load, LatexInfestationSavedData::new, NAME);
    }

    public static LatexInfestationSavedData load(CompoundTag tag) {
        LatexInfestationSavedData data = new LatexInfestationSavedData();

        ListTag heartsTag = tag.getList("hearts", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < heartsTag.size(); i++) {
            CompoundTag heartTag = heartsTag.getCompound(i);
            HeartRecord heart = HeartRecord.load(heartTag);
            data.hearts.put(heart.id(), heart);
        }

        ListTag claimsTag = tag.getList("claims", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < claimsTag.size(); i++) {
            CompoundTag claimTag = claimsTag.getCompound(i);
            if (claimTag.hasUUID("heart")) {
                data.putClaim(BlockPos.of(claimTag.getLong("pos")), claimTag.getUUID("heart"));
            }
        }

        ListTag nodesTag = tag.getList("nodes", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < nodesTag.size(); i++) {
            CompoundTag nodeTag = nodesTag.getCompound(i);
            if (nodeTag.hasUUID("heart")) {
                data.putNode(BlockPos.of(nodeTag.getLong("pos")), nodeTag.getUUID("heart"));
            }
        }

        ListTag decorationsTag = tag.getList("decorations", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < decorationsTag.size(); i++) {
            CompoundTag decorationTag = decorationsTag.getCompound(i);
            if (decorationTag.hasUUID("heart")) {
                ResourceLocation blockId = decorationTag.contains("block", net.minecraft.nbt.Tag.TAG_STRING) ? ResourceLocation.tryParse(decorationTag.getString("block")) : null;
                data.putDecoration(BlockPos.of(decorationTag.getLong("pos")), decorationTag.getUUID("heart"), blockId);
            }
        }

        ListTag nodeCooldownsTag = tag.getList("nodeCooldowns", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < nodeCooldownsTag.size(); i++) {
            CompoundTag cooldownTag = nodeCooldownsTag.getCompound(i);
            data.nodeCooldowns.put(BlockPos.of(cooldownTag.getLong("pos")), cooldownTag.getLong("until"));
        }

        ListTag mobsTag = tag.getList("mobs", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < mobsTag.size(); i++) {
            CompoundTag mobTag = mobsTag.getCompound(i);
            if (mobTag.hasUUID("heart") && mobTag.hasUUID("mob")) {
                data.putMob(mobTag.getUUID("mob"), mobTag.getUUID("heart"));
            }
        }

        ListTag exhaustedTag = tag.getList("exhaustedCover", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < exhaustedTag.size(); i++) {
            BlockPos pos = BlockPos.of(exhaustedTag.getCompound(i).getLong("pos"));
            data.exhaustedCover.add(pos);
            data.claimedBy(pos).ifPresent(heart -> removeFromIndex(data.activeClaimsByHeart, data.activeClaimListsByHeart, heart, pos));
        }

        ListTag chunkTag = tag.getList("generatedChunks", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < chunkTag.size(); i++) {
            CompoundTag generated = chunkTag.getCompound(i);
            data.generatedChunks.add(new ChunkPos(generated.getInt("x"), generated.getInt("z")));
        }

        ListTag secretionsTag = tag.getList("playerSecretions", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < secretionsTag.size(); i++) {
            CompoundTag secretion = secretionsTag.getCompound(i);
            BlockPos pos = BlockPos.of(secretion.getLong("pos"));
            if (!data.claims.containsKey(pos)) {
                data.playerSecretions.put(pos, secretion.getLong("decayTick"));
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag heartsTag = new ListTag();
        for (HeartRecord heart : hearts.values()) {
            heartsTag.add(heart.save());
        }
        tag.put("hearts", heartsTag);

        ListTag claimsTag = new ListTag();
        claims.forEach((pos, heart) -> {
            CompoundTag claimTag = new CompoundTag();
            claimTag.putLong("pos", pos.asLong());
            claimTag.putUUID("heart", heart);
            claimsTag.add(claimTag);
        });
        tag.put("claims", claimsTag);

        ListTag nodesTag = new ListTag();
        nodes.forEach((pos, heart) -> {
            CompoundTag nodeTag = new CompoundTag();
            nodeTag.putLong("pos", pos.asLong());
            nodeTag.putUUID("heart", heart);
            nodesTag.add(nodeTag);
        });
        tag.put("nodes", nodesTag);

        ListTag decorationsTag = new ListTag();
        decorations.forEach((pos, decoration) -> {
            CompoundTag decorationTag = new CompoundTag();
            decorationTag.putLong("pos", pos.asLong());
            decorationTag.putUUID("heart", decoration.heart());
            if (decoration.blockId() != null) {
                decorationTag.putString("block", decoration.blockId().toString());
            }
            decorationsTag.add(decorationTag);
        });
        tag.put("decorations", decorationsTag);

        ListTag nodeCooldownsTag = new ListTag();
        nodeCooldowns.forEach((pos, until) -> {
            CompoundTag cooldownTag = new CompoundTag();
            cooldownTag.putLong("pos", pos.asLong());
            cooldownTag.putLong("until", until);
            nodeCooldownsTag.add(cooldownTag);
        });
        tag.put("nodeCooldowns", nodeCooldownsTag);

        ListTag mobsTag = new ListTag();
        mobs.forEach((mob, heart) -> {
            CompoundTag mobTag = new CompoundTag();
            mobTag.putUUID("mob", mob);
            mobTag.putUUID("heart", heart);
            mobsTag.add(mobTag);
        });
        tag.put("mobs", mobsTag);

        ListTag exhaustedTag = new ListTag();
        exhaustedCover.forEach(pos -> {
            CompoundTag exhausted = new CompoundTag();
            exhausted.putLong("pos", pos.asLong());
            exhaustedTag.add(exhausted);
        });
        tag.put("exhaustedCover", exhaustedTag);

        ListTag chunkTag = new ListTag();
        generatedChunks.forEach(pos -> {
            CompoundTag generated = new CompoundTag();
            generated.putInt("x", pos.x);
            generated.putInt("z", pos.z);
            chunkTag.add(generated);
        });
        tag.put("generatedChunks", chunkTag);

        ListTag secretionsTag = new ListTag();
        playerSecretions.forEach((pos, decayTick) -> {
            CompoundTag secretion = new CompoundTag();
            secretion.putLong("pos", pos.asLong());
            secretion.putLong("decayTick", decayTick);
            secretionsTag.add(secretion);
        });
        tag.put("playerSecretions", secretionsTag);
        return tag;
    }

    public List<HeartRecord> activeHearts() {
        return hearts.values().stream().filter(HeartRecord::alive).toList();
    }

    public List<HeartRecord> deadHearts() {
        return hearts.values().stream().filter(heart -> !heart.alive()).toList();
    }

    public int activeHeartCount() {
        int count = 0;
        for (HeartRecord heart : hearts.values()) {
            if (heart.alive()) {
                count++;
            }
        }
        return count;
    }

    public Optional<HeartRecord> heartAt(BlockPos pos) {
        return hearts.values().stream().filter(heart -> heart.pos().equals(pos)).findFirst();
    }

    public Optional<HeartRecord> heart(UUID id) {
        return Optional.ofNullable(hearts.get(id));
    }

    public HeartRecord addHeart(BlockPos pos, LatexHeartBlock.Kind kind) {
        HeartRecord existing = heartAt(pos).orElse(null);
        if (existing != null) {
            if (existing.alive()) {
                return existing;
            }
            hearts.remove(existing.id());
        }

        HeartRecord heart = new HeartRecord(UUID.randomUUID(), pos.immutable(), kind, true, 0L, 0L, 0L, 0L);
        hearts.put(heart.id(), heart);
        claim(pos, heart.id());
        setDirty();
        return heart;
    }

    public void removeHeart(BlockPos pos) {
        heartAt(pos).ifPresent(heart -> {
            hearts.put(heart.id(), heart.withAlive(false));
            setDirty();
        });
    }

    public void removeHeart(UUID id) {
        HeartRecord heart = hearts.get(id);
        if (heart != null) {
            hearts.put(id, heart.withAlive(false));
            setDirty();
        }
    }

    public void updateHeart(HeartRecord heart) {
        hearts.put(heart.id(), heart);
        setDirty();
    }

    public void claim(BlockPos pos, UUID heart) {
        putClaim(pos, heart);
        setDirty();
    }

    private void putClaim(BlockPos pos, UUID heart) {
        BlockPos immutable = pos.immutable();
        UUID oldHeart = claims.put(immutable, heart);
        if (oldHeart != null && !oldHeart.equals(heart)) {
            removeFromIndex(claimsByHeart, claimListsByHeart, oldHeart, immutable);
            removeFromIndex(activeClaimsByHeart, activeClaimListsByHeart, oldHeart, immutable);
        }
        addToIndex(claimsByHeart, claimListsByHeart, heart, immutable);
        exhaustedCover.remove(immutable);
        playerSecretions.remove(immutable);
        addToIndex(activeClaimsByHeart, activeClaimListsByHeart, heart, immutable);
    }

    public Optional<UUID> claimedBy(BlockPos pos) {
        return Optional.ofNullable(claims.get(pos));
    }

    public int claimCount(UUID heart) {
        return claimsByHeart.getOrDefault(heart, Collections.emptySet()).size();
    }

    public List<BlockPos> claimsFor(UUID heart) {
        return new ArrayList<>(claimListsByHeart.getOrDefault(heart, Collections.emptyList()));
    }

    public Collection<BlockPos> claimPositions(UUID heart) {
        return claimListsByHeart.getOrDefault(heart, Collections.emptyList());
    }

    public List<BlockPos> claimPositionList(UUID heart) {
        return claimListsByHeart.getOrDefault(heart, Collections.emptyList());
    }

    public List<BlockPos> activeClaimsFor(UUID heart) {
        return new ArrayList<>(activeClaimListsByHeart.getOrDefault(heart, Collections.emptyList()));
    }

    public Collection<BlockPos> activeClaimPositions(UUID heart) {
        return activeClaimListsByHeart.getOrDefault(heart, Collections.emptyList());
    }

    public List<BlockPos> activeClaimPositionList(UUID heart) {
        return activeClaimListsByHeart.getOrDefault(heart, Collections.emptyList());
    }

    public List<BlockPos> claimsForDeadHeartPositions(int limit) {
        List<BlockPos> positions = new ArrayList<>();
        for (Map.Entry<UUID, Set<BlockPos>> entry : claimsByHeart.entrySet()) {
            HeartRecord heart = hearts.get(entry.getKey());
            if (heart != null && heart.alive()) {
                continue;
            }
            for (BlockPos pos : claimListsByHeart.getOrDefault(entry.getKey(), Collections.emptyList())) {
                positions.add(pos);
                if (limit > 0 && positions.size() >= limit) {
                    return positions;
                }
            }
        }
        return positions;
    }

    public void unclaim(BlockPos pos) {
        UUID heart = claims.remove(pos);
        if (heart != null) {
            removeFromIndexedList(claimsByHeart, claimListsByHeart, heart, pos);
            removeFromIndex(activeClaimsByHeart, activeClaimListsByHeart, heart, pos);
            forgetDeadHeartIfUnclaimed(heart);
        }
        removeNode(pos);
        removeDecoration(pos);
        nodeCooldowns.remove(pos);
        exhaustedCover.remove(pos);
        setDirty();
    }

    public void forgetDeadHeartIfUnclaimed(UUID id) {
        HeartRecord heart = hearts.get(id);
        if (heart == null || heart.alive() || claimsByHeart.containsKey(id)) {
            return;
        }
        hearts.remove(id);
        claimListsByHeart.remove(id);
        activeClaimsByHeart.remove(id);
        activeClaimListsByHeart.remove(id);
        nodesByHeart.remove(id);
        decorationsByHeart.remove(id);
        mobsByHeart.remove(id);
        setDirty();
    }

    public boolean isCoverExhausted(BlockPos pos) {
        return exhaustedCover.contains(pos);
    }

    public void markCoverExhausted(BlockPos pos) {
        BlockPos immutable = pos.immutable();
        if (exhaustedCover.add(immutable)) {
            claimedBy(immutable).ifPresent(heart -> removeFromIndex(activeClaimsByHeart, activeClaimListsByHeart, heart, immutable));
            setDirty();
        }
    }

    public void wakeCover(BlockPos pos) {
        BlockPos immutable = pos.immutable();
        if (exhaustedCover.remove(immutable)) {
            claimedBy(immutable).ifPresent(heart -> addToIndex(activeClaimsByHeart, activeClaimListsByHeart, heart, immutable));
            setDirty();
        }
    }

    public void addNode(BlockPos pos, UUID heart) {
        putNode(pos, heart);
        claim(pos, heart);
        setDirty();
    }

    private void putNode(BlockPos pos, UUID heart) {
        BlockPos immutable = pos.immutable();
        nodeCooldowns.remove(immutable);
        UUID oldHeart = nodes.put(immutable, heart);
        if (oldHeart != null && !oldHeart.equals(heart)) {
            removeFromIndex(nodesByHeart, oldHeart, immutable);
        }
        addToIndex(nodesByHeart, heart, immutable);
    }

    public void removeNode(BlockPos pos) {
        removeNode(pos, 0L);
    }

    public void removeNode(BlockPos pos, long cooldownUntil) {
        BlockPos immutable = pos.immutable();
        UUID heart = nodes.remove(immutable);
        boolean changed = false;
        if (heart != null) {
            removeFromIndex(nodesByHeart, heart, immutable);
            changed = true;
        }
        if (cooldownUntil > 0L) {
            nodeCooldowns.put(immutable, cooldownUntil);
            changed = true;
        }
        if (changed) {
            setDirty();
        }
    }

    public boolean isNodeOnCooldown(BlockPos pos, long gameTime) {
        Long cooldownUntil = nodeCooldowns.get(pos);
        return cooldownUntil != null && cooldownUntil > gameTime;
    }

    public void cleanupNodeCooldowns(long gameTime) {
        if (nodeCooldowns.entrySet().removeIf(entry -> entry.getValue() <= gameTime)) {
            setDirty();
        }
    }

    public Optional<UUID> nodeOwner(BlockPos pos) {
        return Optional.ofNullable(nodes.get(pos));
    }

    public int nodeCount(UUID heart) {
        return nodesByHeart.getOrDefault(heart, Collections.emptySet()).size();
    }

    public List<BlockPos> nodesFor(UUID heart) {
        return new ArrayList<>(nodesByHeart.getOrDefault(heart, Collections.emptySet()));
    }

    public Collection<BlockPos> nodePositions(UUID heart) {
        return nodesByHeart.getOrDefault(heart, Collections.emptySet());
    }

    public void addDecoration(BlockPos pos, UUID heart, Block block) {
        putDecoration(pos, heart, BuiltInRegistries.BLOCK.getKey(block));
        claim(pos, heart);
        setDirty();
    }

    private void putDecoration(BlockPos pos, UUID heart, ResourceLocation blockId) {
        BlockPos immutable = pos.immutable();
        DecorationRecord oldDecoration = decorations.put(immutable, new DecorationRecord(heart, blockId));
        if (oldDecoration != null && !oldDecoration.heart().equals(heart)) {
            removeFromIndex(decorationsByHeart, oldDecoration.heart(), immutable);
        }
        addToIndex(decorationsByHeart, heart, immutable);
    }

    public void removeDecoration(BlockPos pos) {
        DecorationRecord decoration = decorations.remove(pos);
        if (decoration != null) {
            removeFromIndex(decorationsByHeart, decoration.heart(), pos);
            setDirty();
        }
    }

    public boolean isDecoration(BlockPos pos) {
        return decorations.containsKey(pos);
    }

    public boolean shouldRemoveDecorationBlock(BlockPos pos, BlockState state) {
        DecorationRecord decoration = decorations.get(pos);
        if (decoration == null || state.isAir()) {
            return false;
        }

        ResourceLocation currentBlock = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (decoration.blockId() != null) {
            return decoration.blockId().equals(currentBlock) || isChangedDecorationBlock(currentBlock);
        }

        return isChangedDecorationBlock(currentBlock);
    }

    public static boolean isChangedDecorationBlock(BlockState state) {
        return isChangedDecorationBlock(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    public static boolean isAttachedDecorationPart(BlockState state) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!"changed".equals(blockId.getNamespace())) {
            return false;
        }

        String path = blockId.getPath();
        return path.contains("crystal") || path.contains("pillar");
    }

    private static boolean isChangedDecorationBlock(ResourceLocation blockId) {
        if (!"changed".equals(blockId.getNamespace())) {
            return false;
        }

        String path = blockId.getPath();
        return path.contains("crystal") || path.contains("pillar") || path.contains("latex");
    }

    public int decorationCount(UUID heart) {
        return decorationsByHeart.getOrDefault(heart, Collections.emptySet()).size();
    }

    public List<BlockPos> decorationsFor(UUID heart) {
        return new ArrayList<>(decorationsByHeart.getOrDefault(heart, Collections.emptySet()));
    }

    public Collection<BlockPos> decorationPositions(UUID heart) {
        return decorationsByHeart.getOrDefault(heart, Collections.emptySet());
    }

    public void addMob(UUID mob, UUID heart) {
        putMob(mob, heart);
        setDirty();
    }

    private void putMob(UUID mob, UUID heart) {
        UUID oldHeart = mobs.put(mob, heart);
        if (oldHeart != null && !oldHeart.equals(heart)) {
            removeFromIndex(mobsByHeart, oldHeart, mob);
        }
        addToIndex(mobsByHeart, heart, mob);
    }

    public void removeMob(UUID mob) {
        UUID heart = mobs.remove(mob);
        if (heart != null) {
            removeFromIndex(mobsByHeart, heart, mob);
            setDirty();
        }
    }

    public int mobCount(UUID heart) {
        return mobsByHeart.getOrDefault(heart, Collections.emptySet()).size();
    }

    public List<UUID> mobsFor(UUID heart) {
        return new ArrayList<>(mobsByHeart.getOrDefault(heart, Collections.emptySet()));
    }

    public boolean markChunkGenerated(ChunkPos pos) {
        boolean added = generatedChunks.add(pos);
        if (added) {
            setDirty();
        }
        return added;
    }

    public void addPlayerSecretion(BlockPos pos, long decayTick) {
        Long oldDecayTick = playerSecretions.put(pos.immutable(), decayTick);
        if (oldDecayTick == null || oldDecayTick != decayTick) {
            setDirty();
        }
    }

    public boolean isPlayerSecretion(BlockPos pos) {
        return playerSecretions.containsKey(pos);
    }

    public void removePlayerSecretion(BlockPos pos) {
        if (playerSecretions.remove(pos) != null) {
            setDirty();
        }
    }

    public List<PlayerSecretionRecord> playerSecretions() {
        return playerSecretions.entrySet().stream()
                .map(entry -> new PlayerSecretionRecord(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static <T> void addToIndex(Map<UUID, Set<T>> index, UUID heart, T value) {
        index.computeIfAbsent(heart, ignored -> new HashSet<>()).add(value);
    }

    private static <T> void addToIndex(Map<UUID, Set<T>> index, Map<UUID, List<T>> listIndex, UUID heart, T value) {
        if (index.computeIfAbsent(heart, ignored -> new HashSet<>()).add(value)) {
            listIndex.computeIfAbsent(heart, ignored -> new ArrayList<>()).add(value);
        }
    }

    private static <T> void removeFromIndex(Map<UUID, Set<T>> index, UUID heart, T value) {
        Set<T> values = index.get(heart);
        if (values == null) {
            return;
        }
        values.remove(value);
        if (values.isEmpty()) {
            index.remove(heart);
        }
    }

    private static <T> void removeFromIndex(Map<UUID, Set<T>> index, Map<UUID, List<T>> listIndex, UUID heart, T value) {
        removeFromIndex(index, heart, value);
        removeFromIndexedList(listIndex, heart, value);
    }

    private static <T> void removeFromIndexedList(Map<UUID, Set<T>> setIndex, Map<UUID, List<T>> listIndex, UUID heart, T value) {
        removeFromIndex(setIndex, heart, value);
        removeFromIndexedList(listIndex, heart, value);
    }

    private static <T> void removeFromIndexedList(Map<UUID, List<T>> index, UUID heart, T value) {
        List<T> values = index.get(heart);
        if (values == null) {
            return;
        }
        values.remove(value);
        if (values.isEmpty()) {
            index.remove(heart);
        }
    }

    public record HeartRecord(UUID id, BlockPos pos, LatexHeartBlock.Kind kind, boolean alive, long nextGrowthTick, long nextDecayTick, long nextMobTick, long nextNodeTick) {
        public HeartRecord withAlive(boolean alive) {
            return new HeartRecord(id, pos, kind, alive, nextGrowthTick, nextDecayTick, nextMobTick, nextNodeTick);
        }

        public HeartRecord withNextGrowthTick(long tick) {
            return new HeartRecord(id, pos, kind, alive, tick, nextDecayTick, nextMobTick, nextNodeTick);
        }

        public HeartRecord withNextDecayTick(long tick) {
            return new HeartRecord(id, pos, kind, alive, nextGrowthTick, tick, nextMobTick, nextNodeTick);
        }

        public HeartRecord withNextMobTick(long tick) {
            return new HeartRecord(id, pos, kind, alive, nextGrowthTick, nextDecayTick, tick, nextNodeTick);
        }

        public HeartRecord withNextNodeTick(long tick) {
            return new HeartRecord(id, pos, kind, alive, nextGrowthTick, nextDecayTick, nextMobTick, tick);
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("id", id);
            tag.putLong("pos", pos.asLong());
            tag.putString("kind", kind.name());
            tag.putBoolean("alive", alive);
            tag.putLong("nextGrowthTick", nextGrowthTick);
            tag.putLong("nextDecayTick", nextDecayTick);
            tag.putLong("nextMobTick", nextMobTick);
            tag.putLong("nextNodeTick", nextNodeTick);
            return tag;
        }

        private static HeartRecord load(CompoundTag tag) {
            LatexHeartBlock.Kind kind = "DARK".equals(tag.getString("kind")) ? LatexHeartBlock.Kind.DARK : LatexHeartBlock.Kind.WHITE;
            return new HeartRecord(tag.getUUID("id"), BlockPos.of(tag.getLong("pos")), kind, tag.getBoolean("alive"), tag.getLong("nextGrowthTick"), tag.getLong("nextDecayTick"), tag.getLong("nextMobTick"), tag.getLong("nextNodeTick"));
        }
    }

    public record PlayerSecretionRecord(BlockPos pos, long decayTick) {
    }

    private record DecorationRecord(UUID heart, ResourceLocation blockId) {
    }
}
