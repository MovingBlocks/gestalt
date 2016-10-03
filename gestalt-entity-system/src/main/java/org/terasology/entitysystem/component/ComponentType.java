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

import org.terasology.entitysystem.core.Component;

import java.util.function.Supplier;

/**
 * ComponentType provides type information for a component, including reflection-like functionality to create, copy and access the properties of a component instance.
 */
public final class ComponentType<T extends Component> {
    private final Class<T> interfaceType;
    private final Supplier<T> constructor;
    private final ComponentPropertyInfo<T> type;
    private final ComponentCopyFunction<T> copyStrategy;

    /**
     * Creates a component type
     *
     * @param constructor   A supplier that will construct an instance of this component type
     * @param interfaceType The type of the interface of this component
     * @param propertyInfo  The property info for this type
     * @param copyStrategy  The function for copying a component
     */
    public ComponentType(Supplier<T> constructor, Class<T> interfaceType, ComponentPropertyInfo<T> propertyInfo, ComponentCopyFunction<T> copyStrategy) {
        this.constructor = constructor;
        this.interfaceType = interfaceType;
        this.type = propertyInfo;
        this.copyStrategy = copyStrategy;
    }

    public T create() {
        return constructor.get();
    }

    public T copy(T from, T to) {
        return copyStrategy.apply(from, to);
    }

    public Class<T> getComponentClass() {
        return interfaceType;
    }

    public ComponentPropertyInfo<T> getPropertyInfo() {
        return type;
    }

}


