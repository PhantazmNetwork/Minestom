package net.minestom.server.utils.function;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@FunctionalInterface
public interface IntegerBiPredicate {
    boolean accept(int v1, int v2);
}