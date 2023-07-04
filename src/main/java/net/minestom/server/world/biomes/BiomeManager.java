package net.minestom.server.world.biomes;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minestom.server.utils.NamespaceID;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTType;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;


/**
 * Allows servers to register custom dimensions. Also used during player joining to send the list of all existing dimensions.
 * <p>
 * Contains {@link Biome#PLAINS} by default but can be removed.
 */
public final class BiomeManager {
    private final Object2IntMap<NamespaceID> nameToId = new Object2IntOpenHashMap<>();
    private final Int2ObjectMap<Biome> idToBiome = new Int2ObjectOpenHashMap<>();

    public BiomeManager() {
        addBiome(0, Biome.PLAINS);
    }

    /**
     * Adds a new biome. This does NOT send the new list to players.
     *
     * @param biome the biome to add
     */
    public synchronized void addBiome(int id, Biome biome) {
        this.nameToId.put(biome.name(), id);
        this.idToBiome.put(id, biome);
    }

    /**
     * Removes a biome. This does NOT send the new list to players.
     *
     * @param biome the biome to remove
     */
    public synchronized void removeBiome(Biome biome) {
        int id = this.nameToId.removeInt(biome.name());
        this.idToBiome.remove(id);
    }

    /**
     * Returns an immutable copy of the biomes already registered.
     *
     * @return an immutable copy of the biomes already registered
     */
    public synchronized Collection<Biome> unmodifiableCollection() {
        return Collections.unmodifiableCollection(idToBiome.values());
    }

    /**
     * Gets a biome by its id.
     *
     * @param id the id of the biome
     * @return the {@link Biome} linked to this id
     */
    public synchronized Biome getById(int id) {
        return this.idToBiome.get(id);
    }

    public synchronized Biome getByName(NamespaceID namespaceID) {
        return this.idToBiome.get(this.nameToId.getInt(namespaceID));
    }

    public synchronized int getId(NamespaceID namespaceID) {
        return this.nameToId.getInt(namespaceID);
    }

    public synchronized int getId(Biome biome) {
        return this.nameToId.getInt(biome.name());
    }

    public synchronized NBTCompound toNBT() {
        return NBT.Compound(Map.of(
                "type", NBT.String("minecraft:worldgen/biome"),
                "value", NBT.List(NBTType.TAG_Compound, idToBiome.int2ObjectEntrySet().stream().map(entry -> entry.getValue().toNbt(entry.getIntKey())).toList())));
    }
}
