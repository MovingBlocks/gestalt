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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitysystem.core.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class StandardComponentManager implements ComponentManager {

    private static final Logger logger = LoggerFactory.getLogger(StandardComponentManager.class);
    private static final Converter<String, String> TO_LOWER_CAMEL = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL);
    private static final Set<String> IGNORE_PROPERTIES = ImmutableSet.of("Dirty");

    private Map<Class<?>, ComponentType<?>> componentTypes = Maps.newLinkedHashMap();


    @Override
    public <T extends Component> T create(Class<T> type) {
        ComponentType<T> typeInfo = getType(type);
        return typeInfo.create();
    }

    @Override
    public <T extends Component> T copy(T instance) {
        ComponentType<T> typeInfo = getType(instance);
        return typeInfo.createCopy(instance);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> ComponentType<T> getType(Class<T> type) {
        ComponentType<T> typeInfo = (ComponentType<T>) componentTypes.get(type);
        if (typeInfo == null) {
            typeInfo = createComponentType(type);
            componentTypes.put(type, typeInfo);
        }
        return typeInfo;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> ComponentType<T> getType(T instance) {
        return (ComponentType<T>) getType(instance.getClass());
    }

    private <T extends Component> ComponentType<T> createComponentType(Class<T> type) {
        MethodAccess methodAccess = MethodAccess.get(type);
        Collection<PropertyAccessor<T, ?>> propertyAccessors = discoverProperties(type, methodAccess);
        if (propertyAccessors.isEmpty()) {
            return createSingletonComponentType(type);
        }

        Supplier<T> emptyConstructor = getEmptyConstructor(type);
        if (emptyConstructor == null) {
            logger.error("Component {} missing empty constructor", type);
            return null;
        }

        Function<T, T> copyConstructor = getCopyConstructor(type, emptyConstructor);
        return new ComponentType<>(type, emptyConstructor, copyConstructor, new ComponentPropertyInfo<>(propertyAccessors));
    }

    @NonNull
    private <T extends Component> Function<T, T> getCopyConstructor(Class<T> type, Supplier<T> emptyConstructor) {
        Function<T, T> copyConstructor;
        try {
            Constructor<T> constructor = type.getDeclaredConstructor(type);
            copyConstructor = (T from) -> {
                try {
                    return constructor.newInstance(from);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException("Failed to instantiate " + type, e);
                }
            };
        } catch (NoSuchMethodException e) {
            copyConstructor = (T from) -> {
                T result = emptyConstructor.get();
                result.copy(from);
                result.setDirty(false);
                return result;
            };
        }
        return copyConstructor;
    }

    @Nullable
    private <T extends Component> Supplier<T> getEmptyConstructor(Class<T> type) {
        Supplier<T> emptyConstructor;
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            emptyConstructor = () -> {
                try {
                    return constructor.newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException("Failed to instantiate " + type, e);
                }
            };
        } catch (NoSuchMethodException e) {
            // TODO: Exception
            return null;
        }
        return emptyConstructor;
    }

    @Nullable
    private <T extends Component> ComponentType<T> createSingletonComponentType(Class<T> type) {
        try {
            T instance = type.newInstance();
            return new ComponentType<>(type, () -> instance, t -> instance, new ComponentPropertyInfo<>(Collections.emptySet()));
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("Component lacks an empty constructor: {}", type, e);
            return null;
        }
    }

    private <T extends Component> Collection<PropertyAccessor<T, ?>> discoverProperties(Class<T> componentType, MethodAccess methodAccess) {
        List<PropertyAccessor<T, ?>> accessorList = Lists.newArrayList();

        for (Method method : componentType.getDeclaredMethods()) {
            if (method.getGenericReturnType() == Void.TYPE && method.getName().startsWith("set") && method.getParameterCount() == 1) {
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
                    // TODO: Exception
                    continue;
                }
                if (!getter.getGenericReturnType().equals(propertyType)) {
                    logger.error("Property type mismatch for '{}' between getter and setter", TO_LOWER_CAMEL.convert(propertyName));
                    // TODO: Exception
                    continue;
                }

                int getterIndex = methodAccess.getIndex(getter.getName(), 0);
                int setterIndex = methodAccess.getIndex(method.getName(), 1);

                accessorList.add(new PropertyAccessor<>(TO_LOWER_CAMEL.convert(propertyName), componentType, propertyType,
                        t -> methodAccess.invoke(t, getterIndex),
                        (t, o) -> methodAccess.invoke(t, setterIndex, o)));
            }
        }
        return accessorList;
    }
}
