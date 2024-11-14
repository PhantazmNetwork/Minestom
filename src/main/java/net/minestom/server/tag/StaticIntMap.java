package net.minestom.server.tag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.function.Consumer;

sealed interface StaticIntMap<T> permits StaticIntMap.Array, StaticIntMap.Hash {

    T get(@Range(from = 0, to = Integer.MAX_VALUE) int key);

    void forValues(@NotNull Consumer<T> consumer);

    @NotNull StaticIntMap<T> copy();

    // Methods potentially causing re-hashing

    void put(@Range(from = 0, to = Integer.MAX_VALUE) int key, T value);

    void remove(@Range(from = 0, to = Integer.MAX_VALUE) int key);

    void updateContent(@NotNull StaticIntMap<T> content);

    void clear();

    final class Array<T> implements StaticIntMap<T> {
        private static final Object[] EMPTY_ARRAY = new Object[0];

        private T[] array;

        public Array(T[] array) {
            this.array = array;
        }

        public Array() {
            //noinspection unchecked
            this.array = (T[]) EMPTY_ARRAY;
        }

        @Override
        public T get(int key) {
            final T[] array = this.array;
            return key < array.length ? array[key] : null;
        }

        @Override
        public void forValues(@NotNull Consumer<T> consumer) {
            final T[] array = this.array;
            for (T value : array) {
                if (value != null) consumer.accept(value);
            }
        }

        @Override
        public @NotNull StaticIntMap<T> copy() {
            return new Array<>(array.clone());
        }

        @Override
        public void put(int key, T value) {
            T[] array = this.array;
            if (key >= array.length) {
                array = updateArray(Arrays.copyOf(array, key * 2 + 1));
            }
            array[key] = value;
        }

