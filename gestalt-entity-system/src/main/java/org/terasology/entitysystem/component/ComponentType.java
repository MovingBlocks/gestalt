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

package org.terasology.entitysystem.component;

import com.google.common.base.Preconditions;

import org.terasology.entitysystem.core.Component;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * ComponentType provides type information for a component, including reflection-like functionality to create, copy and access the properties of a component instance.
 */
public final class ComponentType<T extends Component> {
    private final Class<T> type;
    private final Supplier<T> constructor;
    private final Function<T, T> copyConstructor;
    private final ComponentPropertyInfo<T> propertyInfo;


    /**
     *
     * @param type The type of the component
     * @param constructor A supplier that creates a new instance of the component
     * @param copyConstructor A function that create a new instance of the component, copying another component
     * @param propertyInfo Information on all properties of the component type
     */
    public ComponentType(Class<T> type, Supplier<T> constructor, Function<T, T> copyConstructor, ComponentPropertyInfo<T> propertyInfo) {
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(constructor);
        Preconditions.checkNotNull(copyConstructor);
        this.type = type;
        this.constructor = constructor;
        this.copyConstructor = copyConstructor;
        this.propertyInfo = propertyInfo;
    }

    /**
     * @return A new instance of this component
     */
    public T create() {
        return constructor.get();
    }

    /**
     * @param original The component to copy
     * @return A new copy of the original component
     */
    public T createCopy(T original) {
        return copyConstructor.apply(original);
    }

    /**
     * @return The propertyInfo of component this the ComponentType is for
     */
    public Class<T> getComponentClass() {
        return type;
    }

    /**
     * @return Information on the properties of the component propertyInfo.
     */
    public ComponentPropertyInfo<T> getPropertyInfo() {
        return propertyInfo;
    }

}


