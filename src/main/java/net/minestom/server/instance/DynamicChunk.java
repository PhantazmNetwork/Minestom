package net.minestom.server.instance;

import it.unimi.dsi.fastutil.ints.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PreSendChunkEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.CachedPacket;
import net.minestom.server.network.packet.server.play.ChunkDataPacket;
import net.minestom.server.network.packet.server.play.UpdateLightPacket;
import net.minestom.server.network.packet.server.play.data.ChunkBiomeData;
import net.minestom.server.network.packet.server.play.data.ChunkData;
import net.minestom.server.network.packet.server.play.data.LightData;
import net.minestom.server.snapshot.ChunkSnapshot;
import net.minestom.server.snapshot.SnapshotImpl;
import net.minestom.server.snapshot.SnapshotUpdater;
import net.minestom.server.utils.ArrayUtils;
import net.minestom.server.utils.MathUtils;
import net.minestom.server.utils.ObjectPool;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.*;

import static net.minestom.server.utils.chunk.ChunkUtils.toSectionRelativeCoordinate;

/**
 * Represents a {@link Chunk} which store each individual block in memory.
 * <p>
 * WARNING: not thread-safe.
 */
public class DynamicChunk extends Chunk {

    private Section[] sections;

    // Key = ChunkUtils#getBlockIndex
    protected final Int2ObjectOpenHashMap<Block> entries;
    protected final Int2ObjectOpenHashMap<Block> tickableMap;

    private long lastChange;
    final CachedPacket chunkCache = new CachedPacket(this::createChunkPacket);
    final CachedPacket lightCache = new CachedPacket(this::createLightPacket);

    public DynamicChunk(@NotNull Instance instance, int chunkX, int chunkZ) {
        super(instance, chunkX, chunkZ, true);
        var sectionsTemp = new Section[maxSection - minSection];
        Arrays.setAll(sectionsTemp, value -> new Section());
        this.sections = sectionsTemp;

        this.entries = new Int2ObjectOpenHashMap<>(0);
        this.tickableMap = new Int2ObjectOpenHashMap<>(0);
    }

    public DynamicChunk(@NotNull Instance instance, int chunkX, int chunkZ, @NotNull Section[] sections,
                        @NotNull Int2ObjectMap<Block> blockEntries) {
        super(instance, chunkX, chunkZ, true);
        this.sections = sections;

        Int2ObjectOpenHashMap<Block> newEntries = new Int2ObjectOpenHashMap<>(blockEntries.size());
        Int2ObjectOpenHashMap<Block> tickableMap = new Int2ObjectOpenHashMap<>();
        for (Int2ObjectMap.Entry<Block> blockEntry : blockEntries.int2ObjectEntrySet()) {
            int key = blockEntry.getIntKey();
            Block block = blockEntry.getValue();
            if (block == null) {
                continue;
            }

            BlockHandler handler = block.handler();
            if (handler != null || block.hasNbt() || block.registry().isBlockEntity()) {
                newEntries.put(key, block);
            }

            if (handler != null && handler.isTickable()) {
                tickableMap.put(key, block);
            }
        }

        this.entries = newEntries;
        this.tickableMap = tickableMap;
    }

    @Override
    public void setBlock(int x, int y, int z, @NotNull Block block) {
        assertLock();
        this.lastChange = System.currentTimeMillis();
        this.chunkCache.invalidate();
        this.lightCache.invalidate();

        Section section = getSectionAt(y);
        section.blockPalette()
                .set(toSectionRelativeCoordinate(x), toSectionRelativeCoordinate(y), toSectionRelativeCoordinate(z), block.stateId());

        final int index = ChunkUtils.getBlockIndex(x, y, z);
        // Handler
        final BlockHandler handler = block.handler();
        if (handler != null || block.hasNbt() || block.registry().isBlockEntity()) {
            this.entries.put(index, block);
        } else {
            this.entries.remove(index);
        }
        // Block tick
        if (handler != null && handler.isTickable()) {
            this.tickableMap.put(index, block);
        } else {
            this.tickableMap.remove(index);
        }
    }

