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
import java.util.concurrent.locks.StampedLock;


/**
 * Allows servers to register custom dimensions. Also used during player joining to send the list of all existing dimensions.
 * <p>
 * Contains {@link Biome#PLAINS} by default but can be removed.
 */
public final class BiomeManager {
    private final Object2IntMap<NamespaceID> nameToId = new Object2IntOpenHashMap<>();
    private final Int2ObjectMap<Biome> idToBiome = new Int2ObjectOpenHashMap<>();
    private final StampedLock lock = new StampedLock();

    public BiomeManager() {
        addBiome(0, Biome.PLAINS);
    }

    /**
     * Adds a new biome. This does NOT send the new list to players.
     *
     * @param biome the biome to add
     */
    public synchronized void addBiome(int id, Biome biome) {
        long stamp = lock.writeLock();
        try {
            this.nameToId.put(biome.name(), id);
            this.idToBiome.put(id, biome);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Removes a biome. This does NOT send the new list to players.
     *
     * @param biome the biome to remove
     */
    public synchronized void removeBiome(Biome biome) {
        long stamp = lock.writeLock();
        try {
            int id = this.nameToId.removeInt(biome.name());
            this.idToBiome.remove(id);
        } finally {
            lock.unlockWrite(stamp);
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
        long optimisticReadStamp = lock.tryOptimisticRead();
        if (lock.validate(optimisticReadStamp)) {
            Biome biome = this.idToBiome.get(id);
            if (lock.validate(optimisticReadStamp)) {
                return biome;
            }
        }

        long readStamp = lock.readLock();
        try {
            return this.idToBiome.get(id);
        } finally {
            lock.unlockRead(readStamp);
        }
    }

    public Biome getByName(NamespaceID namespaceID) {
        long optimisticReadStamp = lock.tryOptimisticRead();
        if (lock.validate(optimisticReadStamp)) {
            Biome biome = this.idToBiome.get(this.nameToId.getInt(namespaceID));
            if (lock.validate(optimisticReadStamp)) {
                return biome;
            }
        }

        long readStamp = lock.readLock();
        try {
            return this.idToBiome.get(this.nameToId.getInt(namespaceID));
        } finally {
            lock.unlockRead(readStamp);
        }
    }

    public int getId(NamespaceID namespaceID) {
        long optimisticReadStamp = lock.tryOptimisticRead();
        if (lock.validate(optimisticReadStamp)) {
            int id = this.nameToId.getInt(namespaceID);
            if (lock.validate(optimisticReadStamp)) {
                return id;
            }
        }

        long readStamp = lock.readLock();
        try {
            return this.nameToId.getInt(namespaceID);
        } finally {
            lock.unlockRead(readStamp);
        }
    }

    public int getId(Biome biome) {
        return getId(biome.name());
    }

    public synchronized NBTCompound toNBT() {
        List<NBTCompound> biomeNBT;
        long readStamp = lock.readLock();
        try {
            biomeNBT = new ArrayList<>(idToBiome.size());
            for (Int2ObjectMap.Entry<Biome> entry : idToBiome.int2ObjectEntrySet()) {
                biomeNBT.add(entry.getValue().toNbt(entry.getIntKey()));
            }
        } finally {
            lock.unlockRead(readStamp);
        }

        return NBT.Compound(Map.of(
                "type", NBT.String("minecraft:worldgen/biome"),
                "value", NBT.List(NBTType.TAG_Compound, biomeNBT)));
    }
}
