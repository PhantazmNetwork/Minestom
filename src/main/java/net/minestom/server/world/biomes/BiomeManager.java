package net.minestom.server.world.biomes;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTType;

import java.util.*;

/**
 * Allows servers to register custom dimensions. Also used during player joining to send the list of all existing dimensions.
 * <p>
 * Contains {@link Biome#PLAINS} by default but can be removed.
 */
public final class BiomeManager {
    private volatile Maps maps = new Maps(new HashMap<>(), new Int2ObjectOpenHashMap<>(),
            new Object2IntOpenHashMap<>());
    private final Object lock = new Object();

    private record Maps(Map<NamespaceID, Biome> nameToBiome, Int2ObjectMap<Biome> idToBiome,
                        Object2IntMap<Biome> biomeToId) {
        private Maps copy() {
            return new Maps(new HashMap<>(nameToBiome), new Int2ObjectOpenHashMap<>(idToBiome),
                    new Object2IntOpenHashMap<>(biomeToId));
        }
    }

    public BiomeManager() {
        addBiome(0, Biome.PLAINS);
    }

    /**
     * Adds a new biome. This does NOT send the new list to players.
     *
     * @param biome the biome to add
     */
    public void addBiome(int id, Biome biome) {
        synchronized (lock) {
            Maps maps = this.maps;
            Maps newMaps = maps.copy();

            newMaps.nameToBiome.put(biome.name(), biome);
            newMaps.idToBiome.put(id, biome);
            newMaps.biomeToId.put(biome, id);

            this.maps = newMaps;
        }
    }

    /**
     * Adds multiple biomes to the manager. This does NOT send the new list to players.
     *
     * @param biomes the biomes to add, along with their identifiers.
     */
    public void addBiomes(@NotNull Collection<IntObjectPair<Biome>> biomes) {
        synchronized (lock) {
            Maps maps = this.maps;
            Maps newMaps = maps.copy();

            for (IntObjectPair<Biome> pair : biomes) {
                newMaps.nameToBiome.put(pair.right().name(), pair.right());
                newMaps.idToBiome.put(pair.firstInt(), pair.right());
                newMaps.biomeToId.put(pair.right(), pair.firstInt());
            }

            this.maps = newMaps;
        }
    }

    /**
     * Removes a biome. This does NOT send the new list to players.
     *
     * @param biome the biome to remove
     */
    public void removeBiome(Biome biome) {
        synchronized (lock) {
            Maps maps = this.maps;
            Maps newMaps = maps.copy();

            int id = newMaps.biomeToId.removeInt(biome);
            newMaps.nameToBiome.remove(biome.name());
            newMaps.idToBiome.remove(id);

            this.maps = newMaps;
        }
    }

    /**
     * Returns an immutable copy of the biomes already registered.
     *
     * @return an immutable copy of the biomes already registered
     */
    public Collection<Biome> unmodifiableCollection() {
        return Collections.unmodifiableCollection(maps.idToBiome.values());
    }

    /**
     * Gets a biome by its id.
     *
     * @param id the id of the biome
     * @return the {@link Biome} linked to this id
     */
    public Biome getById(int id) {
        return maps.idToBiome.get(id);
    }

    public Biome getByName(NamespaceID namespaceID) {
        return this.maps.nameToBiome.get(namespaceID);
    }

    public int getId(NamespaceID namespaceID) {
        Maps maps = this.maps;
        Biome biome = maps.nameToBiome.get(namespaceID);
        return maps.biomeToId.getInt(biome);
    }

    public int getIdOrDefault(NamespaceID namespaceID, int id) {
        Maps maps = this.maps;
        Biome biome = maps.nameToBiome.get(namespaceID);
        return biome == null ? id : maps.biomeToId.getInt(biome);
    }

    public int getId(Biome biome) {
        return this.maps.biomeToId.getInt(biome);
    }

    public NBTCompound toNBT() {
        Maps maps = this.maps;

        List<NBTCompound> biomeNBT = new ArrayList<>(maps.idToBiome.size());
        for (Int2ObjectMap.Entry<Biome> entry : maps.idToBiome.int2ObjectEntrySet()) {
            biomeNBT.add(entry.getValue().toNbt(entry.getIntKey()));
        }

        return NBT.Compound(Map.of(
                "type", NBT.String("minecraft:worldgen/biome"),
                "value", NBT.List(NBTType.TAG_Compound, biomeNBT)));
    }
}
