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
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitysystem.core.Component;
import org.terasology.valuetype.TypeLibrary;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A ComponentManager that generates component implementation classes. This expect component interfaces to define properties with getter and setter methods -
 */
public class CodeGenComponentManager implements ComponentManager {

    private static final Logger logger = LoggerFactory.getLogger(CodeGenComponentManager.class);

    private static final Converter<String, String> TO_LOWER_CAMEL = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL);
    private static final Converter<String, String> TO_UPPER_CAMEL = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_CAMEL);

    private static final Joiner BOOLEAN_AND_JOINER = Joiner.on(" && ");
    private static final Joiner COMMA_JOINER = Joiner.on(", ");

    private Map<Class<? extends Component>, ComponentType> componentMetadataLookup = new MapMaker().concurrencyLevel(4).makeMap();

    private ClassPool pool;
    private URLClassLoader targetLoader;

    private final TypeLibrary typeLibrary;

    public CodeGenComponentManager(TypeLibrary typeLibrary) {
        this.typeLibrary = typeLibrary;
        this.targetLoader = new URLClassLoader(new URL[0]);
        ClassPool.doPruning = true;
        pool = new ClassPool(ClassPool.getDefault());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> ComponentType<T> getType(Class<T> type) {
        Preconditions.checkArgument(type.isInterface(), "Expected component type to be an interface");
        return this.componentMetadataLookup.computeIfAbsent(type, k -> createComponentType(type));
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
            return (T) copy(instance, getType(instance.getType()).create());
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> T copy(T from, T to) {
        Preconditions.checkNotNull(from);
        Preconditions.checkNotNull(to);
        Preconditions.checkArgument(from.getClass().equals(to.getClass()), "Components from and to must be of the same type");
        ComponentType<T> metadata = (ComponentType<T>) getType(from.getType());
        return metadata.copy(from, to);
    }

    /**
     * Generates a component type, constructing an implementation class for the component.
     * <p>
     * This uses javassist to generate the implementation of the getters and setters for the component
     *
     * @param type The interface type for the component
     * @param <T>  The interface type of the component
     * @return The ComponentType for the component
     */
    @SuppressWarnings("unchecked")
    private <T extends Component> ComponentType<T> createComponentType(Class<T> type) {
        try {
            CtClass componentInterface = pool.get(type.getName());

            CtClass componentClass = pool.makeClass(type.getName() + "Imp");
            componentClass.setInterfaces(new CtClass[]{componentInterface});

            Collection<PropertyAccessor<T, ?>> accessorList = discoverProperties(type);

            for (PropertyAccessor<T, ?> accessor : accessorList) {
                generateGettersAndSetters(componentClass, accessor);
            }

            generateConstructor(componentClass);
            generateEquals(componentClass, accessorList);
            generateHashCode(componentClass, accessorList);
            generateGetType(componentClass, type);

            Class<? extends T> implementationClass = componentClass.toClass(targetLoader, type.getProtectionDomain());
            ComponentPropertyInfo<T> propertyInfo = new ComponentPropertyInfo<>(accessorList);
            return new ComponentType<>(() -> {
                try {
                    return implementationClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException("Failed to instantiate component " + type.getName(), e);
                }
            }, type, propertyInfo, new ComponentCopyFunction<>(propertyInfo, typeLibrary));
        } catch (CannotCompileException e) {
            throw new RuntimeException("Error compiling component implementation '" + type.getName() + "'", e);
        } catch (NotFoundException e) {
            throw new RuntimeException("Error resolving component interface '" + type.getName() + "'", e);
        }
    }

    private void generateConstructor(CtClass componentClass) throws CannotCompileException {
        CtConstructor ctConstructor = CtNewConstructor.defaultConstructor(componentClass);
        componentClass.addConstructor(ctConstructor);
    }

    private <T extends Component> void generateEquals(CtClass componentClass, Collection<PropertyAccessor<T, ?>> accessorList) throws CannotCompileException {
        StringBuilder builder = new StringBuilder();
        builder.append("public boolean equals(Object obj) {")
                .append("if (obj == this) { return true; }")
                .append("if (obj instanceof ").append(componentClass.getName()).append(") {")
                .append(componentClass.getName()).append(" other = (").append(componentClass.getName()).append(") obj;")
                .append("return ");
        BOOLEAN_AND_JOINER.appendTo(builder, accessorList.stream().map((t) -> Objects.class.getCanonicalName() + ".equals(" + t.getName() + ", other." + t.getName() + ")").collect(Collectors.toList()));
        builder.append("; } return false; }");
        CtMethod equals = CtNewMethod.make(builder.toString(), componentClass);
        componentClass.addMethod(equals);
    }

    private <T extends Component> void generateHashCode(CtClass componentClass, Collection<PropertyAccessor<T, ?>> accessorList) throws CannotCompileException {
        StringBuilder builder = new StringBuilder();
        builder.append("public int hashCode() {")
                .append("return ").append(Objects.class.getCanonicalName()).append(".hash(new Object[]{");
        COMMA_JOINER.appendTo(builder, accessorList.stream().map(PropertyAccessor::getName).collect(Collectors.toList()));
        builder.append("}); }");
        CtMethod equals = CtNewMethod.make(builder.toString(), componentClass);
        componentClass.addMethod(equals);
    }

    private <T extends Component> void generateGetType(CtClass componentClass, Class<? extends Component> interfaceType) throws CannotCompileException {
        CtMethod getType = CtNewMethod.make("public Class getType() { return " + interfaceType.getCanonicalName() + ".class; }", componentClass);
        componentClass.addMethod(getType);
    }

    private <T extends Component> void generateGettersAndSetters(CtClass componentClass, PropertyAccessor<T, ?> accessor) throws CannotCompileException {
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

    /**
     * Scans a component interface, discovering the properties it declares and creating a property accessor for the component.
     *
     * @param componentType The type of the component to scan
     * @param <T>           The type of the component to scan
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
