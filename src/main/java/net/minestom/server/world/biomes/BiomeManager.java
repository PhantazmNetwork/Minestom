package net.minestom.server.world.biomes;

import net.minestom.server.utils.NamespaceID;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTType;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Allows servers to register custom dimensions. Also used during player joining to send the list of all existing dimensions.
 * <p>
 * Contains {@link Biome#PLAINS} by default but can be removed.
 */
public final class BiomeManager {
    private final Map<NamespaceID, Integer> nameToId = new ConcurrentHashMap<>();
    private final Map<Integer, Biome> idToBiome = new ConcurrentHashMap<>();
    private final Map<NamespaceID, Biome> nameToBiome = new ConcurrentHashMap<>();
    private final Object sync = new Object();

    public BiomeManager() {
        addBiome(0, Biome.PLAINS);
    }

    /**
     * Adds a new biome. This does NOT send the new list to players.
     *
     * @param biome the biome to add
     */
    public void addBiome(int id, Biome biome) {
        synchronized (sync) {
            this.nameToId.put(biome.name(), id);
            this.idToBiome.put(id, biome);
            this.nameToBiome.put(biome.name(), biome);
        }
    }

    /**
     * Removes a biome. This does NOT send the new list to players.
     *
     * @param biome the biome to remove
     */
    public void removeBiome(Biome biome) {
        synchronized (sync) {
            int id = this.nameToId.remove(biome.name());
            this.idToBiome.remove(id);
            this.nameToBiome.remove(biome.name());
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
        return this.idToBiome.get(id);
    }

    public Biome getByName(NamespaceID namespaceID) {
        return this.nameToBiome.get(namespaceID);
    }

    public int getId(NamespaceID namespaceID) {
        return this.nameToId.get(namespaceID);
    }

    public int getId(Biome biome) {
        return this.nameToId.get(biome.name());
    }

    public synchronized NBTCompound toNBT() {
        return NBT.Compound(Map.of(
                "type", NBT.String("minecraft:worldgen/biome"),
                "value", NBT.List(NBTType.TAG_Compound, idToBiome.entrySet().stream().map(entry -> entry.getValue().toNbt(entry.getKey())).toList())));
    }
}
