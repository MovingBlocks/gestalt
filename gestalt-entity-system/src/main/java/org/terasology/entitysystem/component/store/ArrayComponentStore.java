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
import org.terasology.entitysystem.component.management.ComponentType;
import org.terasology.entitysystem.component.Component;
import org.terasology.entitysystem.entity.EntityRef;

import java.lang.reflect.Array;

/**
 * ArrayComponentStore is a ComponentStore built atop a simple array. This provides good performance
 * for gets and sets, but memory usage and iteration speed is based on the number of possibly entities.
 *
 * This is the recommended store for components with high usage, and otherwise if there is any doubt
 * which store to use.
 * @param <T> The type of component stored in this ComponentStore
 */
public class ArrayComponentStore<T extends Component<T>> implements ComponentStore<T> {
    private final ComponentType<T> type;
    private T[] store;

    /**
     * @param componentType Type information for the component type to store
     */
    public ArrayComponentStore(ComponentType<T> componentType) {
        this(componentType, 1000);
    }

    /**
     *
     * @param type Type information for the component type to store
     * @param initialCapacity The initial capacity of the array
     */
    @SuppressWarnings("unchecked")
    public ArrayComponentStore(ComponentType<T> type, int initialCapacity) {
        this.type = type;
        store = (T[]) Array.newInstance(type.getComponentClass(), initialCapacity);
    }

    @Override
    public ComponentType<T> getType() {
        return type;
    }

    @Override
    public boolean has(int entityId) {
        return store[entityId] != null;
    }

    @Override
    public boolean get(int entityId, T into) {
        T value = store[entityId];
        if (value != null) {
            into.copy(store[entityId]);
            return true;
        }
        return false;
    }

    @Override
    public boolean set(int entityId, T component) {
        if (store[entityId] == null) {
            store[entityId] = type.createCopy(component);
            return true;
        } else {
            store[entityId].copy(component);
            return false;
        }
    }

    @Override
    public T remove(int entityId) {
        T result = store[entityId];
        store[entityId] = null;
        return result;
    }

    @Override
    public int iterationCost() {
        return store.length;
    }

    @Override
    public ComponentIterator<T> iterate() {
        return new ArrayComponentIterator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void extend(int capacity) {
        if (capacity >= store.length) {
            T[] newStore = (T[]) Array.newInstance(type.getComponentClass(), capacity * 2);
            System.arraycopy(store, 0, newStore, 0, store.length);
            store = newStore;
        }
    }

    private class ArrayComponentIterator implements ComponentIterator<T> {

        private int index = -1;
        private int endIndex = store.length;

        @Override
        public boolean next() {
            index++;
            while (index < endIndex && store[index] == null) {
                index++;
            }
            return index < endIndex;
        }

        @Override
        public void getComponent(Component<T> component) {
            component.copy(store[index]);
        }

        @Override
        public int getEntityId() {
            return index;
        }

        @Override
        public String toString() {
            return index + " of " + endIndex;
        }
    }
}
