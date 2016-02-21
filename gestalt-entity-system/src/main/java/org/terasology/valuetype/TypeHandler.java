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

package org.terasology.valuetype;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.Function;

/**
 *
 */
public class TypeHandler<T> {

    private Type type;
    private final Function<T, T> copyFunction;

    public TypeHandler(Class<T> type, Function<T, T> copyFunction) {
        this.type = type;
        this.copyFunction = copyFunction;
    }

    public TypeHandler(Type type, Function<T, T> copyFunction) {
        this.type = type;
        this.copyFunction = copyFunction;
    }

    public Type getType() {
        return type;
    }

    public T copy(T value) {
        if (value == null) {
            return null;
        }
        return copyFunction.apply(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof TypeHandler) {
            TypeHandler other = (TypeHandler) obj;
            return Objects.equals(type, other.type);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public String toString() {
        return "TypeHandler::" + type.toString();
    }
}
