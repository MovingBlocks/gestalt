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

import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.gestalt.util.reflection.GenericsUtil;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A ComponentType factory making use of Java 8's LambdaMetafactory class to create high performance constructors an accessors for Components.
 */
public class LambdaComponentTypeFactory extends AbstractComponentTypeFactory {

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Component<T>> Function<T, T> getCopyConstructor(Class<T> type) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            MethodHandle copyCon = lookup.findConstructor(type, MethodType.methodType(Void.TYPE, type));
            CallSite site = LambdaMetafactory.metafactory(lookup, "apply", MethodType.methodType(Function.class), copyCon.type().erase(), copyCon, copyCon.type());
            return (Function<T, T>) site.getTarget().invoke();
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    @NonNull
    @SuppressWarnings("unchecked")
    protected <T extends Component<T>> Supplier<T> getEmptyConstructor(Class<T> type) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            MethodHandle constructor = lookup.findConstructor(type, MethodType.methodType(Void.TYPE));
            CallSite site = LambdaMetafactory.metafactory(lookup, "get", MethodType.methodType(Supplier.class), constructor.type().erase(), constructor, constructor.type());
            return (Supplier<T>) site.getTarget().invokeExact();
        } catch (Throwable e) {
            throw new ComponentTypeGenerationException("Failed to access empty constructor of " + type, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Component<T>> Function<T, Object> createGetterFunction(Method method, String propertyName, Type propertyType, Class<T> componentType) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle handle = lookup.findVirtual(componentType, method.getName(), MethodType.methodType(GenericsUtil.getClassOfType(propertyType)));
        return (Function<T, Object>) LambdaMetafactory.metafactory(lookup, "apply", MethodType.methodType(Function.class), handle.type().erase(), handle, handle.type()).getTarget().invoke();
    }

    @Override
    protected <T extends Component<T>> Function<T, Object> createGetterFunction(Field field, String propertyName, Type propertyType, Class<T> componentType) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle handle = lookup.unreflectGetter(field);
        return t -> {
            try {
                return handle.invoke(t);
            } catch (Throwable e) {
                throw new ComponentTypeGenerationException("Failed to access field " + propertyName + " of " + componentType, e);
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Component<T>> BiConsumer<T, Object> createSetterFunction(Method method, String propertyName, Type propertyType, Class<T> componentType) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle handle = lookup.findVirtual(componentType, method.getName(), MethodType.methodType(Void.TYPE, GenericsUtil.getClassOfType(propertyType)));
        return (BiConsumer<T, Object>) LambdaMetafactory.metafactory(lookup, "accept", MethodType.methodType(BiConsumer.class), handle.type().erase(), handle, handle.type()).getTarget().invoke();
    }

    @Override
    protected <T extends Component<T>> BiConsumer<T, Object> createSetterFunction(Field field, String propertyName, Type propertyType, Class<T> componentType) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodHandle handle = lookup.unreflectSetter(field);
        return (t, o) -> {
            try {
                handle.invokeWithArguments(t, o);
            } catch (Throwable e) {
                throw new ComponentTypeGenerationException("Failed to set property " + propertyName + " of " + componentType, e);
            }
        };
    }


}