    @Override
    public @NotNull Int2ObjectMap<Block> getEntries() {
        return Int2ObjectMaps.unmodifiable(entries);
    }

    @Override
    public void setBiomeById(int x, int y, int z, int biomeId) {
        assertLock();
        this.chunkCache.invalidate();
        Section section = getSectionAt(y);
        section.biomePalette().set(
                toSectionRelativeCoordinate(x) / 4,
                toSectionRelativeCoordinate(y) / 4,
                toSectionRelativeCoordinate(z) / 4, biomeId);
    }

    @Override
    public void setBiome(int x, int y, int z, @NotNull Biome biome) {
        assertLock();
        this.chunkCache.invalidate();
        Section section = getSectionAt(y);
        section.biomePalette().set(
                toSectionRelativeCoordinate(x) / 4,
                toSectionRelativeCoordinate(y) / 4,
                toSectionRelativeCoordinate(z) / 4, MinecraftServer.getBiomeManager().getId(biome));
    }

    @Override
    public @NotNull List<Section> getSections() {
        return Collections.unmodifiableList(Arrays.asList(sections));
    }

    @Override
    public @NotNull Section getSection(int section) {
        return sections[section - minSection];
    }

    @Override
    public @NotNull Section[] sectionCopy() {
        Section[] newSections = new Section[sections.length];
        for (int i = 0; i < sections.length; i++) {
            newSections[i] = sections[i].clone();
        }

        return newSections;
    }

    @Override
    public void tick(long time) {
        if (tickableMap.isEmpty()) return;
        tickableMap.int2ObjectEntrySet().fastForEach(entry -> {
            final int index = entry.getIntKey();
            final Block block = entry.getValue();
            final BlockHandler handler = block.handler();
            if (handler == null) return;
            final Point blockPosition = ChunkUtils.getBlockPosition(index, chunkX, chunkZ);
            handler.tick(new BlockHandler.Tick(block, instance, blockPosition));
        });
    }

    @Override
    public @Nullable Block getBlock(int x, int y, int z, @NotNull Condition condition) {
        assertLock();
        return getBlock_UNSAFE(x, y, z, condition);
    }

    /**
     * Unsafe version of {@link DynamicChunk#getBlock(int, int, int, Condition)} that does not ensure a lock is held on
     * the chunk.
     *
     * @param x         the x-coordinate of the block
     * @param y         the y-coordinate of the block
     * @param z         the z-coordinate of the block
     * @param condition the condition used to potentially optimize block retrieval
     * @return the block at the given coordinate
     */
    protected Block getBlock_UNSAFE(int x, int y, int z, @NotNull Condition condition) {
        if (y < minSection * CHUNK_SECTION_SIZE || y >= maxSection * CHUNK_SECTION_SIZE)
            return Block.AIR; // Out of bounds

        // Verify if the block object is present
        if (condition != Condition.TYPE) {
            final Block entry = !entries.isEmpty() ?
                    entries.get(ChunkUtils.getBlockIndex(x, y, z)) : null;
            if (entry != null || condition == Condition.CACHED) {
                return entry;
            }
        }
        // Retrieve the block from state id
        final Section section = getSectionAt(y);
        final int blockStateId = section.blockPalette()
                .get(toSectionRelativeCoordinate(x), toSectionRelativeCoordinate(y), toSectionRelativeCoordinate(z));
        return Objects.requireNonNullElse(Block.fromStateId((short) blockStateId), Block.AIR);
    }

    @Override
    public @NotNull Biome getBiome(int x, int y, int z) {
        assertLock();
        final Section section = getSectionAt(y);
        final int id = section.biomePalette()
                .get(toSectionRelativeCoordinate(x) / 4, toSectionRelativeCoordinate(y) / 4, toSectionRelativeCoordinate(z) / 4);
        return MinecraftServer.getBiomeManager().getById(id);
    }

    @Override
    public long getLastChangeTime() {
        return lastChange;
    }

