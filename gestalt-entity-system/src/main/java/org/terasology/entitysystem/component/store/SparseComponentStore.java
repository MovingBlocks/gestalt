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

package org.terasology.entitysystem.component.store;

import org.terasology.entitysystem.component.ComponentIterator;
import org.terasology.entitysystem.component.ComponentStore;
import org.terasology.entitysystem.component.management.ComponentType;
import org.terasology.entitysystem.component.Component;
import org.terasology.entitysystem.entity.EntityRef;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class SparseComponentStore<T extends Component<T>> implements ComponentStore<T> {
    private final ComponentType<T> type;
    private final TIntObjectMap<T> store = new TIntObjectHashMap<>();

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
    public boolean set(EntityRef entity, T component) {
        int id = entity.getId();
        T stored = store.get(id);
        if (stored == null) {
            store.put(id, type.createCopy(component));
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
    public T remove(EntityRef entity) {
        return store.remove(entity.getId());
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
