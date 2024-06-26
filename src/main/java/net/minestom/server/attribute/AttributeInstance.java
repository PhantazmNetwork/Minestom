package net.minestom.server.attribute;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Represents an instance of an attribute and its modifiers.
 */
public final class AttributeInstance {
    private final Attribute attribute;
    private final Map<UUID, AttributeModifier> modifiers = new ConcurrentHashMap<>();
    private final Consumer<AttributeInstance> propertyChangeListener;
    private float baseValue;
    private float cachedValue = 0.0f;

    public AttributeInstance(@NotNull Attribute attribute, @Nullable Consumer<AttributeInstance> listener) {
        this.attribute = attribute;
        this.propertyChangeListener = listener;
        this.baseValue = attribute.defaultValue();
        refreshCachedValue();
    }

    /**
     * Gets the attribute associated to this instance.
     *
     * @return the associated attribute
     */
    public @NotNull Attribute getAttribute() {
        return attribute;
    }

    /**
     * The base value of this instance without modifiers
     *
     * @return the instance base value
     * @see #setBaseValue(float)
     */
    public float getBaseValue() {
        return baseValue;
    }

    /**
     * Sets the base value of this instance.
     *
     * @param baseValue the new base value
     * @see #getBaseValue()
     */
    public void setBaseValue(float baseValue) {
        if (this.baseValue != baseValue) {
            this.baseValue = baseValue;
            refreshCachedValue();
        }
    }

    /**
     * Add a modifier to this instance.
     *
     * @param modifier the modifier to add
     */
    public void addModifier(@NotNull AttributeModifier modifier) {
        if (modifiers.putIfAbsent(modifier.getId(), modifier) == null) {
            refreshCachedValue();
        }
    }

    /**
     * Remove a modifier from this instance.
     *
     * @param modifier the modifier to remove
     */
    public void removeModifier(@NotNull AttributeModifier modifier) {
        removeModifier(modifier.getId());
    }

    /**
     * Remove a modifier from this instance.
     *
     * @param uuid The UUID of the modifier to remove
     */
    public AttributeModifier removeModifier(@NotNull UUID uuid) {
        AttributeModifier modifier = modifiers.remove(uuid);
        if (modifier != null) {
            refreshCachedValue();
        }

        return modifier;
    }

    /**
     * Get the modifiers applied to this instance.
     *
     * @return the modifiers.
     */
    @NotNull
    public @Unmodifiable Collection<AttributeModifier> getModifiers() {
        return List.copyOf(modifiers.values());
    }

    /**
     * Gets the value of this instance calculated with modifiers applied.
     *
     * @return the attribute value
     */
    public float getValue() {
        return cachedValue;
    }

    /**
     * Recalculate the value of this attribute instance using the modifiers.
     */
    private void refreshCachedValue() {
        final Collection<AttributeModifier> modifiers = getModifiers();
        float base = getBaseValue();

        for (AttributeModifier modifier : modifiers) {
            if (modifier.getOperation() == AttributeOperation.ADDITION) {
                base += (float) modifier.getAmount();
            }
        }

        float result = base;
        for (AttributeModifier modifier : modifiers) {
            if (modifier.getOperation() == AttributeOperation.MULTIPLY_BASE) {
                result += (float) (base * modifier.getAmount());
            }
        }

        for (AttributeModifier modifier : modifiers) {
            if (modifier.getOperation() == AttributeOperation.MULTIPLY_TOTAL) {
                result *= (float) (1.0f + modifier.getAmount());
            }
        }

        this.cachedValue = result;

        // Signal entity
        if (propertyChangeListener != null) {
            propertyChangeListener.accept(this);
        }
    }
}
