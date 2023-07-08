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
    private volatile Maps maps = new Maps(new Object2IntOpenHashMap<>(), new Int2ObjectOpenHashMap<>());
    private final Object lock = new Object();

    private record Maps(Object2IntMap<NamespaceID> nameToId, Int2ObjectMap<Biome> idToBiome) {
        private Maps copy() {
            return new Maps(new Object2IntOpenHashMap<>(nameToId), new Int2ObjectOpenHashMap<>(idToBiome));
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

            newMaps.nameToId.put(biome.name(), id);
            newMaps.idToBiome.put(id, biome);

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
                newMaps.nameToId.put(pair.right().name(), pair.firstInt());
                newMaps.idToBiome.put(pair.firstInt(), pair.right());
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

            int id = newMaps.nameToId.removeInt(biome.name());
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
        Maps maps = this.maps;
        return maps.idToBiome.get(maps.nameToId.getInt(namespaceID));
    }

    public int getId(NamespaceID namespaceID) {
        return maps.nameToId.getInt(namespaceID);
    }

    public int getId(Biome biome) {
        return getId(biome.name());
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
