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

package org.terasology.entitysystem.persistence.proto;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.terasology.entitysystem.component.ComponentType;
import org.terasology.entitysystem.component.PropertyAccessor;
import org.terasology.entitysystem.core.Component;
import org.terasology.naming.Name;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Metadata describing a component, used during persistence. This includes the id used to persist the component, and the ids used to persist each field.
 */
public class ComponentMetadata<T extends Component> {

    private final int id;
    private final Name module;
    private final Name name;
    private final ComponentType<T> componentType;
    private final BiMap<Integer, String> fieldIds;

    /**
     * Constructs the metadata for a component. The field id mapping will be generated for all fields, if the component type is available.
     *
     * @param id     The id of the component
     * @param module The module providing the component
     * @param name   The name of the component
     * @param type   The type of the component (or null if not available)
     */
    public ComponentMetadata(int id, Name module, Name name, ComponentType<T> type) {
        this(id, module, name, type, Collections.emptyMap());
    }

    /**
     * @param id               The id of the component
     * @param module           The module providing the component
     * @param name             The name of the component
     * @param type             The type of the component (or null if not available)
     * @param existingFieldIds Any existing field id mappings. Id mappings will be generated for any fields not part of this map.
     */
    public ComponentMetadata(int id, Name module, Name name, ComponentType<T> type, Map<Integer, String> existingFieldIds) {
        this.id = id;
        this.name = name;
        this.module = module;
        this.componentType = type;
        fieldIds = HashBiMap.create(existingFieldIds);
        int nextId = 1;
        for (int fieldId : existingFieldIds.keySet()) {
            if (fieldId >= nextId) {
                nextId = fieldId + 1;
            }
        }
        for (PropertyAccessor<?, ?> field : type.getPropertyInfo().getProperties().values()) {
            if (!fieldIds.inverse().containsKey(field.getName())) {
                fieldIds.put(nextId++, field.getName());
            }
        }
    }

    /**
     * @return The id for the component
     */
    public int getId() {
        return id;
    }

    /**
     * This is the shortened name for the component, which will be used when looking up the component type.
     *
     * @return The name of the component.
     */
    public Name getName() {
        return name;
    }

    /**
     * This is the module providing the component. This will be used when looking up the component type.
     *
     * @return The module providing the component.
     */
    public Name getModule() {
        return module;
    }

    /**
     * @return The type of the component, or Optional::absent if not available.
     */
    public Optional<ComponentType<T>> getComponentType() {
        return Optional.ofNullable(componentType);
    }

    /**
     * @param field The field to get the id for
     * @return The id for the field.
     * @throws IllegalArgumentException If the component doesn't have a field with the given name.
     */
    public int getFieldId(String field) {
        Preconditions.checkArgument(componentType.getPropertyInfo().getProperty(field).isPresent());
        return fieldIds.inverse().get(field);
    }

    /**
     * @param fieldId The id of a field
     * @return The property accessor for getting or setting the field with the given id.
     * @throws IllegalArgumentException If there is no field with the given id.
     */
    public PropertyAccessor<T, ?> getFieldAccessor(int fieldId) {
        return componentType.getPropertyInfo().getProperty(fieldIds.get(fieldId)).orElseThrow(IllegalArgumentException::new);
    }

    /**
     * @return an unmodifiable set of the field mappings
     */
    public Iterable<Map.Entry<Integer, String>> allFields() {
        return Collections.unmodifiableSet(fieldIds.entrySet());
    }
}
