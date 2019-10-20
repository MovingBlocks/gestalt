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

package org.terasology.entitysystem.entity.manager;

import com.google.common.collect.Sets;

import net.jcip.annotations.ThreadSafe;

import org.terasology.entitysystem.component.Component;
import org.terasology.entitysystem.component.store.ComponentStore;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.util.collection.TypeKeyedMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@ThreadSafe
class ManagedEntityRef implements EntityRef {
    private volatile CoreEntityManager entityManager;
    private final int id;

    ManagedEntityRef(CoreEntityManager entityManager, int entityId) {
        this.entityManager = entityManager;
        this.id = entityId;
    }


    @Override
    public int getId() {
        return id;
    }

    @Override
    public boolean exists() {
        return entityManager != null;
    }

    @Override
    public <T extends Component<T>> boolean hasComponent(Class<T> type) {
        if (entityManager != null) {
            return entityManager.getComponentStore(type).has(id);
        } else {
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component<T>> boolean getComponent(T component) {
        if (entityManager != null) {
            return entityManager.getComponentStore(component.getClass()).get(id, component);
        } else {
            return false;
        }
    }

    @Override
    public <T extends Component<T>> Optional<T> getComponent(Class<T> componentType) {
        ComponentStore<T> componentStore = entityManager.getComponentStore(componentType);
        T result = componentStore.getType().create();
        if (componentStore.get(id, result)) {
            return Optional.of(result);
        }
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component<T>> boolean setComponent(T component) {
        if (entityManager != null) {
            return entityManager.getComponentStore(component.getClass()).set(id, component);
        }
        return false;
    }

    @Override
    public <T extends Component<T>> T removeComponent(Class<T> componentType) {
        if (entityManager != null) {
            return entityManager.getComponentStore(componentType).remove(id);
        }
        return null;
    }

    @Override
    public Set<Component<?>> removeComponents(Collection<Class<? extends Component>> componentTypes) {
        return null;
    }

    @Override
    public Set<Class<? extends Component>> getComponentTypes() {
        if (entityManager != null) {
            Set<Class<? extends Component>> types = Sets.newLinkedHashSet();
            for (ComponentStore<?> store : entityManager.allComponentStores()) {
                if (store.has(id)) {
                    types.add(store.getType().getComponentClass());
                }
            }
            return types;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public TypeKeyedMap<Component> getAllComponents() {
        if (entityManager != null) {
            TypeKeyedMap<Component> components = new TypeKeyedMap<>();
            for (ComponentStore<?> store : entityManager.allComponentStores()) {
                addComponentFromStore(store, components);
            }
            return components;
        } else {
            return TypeKeyedMap.empty();
        }
    }

    /*
     * Supports getAllComponents, separate method to support generic trickery
     */
    private <T extends Component<T>> void addComponentFromStore(ComponentStore<T> store, TypeKeyedMap<Component> components) {
        T component = store.getType().create();
        store.get(id, component);
        components.put(component);
    }

    @Override
    public Set<Component<?>> delete() {
        Set<Component<?>> removedComponents = Sets.newLinkedHashSet();
        if (entityManager != null) {
            for (ComponentStore<?> store : entityManager.allComponentStores()) {
                Component<?> removed = store.remove(id);
                if (removed != null) {
                    removedComponents.add(removed);
                }
            }
            entityManager.freeEntityId(id);
            entityManager = null;
        }
        return removedComponents;
    }

    @Override
    public String toString() {
        if (entityManager != null) {
            return "EntityRef(" + id + ")";
        } else {
            return "EntityRef(deleted)";
        }
    }
}
