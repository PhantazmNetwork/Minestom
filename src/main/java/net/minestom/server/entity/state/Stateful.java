package net.minestom.server.entity.state;

import org.jetbrains.annotations.NotNull;

public interface Stateful<T> {
    CancellableState.@NotNull Holder<T> stateHolder();
}
