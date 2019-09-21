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
import org.terasology.util.collection.TypeKeyedMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * An entity ref is a reference to an entity or entity-like structure. This is intentionally left a little vague, to allow for a variety of usages - such as an EntityRef
 * to an entity template in a prefab.
 * <p>
 * EntityRef provides some simple methods for manipulating the entity that is being referenced.
 */
public interface EntityRef {

    /**
     * @return The id of this entity ref. This will be 0 for null entity refs or new entities that have not yet been created.
     */
    int getId();

    /**
     * Whether an entity ref points to an actual entity. This supports {@link NullEntityRef} - the null object for entity refs -
     * and also the deletion of entities.
     *
     * @return Whether this EntityRef references an entity that exists.
     */
    boolean exists();

    /**
     * Retrieves a component from the EntityRef, into the provided component
     *
     * @param component     The component to copy the retrieved component details into
     * @param <T>           The type of the component to retrieve
     * @return True if the entity has a component of that type
     */
    <T extends Component<T>> boolean getComponent(T component);

    /**
     * Retrieves a component from the EntityRef into a new component instance
     *
     * @param componentType     The component to copy the retrieved component details into
     * @param <T>           The type of the component to retrieve
     * @return An optional containing the requested component, if the entity has a component of that type
     */
    <T extends Component<T>> Optional<T> getComponent(Class<T> componentType);

    /**
     * @param type The type of component to check for the presence of
     * @return Whether this entity has a component of the given type
     */
    <T extends Component<T>> boolean hasComponent(Class<T> type);

    /**
     * @return Types of components that the EntityRef has.
     */
    Set<Class<? extends Component>> getComponentTypes();

    /**
     * Retrieves all component used by this entity.
     * @return The components composing this entity
     */
    TypeKeyedMap<Component> getAllComponents();

    /**
     * Updates the entity with the given component, either adding it if missing or replacing the
     * existing component otherwise.
     *
     * @param component     The component to set
     * @param <T>           The type of the component to set
     * @return If the component was new (as opposed to updated)
     */
    <T extends Component<T>> boolean setComponent(T component);

    /**
     * Updates the entity with the given components, adding the missing components and replacing
     * the existing components
     *
     * @param components     The components to set
     */
    default void setComponents(Component ... components) {
        setComponents(Arrays.asList(components));
    }

    /**
     * Updates the entity with the given components, adding the missing components and replacing
     * the existing components
     *
     * @param components     The components to set
     */
    default void setComponents(Collection<Component> components) {
        if (exists()) {
            for (Component component : components) {
                setComponent(component);
            }
        }
    }

    /**
     * Removes a component from the entity
     *
     * @param componentType The type of the component to remove
     * @param <T>           The type of the component to remove
     * @return The removed component, if any
     */
    <T extends Component<T>> T removeComponent(Class<T> componentType);

    /**
     * Removes components from the entity
     *
     * @param componentTypes The type of the components to remove
     * @return The removed components
     */
    default Set<Component<?>> removeComponents(Class<? extends Component> ...  componentTypes) {
        return removeComponents(Arrays.asList(componentTypes));
    }

    /**
     * Removes components from the entity
     *
     * @param componentTypes The type of the components to remove
     * @return The removed components
     */
    Set<Component<?>> removeComponents(Collection<Class<? extends Component>> componentTypes);

    /**
     * Removes all the components from the entity and deletes it
     * @return The final set of components the entity had
     */
    Set<Component<?>> delete();

}