        @Override
        public void updateContent(@NotNull StaticIntMap<T> content) {
            if (content instanceof StaticIntMap.Array<T> arrayMap) {
                updateArray(arrayMap.array.clone());
            } else {
                throw new IllegalArgumentException("Invalid content type: " + content.getClass());
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void clear() {
            this.array = (T[]) EMPTY_ARRAY;
        }

        @Override
        public void remove(int key) {
            T[] array = this.array;
            if (key < array.length) array[key] = null;
        }

        T[] updateArray(T[] result) {
            this.array = result;
            return result;
        }
    }

    final class Hash<T> implements StaticIntMap<T> {
        private static final Entries EMPTY_ENTRIES = new Entries(new int[0], new Object[0]);
        private static final float LOAD_FACTOR = 0.75F;

        private record Entries(int[] keys, Object[] values) {
            private static Entries copy(int[] keys, Object[] values) {
                assert keys.length == values.length;

                final int[] newKeys = new int[keys.length];
                final Object[] newValues = new Object[keys.length];

                for (int i = 0; i < newKeys.length; i++) {
                    final int k = keys[i];
                    final Object v = values[i];

                    newKeys[i] = k;
                    if (k == -1 || k > 0) newValues[i] = v;
                }

                return new Entries(newKeys, newValues);
            }
        }

        private volatile Entries entries;
        private int size;

        private Hash(Entries entries) {
            this.entries = entries;
            this.size = computeSize(entries.keys);
        }

        public Hash() {
            this(EMPTY_ENTRIES);
        }

        private static int computeSize(int[] k) {
            int size = 0;
            for (int key : k) {
                if (key == -1 || key > 0) size++;
            }
            return size;
        }

        private static int probeIndex(int start, int i, int mask) {
            return (((start << 1) + i + (i * i)) >> 1) & mask;
        }

        private static int probeKey(int key, int[] k) {
            final int mask = k.length - 1;
            final int start = key & mask;

            for (int i = 0; i < k.length; i++) {
                final int probeIndex = probeIndex(start, i, mask);
                final int sample = k[probeIndex];

                if (sample == key) return probeIndex;
                else if (sample == 0) return -1;
            }

            return -1;
        }

        private static int probeEmpty(int key, int[] k) {
            final int mask = k.length - 1;
            final int start = key & mask;

            for (int i = 0; i < k.length; i++) {
                final int probeIndex = probeIndex(start, i, mask);

                if (k[probeIndex] == 0) return probeIndex;
            }

            return -1;
        }

        private static int probePut(int key, int[] k) {
            final int mask = k.length - 1;
            final int start = key & mask;

            int tombstoneIndex = -1;
            for (int i = 0; i < k.length; i++) {
                final int probeIndex = probeIndex(start, i, mask);
                final int sample = k[probeIndex];

                if (tombstoneIndex == -1 && sample == -2) tombstoneIndex = probeIndex;
                else if (sample == key) return probeIndex;
                else if (sample == 0) return tombstoneIndex == -1 ? probeIndex : tombstoneIndex;
            }

            return tombstoneIndex;
        }

        @SuppressWarnings("unchecked")
        private void rehash(int newSize) {
            final Entries entries = this.entries;
            final int[] k = entries.keys;
            final T[] v = (T[]) entries.values;

            final int[] newK = new int[newSize];
            final T[] newV = (T[]) new Object[newSize];

            for (int i = 0; i < k.length; i++) {
                final int oldKey = k[i];
                if (oldKey == 0 || oldKey == -2) continue;

                final int newIndex = probeEmpty(oldKey, newK);

                // shouldn't happen unless rehashing to a newSize that can't fit all elements
                assert newIndex != -1 : "Could not find space for rehashed element";

                newK[newIndex] = oldKey;
                newV[newIndex] = v[i];
            }

            this.entries = new Entries(newK, newV);
        }

        @SuppressWarnings("unchecked")
        @Override
        public T get(int key) {
            final Entries entries = this.entries;
            final int[] k = entries.keys;
            final T[] v = (T[]) entries.values;

            if (k.length == 0) return null;

            if (key == 0) key--;
            final int index = probeKey(key, k);
            if (index == -1) return null;

            return v[index];
        }

        @SuppressWarnings("unchecked")
        @Override
        public void forValues(Consumer<T> consumer) {
            final Entries entries = this.entries;
            final int[] k = entries.keys;
            final T[] v = (T[]) entries.values;

            for (int i = 0; i < k.length; i++) {
                final int index = k[i];
                if (index == 0 || index == -2) continue;
                consumer.accept(v[i]);
            }
        }

        @Override
        public StaticIntMap<T> copy() {
            final Entries entries = this.entries;
            return new Hash<>(Entries.copy(entries.keys, entries.values));
        }

        @SuppressWarnings("unchecked")
        @Override
        public void put(int key, T value) {
            final Entries entries = this.entries;

            final int[] k = entries.keys;
            final T[] v = (T[]) entries.values;
            if (key == 0) key--;

            if (k.length == 0) {
                final int[] newK = new int[4];
                final T[] newV = (T[]) new Object[4];

                int index = key & 3;
                newK[index] = key;
                newV[index] = value;

                this.entries = new Entries(newK, newV);
                this.size = 1;
                return;
            }

            final int index = probePut(key, k);
            if (index != -1) {
                v[index] = value;
                VarHandle.storeStoreFence(); // new value must appear before the key is set
                final int oldKey = k[index];
                if (oldKey == 0 || oldKey == -2) {
                    k[index] = key;
                    this.size++;
                }

                if (this.size + 1 >= (int) (k.length * LOAD_FACTOR)) rehash(k.length << 1);
                return;
            }

            // should be unreachable, we always reserve a bit of space even if the load factor is 1
            throw new IllegalStateException("Unable to find space for value");
        }

        @SuppressWarnings("unchecked")
        @Override
        public void remove(int key) {
            final Entries entries = this.entries;
            final int[] k = entries.keys;
            final T[] v = (T[]) entries.values;

            if (k.length == 0) return;

            if (key == 0) key--;
            final int index = probeKey(key, k);
            if (index == -1) return;

            k[index] = -2;
            VarHandle.storeStoreFence(); // key must indicate "no value" before the actual value is nulled
            v[index] = null;

            if (--this.size == 0) this.entries = EMPTY_ENTRIES;
            else if (this.size + 1 <= (int) ((1F - LOAD_FACTOR) * k.length)) rehash(k.length >> 1);
        }

        @Override
        public void updateContent(StaticIntMap<T> content) {
            if (content instanceof StaticIntMap.Hash<T> other) {
                final Entries otherEntries = other.entries;

                final Entries newEntries = Entries.copy(otherEntries.keys, otherEntries.values);
                final int newSize = computeSize(newEntries.keys);

                this.entries = newEntries;
                this.size = newSize;
            } else throw new IllegalArgumentException("Invalid content type");
        }

        @Override
        public void clear() {
            this.entries = EMPTY_ENTRIES;
            this.size = 0;
        }
    }
}
