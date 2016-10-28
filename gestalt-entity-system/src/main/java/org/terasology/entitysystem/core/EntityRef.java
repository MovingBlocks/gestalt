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

import org.terasology.entitysystem.transaction.exception.ComponentAlreadyExistsException;
import org.terasology.entitysystem.transaction.exception.ComponentDoesNotExistException;
import org.terasology.util.collection.TypeKeyedMap;

import java.util.Optional;
import java.util.Set;

/**
 * An entity ref is a reference to an entity or entity-like structure. This is intentionally left a little vague, to allow for a variety of usages - such as an EntityRef
 * to an entity template in a prefab, or to an entity being created by a transaction.
 * <p>
 * EntityRef provides some simple methods for manipulating the entity that is being referenced.
 */
public interface EntityRef {

    /**
     * @return The id of this entity ref. This will be 0 for null entity refs or new entities that have not yet been created.
     */
    long getId();

    /**
     * @return The revision of this entity.
     */
    long getRevision();

    /**
     * An entity exists if it is new this transaction, or if it was created in a previous transaction and still has components. Once all components are removed from an entity
     * it ceases to exist.
     * @return Whether this EntityRef references an entity that exists.
     */
    boolean isPresent();

    /**
     * Retrieves a component from the EntityRef.
     *
     * @param componentType The type of the component to retrieve
     * @param <T>           The type of the component to retrieve
     * @return An optional containing the requested component, if the entity has a component of that type
     */
    <T extends Component> Optional<T> getComponent(Class<T> componentType);

    /**
     * @return Types of components that the EntityRef has.
     */
    Set<Class<? extends Component>> getComponentTypes();

    /**
     * @return The components composing this entity
     */
    TypeKeyedMap<Component> getComponents();

    /**
     * Adds a component to an EntityRef, returning it
     *
     * @param componentType The type of the component to add
     * @param <T>           The type of the component to add
     * @return The added component
     * @throws ComponentAlreadyExistsException if the entity already has this component
     */
    <T extends Component> T addComponent(Class<T> componentType);

    /**
     * Removes a component from the EntityRef
     *
     * @param componentType The type of the component to remove
     * @param <T>           The type of the component to remove
     * @throws ComponentDoesNotExistException if the entity does not have this component
     */
    <T extends Component> void removeComponent(Class<T> componentType);

    /**
     * Removes all the components from the EntityRef and deletes it
     */
    void delete();

}
