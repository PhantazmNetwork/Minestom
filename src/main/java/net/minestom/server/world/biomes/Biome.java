package net.minestom.server.world.biomes;

import net.minestom.server.coordinate.Point;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public final class Biome {
    private static final BiomeEffects DEFAULT_EFFECTS = BiomeEffects.builder()
            .fogColor(0xC0D8FF)
            .skyColor(0x78A7FF)
            .waterColor(0x3F76E4)
            .waterFogColor(0x50533)
            .build();

    //A plains biome has to be registered or else minecraft will crash
    public static final Biome PLAINS = Biome.builder()
            .name(NamespaceID.from("minecraft:plains"))
            .temperature(0.8F)
            .downfall(0.4F)
            .effects(DEFAULT_EFFECTS)
            .build();

    private final NamespaceID name;
    private final float temperature;
    private final float downfall;
    private final BiomeEffects effects;
    private final Precipitation precipitation;
    private final TemperatureModifier temperatureModifier;

    Biome(NamespaceID name, float temperature, float downfall, BiomeEffects effects, Precipitation precipitation, TemperatureModifier temperatureModifier) {
        this.name = name;
        this.temperature = temperature;
        this.downfall = downfall;
        this.effects = effects;
        this.precipitation = precipitation;
        this.temperatureModifier = temperatureModifier;
    }

    public static Builder builder() {
        return new Builder();
    }

    public @NotNull NBTCompound toNbt(int id) {
        Check.notNull(name, "The biome namespace cannot be null");
        Check.notNull(effects, "The biome effects cannot be null");

        return NBT.Compound(nbt -> {
            nbt.setString("name", name.toString());
            nbt.setInt("id", id);

            nbt.set("element", NBT.Compound(element -> {
                element.setFloat("temperature", temperature);
                element.setFloat("downfall", downfall);
                element.setString("precipitation", precipitation.name().toLowerCase(Locale.ROOT));
                if (temperatureModifier != TemperatureModifier.NONE)
                    element.setString("temperature_modifier", temperatureModifier.name().toLowerCase(Locale.ROOT));
                element.set("effects", effects.toNbt());
            }));
        });
    }

    public NamespaceID name() {
        return this.name;
    }

    public float temperature() {
        return this.temperature;
    }

    public float downfall() {
        return this.downfall;
    }

    public BiomeEffects effects() {
        return this.effects;
    }

    public Precipitation precipitation() {
        return this.precipitation;
    }

    public TemperatureModifier temperatureModifier() {
        return this.temperatureModifier;
    }

    public enum Precipitation {
        NONE, RAIN, SNOW;
    }

    public enum TemperatureModifier {
        NONE, FROZEN;
    }

    public static final class Builder {
        private NamespaceID name;
        private float temperature = 0.25f;
        private float downfall = 0.8f;
        private BiomeEffects effects = DEFAULT_EFFECTS;
        private Precipitation precipitation = Precipitation.RAIN;
        private TemperatureModifier temperatureModifier = TemperatureModifier.NONE;

        Builder() {
        }

        public Builder name(NamespaceID name) {
            this.name = name;
            return this;
        }

        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder downfall(float downfall) {
            this.downfall = downfall;
            return this;
        }

        public Builder effects(BiomeEffects effects) {
            this.effects = effects;
            return this;
        }

        public Builder precipitation(Precipitation precipitation) {
            this.precipitation = precipitation;
            return this;
        }

        public Builder temperatureModifier(TemperatureModifier temperatureModifier) {
            this.temperatureModifier = temperatureModifier;
            return this;
        }

        public Biome build() {
            return new Biome(name, temperature, downfall, effects, precipitation, temperatureModifier);
        }
    }

    public interface Setter {
        void setBiome(int x, int y, int z, @NotNull Biome biome);

        default void setBiome(@NotNull Point blockPosition, @NotNull Biome biome) {
            setBiome(blockPosition.blockX(), blockPosition.blockY(), blockPosition.blockZ(), biome);
        }
    }

    public interface Getter {
        @NotNull Biome getBiome(int x, int y, int z);

        default @NotNull Biome getBiome(@NotNull Point point) {
            return getBiome(point.blockX(), point.blockY(), point.blockZ());
        }
    }
}
