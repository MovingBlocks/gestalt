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

package org.terasology.gestalt.entitysystem.component.management;

import com.google.common.collect.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.gestalt.entitysystem.component.Component;

import java.util.Map;

/**
 * Manager for components. Provides the ability to create or copy components, or retrieve information on the properties of the component that allows the individual
 * properties to be accessed.
 */
public class ComponentManager {

    private static final Logger logger = LoggerFactory.getLogger(ComponentManager.class);

    private ComponentTypeFactory componentTypeFactory;
    private Map<Class<?>, ComponentType<?>> componentTypes = Maps.newLinkedHashMap();

    /**
     * Creates the ComponentManager using the default {@link ComponentTypeFactory} - {@link ReflectionComponentTypeFactory}.
     * In environments where it they are available it is recommended to use a high performance replacements from gestalt-es-perf
     */
    public ComponentManager() {
        this.componentTypeFactory = new ReflectionComponentTypeFactory();
    }

    /**
     * Creates the ComponentManager using the provided componentTypeFactory
     * @param componentTypeFactory The ComponentTypeFactory to use to generate ComponentTypes
     */
    public ComponentManager(ComponentTypeFactory componentTypeFactory) {
        this.componentTypeFactory = componentTypeFactory;
    }

    /**
     * Create an instance of a component of the given type
     *
     * @param type The type of the component
     * @param <T>  The type of the component
     * @return A new instance of the component
     */
    public <T extends Component> T create(Class<T> type) {
        ComponentType<T> typeInfo = getType(type);
        return typeInfo.create();
    }

    /**
     * Creates a new instance that is a copy of an existing component instance
     *
     * @param instance The component to copy
     * @param <T>      The type of the component
     * @return A new instance of the component
     */
    public <T extends Component> T copy(T instance) {
        ComponentType<T> typeInfo = getType(instance);
        return typeInfo.createCopy(instance);
    }

    /**
     * Provides a ComponentType, allowing for reflection like operations.
     *
     * @param type The type of component
     * @param <T>  The type of component
     * @return The ComponentType for the given type of component.
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> ComponentType<T> getType(Class<T> type) {
        ComponentType<T> typeInfo = (ComponentType<T>) componentTypes.get(type);
        if (typeInfo == null) {
            typeInfo = componentTypeFactory.createComponentType(type);
            componentTypes.put(type, typeInfo);
        }
        return typeInfo;
    }

    /**
     * Provides a ComponentType, allowing for reflection like operations.
     *
     * @param instance An instance of component
     * @param <T>      The type of component
     * @return The ComponentType for the given type of component.
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> ComponentType<T> getType(T instance) {
        return (ComponentType<T>) getType(instance.getClass());
    }
}
