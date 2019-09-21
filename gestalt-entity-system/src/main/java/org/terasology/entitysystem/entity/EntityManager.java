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

package org.terasology.entitysystem.entity;

import org.terasology.entitysystem.component.Component;
import org.terasology.entitysystem.component.store.ComponentStore;
import org.terasology.entitysystem.prefab.Prefab;
import org.terasology.naming.Name;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * EntityManager is the core of the entity system. It manages the storage of entities and component
 * stores.
 */
public interface EntityManager {

    /**
     * Creates a new entity.
     *
     * @return The new entity.
     */
    EntityRef createEntity();

    /**
     * Creates a new entity with a set of components
     * @param components The components to create the entity with
     * @return The new entity.
     */
    default EntityRef createEntity(Component ... components) {
        return createEntity(Arrays.asList(components));
    }

    /**
     * Creates a new entity with a set of components
     * @param components The components to create the entity with
     * @return The new entity.
     */
    EntityRef createEntity(Collection<Component> components);

    /**
     * Creates an instance of each entity in a prefab, and returns the root entity
     *
     * @param prefab The prefab to create entities from
     * @return The new entity based on the root entity recipe
     */
    EntityRef createEntity(Prefab prefab);

    /**
     * Creates entities based on a prefab
     *
     * @param prefab The prefab to create entities from
     * @return A map of EntityRefs created, by the name of the entity prefab.
     */
    Map<Name, EntityRef> createEntities(Prefab prefab);

    /**
     * @param id The id of the entity to return
     * @return The entity ref for the given id. If the entity doesn't exist, this will be a null entity ref .
     */
    EntityRef getEntity(int id);

    /**
     * @return The number of entities stored
     */
    int size();

    /**
     * Iterates over all entities with the given components. The components are updated to match
     * the contents as iteration occurs
     * @param components The set of components to iterate over
     * @return An iterator over entities
     */
    EntityIterator iterate(Component ... components);

    /**
     * Returns the low-level store for a particular type of component. Should be used to drive
     * repeated processes over the same component
     * @param componentType The type of component
     * @param <T> The type of component
     * @return The component store for the given component type
     */
    <T extends Component<T>> ComponentStore<T> getComponentStore(Class<T> componentType);

    /**
     * @return Iterable access over all entities. This is not thread safe in the presence of changes off-thread
     */
    Iterable<EntityRef> allEntities();

    /**
     * @return Iterable access over all component stores;
     */
    Iterable<ComponentStore<?>> allComponentStores();
}
