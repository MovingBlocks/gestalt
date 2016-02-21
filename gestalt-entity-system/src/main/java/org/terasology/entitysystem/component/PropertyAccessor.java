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

import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Property accessor encapsulates information on a property, and the ability to access the property.
 * @param <T> The type this property belongs to
 * @param <U> The type of this property
 */
public class PropertyAccessor<T, U> {
    private final String name;
    private final Class<T> owningClass;
    private final Type propertyType;
    private final Function<T, U> getter;
    private final BiConsumer<T, U> setter;

    /**
     * Constructs a property accessor
     * @param name The name of the property
     * @param owningClass The class that contains the property
     * @param propertyType The type of the property
     * @param getter A method to get the property
     * @param setter A method to set the property
     */
    public PropertyAccessor(String name, Class<T> owningClass, Type propertyType, Function<T, U> getter, BiConsumer<T, U> setter) {
        this.name = name;
        this.owningClass = owningClass;
        this.propertyType = propertyType;
        this.getter = getter;
        this.setter = setter;
    }

    /**
     * @return The name of the property
     */
    public String getName() {
        return name;
    }

    /**
     * @return The class that has this property
     */
    public Class<T> getOwningClass() {
        return owningClass;
    }

    /**
     * @return The type of the property
     */
    public Type getPropertyType() {
        return propertyType;
    }

    /**
     * @return The class of the property
     */
    @SuppressWarnings("unchecked")
    public Class<U> getPropertyClass() {
        return (Class<U>) propertyType.getClass();
    }

    /**
     * @param instance The instance to retrieve the value of the property from
     * @return Get the value of the property from the given instance
     */
    public U get(T instance) {
        return getter.apply(instance);
    }

    /**
     * @param instance The instance to set the value of a property of
     * @param value The value to set the property to
     */
    public void set(T instance, U value) {
        setter.accept(instance, value);
    }


}
