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
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.component.ComponentType;
import org.terasology.entitysystem.component.module.ComponentTypeIndex;
import org.terasology.entitysystem.core.Component;
import org.terasology.module.ModuleEnvironment;
import org.terasology.naming.Name;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Holds information on components, for use during serialization and deserialization. Primarily this is the mapping between component indexes used in serialized formats, and
 * the Component type this represents.
 */
public class ComponentManifest {

    private final ModuleEnvironment moduleEnvironment;
    private ComponentManager componentManager;
    private int nextComponentId;
    private final List<ComponentMetadata<?>> componentInfo;
    private final Map<Class<? extends Component>, ComponentMetadata<?>> componentInfoByType;
    private final TIntObjectMap<ComponentMetadata> componentInfoById;

    /**
     * @param moduleEnvironment The active moduleEnvironment. Used to determine what module a component type belongs to
     * @param componentManager The active componentManager. Used to obtain the ComponentType for a given component
     */
    public ComponentManifest(ModuleEnvironment moduleEnvironment, ComponentManager componentManager) {
        this.moduleEnvironment = moduleEnvironment;
        this.componentManager = componentManager;
        componentInfo = Lists.newArrayList();
        componentInfoByType = Maps.newHashMap();
        componentInfoById = new TIntObjectHashMap<>();
    }

    /**
     * Adds component metadata to the manifest.
     * @param metadata The metadata to add
     * @param <T>
     * @throws IllegalArgumentException If the id or component type in the metadata has already been used in the manifest
     */
    public <T extends Component> void addComponentMetadata(ComponentMetadata<T> metadata) {
        Preconditions.checkArgument(!componentInfoById.containsKey(metadata.getId()), "ComponentMetadata with id " + metadata.getId() + " already registered");
        Optional<ComponentType<T>> componentType = metadata.getComponentType();
        if (componentType.isPresent()) {
            Preconditions.checkArgument(!componentInfoByType.containsKey(componentType.get().getComponentClass()), "ComponentMetadata for type " + componentType + " already registered");
        }

        componentInfo.add(metadata);
        componentInfoById.put(metadata.getId(), metadata);
        if (componentType.isPresent()) {
            componentInfoByType.put(componentType.get().getComponentClass(), metadata);
        }
        if (metadata.getId() >= nextComponentId) {
            nextComponentId = metadata.getId() + 1;
        }
    }


    /**
     * @param type The type of component to get metadata for
     * @param <T>
     * @return The component metadata for the given type, generating it and assigning it a free id if it wasn't already in the manifest
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> ComponentMetadata getComponentMetadata(Class<T> type) {
        ComponentMetadata<T> componentMetadata = (ComponentMetadata<T>) componentInfoByType.get(type);
        if (componentMetadata == null) {
            String name = type.getSimpleName();
            if (name.endsWith(ComponentTypeIndex.COMPONENT_SUFFIX)) {
                name = name.substring(0, name.length() - ComponentTypeIndex.COMPONENT_SUFFIX.length());
            }

            componentMetadata = new ComponentMetadata<>(nextComponentId++, moduleEnvironment.getModuleProviding(type), new Name(name), componentManager.getType(type));
            addComponentMetadata(componentMetadata);
        }
        return componentMetadata;
    }

    /**
     * @param id
     * @return The component metadata associated with the given id, or null if no component is assigned that id
     */
    public Optional<ComponentMetadata<?>> getComponentMetadata(int id) {
        return Optional.ofNullable(componentInfoById.get(id));
    }

    /**
     * @return An unmodifiable list of all the component metadata in the manifest
     */
    public List<ComponentMetadata<?>> allComponentMetadata() {
        return Collections.unmodifiableList(componentInfo);
    }
}
