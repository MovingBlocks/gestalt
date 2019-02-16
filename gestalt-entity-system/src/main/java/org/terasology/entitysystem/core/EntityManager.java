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

package org.terasology.entitysystem.core;

import org.terasology.entitysystem.prefab.Prefab;
import org.terasology.naming.Name;

import java.util.Iterator;
import java.util.Map;

import gnu.trove.TLongCollection;

/**
 * EntityManager is the core of the entity system.
 */
public interface EntityManager {

    /**
     * Creates a new entity.
     *
     * @return The new entity.
     * @throws IllegalStateException If no transaction is active
     */
    EntityRef createEntity();

    /**
     * @param id The id of the entity to return
     * @return The entity ref for the given id. If the entity doesn't exist, this may be a null entity ref .
     */
    EntityRef getEntity(long id);

    /**
     * @param ids A collection of entity ids
     * @return The entity refs for the given ids. If an entity doesn't exist, it will be a null entity ref.
     */
    Iterable<EntityRef> getEntities(TLongCollection ids);

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
     * @return An iterator over all entities. Note that entities could be deleted between when they are fetched and when they are accessed. This iterator is otherwise
     * safe in the presence of entity changes. Can be used outside of a transaction.
     */
    Iterator<EntityRef> allEntities();

    // TODO: Remove?

    /**
     * @return The value of the next entity id
     */
    long getNextId();
}
