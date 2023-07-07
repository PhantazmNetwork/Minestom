package net.minestom.server.world.biomes;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minestom.server.utils.NamespaceID;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTType;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * Allows servers to register custom dimensions. Also used during player joining to send the list of all existing dimensions.
 * <p>
 * Contains {@link Biome#PLAINS} by default but can be removed.
 */
public final class BiomeManager {
    private final Object2IntMap<NamespaceID> nameToId = new Object2IntOpenHashMap<>();
    private final Int2ObjectMap<Biome> idToBiome = new Int2ObjectOpenHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public BiomeManager() {
        addBiome(0, Biome.PLAINS);
    }

    /**
     * Adds a new biome. This does NOT send the new list to players.
     *
     * @param biome the biome to add
     */
    public void addBiome(int id, Biome biome) {
        lock.writeLock().lock();
        try {
            this.nameToId.put(biome.name(), id);
            this.idToBiome.put(id, biome);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes a biome. This does NOT send the new list to players.
     *
     * @param biome the biome to remove
     */
    public void removeBiome(Biome biome) {
        lock.writeLock().lock();
        try {
            int id = this.nameToId.removeInt(biome.name());
            this.idToBiome.remove(id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns an immutable copy of the biomes already registered.
     *
     * @return an immutable copy of the biomes already registered
     */
    public Collection<Biome> unmodifiableCollection() {
        return Collections.unmodifiableCollection(idToBiome.values());
    }

    /**
     * Gets a biome by its id.
     *
     * @param id the id of the biome
     * @return the {@link Biome} linked to this id
     */
    public Biome getById(int id) {
        lock.readLock().lock();
        try {
            return this.idToBiome.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Biome getByName(NamespaceID namespaceID) {
        lock.readLock();
        try {
            return this.idToBiome.get(this.nameToId.getInt(namespaceID));
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getId(NamespaceID namespaceID) {
        lock.readLock();
        try {
            return this.nameToId.getInt(namespaceID);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getId(Biome biome) {
        return getId(biome.name());
    }

    public NBTCompound toNBT() {
        List<NBTCompound> biomeNBT;
        lock.readLock();
        try {
            biomeNBT = new ArrayList<>(idToBiome.size());
            for (Int2ObjectMap.Entry<Biome> entry : idToBiome.int2ObjectEntrySet()) {
                biomeNBT.add(entry.getValue().toNbt(entry.getIntKey()));
            }
        } finally {
            lock.readLock().unlock();
        }

        return NBT.Compound(Map.of(
                "type", NBT.String("minecraft:worldgen/biome"),
                "value", NBT.List(NBTType.TAG_Compound, biomeNBT)));
    }
}
