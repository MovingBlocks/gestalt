/*
 * Copyright 2019 MovingBlocks
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
package org.terasology.benchmarks.bouncingballs.sparsestore;

import com.google.common.collect.Lists;

import org.terasology.benchmarks.bouncingballs.arraystore.ArrayComponentStore;
import org.terasology.benchmarks.bouncingballs.common.ComponentIterator;
import org.terasology.benchmarks.bouncingballs.common.ComponentSpliterator;
import org.terasology.benchmarks.bouncingballs.common.ComponentStore;
import org.terasology.entitysystem.component.ComponentType;
import org.terasology.entitysystem.core.Component;

import java.lang.reflect.Array;
import java.util.List;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * A table for storing entities and components. Focused on allowing iteration across a components of a given type
 *
 * @author Immortius
 */
public class SparseComponentStore<T extends Component<T>> implements ComponentStore<T> {
    private final ComponentType<T> type;
    private final TIntObjectMap<T> ids = new TIntObjectHashMap<>();

    public SparseComponentStore(ComponentType<T> type) {
        this.type = type;
    }

    @Override
    public boolean get(int entityId, T into) {
        if (ids.get(entityId) == null) {
            return false;
        }
        into.copy(ids.get(entityId));
        return true;
    }

    public void set(int entityId, T component) {
        T stored = ids.get(entityId);
        if (stored == null) {
            stored = type.createCopy(component);
            ids.put(entityId, stored);
        } else {
            stored.copy(component);
        }
    }

    @Override
    public int size() {
        return ids.size();
    }

    @Override
    public ComponentIterator iterate() {
        return new SparseComponentIterator<T>(ids);
    }

    @Override
    public ComponentSpliterator<T> spliterate() {
        return new SparseComponentSpliterator<>(ids);
    }

    @Override
    public void remove(int id) {
        ids.remove(id);
    }

    @Override
    public Class<T> getType() {
        return type.getComponentClass();
    }

    @Override
    public void extend(int capacity) {
    }

    private static class SparseComponentIterator<T extends Component<T>> implements ComponentIterator<T> {

        final TIntObjectIterator<T> iterator;
        int currentId = -1;

        SparseComponentIterator(TIntObjectMap<T> ids) {
            iterator = ids.iterator();
        }

        @Override
        public boolean next(T component) {
            if (iterator.hasNext()) {
                iterator.advance();
                currentId = iterator.key();
                component.copy(iterator.value());
                return true;
            }
            return false;
        }

        @Override
        public int getEntityId() {
            return currentId;
        }
    }

    private static class SparseComponentSpliterator<T extends Component<T>> implements ComponentSpliterator<T> {

        private final int[] keys;
        private final TIntObjectMap<T> store;
        private int index;
        private int endIndex;

        public SparseComponentSpliterator(TIntObjectMap<T> ids) {
            this.keys = ids.keys();
            this.store = ids;
            this.index = -1;
            this.endIndex = keys.length;
        }

        private SparseComponentSpliterator(TIntObjectMap<T> ids, int[] keys, int start, int length) {
            this.keys = keys;
            this.store = ids;
            this.endIndex = length + start;
            this.index = start - 1;
        }

        @Override
        public boolean next(T component) {
            index++;
            if (index < endIndex) {
                component.copy(store.get(keys[index]));
                return true;
            }
            return false;
        }

        @Override
        public ComponentSpliterator<T> split() {
            int length = endIndex - index + 1;
            int splitLength = length / 2;
            endIndex -= splitLength;

            return new SparseComponentSpliterator<>(store, keys, endIndex, splitLength);
        }

        @Override
        public int getEntityId() {
            return index;
        }

    }
}
