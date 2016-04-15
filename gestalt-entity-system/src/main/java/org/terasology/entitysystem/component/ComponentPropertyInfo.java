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

import com.google.common.collect.ImmutableMap;
import org.terasology.entitysystem.entity.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Information and access to a property of a component
 */
public class ComponentPropertyInfo<T extends Component> {

    private final Map<String, PropertyAccessor<T, ?>> properties;

    /**
     * Constructs the property info
     * @param accessors
     */
    public ComponentPropertyInfo(Collection<PropertyAccessor<T, ?>> accessors) {
        ImmutableMap.Builder<String, PropertyAccessor<T, ?>> builder = ImmutableMap.builder();
        accessors.forEach(x -> builder.put(x.getName(), x));
        this.properties = builder.build();
    }

    /**
     * @return An immutable map of property accessors
     */
    public Map<String, PropertyAccessor<T, ?>> getProperties() {
        return properties;
    }

    /**
     * @param name The name of a property
     * @return The accessor for the requested property, or Optional.empty()
     */
    public Optional<PropertyAccessor<T, ?>> getProperty(String name) {
        return Optional.ofNullable(properties.get(name));
    }
}
