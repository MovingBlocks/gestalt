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

package org.terasology.entitysystem.entity.inmemory;

import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.transaction.exception.ComponentAlreadyExistsException;
import org.terasology.entitysystem.transaction.exception.ComponentDoesNotExistException;
import org.terasology.util.collection.TypeKeyedMap;

import java.util.Optional;

/**
 * The interface for low-level entity transactions, working directly with entity ids. This is intended for use by entity refs.
 * <p>
 * Transactions allow for one or more entities to be updated consistently in the presence of multiple threads. Once a transaction is started, each component accessed
 * is cached in the transaction, and can be updated freely. When the transaction is committed, the changes are applied to the entity system and available for other threads.
 * If a thread has made a change to an involved entity in the meantime, then a ConcurrentModificationException is thrown and no change occurs. A transaction can also be rolled
 * back, throwing away all local modifications.
 * <p>
 * A transaction is not threadsafe - it should be used only in its thread of origin.
 */
interface ReferenceAdaptor {

    /**
     * @param id The id of the entity
     * @return Whether an entity with the given id exists
     * @throws IllegalStateException If no transaction is active
     */
    boolean exists(long id);

    /**
     * @param id The id of the entity
     * @return The revision of the entity.
     * @throws IllegalStateException If no transaction is active
     */
    int getRevision(long id);

    /**
     * Retrieves a component from an entity.
     * <p>
     * This component is a "live view" - any changes to the component will be saved when the transaction is committed. Getting a component multiple times will
     * retrieve the same component, unless it is removed and a new component of that type added to an entity within the same transaction.
     *
     * @param entityId      The id of the entity to retrieve the component from.
     * @param componentType The type of the component to retrieve
     * @param <T>           The type of the component to retrieve
     * @return An optional containing the requested component, if the entity has a component of that type
     * @throws IllegalStateException If no transaction is active
     */
    <T extends Component> Optional<T> getComponent(long entityId, Class<T> componentType);

    /**
     * @param entityId The id of the entity to retrieve the component from.
     * @return Retrieves a type keyed map of the components composing this entity
     * @throws IllegalStateException If no transaction is active
     */
    TypeKeyedMap<Component> getEntityComposition(long entityId);

    /**
     * Adds a component to an entity, returning it.
     * <p>
     * This component is a "live view" - any changes to the component will be saved when the transaction is committed. Getting a component multiple times will
     * retrieve the same component, unless it is removed and a new component of that type added to an entity within the same transaction.
     *
     * @param entityId      The id of the entity to add the component to
     * @param componentType The type of the component to add
     * @param <T>           The type of the component to add
     * @return The added component
     * @throws ComponentAlreadyExistsException if the entity already has this component
     * @throws IllegalStateException           If no transaction is active
     */
    <T extends Component> T addComponent(long entityId, Class<T> componentType);

    /**
     * Removes a component from an entity
     *
     * @param entityId      The id of the entity to add the component to
     * @param componentType The type of the component to remove
     * @param <T>           The type of the component to remove
     * @throws ComponentDoesNotExistException if the entity does not have this component
     * @throws IllegalStateException          If no transaction is active
     */
    <T extends Component> void removeComponent(long entityId, Class<T> componentType);


}
