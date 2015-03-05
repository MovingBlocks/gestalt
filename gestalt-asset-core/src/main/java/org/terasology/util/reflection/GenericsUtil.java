/*
 * Copyright 2015 MovingBlocks
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

package org.terasology.util.reflection;

import com.google.common.base.Optional;
import com.googlecode.gentyref.GenericTypeReflector;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Utility methods for working with Generics in reflection.
 *
 * @author Immortius
 */
public final class GenericsUtil {

    private GenericsUtil() {
    }

    /**
     * Used to obtain the bound value for a generic parameter of a type. Example, for a field of type List&ltString>, the 0th generic parameter is String.class.
     * A List with no parameter will return Optional.absent()
     *
     * @param target The type to obtain the generic parameter of.
     * @param index  The index of the the parameter to obtain
     * @return An optional that contains the parameter type if bound.
     */
    public static Optional<Type> getTypeParameterBinding(Type target, int index) {
        return getTypeParameterBindingForInheritedClass(target, getClassOfType(target), index);
    }

    /**
     * Used to obtained the bound value for a generic parameter of a particular class or interface that the type inherits.
     *
     * @param target     The type to obtain the generic parameter of.
     * @param superClass The superclass which the parameter belongs to
     * @param index      The index of the parameter to obtain
     * @param <T>        The type of the superclass that the parameter belongs to
     * @return An optional that contains the parameter if bound.
     */
    public static <T> Optional<Type> getTypeParameterBindingForInheritedClass(Type target, Class<T> superClass, int index) {
        if (superClass.getTypeParameters().length == 0) {
            throw new IllegalArgumentException("Class '" + superClass + "' is not parameterized");
        }
        if (!superClass.isAssignableFrom(getClassOfType(target))) {
            throw new IllegalArgumentException("Class '" + target + "' does not implement '" + superClass + "'");
        }

        Type type = GenericTypeReflector.getExactSuperType(target, superClass);
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type paramType = parameterizedType.getActualTypeArguments()[index];
            if (paramType instanceof Class || paramType instanceof ParameterizedType) {
                return Optional.of(paramType);
            }
        }
        return Optional.absent();
    }

    /**
     * Returns the raw class of a type, or null if the type doesn't represent a class.
     *
     * @param type The type to get the class of
     * @return the raw class of a type, or null if the type doesn't represent a class.
     */
    public static Class<?> getClassOfType(Type type) {
        if (type instanceof Class) {
            return (Class) type;
        } else if (type instanceof ParameterizedType) {
            return (Class) ((ParameterizedType) type).getRawType();
        }
        return null;
    }

}
