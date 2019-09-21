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

import org.terasology.entitysystem.component.Component;
import org.terasology.entitysystem.component.ComponentIterator;
import org.terasology.entitysystem.component.management.ComponentType;
import org.terasology.entitysystem.entity.EntityRef;

/**
 * A component store holds definitive copies of components of a specific type used by entities.
 * @param <T> The type of component handled be the ComponentStore
 */
public interface ComponentStore<T extends Component<T>> {

    /**
     * @return The type of component handled by this store
     */
    ComponentType<T> getType();

    /**
     * @param entityId The entity
     * @return Whether the given entity has a component in this store
     */
    boolean has(int entityId);

    /**
     * Copies a stored component into the provided component
     * @param entityId The entity
     * @param into The component to copy the stored component into
     * @return Whether a component existed for the given entity id
     */
    boolean get(int entityId, T into);

    /**
     * Stores a copy of the provided component against the given entity
     * @param entity The entity to set the component for
     * @param component The component to store. It will be copied into the existing component
     *                  stored against the entity, or a new component will created from the existing
     * @return True if the component was added to the entity (as opposed to existing and updated)
     */
    boolean set(EntityRef entity, T component);

    /**
     * Removes/deletes the component - if any - for the given entity
     * @param entity The entity to delete the component for
     * @return The removed component
     */
    T remove(EntityRef entity);

    /**
     * @return The iterationCost of the component store - used to estimate the cost of iteration compared to another component store. Larger is higher.
     */
    int iterationCost();

    /**
     * Used to indicate the number of entities the ComponentStore needs to support.
     * @param capacity The minimum number of entities the ComponentStore must support.
     */
    void extend(int capacity);

    /**
     * @return An iterator over components contained in this store
     */
    ComponentIterator<T> iterate();

}
