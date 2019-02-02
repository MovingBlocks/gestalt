/*
 * Copyright 2015 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.util.collection;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * TypeKeyedMap is a specialized map wrapper for use when the key of the map is a the class that the value implements. It more strongly enforces that values must be
 * instances of their respective key, and provides a nicer interface for retrieving values.
 */
public class TypeKeyedMap<T> {
    private Map<Class<? extends T>, T> inner;

    /**
     * Constructs a TypeKeyedMap, based on a LinkedHashMap.
     */
    public TypeKeyedMap() {
        this(Maps::newLinkedHashMap);
    }

    /**
     * Constructs a TypeKeyedMap, using the provided supplier to produce the implementing map
     *
     * @param mapSupplier The supplier of the map implementation
     */
    public TypeKeyedMap(Supplier<Map<Class<? extends T>, T>> mapSupplier) {
        this(mapSupplier.get());
    }

    /**
     * Constructs a TypeKeyedMap, using the provided implementing map
     *
     * @param baseMap The map to encapsulate
     */
    public TypeKeyedMap(Map<Class<? extends T>, T> baseMap) {
        inner = baseMap;
    }

    /**
     * @return The number of entries in the map. Greater or equal to 0
     */
    public int size() {
        return inner.size();
    }

    /**
     * @return Whether them map is empty (size == 0)
     */
    public boolean isEmpty() {
        return inner.isEmpty();
    }

    /**
     * @param key
     * @return Whether the map contains the provided key
     */
    public boolean containsKey(Class<? extends T> key) {
        return inner.containsKey(key);
    }

    /**
     * @param value
     * @param <U>
     * @return Whether the map contains the provided value
     */
    public <U extends T> boolean containsValue(U value) {
        return inner.containsValue(value);
    }

    /**
     * @param key
     * @param <U>
     * @return The value associated with the provided key, or null if there is no such value
     */
    public <U extends T> U get(Class<U> key) {
        return key.cast(inner.get(key));
    }

    /**
     * Adds a value to the map, against the provided key
     *
     * @param key
     * @param value
     * @param <U>
     * @return The previous value associated with the key
     */
    public <U extends T> U put(Class<U> key, U value) {
        Preconditions.checkArgument(key.isInstance(value));
        return key.cast(inner.put(key, value));
    }

    /**
     * Removes a key and its associated value from the map.
     *
     * @param key
     * @param <U>
     * @return The value associated with the removed key
     */
    public <U extends T> U remove(Class<U> key) {
        return key.cast(inner.remove(key));
    }

    /**
     * Removes all entries from the map
     */
    public void clear() {
        inner.clear();
    }

    /**
     * @return A set of all keys in the map. This is a live view - changes to the set will change the map.
     */
    public Set<Class<? extends T>> keySet() {
        return inner.keySet();
    }

    /**
     * @return A collection of all values in the map. This is a live view - changes to the collection will change the map.
     */
    public Collection<T> values() {
        return inner.values();
    }

    /**
     * @return A collection of all entries in the map. This is a live view - changes to the set will change the map.
     */
    public Set<Entry<? extends T>> entrySet() {
        return new EntrySet(inner.entrySet());
    }

    public void forEach(EntryConsumer<T> action) {
        for (Entry<? extends T> entry : entrySet()) {
            entry.handle(action);
        }
    }

    /**
     * @return The internal map
     */
    public Map<Class<? extends T>, T> getInner() {
        return inner;
    }

    @FunctionalInterface
    public interface EntryConsumer<T> {
        <U extends T> void accept(Class<U> type, U value);
    }

    private static class EntrySet<T> implements Set<Entry<T>> {

        private Set<Map.Entry<Class<? extends T>, T>> innerSet;

        private EntrySet(Set<Map.Entry<Class<? extends T>, T>> inner) {
            this.innerSet = inner;
        }

        @Override
        public int size() {
            return innerSet.size();
        }

        @Override
        public boolean isEmpty() {
            return innerSet.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return innerSet.contains(o);
        }

        @Override
        public Iterator<Entry<T>> iterator() {
            return new EntrySetIterator(innerSet.iterator());
        }

        @Override
        public Object[] toArray() {
            return innerSet.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return innerSet.toArray(a);
        }

        @Override
        public boolean add(Entry<T> tEntry) {
            throw new UnsupportedOperationException("Cannot add to entry set");
        }

        @Override
        public boolean remove(Object o) {
            return o instanceof Entry && innerSet.remove(((Entry) o).getInnerEntry());
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends Entry<T>> c) {
            throw new UnsupportedOperationException("Cannot add to entry set");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            innerSet.clear();
        }
    }

    private static class EntrySetIterator<T> implements Iterator<Entry<? extends T>> {
        private Iterator<Map.Entry<Class<? extends T>, T>> inner;

        private EntrySetIterator(Iterator<Map.Entry<Class<? extends T>, T>> iterator) {
            this.inner = iterator;
        }

        @Override
        public boolean hasNext() {
            return inner.hasNext();
        }

        @Override
        public Entry<? extends T> next() {
            Map.Entry<Class<? extends T>, T> next = inner.next();
            return new Entry(next);
        }
    }

    /**
     * An entry in a TypeKeyedMap.
     *
     * @param <T>
     */
    public static class Entry<T> {
        private Map.Entry<Class<T>, T> innerEntry;

        private Entry(Map.Entry<Class<T>, T> innerEntry) {
            this.innerEntry = innerEntry;
        }

        public Class<T> getKey() {
            return innerEntry.getKey();
        }

        public T getValue() {
            return innerEntry.getValue();
        }

        private Map.Entry<Class<T>, T> getInnerEntry() {
            return innerEntry;
        }

        public void handle(EntryConsumer<? super T> action) {
            action.accept(getKey(), getValue());
        }
    }
}
