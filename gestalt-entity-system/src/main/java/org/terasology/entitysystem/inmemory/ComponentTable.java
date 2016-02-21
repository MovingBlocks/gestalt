/*
 * Copyright 2016 MovingBlocks
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
package org.terasology.entitysystem.inmemory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import gnu.trove.TCollections;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.terasology.entitysystem.Component;
import org.terasology.entitysystem.component.ComponentManager;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A table for storing entities and components. Focused on allowing iteration across a components of a given type
 *
 * @author Immortius
 */
class ComponentTable implements EntityStore {
    private Map<Class, TLongObjectMap<Component>> store = Maps.newConcurrentMap();
    private ComponentManager componentManager;

    public ComponentTable(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    @Override
    public <T extends Component> T get(long entityId, Class<T> componentClass) {
        TLongObjectMap<Component> entityMap = store.get(componentClass);
        if (entityMap != null) {
            return componentManager.copy(componentClass.cast(entityMap.get(entityId)));
        }
        return null;
    }

    @Override
    public synchronized <T extends Component> boolean add(long entityId, Class<T> componentType, T component) {
        TLongObjectMap<Component> entityMap = store.get(componentType);
        if (entityMap == null) {
            entityMap = TCollections.synchronizedMap(new TLongObjectHashMap<>());
            store.put(componentType, entityMap);
        }
        return entityMap.putIfAbsent(entityId, componentManager.copy(component)) == null;
    }

    @Override
    public synchronized <T extends Component> boolean update(long entityId, Class<T> componentType, T component) {
        TLongObjectMap<Component> entityMap = store.get(componentType);
        if (entityMap == null) {
            return false;
        }
        if (entityMap.containsKey(entityId)) {
            entityMap.put(entityId, componentManager.copy(component));
            return true;
        }
        return false;
    }

    /**
     * @return removes the component with the specified class from the entity and returns it.
     * Returns null if no component could be removed.
     */
    @Override
    public <T extends Component> Component remove(long entityId, Class<T> componentClass) {
        TLongObjectMap<Component> entityMap = store.get(componentClass);
        if (entityMap != null) {
            return entityMap.remove(entityId);
        }
        return null;
    }

    @Override
    public List<Component> removeAndReturnComponentsOf(long entityId) {
        List<Component> componentList = Lists.newArrayList();
        for (TLongObjectMap<Component> entityMap : store.values()) {
            Component component = entityMap.remove(entityId);
            if (component != null) {
                componentList.add(component);
            }
        }
        return componentList;
    }

    @Override
    public void removeAll(long entityId) {
        for (TLongObjectMap<Component> entityMap : store.values()) {
            entityMap.remove(entityId);
        }
    }

    @Override
    public void clear() {
        store.clear();
    }

    @Override
    public int getComponentCount(Class<? extends Component> componentClass) {
        TLongObjectMap<Component> map = store.get(componentClass);
        return (map == null) ? 0 : map.size();
    }

    /**
     * @return an iterable that should be only used for iteration over the components. It can't be used to remove
     * components. It should not be used after components have been added or removed from the entity.
     */
    @Override
    public Collection<Component> getComponents(long entityId) {
        List<Component> components = Lists.newArrayList();
        for (TLongObjectMap<Component> componentMap : store.values()) {
            Component comp = componentMap.get(entityId);
            if (comp != null) {
                components.add(componentManager.copy(comp));
            }
        }
        return components;
    }

    @Override
    public <T extends Component> TLongObjectIterator<T> componentIterator(Class<T> componentClass) {
        TLongObjectMap<T> entityMap = (TLongObjectMap<T>) store.get(componentClass);
        if (entityMap != null) {
            return entityMap.iterator();
        }
        return null;
    }

    /**
     * Produces an iterator for iterating over all entities
     * <br><br>
     * This is not designed to be performant, and in general usage entities should not be iterated over.
     *
     * @return An iterator over all entity ids.
     */
    @Override
    public TLongIterator entityIdIterator() {
        TLongSet idSet = new TLongHashSet();
        for (TLongObjectMap<Component> componentMap : store.values()) {
            idSet.addAll(componentMap.keys());
        }
        return idSet.iterator();
    }

    @Override
    public int entityCount() {
        TLongSet idSet = new TLongHashSet();
        for (TLongObjectMap<Component> componentMap : store.values()) {
            idSet.addAll(componentMap.keys());
        }
        return idSet.size();
    }

    @Override
    public boolean isAvailable(long entityId) {
        for (TLongObjectMap<Component> componentMap : store.values()) {
            Component comp = componentMap.get(entityId);
            if (comp != null) {
                return true;
            }
        }
        return false;
    }
}
