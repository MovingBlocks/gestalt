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
import org.terasology.naming.Name;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 *
 */
public class ComponentMetadata {

    private final int id;
    private final Name module;
    private final Name name;
    private final ComponentType<?> componentType;
    private final BiMap<Integer, String> fieldIds;

    public ComponentMetadata(int id, String module, String name, ComponentType<?> type) {
        this(id, new Name(module), new Name(name), type, Collections.emptyMap());
    }

    public ComponentMetadata(int id, Name module, Name name, ComponentType<?> type) {
        this(id, module, name, type, Collections.emptyMap());
    }

    public ComponentMetadata(int id, Name module, Name name, ComponentType<?> type, Map<Integer, String> existingFieldIds) {
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

    public int getId() {
        return id;
    }

    public Name getName() {
        return name;
    }

    public Name getModule() {
        return module;
    }

    public Optional<ComponentType<?>> getComponentType() {
        return Optional.ofNullable(componentType);
    }

    public int getFieldId(String field) {
        Preconditions.checkArgument(componentType.getPropertyInfo().getProperty(field).isPresent());
        return fieldIds.inverse().get(field);
    }

    public PropertyAccessor<?, ?> getFieldAccessor(int fieldId) {
        return componentType.getPropertyInfo().getProperty(fieldIds.get(fieldId)).orElseThrow(IllegalArgumentException::new);
    }

    public Iterable<Map.Entry<Integer, String>> allFields() {
        return fieldIds.entrySet();
    }
}
