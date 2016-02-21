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

package org.terasology.entitysystem;

import java.util.Collection;
import java.util.Optional;

/**
 * EntityManager is the core of the entity system. It provides atomic operations on the entity system, or to start a transaction.
  */
public interface EntityManager {

    /**
     * Provides a new instance of the desired component type. After use this instance should be released to allow
     * @param componentClass The type of component to create
     * @param <T> The type of component to create
     * @return A new component of the given class
     */
    <T extends Component> T createComponent(Class<T> componentClass);

    /**
     * Creates an entity with the given components
     * @param first The first component
     * @param additional Any additional components
     * @return The id of the newly created entity
     */
    long createEntity(Component first, Component... additional);

    /**
     * Creates an entity with the given components
     * @param components The component to create the entity with
     * @return The id of the newly created entity
     */
    long createEntity(Collection<Component> components);

    /**
     * Adds a component to an entity by id. This will fail if the entity already has that component
     * @param entityId The id of the entity
     * @param component The component to add
     * @return Whether adding the component succeeded.
     */
    boolean addComponent(long entityId, Component component);

    /**
     * Retrieves a component from an entity
     * @param entityId The id of the entity
     * @param componentClass The type of component to retrieve
     * @param <T> The type of component to retrieve
     * @return An Optional containing the requested component, or Optional.empty() if the entity does not have that component
     */
    <T extends Component> Optional<T> getComponent(long entityId, Class<T> componentClass);

    /**
     * Updates a component on the given entity. This will fail if the entity does not have that component to update.
     * @param entityId The id of the entity
     * @param component The updated component
     * @return Whether the update succeeded.
     */
    boolean updateComponent(long entityId, Component component);

    /**
     * Removes a component from the given entity. This will fail if the entity
     * @param entityId The id of the entity
     * @param componentClass The type of the component to remove.
     * @return Whether removing the component succeeded.
     */
    boolean removeComponent(long entityId, Class<? extends Component> componentClass);

}
