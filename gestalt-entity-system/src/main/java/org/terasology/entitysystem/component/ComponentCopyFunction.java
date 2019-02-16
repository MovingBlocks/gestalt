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

import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitysystem.core.Component;
import org.terasology.valuetype.TypeHandler;
import org.terasology.valuetype.TypeLibrary;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * A function for copying the properties across from one component to another
 */
public class ComponentCopyFunction<T extends Component> implements BiFunction<T, T, T> {

    private static final Logger logger = LoggerFactory.getLogger(ComponentCopyFunction.class);

    private final List<BiConsumer<T, T>> propertyCopiers;

    @SuppressWarnings("unchecked")
    public ComponentCopyFunction(ComponentPropertyInfo<T> typeInfo, TypeLibrary typeLibrary) {
        this.propertyCopiers = Lists.newArrayListWithCapacity(typeInfo.getProperties().size());
        for (PropertyAccessor accessor : typeInfo.getProperties().values()) {
            Optional<TypeHandler<?>> handler = typeLibrary.getHandlerFor(accessor.getPropertyType());
            if (handler.isPresent()) {
                propertyCopiers.add(createPropertyCopy(accessor, handler.get()));
            } else {
                logger.error("No type handler available for {}, property {}::{} will not be supported", accessor.getPropertyType(), accessor.getOwningClass().getTypeName(), accessor.getName());
            }
        }
    }

    private static <T extends Component, U> BiConsumer<T, T> createPropertyCopy(PropertyAccessor<T, U> accessor, TypeHandler<U> handler) {
        return (from, to) -> accessor.set(to, handler.copy(accessor.get(from)));
    }

    @Override
    public T apply(T from, T to) {
        for (BiConsumer<T, T> propertyCopier : propertyCopiers) {
            propertyCopier.accept(from, to);
        }
        return to;
    }

}
