package net.minestom.server.entity.state;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

public sealed interface CancellableState<T> permits CancellableState.CancellableStateImpl {
    sealed interface Holder<V> permits HolderImpl {
        @NotNull @Unmodifiable Set<Key> stages();

        @NotNull @Unmodifiable Set<CancellableState<V>> states(@Nullable Key stage);

        void setStage(@Nullable Key newStage);

        void registerState(@NotNull Key stage, @NotNull CancellableState<V> state);

        void removeState(@NotNull Key stage, @NotNull CancellableState<V> state);

        @Nullable Key currentStage();

        @NotNull V self();
    }

    sealed interface Builder<V> permits BuilderImpl {
        @NotNull Builder<V> start(@NotNull Consumer<? super @NotNull V> start);

        @NotNull Builder<V> end(@NotNull Consumer<? super @NotNull V> end);

        @NotNull CancellableState<V> build();
    }

    static <T> @NotNull Holder<T> holder(@NotNull T self) {
        Objects.requireNonNull(self);
        return new HolderImpl<>(self);
    }

    static <T> @NotNull CancellableState<T> state(@NotNull T self, @NotNull Consumer<? super @NotNull T> start,
                                                  @NotNull Consumer<? super @NotNull T> end) {
        Objects.requireNonNull(self);
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);

        return new CancellableStateImpl<>(self, start, end);
    }

    static <T> @NotNull Builder<T> builder(@NotNull T self) {
        Objects.requireNonNull(self);
        return new BuilderImpl<>(self);
    }

    @NotNull T self();

    void start();

    void end();

    final class BuilderImpl<V> implements Builder<V> {
        private final V self;
        private Consumer<? super @NotNull V> start = ignored -> {
        };
        private Consumer<? super @NotNull V> end = ignored -> {
        };

        private BuilderImpl(V self) {
            this.self = self;
        }

        @Override
        public @NotNull Builder<V> start(@NotNull Consumer<? super @NotNull V> start) {
            Objects.requireNonNull(start);
            this.start = start;
            return this;
        }

        @Override
        public @NotNull Builder<V> end(@NotNull Consumer<? super @NotNull V> end) {
            Objects.requireNonNull(end);
            this.end = end;
            return this;
        }

        @Override
        public @NotNull CancellableState<V> build() {
            return new CancellableStateImpl<>(self, start, end);
        }
    }

    final class CancellableStateImpl<V> implements CancellableState<V> {
        private final V self;
        private final Consumer<? super V> start;
        private final Consumer<? super V> end;

        private CancellableStateImpl(V self, Consumer<? super V> start, Consumer<? super V> end) {
            this.self = self;
            this.start = start;
            this.end = end;
        }

        @Override
        public @NotNull V self() {
            return self;
        }

        @Override
        public void start() {
            start.accept(self);
        }

        @Override
        public void end() {
            end.accept(self);
        }
    }

    final class HolderImpl<V> implements Holder<V> {
        private final Map<Key, Set<CancellableState<V>>> stateMap;
        private final Object lock = new Object();

        private final V self;

        private volatile Key currentStage;
        private volatile Set<CancellableState<V>> currentStates;

        private HolderImpl(V self) {
            this.stateMap = new ConcurrentHashMap<>();
            this.self = self;
        }

        @Override
        public @NotNull @Unmodifiable Set<Key> stages() {
            return Set.copyOf(stateMap.keySet());
        }

        @Override
        public @NotNull @Unmodifiable Set<CancellableState<V>> states(@Nullable Key stage) {
            if (stage == null) {
                return Set.of();
            }

            return Set.copyOf(stateMap.getOrDefault(stage, Set.of()));
        }

        @Override
        public void setStage(@Nullable Key newStage) {
            synchronized (lock) {
                Key currentStage = this.currentStage;
                if (Objects.equals(currentStage, newStage)) {
                    return;
                }

                Set<CancellableState<V>> currentStates = this.currentStates;
                if (newStage == null) {
                    if (currentStates == null) {
                        return;
                    }

                    for (CancellableState<V> state : currentStates) {
                        state.end();
                    }

                    return;
                }

                Set<CancellableState<V>> newStates = stateMap.get(newStage);
                if (newStates == null) {
                    throw new IllegalArgumentException("stage " + newStage + " does not exist");
                }

                if (currentStates != null) {
                    for (CancellableState<V> state : currentStates) {
                        state.end();
                    }
                }

                for (CancellableState<V> state : newStates) {
                    state.start();
                }

                this.currentStage = newStage;
                this.currentStates = newStates;
            }
        }

        @Override
        public void registerState(@NotNull Key stage, @NotNull CancellableState<V> state) {
            Objects.requireNonNull(stage);
            Objects.requireNonNull(state);

            if (state.self() != self) {
                throw new IllegalArgumentException("cannot register state belonging to another object");
            }

            synchronized (lock) {
                if (stage.equals(this.currentStage)) {
                    state.start();
                }

                stateMap.computeIfAbsent(stage, states -> new CopyOnWriteArraySet<>()).add(state);
            }
        }

        @Override
        public void removeState(@NotNull Key stage, @NotNull CancellableState<V> state) {
            Objects.requireNonNull(stage);
            Objects.requireNonNull(state);

            if (state.self() != self) {
                throw new IllegalArgumentException("cannot remove state belonging to another object");
            }

            synchronized (lock) {
                Set<CancellableState<V>> states = stateMap.get(stage);
                if (states == null) {
                    throw new IllegalArgumentException("stage " + stage + " does not exist");
                }

                if (!states.remove(state)) {
                    return;
                }

                if (stage.equals(this.currentStage)) {
                    state.end();
                }

                if (states.isEmpty()) {
                    stateMap.remove(stage);
                }
            }
        }

        @Override
        public @Nullable Key currentStage() {
            return currentStage;
        }

        @Override
        public @NotNull V self() {
            return self;
        }
    }
}