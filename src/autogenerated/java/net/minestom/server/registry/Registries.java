// AUTOGENERATED by net.minestom.codegen.RegistriesGenerator
package net.minestom.server.registry;

import net.kyori.adventure.key.Key;
import net.minestom.server.fluid.Fluid;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * AUTOGENERATED
 */
public final class Registries {

    /**
     * Should only be used for internal code, please use the get* methods.
     */
    @Deprecated
    public static final HashMap<NamespaceID, Fluid> fluids = new HashMap<>();

    /**
     * Returns the corresponding Fluid matching the given id. Returns 'EMPTY' if none match.
     */
    @NotNull
    public static Fluid getFluid(String id) {
        return getFluid(NamespaceID.from(id));
    }

    /**
     * Returns the corresponding Fluid matching the given id. Returns 'EMPTY' if none match.
     */
    @NotNull
    public static Fluid getFluid(NamespaceID id) {
        return fluids.getOrDefault(id, Fluid.EMPTY);
    }

    /**
     * Returns the corresponding Fluid matching the given key. Returns 'EMPTY' if none match.
     */
    @NotNull
    public static Fluid getFluid(Key key) {
        return getFluid(NamespaceID.from(key));
    }
}