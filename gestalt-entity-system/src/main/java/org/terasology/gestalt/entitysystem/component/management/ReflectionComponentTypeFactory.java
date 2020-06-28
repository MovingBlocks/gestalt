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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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

import static org.terasology.gestalt.util.reflection.GenericsUtil.getClassOfType;

/**
 * A reflection based component manager - suitable for use in situations where more performant mechanisms are not available
 * (notably older versions of Android).
 */
public class ReflectionComponentTypeFactory extends AbstractComponentTypeFactory {
    private static final Logger logger = LoggerFactory.getLogger(ReflectionComponentTypeFactory.class);

    @Override
    protected <T extends Component<T>> Function<T, T> getCopyConstructor(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor(type);
            return (T from) -> {
                try {
                    return constructor.newInstance(from);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new ComponentTypeGenerationException("Failed to instantiate " + type, e);
                }
            };
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Override
    @NonNull
    protected <T extends Component<T>> Supplier<T> getEmptyConstructor(Class<T> type) {
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

    @Override
    protected <T extends Component<T>> Function<T, Object> createGetterFunction(Method method, String propertyName, Type propertyType, Class<T> componentType) {
        return t -> {
            try {
                return method.invoke(t);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ComponentTypeGenerationException("Failed to invoke getter for '" + propertyName + "' of '" + componentType + "'", e);
            }
        };
    }

    @Override
    protected <T extends Component<T>> Function<T, Object> createGetterFunction(Field field, String propertyName, Type propertyType, Class<T> componentType) throws Throwable {
        return t -> {
            try {
                return field.get(t);
            } catch (IllegalAccessException e) {
                throw new ComponentTypeGenerationException("Failed to get value for '" + propertyName + "' of '" + componentType + "'", e);
            }
        };
    }

    @Override
    protected <T extends Component<T>> BiConsumer<T, Object> createSetterFunction(Method method, String propertyName, Type propertyType, Class<T> componentType) {
        return (t, o) -> {
            try {
                method.invoke(t, o);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ComponentTypeGenerationException("Failed to invoke setter for '" + propertyName + "' of '" + componentType + "'", e);
            }
        };
    }

    @Override
    protected <T extends Component<T>> BiConsumer<T, Object> createSetterFunction(Field field, String propertyName, Type propertyType, Class<T> componentType) throws Throwable {
        return (t, o) -> {
            try {
                field.set(t, o);
            } catch (IllegalAccessException e) {
                throw new ComponentTypeGenerationException("Failed to set value for '" + propertyName + "' of '" + componentType + "'", e);
            }
        };
    }


}
