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

package org.terasology.entitysystem.core;

import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.entity.EntityTransaction;
import org.terasology.entitysystem.entity.TransactionEventListener;
import org.terasology.entitysystem.prefab.Prefab;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;

/**
 * EntityManager is the core of the entity system.
 */
public interface EntityManager {

    /**
     * Registers a listener to receive transaction events
     *
     * @param listener The listener to register
     */
    void registerTransactionListener(TransactionEventListener listener);

    /**
     * Unregisters a listener from receiving transaction events
     *
     * @param listener The listener to unregister
     */
    void unregisterTransactionListener(TransactionEventListener listener);

    /**
     * Whether there is currently an active transaction. A transaction is active if at least one transaction is started without being committed or rolled back.
     *
     * @return Whether a transaction is active
     */
    boolean isTransactionActive();

    /**
     * Begins a new transaction. If the transaction is already active, then the current state suspended. When the new transaction is committed or rolled back then the
     * previous transaction state is restored.
     */
    void beginTransaction();

    /**
     * Commits the current transaction. This also clears the transaction, whether the commit succeeds for fails.
     * <p>
     * If a conflicting change has occurred to the entity system outside of the transaction, then no change will occur.
     *
     * @throws IllegalStateException           If no transaction is active
     * @throws ConcurrentModificationException If a change has happened to the entity system that conflicts with this transaction.
     */
    void commit() throws ConcurrentModificationException;

    /**
     * Rolls back the current transaction, dropping all changes and clearing the transaction.
     *
     * @throws IllegalStateException If no transaction is active
     */
    void rollback();

    /**
     * This is intended for use by EntityRef implementations, providing access to methods working on specific entity ids.
     *
     * @return A raw transaction.
     */
    EntityTransaction getRawTransaction();

    /**
     * Creates a new entity.
     *
     * @return The new entity.
     * @throws IllegalStateException If no transaction is active
     */
    EntityRef createEntity();

    /**
     * Creates an instance of each entity in a prefab, and returns the root entity
     *
     * @param prefab The prefab to create entities from
     * @return The new entity
     */
    EntityRef createEntity(Prefab prefab);

    /**
     * Find entities with the desired components. Note that the components could potentially be removed from the entities between when they are found and when the components
     * are requested.
     *
     * @param first      A component all the found entities should have.
     * @param additional Additional components the entities should have.
     * @return An iterator over the entities with the desired component(s)
     */
    Iterator<EntityRef> findEntitiesWithComponents(Class<? extends Component> first, Class<? extends Component>... additional);

    /**
     * Find entities with the desired components. Note that the components could potentially be removed from the entities between when they are found and when the components
     * are requested.
     *
     * @param componentTypes The desired components. Must contain at least one component
     * @return An iterator over the entities with the desired component(s)
     */

    Iterator<EntityRef> findEntitiesWithComponents(Set<Class<? extends Component>> componentTypes);

    /**
     * @return An iterator over all entities. Note that entities could be deleted between when they are fetched and when they are accessed. This iterator is otherwise
     * safe in the presence of entity changes. Can be used outside of a transaction.
     */
    Iterator<EntityRef> allEntities();

    /**
     * @return The value of the next entity id
     */
    long getNextId();
}
