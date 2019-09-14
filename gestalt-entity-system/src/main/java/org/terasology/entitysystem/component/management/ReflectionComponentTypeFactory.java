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

package org.terasology.entitysystem.component.management;

import android.support.annotation.NonNull;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitysystem.component.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A reflection based component manager - suitable for use in situations where more performant mechanisms are not available
 * (notably older versions of Android).
 */
public class ReflectionComponentTypeFactory implements ComponentTypeFactory {

    private static final Logger logger = LoggerFactory.getLogger(ReflectionComponentTypeFactory.class);
    private static final Converter<String, String> TO_LOWER_CAMEL = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL);
    private static final Set<String> IGNORE_PROPERTIES = ImmutableSet.of("Dirty");

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

        Function<T, T> copyConstructor = getCopyConstructor(type, emptyConstructor);
        return new ComponentType<>(type, emptyConstructor, copyConstructor, new ComponentPropertyInfo<>(propertyAccessors));
    }

    @NonNull
    private <T extends Component<T>> Function<T, T> getCopyConstructor(Class<T> type, Supplier<T> emptyConstructor) {
        Function<T, T> copyConstructor;
        try {
            Constructor<T> constructor = type.getDeclaredConstructor(type);
            copyConstructor = (T from) -> {
                try {
                    return constructor.newInstance(from);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new ComponentTypeGenerationException("Failed to instantiate " + type, e);
                }
            };
        } catch (NoSuchMethodException e) {
            copyConstructor = (T from) -> {
                T result = emptyConstructor.get();
                result.copy(from);
                return result;
            };
        }
        return copyConstructor;
    }

    private <T extends Component<T>> Supplier<T> getEmptyConstructor(Class<T> type) {
        Supplier<T> emptyConstructor;
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            emptyConstructor = () -> {
                try {
                    return constructor.newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new ComponentTypeGenerationException("Failed to instantiate " + type, e);
                }
            };
        } catch (NoSuchMethodException e) {
            throw new ComponentTypeGenerationException("Component type missing empty constructor: " + type, e);
        }
        return emptyConstructor;
    }

    private <T extends Component<T>> ComponentType<T> createSingletonComponentType(Class<T> type) {
        try {
            T instance = type.newInstance();
            return new ComponentType<>(type, () -> instance, t -> instance, new ComponentPropertyInfo<>(Collections.emptySet()));
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ComponentTypeGenerationException("Component lacks an empty constructor: " + type, e);
        }
    }

    private <T extends Component<T>> Collection<PropertyAccessor<T, ?>> discoverProperties(Class<T> componentType) {
        List<PropertyAccessor<T, ?>> accessorList = Lists.newArrayList();

        for (Method method : componentType.getDeclaredMethods()) {
            if (method.getGenericReturnType() == Void.TYPE && method.getName().startsWith("set") && method.getParameterTypes().length == 1) {
                String propertyName = method.getName().substring(3);
                if (IGNORE_PROPERTIES.contains(propertyName)) {
                    continue;
                }
                Type propertyType = method.getGenericParameterTypes()[0];

                String getterMethodName;
                if (Boolean.TYPE.equals(propertyType)) {
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
                if (!getter.getGenericReturnType().equals(propertyType)) {
                    logger.error("Property type mismatch for '{}' between getter and setter", TO_LOWER_CAMEL.convert(propertyName));
                    continue;
                }

                accessorList.add(new PropertyAccessor<>(TO_LOWER_CAMEL.convert(propertyName), componentType, propertyType,
                        t -> {
                            try {
                                return getter.invoke(t);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new ComponentTypeGenerationException("Failed to invoke getter for '" + propertyName + "' of '" + componentType + "'", e);
                            }
                        },
                        (t, o) -> {
                            try {
                                method.invoke(t, o);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new ComponentTypeGenerationException("Failed to invoke setter for '" + propertyName + "' of '" + componentType + "'", e);
                            }
                        }));
            }
        }
        return accessorList;
    }


}
