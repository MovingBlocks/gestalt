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
package org.terasology.benchmarks.bouncingballs.arraystore;

import org.terasology.benchmarks.bouncingballs.common.ComponentIterator;
import org.terasology.benchmarks.bouncingballs.common.ComponentSpliterator;
import org.terasology.benchmarks.bouncingballs.common.ComponentStore;
import org.terasology.entitysystem.component.ComponentType;
import org.terasology.entitysystem.core.Component;

import java.lang.reflect.Array;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * A table for storing entities and components. Focused on allowing iteration across a components of a given type
 *
 * @author Immortius
 */
public class ArrayComponentStore<T extends Component<T>> implements ComponentStore<T> {
    private final ComponentType<T> type;
    private T[] store;

    public ArrayComponentStore(ComponentType<T> componentType) {
        this(componentType, 1000);
    }

    public ArrayComponentStore(ComponentType<T> type, int initialCapacity) {
        this.type = type;
        store = (T[]) Array.newInstance(type.getComponentClass(), initialCapacity);
    }

    public boolean get(int entityId, T into) {
        T value = store[entityId];
        if (value != null) {
            into.copy(store[entityId]);
            return true;
        }
        return false;
    }

    public void set(int entityId, T component) {
        if (store[entityId] == null) {
            store[entityId] = type.createCopy(component);
        } else {
            store[entityId].copy(component);
        }
    }

    @Override
    public int size() {
        return store.length;
    }

    public ComponentIterator<T> iterate() {
        return new SingleComponentIterator<>(store, 0, store.length);
    }

    public ComponentSpliterator<T> spliterate() {
        return new SingleComponentIterator<>(store, 0, store.length);
    }

    @Override
    public void remove(int id) {
        store[id] = null;
    }

    @Override
    public Class<T> getType() {
        return type.getComponentClass();
    }

    @Override
    public void extend(int capacity) {
        if (capacity >= store.length) {
            T[] newStore = (T[]) Array.newInstance(type.getComponentClass(), capacity * 2);
            System.arraycopy(store, 0, newStore, 0, store.length);
            store = newStore;
        }
    }

    private static class SingleComponentIterator<T extends Component<T>> implements ComponentSpliterator<T> {

        private T[] store;
        private int index = -1;
        private int endIndex;

        public SingleComponentIterator(T[] store, int start, int length) {
            this.store = store;
            this.endIndex = length + start;
            this.index = start - 1;
        }

        @Override
        public boolean next(T component) {
            index++;
            while (index < endIndex && store[index] == null) {
                index++;
            }
            if (index < endIndex) {
                component.copy(store[index]);
                return true;
            }
            return false;
        }

        @Override
        public ComponentSpliterator<T> split() {
            int length = endIndex - index + 1;
            int splitLength = length / 2;
            endIndex -= splitLength;

            return new SingleComponentIterator<>(store, endIndex, splitLength); // 501, 500
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
