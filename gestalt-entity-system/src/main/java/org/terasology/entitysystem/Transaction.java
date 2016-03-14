/*
 * Copyright 2015 MovingBlocks
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

import org.terasology.entitysystem.exception.ComponentAlreadyExistsException;
import org.terasology.entitysystem.exception.ComponentDoesNotExistException;

import java.util.ConcurrentModificationException;
import java.util.Optional;

/**
 * An entity system transaction.
 * <p>
 * Transactions allow for one or more entities to be updated consistently in the presence of multiple threads. Once a transaction is started, each component accessed
 * is cached in the transaction, and can be updated freely. When the transaction is committed, the changes are applied to the entity system and available for other threads.
 * If a thread has made a change to an involved entity in the meantime, then a ConcurrentModificationException is thrown and no change occurs. A transaction can also be rolled
 * back, throwing away all local modifications.
 * <p>
 * A transaction is not threadsafe - it should be used only by a single thread (at a time at least).
 */
public interface Transaction {

    /**
     * Creates a new entity.
     * @return The id of the new entity.
     */
    long createEntity();

    /**
     * Retrieves a component from an entity.
     * <p>
     * This component is a "live view" - any changes to the component will be saved when the transaction is committed. Getting a component multiple times will
     * retrieve the same component, unless it is removed and a new component of that type added to an entity within the same transaction.
     *
     * @param entityId The id of the entity to retrieve the component from.
     * @param componentType The type of the component to retrieve
     * @param <T> The type of the component to retrieve
     * @return An optional containing the requested component, if the entity has a component of that type
     */
    <T extends Component> Optional<T> getComponent(long entityId, Class<T> componentType);

    /**
     * Adds a component to an entity, returning it.
     * <p>
     * This component is a "live view" - any changes to the component will be saved when the transaction is committed. Getting a component multiple times will
     * retrieve the same component, unless it is removed and a new component of that type added to an entity within the same transaction.
     *
     * @param entityId The id of the entity to add the component to
     * @param componentType The type of the component to add
     * @param <T> The type of the component to add
     * @return The added component
     * @throws ComponentAlreadyExistsException if the entity already has this component
     */
    <T extends Component> T addComponent(long entityId, Class<T> componentType);

    /**
     * Removes a component from an entity
     *
     * @param entityId The id of the entity to add the component to
     * @param componentType The type of the component to remove
     * @param <T> The type of the component to remove
     * @throws ComponentDoesNotExistException if the entity does not have this component
     */
    <T extends Component> void removeComponent(long entityId, Class<T> componentType);

    /**
     * Commits the transaction. This also clears the transaction, whether the commit succeeds for fails.
     * <p>
     * If a conflicting change has occurred to the entity system outside of the transaction, then no change will occur.
     * @throws ConcurrentModificationException If a change has happened to the entity system that conflicts with this transaction.
     */
    void commit() throws ConcurrentModificationException;

    /**
     * Rolls back the transaction, dropping all changes and clearing the transaction.
     */
    void rollback();
}
