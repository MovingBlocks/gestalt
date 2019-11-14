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
import android.support.annotation.RequiresApi;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.gestalt.util.reflection.GenericsUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A ComponentType factory making use of Java 7's MethodHandle class to create high performance constructors an accessors for Components.
 */
@RequiresApi(26)
public class MethodHandleComponentTypeFactory implements ComponentTypeFactory {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandleComponentTypeFactory.class);
    private static final Converter<String, String> TO_LOWER_CAMEL = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL);
    private static final Set<String> IGNORE_PROPERTIES = ImmutableSet.of("Dirty");

    @Override
    @NonNull
    public <T extends Component<T>> ComponentType<T> createComponentType(Class<T> type) {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        Collection<PropertyAccessor<T, ?>> propertyAccessors = discoverProperties(type, lookup);
        if (propertyAccessors.isEmpty()) {
            return createSingletonComponentType(type);
        }

        Supplier<T> emptyConstructor = getEmptyConstructor(type, lookup);

        Function<T, T> copyConstructor = getCopyConstructor(type, emptyConstructor, lookup);
        return new ComponentType<>(type, emptyConstructor, copyConstructor, new ComponentPropertyInfo<>(propertyAccessors));
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private <T extends Component<T>> Function<T, T> getCopyConstructor(Class<T> type, Supplier<T> emptyConstructor, MethodHandles.Lookup lookup) {
        Function<T, T> copyConstructor;
        try {
            MethodHandle copyCon = lookup.findConstructor(type, MethodType.methodType(Void.TYPE, type));
            copyConstructor = (T from) -> {
                try {
                    return (T) copyCon.invokeWithArguments(from);
                } catch (Throwable e) {
                    throw new ComponentTypeGenerationException("Failed to instatiate " + type, e);
                }
            };
        } catch (NoSuchMethodException | IllegalAccessException e) {
            copyConstructor = (T from) -> {
                T result = emptyConstructor.get();
                result.copy(from);
                return result;
            };
        }
        return copyConstructor;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private <T extends Component<T>> Supplier<T> getEmptyConstructor(Class<T> type, MethodHandles.Lookup lookup) {
        Supplier<T> emptyConstructor;
        try {
            MethodHandle constructor = lookup.findConstructor(type, MethodType.methodType(Void.TYPE));
            emptyConstructor = () -> {
                try {
                    return (T) constructor.invoke();
                } catch (Throwable e) {
                    throw new ComponentTypeGenerationException("Failed to instantiate " + type, e);
                }
            };
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new ComponentTypeGenerationException("Component missing empty constructor: " + type, e);
        }
        return emptyConstructor;
    }

    @NonNull
    private <T extends Component<T>> ComponentType<T> createSingletonComponentType(Class<T> type) {
        try {
            T instance = type.newInstance();
            return new ComponentType<>(type, () -> instance, t -> instance, new ComponentPropertyInfo<>(Collections.emptySet()));
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ComponentTypeGenerationException("Component missing empty constructor: " + type, e);
        }
    }

    private <T extends Component<T>> Collection<PropertyAccessor<T, ?>> discoverProperties(Class<T> componentType, MethodHandles.Lookup lookup) {
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

                try {
                    MethodHandle getterMethod = lookup.findVirtual(componentType, getterMethodName, MethodType.methodType(GenericsUtil.getClassOfType(propertyType)));
                    MethodHandle setterMethod = lookup.findVirtual(componentType, method.getName(), MethodType.methodType(Void.TYPE, GenericsUtil.getClassOfType(propertyType)));

                    accessorList.add(new PropertyAccessor<>(TO_LOWER_CAMEL.convert(propertyName), componentType, propertyType, t -> {
                        try {
                            return getterMethod.invoke(t);
                        } catch (Throwable e) {
                            throw new ComponentTypeGenerationException("Failed to access property " + propertyName + " of " + componentType, e);
                        }
                    }, (t, o) -> {
                        try {
                            setterMethod.invokeWithArguments(t, o);
                        } catch (Throwable e) {
                            throw new ComponentTypeGenerationException("Failed to set property " + propertyName + " of " + componentType, e);
                        }
                    }));
                } catch (NoSuchMethodException | IllegalAccessException e) {
                    logger.error("Failed to create property for {}", propertyName);
                }
            }
        }
        return accessorList;
    }
}
