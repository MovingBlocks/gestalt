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

import java.util.Collections;
import java.util.Map;

/**
 *
 */
public class ComponentMetadata {

    private int id;
    private ComponentType<?> componentType;
    private BiMap<Integer, String> fieldIds;

    public ComponentMetadata(int id, ComponentType<?> type) {
        this(id, type, Collections.emptyMap());
    }

    public ComponentMetadata(int id, ComponentType<?> type, Map<Integer, String> existingFieldIds) {
        this.id = id;
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

    public ComponentType<?> getComponentType() {
        return componentType;
    }

    public int getFieldId(String field) {
        Preconditions.checkArgument(componentType.getPropertyInfo().getProperty(field).isPresent());
        return fieldIds.inverse().get(field);
    }

    public PropertyAccessor<?, ?> getFieldAccessor(int fieldId) {
        return componentType.getPropertyInfo().getProperty(fieldIds.get(fieldId)).orElseThrow(IllegalArgumentException::new);
    }

}
