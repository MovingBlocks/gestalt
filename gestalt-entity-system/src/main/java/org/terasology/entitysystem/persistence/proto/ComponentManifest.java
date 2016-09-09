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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.terasology.entitysystem.component.ComponentType;
import org.terasology.entitysystem.component.module.ComponentTypeIndex;
import org.terasology.module.ModuleEnvironment;
import org.terasology.naming.Name;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *
 */
public class ComponentManifest {

    private final ModuleEnvironment moduleEnvironment;
    private int nextComponentId;
    private final List<ComponentMetadata> componentInfo;
    private final Map<ComponentType<?>, ComponentMetadata> componentInfoByType;
    private final Map<Integer, ComponentMetadata> componentInfoById;

    public ComponentManifest(ModuleEnvironment moduleEnvironment) {
        this.moduleEnvironment = moduleEnvironment;
        componentInfo = Lists.newArrayList();
        componentInfoByType = Maps.newHashMap();
        componentInfoById = Maps.newHashMap();
    }

    public void addComponentMetadata(ComponentMetadata metadata) {
        Preconditions.checkArgument(!componentInfoById.containsKey(metadata.getId()), "ComponentMetadata with id " + metadata.getId() + " already registered");
        Optional<ComponentType<?>> componentType = metadata.getComponentType();
        if (componentType.isPresent()) {
            Preconditions.checkArgument(!componentInfoByType.containsKey(componentType.get()), "ComponentMetadata for type " + componentType + " already registered");
        }

        componentInfo.add(metadata);
        componentInfoById.put(metadata.getId(), metadata);
        if (componentType.isPresent()) {
            componentInfoByType.put(componentType.get(), metadata);
        }
        if (metadata.getId() >= nextComponentId) {
            nextComponentId = metadata.getId() + 1;
        }
    }

    public ComponentMetadata getComponentMetadata(ComponentType<?> type) {
        ComponentMetadata componentMetadata = componentInfoByType.get(type);
        if (componentMetadata == null) {
            String name = type.getInterfaceType().getSimpleName();
            if (name.endsWith(ComponentTypeIndex.COMPONENT_SUFFIX)) {
                name = name.substring(0, name.length() - ComponentTypeIndex.COMPONENT_SUFFIX.length());
            }

            componentMetadata = new ComponentMetadata(nextComponentId++, moduleEnvironment.getModuleProviding(type.getInterfaceType()), new Name(name), type);
            addComponentMetadata(componentMetadata);
        }
        return componentMetadata;
    }

    public ComponentMetadata getComponentMetadata(int id) {
        return componentInfoById.get(id);
    }

    public Iterable<ComponentMetadata> allComponentMetadata() {
        return Collections.unmodifiableList(componentInfo);
    }
}
