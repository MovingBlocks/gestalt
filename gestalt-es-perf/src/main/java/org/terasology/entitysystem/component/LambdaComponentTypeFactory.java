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

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitysystem.core.Component;
import org.terasology.util.reflection.GenericsUtil;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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
 * A ComponentType factory making use of Java 8's LambdaMetafactory class to create high performance constructors an accessors for Components.
 */
public class LambdaComponentTypeFactory implements ComponentTypeFactory {
    private static final Logger logger = LoggerFactory.getLogger(LambdaComponentTypeFactory.class);
    private static final Converter<String, String> TO_LOWER_CAMEL = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL);
    private static final Set<String> IGNORE_PROPERTIES = ImmutableSet.of("Dirty");

    @Override
    @NonNull
    public <T extends Component<T>> ComponentType<T> createComponentType(Class<T> type) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
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
            CallSite site = LambdaMetafactory.metafactory(lookup, "apply", MethodType.methodType(Function.class), copyCon.type().erase(), copyCon, copyCon.type());

            return (Function<T, T>) site.getTarget().invoke();

        } catch (Throwable e) {
            copyConstructor = (T from) -> {
                T result = emptyConstructor.get();
                result.copy(from);
                result.setDirty(false);
                return result;
            };
        }
        return copyConstructor;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private <T extends Component<T>> Supplier<T> getEmptyConstructor(Class<T> type, MethodHandles.Lookup lookup) {
        try {
            MethodHandle constructor = lookup.findConstructor(type, MethodType.methodType(Void.TYPE));
            CallSite site = LambdaMetafactory.metafactory(lookup, "get", MethodType.methodType(Supplier.class), constructor.type().erase(), constructor, constructor.type());
            return (Supplier<T>) site.getTarget().invokeExact();
        } catch (Throwable e) {
            throw new ComponentTypeGenerationException("Failed to access empty constructor of " + type, e);
        }
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

    @SuppressWarnings("unchecked")
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


                    Function<T, Object> getterFunc = (Function<T, Object>) LambdaMetafactory.metafactory(lookup, "apply", MethodType.methodType(Function.class), getterMethod.type().erase(), getterMethod, getterMethod.type()).getTarget().invoke();
                    BiConsumer<T, Object> setterFunc = (BiConsumer<T, Object>) LambdaMetafactory.metafactory(lookup, "accept", MethodType.methodType(BiConsumer.class), setterMethod.type().erase(), setterMethod, setterMethod.type()).getTarget().invoke();


                    accessorList.add(new PropertyAccessor<>(TO_LOWER_CAMEL.convert(propertyName), componentType, propertyType, getterFunc, setterFunc));
                } catch (Throwable e) {
                    logger.error("Failed to create property for {}", propertyName, e);
                }
            }
        }
        return accessorList;
    }
}
