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

package org.terasology.gestalt.entitysystem.component.store;

import org.terasology.gestalt.entitysystem.component.ComponentIterator;
import org.terasology.gestalt.entitysystem.component.management.ComponentType;
import org.terasology.gestalt.entitysystem.component.Component;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * SparseComponentStore is a component store based on a hash map. This has slower performance for gets
 * and sets (although still O(1)), but uses less memory and can be faster for iteration in some cases.
 *
 * SparseComponentStore can be used if memory is an issue, for component types that are used on few
 * entities.
 * @param <T> The type of component stored in this store
 */
public class SparseComponentStore<T extends Component<T>> implements ComponentStore<T> {
    private final ComponentType<T> type;
    private final TIntObjectMap<T> store = new TIntObjectHashMap<>();

    /**
     * @param type Type information for the component type stored in store
     */
    public SparseComponentStore(ComponentType<T> type) {
        this.type = type;
    }

    @Override
    public ComponentType<T> getType() {
        return type;
    }

    @Override
    public boolean has(int entityId) {
        return store.containsKey(entityId);
    }

    @Override
    public boolean get(int entityId, T into) {
        T source = store.get(entityId);
        if (source == null) {
            return false;
        }
        into.copy(source);
        return true;
    }

    @Override
    public boolean set(int entityId, T component) {
        T stored = store.get(entityId);
        if (stored == null) {
            store.put(entityId, type.createCopy(component));
            return true;
        } else {
            stored.copy(component);
            return false;
        }
    }

    @Override
    public int iterationCost() {
        return store.size();
    }

    @Override
    public ComponentIterator<T> iterate() {
        return new SparseComponentIterator();
    }

    @Override
    public T remove(int entityId) {
        return store.remove(entityId);
    }

    @Override
    public void extend(int capacity) {
        // No action required
    }

    private class SparseComponentIterator implements ComponentIterator<T> {

        final TIntObjectIterator<T> iterator;

        private SparseComponentIterator() {
            iterator = store.iterator();
        }

        @Override
        public boolean next() {
            if (iterator.hasNext()) {
                iterator.advance();
                return true;
            }
            return false;
        }

        @Override
        public int getEntityId() {
            return iterator.key();
        }

        @Override
        public void getComponent(Component<T> component) {
            component.copy(iterator.value());
        }
    }

}