    @Override
    public void sendChunk(@NotNull Player player) {
        if (!isLoaded()) return;

        PreSendChunkEvent preSendChunkEvent = new PreSendChunkEvent(this);
        EventDispatcher.call(preSendChunkEvent);
        player.sendPacket(preSendChunkEvent.chunk().chunkCache);
    }

    @Override
    public void sendChunk() {
        if (!isLoaded()) return;

        PreSendChunkEvent preSendChunkEvent = new PreSendChunkEvent(this);
        EventDispatcher.call(preSendChunkEvent);
        sendPacketToViewers(preSendChunkEvent.chunk().chunkCache);
    }

    @Override
    public @NotNull DynamicChunk copy(@NotNull Instance instance, int chunkX, int chunkZ) {
        DynamicChunk dynamicChunk = new DynamicChunk(instance, chunkX, chunkZ);
        dynamicChunk.sections = sectionCopy();
        dynamicChunk.entries.putAll(entries);
        return dynamicChunk;
    }

    @Override
    public void reset() {
        for (Section section : sections) section.clear();
        this.entries.clear();
    }

    private synchronized @NotNull ChunkDataPacket createChunkPacket() {
        final NBTCompound heightmapsNBT;
        // TODO: don't hardcode heightmaps
        // Heightmap
        {
            int dimensionHeight = getInstance().getDimensionType().getHeight();
            int[] motionBlocking = new int[16 * 16];
            int[] worldSurface = new int[16 * 16];
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    motionBlocking[x + z * 16] = 0;
                    worldSurface[x + z * 16] = dimensionHeight - 1;
                }
            }
            final int bitsForHeight = MathUtils.bitsToRepresent(dimensionHeight);
            heightmapsNBT = NBT.Compound(Map.of(
                    "MOTION_BLOCKING", NBT.LongArray(encodeBlocks(motionBlocking, bitsForHeight)),
                    "WORLD_SURFACE", NBT.LongArray(encodeBlocks(worldSurface, bitsForHeight))));
        }
        // Data
        final byte[] data = ObjectPool.PACKET_POOL.use(buffer ->
                NetworkBuffer.makeArray(networkBuffer -> {
                    for (Section section : sections) networkBuffer.write(section);
                }));
        return new ChunkDataPacket(chunkX, chunkZ,
                new ChunkData(heightmapsNBT, data, entries),
                createLightData());
    }

    private synchronized @NotNull UpdateLightPacket createLightPacket() {
        return new UpdateLightPacket(chunkX, chunkZ, createLightData());
    }

    public synchronized @NotNull ChunkBiomeData createBiomeData() {
        final byte[] data = ObjectPool.PACKET_POOL.use(buffer ->
                NetworkBuffer.makeArray(networkBuffer -> {
                    for (Section section : sections) section.biomePalette().write(networkBuffer);
                }));
        return new ChunkBiomeData(chunkX, chunkZ, data);
    }

    private LightData createLightData() {
        BitSet skyMask = new BitSet();
        BitSet blockMask = new BitSet();
        BitSet emptySkyMask = new BitSet();
        BitSet emptyBlockMask = new BitSet();
        List<byte[]> skyLights = new ArrayList<>();
        List<byte[]> blockLights = new ArrayList<>();

        int index = 0;
        for (Section section : sections) {
            index++;
            final byte[] skyLight = section.getSkyLight();
            final byte[] blockLight = section.getBlockLight();
            if (skyLight.length != 0) {
                skyLights.add(skyLight);
                skyMask.set(index);
            } else {
                emptySkyMask.set(index);
            }
            if (blockLight.length != 0) {
                blockLights.add(blockLight);
                blockMask.set(index);
            } else {
                emptyBlockMask.set(index);
            }
        }
        return new LightData(true,
                skyMask, blockMask,
                emptySkyMask, emptyBlockMask,
                skyLights, blockLights);
    }

    @Override
    public @NotNull ChunkSnapshot updateSnapshot(@NotNull SnapshotUpdater updater) {
        Section[] clonedSections = sectionCopy();

        var entities = instance.getEntityTracker().chunkEntities(chunkX, chunkZ, EntityTracker.Target.ENTITIES);
        final int[] entityIds = ArrayUtils.mapToIntArray(entities, Entity::getEntityId);
        return new SnapshotImpl.Chunk(minSection, chunkX, chunkZ,
                clonedSections, entries.clone(), entityIds, updater.reference(instance),
                tagHandler().readableCopy());
    }

    private void assertLock() {
        assert Thread.holdsLock(this) : "Chunk must be locked before access";
    }

    private static final int[] MAGIC = {
            -1, -1, 0, Integer.MIN_VALUE, 0, 0, 1431655765, 1431655765, 0, Integer.MIN_VALUE,
            0, 1, 858993459, 858993459, 0, 715827882, 715827882, 0, 613566756, 613566756,
            0, Integer.MIN_VALUE, 0, 2, 477218588, 477218588, 0, 429496729, 429496729, 0,
            390451572, 390451572, 0, 357913941, 357913941, 0, 330382099, 330382099, 0, 306783378,
            306783378, 0, 286331153, 286331153, 0, Integer.MIN_VALUE, 0, 3, 252645135, 252645135,
            0, 238609294, 238609294, 0, 226050910, 226050910, 0, 214748364, 214748364, 0,
            204522252, 204522252, 0, 195225786, 195225786, 0, 186737708, 186737708, 0, 178956970,
            178956970, 0, 171798691, 171798691, 0, 165191049, 165191049, 0, 159072862, 159072862,
            0, 153391689, 153391689, 0, 148102320, 148102320, 0, 143165576, 143165576, 0,
            138547332, 138547332, 0, Integer.MIN_VALUE, 0, 4, 130150524, 130150524, 0, 126322567,
            126322567, 0, 122713351, 122713351, 0, 119304647, 119304647, 0, 116080197, 116080197,
            0, 113025455, 113025455, 0, 110127366, 110127366, 0, 107374182, 107374182, 0,
            104755299, 104755299, 0, 102261126, 102261126, 0, 99882960, 99882960, 0, 97612893,
            97612893, 0, 95443717, 95443717, 0, 93368854, 93368854, 0, 91382282, 91382282,
            0, 89478485, 89478485, 0, 87652393, 87652393, 0, 85899345, 85899345, 0,
            84215045, 84215045, 0, 82595524, 82595524, 0, 81037118, 81037118, 0, 79536431,
            79536431, 0, 78090314, 78090314, 0, 76695844, 76695844, 0, 75350303, 75350303,
            0, 74051160, 74051160, 0, 72796055, 72796055, 0, 71582788, 71582788, 0,
            70409299, 70409299, 0, 69273666, 69273666, 0, 68174084, 68174084, 0, Integer.MIN_VALUE,
            0, 5};

    private static long[] encodeBlocks(int[] blocks, int bitsPerEntry) {
        final long maxEntryValue = (1L << bitsPerEntry) - 1;
        final char valuesPerLong = (char) (64 / bitsPerEntry);
        final int magicIndex = 3 * (valuesPerLong - 1);
        final long divideMul = Integer.toUnsignedLong(MAGIC[magicIndex]);
        final long divideAdd = Integer.toUnsignedLong(MAGIC[magicIndex + 1]);
        final int divideShift = MAGIC[magicIndex + 2];
        final int size = (blocks.length + valuesPerLong - 1) / valuesPerLong;

        long[] data = new long[size];

        for (int i = 0; i < blocks.length; i++) {
            final long value = blocks[i];
            final int cellIndex = (int) (i * divideMul + divideAdd >> 32L >> divideShift);
            final int bitIndex = (i - cellIndex * valuesPerLong) * bitsPerEntry;
            data[cellIndex] = data[cellIndex] & ~(maxEntryValue << bitIndex) | (value & maxEntryValue) << bitIndex;
        }

        return data;
    }
}
