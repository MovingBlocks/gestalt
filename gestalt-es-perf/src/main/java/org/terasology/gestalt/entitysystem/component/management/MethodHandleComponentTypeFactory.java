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
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A ComponentType factory making use of Java 7's MethodHandle class to create high performance constructors an accessors for Components.
 */
@RequiresApi(26)
public class MethodHandleComponentTypeFactory extends AbstractComponentTypeFactory {

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Component<T>> Function<T, T> getCopyConstructor(Class<T> type) {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        try {
            MethodHandle copyCon = lookup.findConstructor(type, MethodType.methodType(Void.TYPE, type));
            return (T from) -> {
                try {
                    return (T) copyCon.invokeWithArguments(from);
                } catch (Throwable e) {
                    throw new ComponentTypeGenerationException("Failed to instatiate " + type, e);
                }
            };
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }

    @Override
    @NonNull
    @SuppressWarnings("unchecked")
    protected <T extends Component<T>> Supplier<T> getEmptyConstructor(Class<T> type) {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
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

    @Override
    protected <T extends Component<T>> Function<T, Object> createGetterFunction(Method method, String propertyName, Type propertyType, Class<T> componentType) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodHandle handle = lookup.findVirtual(componentType, method.getName(), MethodType.methodType(GenericsUtil.getClassOfType(propertyType)));
        return t -> {
            try {
                return handle.invoke(t);
            } catch (Throwable e) {
                throw new ComponentTypeGenerationException("Failed to access property " + propertyName + " of " + componentType, e);
            }
        };
    }

    @Override
    protected <T extends Component<T>> BiConsumer<T, Object> createSetterFunction(Method method, String propertyName, Type propertyType, Class<T> componentType) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodHandle handle = lookup.findVirtual(componentType, method.getName(), MethodType.methodType(Void.TYPE, GenericsUtil.getClassOfType(propertyType)));
        return (t, o) -> {
            try {
                handle.invokeWithArguments(t, o);
            } catch (Throwable e) {
                throw new ComponentTypeGenerationException("Failed to set property " + propertyName + " of " + componentType, e);
            }
        };
    }
}
