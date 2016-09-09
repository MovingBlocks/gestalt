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

import com.esotericsoftware.reflectasm.MethodAccess;
import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitysystem.entity.Component;
import org.terasology.valuetype.TypeLibrary;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A ComponentManager that generates component implementation classes. This expect component interfaces to define properties with getter and setter methods -
 */
public class CodeGenComponentManager implements ComponentManager {

    private static final Logger logger = LoggerFactory.getLogger(CodeGenComponentManager.class);

    private static final Converter<String, String> TO_LOWER_CAMEL = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL);
    private static final Converter<String, String> TO_UPPER_CAMEL = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_CAMEL);

    private Map<Class<? extends Component>, ComponentType> componentMetadataLookup = new MapMaker().concurrencyLevel(4).makeMap();
    private Map<Class<? extends Component>, ComponentType> componentImplMetadataLookup = new MapMaker().concurrencyLevel(4).makeMap();

    private ClassPool pool;
    private CtClass parent;
    private URLClassLoader targetLoader;

    private final TypeLibrary typeLibrary;

    public CodeGenComponentManager(TypeLibrary typeLibrary) {
        this.typeLibrary = typeLibrary;
        this.targetLoader = new URLClassLoader(new URL[0]);
        ClassPool.doPruning = true;
        pool = new ClassPool(ClassPool.getDefault());
        try {
            parent = pool.get(SharedComponentImpl.class.getName());
        } catch (NotFoundException e) {
            throw new RuntimeException("Unable to resolve SharedComponentImpl", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> ComponentType<T> getType(Class<T> type) {
        if (type.isInterface()) {
            ComponentType<T> componentMetadata = this.componentMetadataLookup.get(type);
            if (componentMetadata == null) {
                componentMetadata = createComponentType(type);
                this.componentMetadataLookup.put(type, componentMetadata);
                this.componentImplMetadataLookup.put(componentMetadata.getImplType(), componentMetadata);
            }
            return componentMetadata;
        } else {
            return this.componentImplMetadataLookup.get(type);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> ComponentType<T> getType(T instance) {
        return (ComponentType<T>) getType(instance.getClass());
    }

    @Override
    public <T extends Component> T create(Class<T> type) {
        ComponentType<T> typeInfo = getType(type);
        return typeInfo.create();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> T copy(T instance) {
        if (instance != null) {
            Class<T> componentClass = (Class<T>) instance.getClass();
            return copy(instance, getType(componentClass).create());
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> T copy(T from, T to) {
        Preconditions.checkNotNull(from);
        Preconditions.checkNotNull(to);
        Preconditions.checkArgument(from.getClass().equals(to.getClass()), "Components from and to must be of the same type");
        Class<T> componentClass = (Class<T>) from.getClass();
        ComponentType<T> metadata = getType(componentClass);
        return metadata.copy(from, to);
    }
    /**
     * Generates a component type, constructing an implementation class for the component.
     * <p>
     * This uses javassist to generate the implementation of the getters and setters for the component
     * @param type The interface type for the component
     * @param <T> The interface type of the component
     * @return The ComponentType for the component
     */
    @SuppressWarnings("unchecked")
    private <T extends Component> ComponentType<T> createComponentType(Class<T> type) {
        try {
            CtClass componentInterface = pool.get(type.getName());

            CtClass componentClass = pool.makeClass(type.getName() + "Imp");
            componentClass.setInterfaces(new CtClass[]{componentInterface});
            componentClass.setSuperclass(parent);

            Collection<PropertyAccessor<T, ?>> accessorList = discoverProperties(type);

            for (PropertyAccessor<T, ?> accessor : accessorList) {
                String typeName = accessor.getPropertyClass().getCanonicalName();
                if (accessor.getPropertyType() instanceof ParameterizedType) {
                    typeName = ((ParameterizedType) accessor.getPropertyType()).getRawType().getTypeName();
                }
                CtField ctField = CtField.make("private " + typeName + " " + accessor.getName() + ";", componentClass);
                componentClass.addField(ctField);

                String getterName;
                if (Boolean.TYPE.equals(accessor.getPropertyType())) {
                    getterName = "is" + TO_UPPER_CAMEL.convert(accessor.getName());
                } else {
                    getterName = "get" + TO_UPPER_CAMEL.convert(accessor.getName());
                }

                CtMethod getter = CtNewMethod.make("public " + typeName + " " + getterName + "() { return " + accessor.getName() + "; }", componentClass);
                componentClass.addMethod(getter);
                CtMethod setter = CtNewMethod.make("public void set" + TO_UPPER_CAMEL.convert(accessor.getName()) + "(" + typeName + " value) { this." + accessor.getName() + " = value; }", componentClass);
                componentClass.addMethod(setter);
            }

            Class<? extends T> implementationClass = componentClass.toClass(targetLoader, type.getProtectionDomain());
            ComponentPropertyInfo<T> propertyInfo = new ComponentPropertyInfo<>(accessorList);
            return new ComponentType<>(() -> {
                try {
                    return implementationClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException("Failed to instantiate component " + type.getName(), e);
                }
            }, type, implementationClass, propertyInfo, new ComponentCopyFunction<>(propertyInfo, typeLibrary));
        } catch (CannotCompileException e) {
            throw new RuntimeException("Error compiling component implementation '" + type.getName() + "'", e);
        } catch (NotFoundException e) {
            throw new RuntimeException("Error resolving component interface '" + type.getName() + "'", e);
        }
    }

    /**
     * Scans a component interface, discovering the properties it declares and creating a property accessor for the component.
     * @param componentType The type of the component to scan
     * @param <T> The type of the component to scan
     * @return A collection of property accessors
     */
    private <T extends Component> Collection<PropertyAccessor<T, ?>> discoverProperties(Class<T> componentType) {
        List<PropertyAccessor<T, ?>> accessorList = Lists.newArrayList();

        for (Method method : componentType.getDeclaredMethods()) {
            if (method.getGenericReturnType() == Void.TYPE && method.getName().startsWith("set") && method.getParameterCount() == 1) {
                String propertyName = method.getName().substring(3);
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
                    logger.error("Unable to find getter is{}", propertyName);
                    // TODO: Exception
                    continue;
                }
                if (!getter.getGenericReturnType().equals(propertyType)) {
                    logger.error("Property type mismatch for '{}' between getter and setter", TO_LOWER_CAMEL.convert(propertyName));
                    // TODO: Exception
                    continue;
                }

                MethodAccess methodAccess = MethodAccess.get(componentType);
                int getterIndex = methodAccess.getIndex(getter.getName());
                int setterIndex = methodAccess.getIndex(method.getName());

                accessorList.add(new PropertyAccessor<>(TO_LOWER_CAMEL.convert(propertyName), componentType, propertyType,
                        t -> methodAccess.invoke(t, getterIndex),
                        (t, o) -> methodAccess.invoke(t, setterIndex, o)));
            }
        }
        return accessorList;
    }

}
