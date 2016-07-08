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

package org.terasology.entitysystem.component;

import org.terasology.entitysystem.entity.Component;

/**
 * Manager for components. Provides the ability to create or copy components, or retrieve information on the properties of the component that allows the individual
 * properties to be accessed.
 */
public interface ComponentManager {

    /**
     * Create an instance of a component of the given type
     * @param type The type of the component
     * @param <T> The type of the component
     * @return A new instance of the component
     */
    <T extends Component> T create(Class<T> type);

    /**
     * Creates a new instance that is a copy of an existing component instance
     * @param instance The component to copy
     * @param <T> The type of the component
     * @return A new instance of the component
     */
    <T extends Component> T copy(T instance);

    /**
     * Copies the values from one component to another component. These components must be of the same type.
     * @param from The component to copy from
     * @param to The component to copy to
     * @param <T> The type of the components
     * @return The component that was copied into
     */
    <T extends Component> T copy(T from, T to);

    /**
     * Provides a ComponentType, allowing for reflection like operations.
     * @param type The type of component
     * @param <T> The type of component
     * @return The ComponentType for the given type of component.
     */
    <T extends Component> ComponentType<T> getType(Class<T> type);
}
