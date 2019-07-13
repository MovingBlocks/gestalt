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

package org.terasology.benchmarks.bouncingballs.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;

import org.terasology.benchmarks.bouncingballs.common.ComponentIterator;
import org.terasology.benchmarks.bouncingballs.common.ComponentStore;
import org.terasology.entitysystem.core.Component;

import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class EntityManagerImpl implements EntityManager {

    private Map<Class<? extends Component>, ComponentStore<?>> componentStores = Maps.newConcurrentMap();
    private Deque<Integer> freedIdQueue = Queues.newArrayDeque();
    private TIntSet freedIds = new TIntHashSet();
    private int nextId = 1;
    private int highWatermark = 1000;

    @Override
    public <T extends Component<T>> ComponentStore<T> addComponentStore(ComponentStore<T> store) {
        componentStores.put(store.getType(), store);
        store.extend(highWatermark);
        return store;
    }

    @Override
    public synchronized int getNewId() {
        if (freedIds.isEmpty()) {
            int id = nextId++;
            if (id > highWatermark) {
                highWatermark *= 2;
                for (ComponentStore<?> store : componentStores.values()) {
                    store.extend(highWatermark);
                }
            }
            return id;
        } else {
            int id = freedIdQueue.pop();
            freedIds.remove(id);
            return id;
        }
    }

    @Override
    public synchronized boolean delete(int id) {
        if (id > 0 && id < nextId && !freedIds.contains(id)) {
            for (ComponentStore store : componentStores.values()) {
                store.remove(id);
            }
            if (id == nextId - 1) {
                nextId--;
            } else {
                freedIds.add(id);
                freedIdQueue.push(id);
            }
            return true;
        }
        return false;
    }

    @Override
    public int size() {
        return nextId - 1 - freedIds.size();

    }

    @Override
    public TIntIterator iterate() {
        return new IdIterator(nextId, freedIds);
    }

    @Override
    public ComponentsIterator iterate(Component... components) {
        List<Component> componentsOrdered = Lists.newArrayList(components);
        componentsOrdered.sort(Comparator.comparing(x -> componentStores.get(x.getClass()).size()));
        List<ComponentStore> stores = componentsOrdered.stream().map(x -> componentStores.get(x.getClass())).collect(Collectors.toList());

        ComponentIterator drivingIterator = stores.get(0).iterate();

        return new ComponentsIterator(drivingIterator, componentsOrdered, stores);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component<T>> ComponentStore<T> getComponentStore(Class<T> componentType) {
        return (ComponentStore<T>) componentStores.get(componentType);
    }

    private class IdIterator implements TIntIterator {

        private final int endIndex;
        private final TIntSet skipIds;
        private int i = 0;
        private int lastIndex = 0;

        IdIterator(int endIndex, TIntSet skipIds) {
            this.endIndex = endIndex;
            this.skipIds = new TIntHashSet(skipIds);
            iterate();
        }

        private void iterate() {
            do {
              i++;
            } while (i < endIndex && skipIds.contains(i));
        }

        @Override
        public int next() {
            lastIndex = i;
            iterate();
            return lastIndex;
        }

        @Override
        public boolean hasNext() {
            return i < endIndex;
        }

        @Override
        public void remove() {
            delete(lastIndex);
        }
    }

    public class ComponentsIterator {
        private ComponentIterator drivingIterator;
        private List<Component> components;
        private List<ComponentStore> componentStores;

        public ComponentsIterator(ComponentIterator drivingIterator, List<Component> components, List<ComponentStore> componentStores) {
            this.drivingIterator = drivingIterator;
            this.components = components;
            this.componentStores = componentStores;
        }

        public boolean next() {
            while (drivingIterator.next(components.get(0))) {
                int entityId = drivingIterator.getEntityId();
                boolean found = true;
                for (int i = 1; i < componentStores.size(); ++i) {
                    if (!componentStores.get(i).get(entityId, components.get(i))) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    return true;
                }
            }
            return false;
        }

        public int getEntityId() {
            return drivingIterator.getEntityId();
        }
    }
}
