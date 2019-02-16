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

package org.terasology.valuetype;


import com.google.common.collect.MapMaker;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

/**
 *
 */
public class TypeLibrary {

    private Map<Type, TypeHandler<?>> typeHandlers = new MapMaker().concurrencyLevel(4).makeMap();

    public Optional<TypeHandler<?>> getHandlerFor(Type type) {
        return Optional.ofNullable(typeHandlers.get(type));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<TypeHandler<T>> getHandlerFor(Class<T> type) {
        return Optional.ofNullable((TypeHandler<T>) typeHandlers.get(type));
    }

    public void addHandler(TypeHandler<?> handler) {
        typeHandlers.put(handler.getType(), handler);
    }
}
