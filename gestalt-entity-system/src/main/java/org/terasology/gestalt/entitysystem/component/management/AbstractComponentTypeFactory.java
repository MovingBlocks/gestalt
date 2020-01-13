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

import android.support.annotation.NonNull;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.gestalt.util.reflection.GenericsUtil;

import java.lang.invoke.LambdaConversionException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Abstract base class for ComponentTypeFactories. Provides the base logic for generating ComponentTypes.
 */
public abstract class AbstractComponentTypeFactory implements ComponentTypeFactory {

    private static final Logger logger = LoggerFactory.getLogger(AbstractComponentTypeFactory.class);
    private static final Converter<String, String> TO_LOWER_CAMEL = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL);

    @Override
    @NonNull
    public <T extends Component<T>> ComponentType<T> createComponentType(Class<T> type) {
        Collection<PropertyAccessor<T, ?>> propertyAccessors = discoverProperties(type);
        if (propertyAccessors.isEmpty()) {
            return createSingletonComponentType(type);
        }

        Supplier<T> emptyConstructor = getEmptyConstructor(type);
        if (emptyConstructor == null) {
            throw new ComponentTypeGenerationException("Component '" + type + "' missing empty constructor");
        }

        Function<T, T> copyConstructor = getCopyConstructor(type);
        if (copyConstructor == null) {
            copyConstructor = (T from) -> {
                T result = emptyConstructor.get();
                result.copy(from);
                return result;
            };
        }
        return new ComponentType<>(type, emptyConstructor, copyConstructor, new ComponentPropertyInfo<>(propertyAccessors));
    }

    /**
     * Produces a function to call the copy constructor to create a copy of another component
     * @param type The type of the component
     * @param <T> The type of the component
     * @return A function that calls the copy constructor, or null if there is no copy constructor
     */
    protected abstract <T extends Component<T>> Function<T, T> getCopyConstructor(Class<T> type);

    /**
     * Produces a function to call the empty constructor
     * @param type The type of the component
     * @param <T> The type of the component
     * @return A function that generates a component with the empty constructor
     * @throws ComponentTypeGenerationException if no empty constructor exists or can be called
     */
    protected abstract <T extends Component<T>> Supplier<T> getEmptyConstructor(Class<T> type);

    /**
     * Creates a function to call a getter
     * @param method The getter method
     * @param propertyName The name of the property
     * @param propertyType The type of the property
     * @param componentType The type of the component
     * @param <T> The type of the component
     * @return A function that calls a getter and returns the value
     * @throws Throwable If something goes wrong setting up the function
     */
    protected abstract <T extends Component<T>> Function<T, Object> createGetterFunction(Method method, String propertyName, Type propertyType, Class<T> componentType) throws Throwable;

    /**
     * Creates a function to call a setter
     * @@param method The getter method
     * @param propertyName The name of the property
     * @param propertyType The type of the property
     * @param componentType The type of the component
     * @param <T> The type of the component
     * @return A function to call the setter
     * @throws Throwable If something goes wrong setting up the function
     */
    protected abstract <T extends Component<T>> BiConsumer<T, Object> createSetterFunction(Method method, String propertyName, Type propertyType, Class<T> componentType) throws Throwable;

    private <T extends Component<T>> ComponentType<T> createSingletonComponentType(Class<T> type) {
        try {
            T instance = type.newInstance();
            return new ComponentType<>(type, () -> instance, t -> instance, new ComponentPropertyInfo<>(Collections.emptySet()));
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ComponentTypeGenerationException("Component lacks an empty constructor: " + type, e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Component<T>> Collection<PropertyAccessor<T, ?>> discoverProperties(Class<T> componentType) {
        List<PropertyAccessor<T, ?>> accessorList = Lists.newArrayList();

        for (Method method : componentType.getDeclaredMethods()) {
            if (method.getGenericReturnType() == Void.TYPE && method.getName().startsWith("set") && method.getParameterTypes().length == 1) {
                String propertyName = method.getName().substring(3);
                Type setterType = method.getGenericParameterTypes()[0];


                String getterMethodName;
                if (Boolean.TYPE.equals(setterType)) {
                    getterMethodName = "is" + propertyName;
                } else {
                    getterMethodName = "get" + propertyName;
                }
                Method getter;
                try {
                    getter = componentType.getDeclaredMethod(getterMethodName);
                } catch (NoSuchMethodException e) {
                    logger.error("Unable to find getter {}", getterMethodName);
                    continue;
                }
                Type getterType = getter.getGenericReturnType();

                Type propertyType = null;
                if (!getterType.equals(setterType)) {
                    Class<?> setClass = GenericsUtil.getClassOfType(setterType);
                    Class<?> getClass = GenericsUtil.getClassOfType(getterType);
                    if (getClass != null && setClass != null) {
                        if (getClass.isAssignableFrom(setClass)) {
                            propertyType = getterType;
                        } else if (setClass.isAssignableFrom(getClass)) {
                            propertyType = setterType;
                        }
                    }
                } else {
                    propertyType = setterType;
                }

                if (propertyType == null) {
                    logger.error("Property type mismatch for '{}' between getter and setter", TO_LOWER_CAMEL.convert(propertyName));
                    continue;
                }

                try {
                    accessorList.add(new PropertyAccessor(TO_LOWER_CAMEL.convert(propertyName), componentType, propertyType,
                            createGetterFunction(getter, propertyName, getterType, componentType),
                            createSetterFunction(method, propertyName, setterType, componentType)));
                } catch (Throwable t) {
                    logger.error("Failed to create accessor for property {} of {}", propertyName, componentType, t);
                }
            }
        }
        return accessorList;
    }

}
