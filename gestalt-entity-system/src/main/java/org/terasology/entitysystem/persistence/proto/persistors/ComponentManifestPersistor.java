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

package org.terasology.entitysystem.persistence.proto.persistors;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.module.ModuleDependencyResolutionStrategy;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.component.module.ComponentTypeIndex;
import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.persistence.proto.ComponentManifest;
import org.terasology.entitysystem.persistence.proto.ComponentMetadata;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;
import org.terasology.module.ModuleEnvironment;
import org.terasology.naming.Name;

import java.util.Map;
import java.util.Optional;

/**
 * Handles serialization and deserialization of a component manifest
 */
public class ComponentManifestPersistor {

    private static final Logger logger = LoggerFactory.getLogger(ComponentManifestPersistor.class);

    private ModuleEnvironment moduleEnvironment;
    private ComponentManager componentManager;
    private ComponentTypeIndex index;

    public ComponentManifestPersistor(ModuleEnvironment moduleEnvironment, ComponentManager componentManager) {
        this.moduleEnvironment = moduleEnvironment;
        this.componentManager = componentManager;
        index = new ComponentTypeIndex(moduleEnvironment, new ModuleDependencyResolutionStrategy(moduleEnvironment));
    }

    public ProtoDatastore.ComponentManifestData.Builder serialize(ComponentManifest manifest) {
        ProtoDatastore.ComponentManifestData.Builder builder = ProtoDatastore.ComponentManifestData.newBuilder();
        for (ComponentMetadata<?> metadata : manifest.allComponentMetadata()) {
            ProtoDatastore.ComponentInfo.Builder componentInfoBuilder = ProtoDatastore.ComponentInfo.newBuilder();
            componentInfoBuilder.setId(metadata.getId());
            componentInfoBuilder.setName(metadata.getName().toString());
            componentInfoBuilder.setModule(metadata.getModule().toString());
            for (Map.Entry<Integer, String> fieldEntry : metadata.allFields()) {
                ProtoDatastore.FieldInfo.Builder fieldBuilder = ProtoDatastore.FieldInfo.newBuilder();
                fieldBuilder.setId(fieldEntry.getKey());
                fieldBuilder.setName(fieldEntry.getValue());
                componentInfoBuilder.addField(fieldBuilder);
            }
            builder.addComponents(componentInfoBuilder);
        }

        return builder;
    }

    public ComponentManifest deserialize(ProtoDatastore.ComponentManifestData manifestData) {
        ComponentManifest manifest = new ComponentManifest(moduleEnvironment, componentManager);
        for (ProtoDatastore.ComponentInfo componentInfo : manifestData.getComponentsList()) {
            Map<Integer, String> fieldMappings = Maps.newLinkedHashMap();
            for (ProtoDatastore.FieldInfo fieldInfo : componentInfo.getFieldList()) {
                fieldMappings.put(fieldInfo.getId(), fieldInfo.getName());
            }
            Name moduleName = new Name(componentInfo.getModule());
            Name name = new Name(componentInfo.getName());
            Optional<Class<? extends Component>> type = index.find(new ResourceUrn(moduleName, name));
            if (type.isPresent()) {
                manifest.addComponentMetadata(new ComponentMetadata(componentInfo.getId(), moduleName, name, componentManager.getType(type.get()), fieldMappings));
            } else {
                logger.warn("Failed to resolve component type '{}:{}' - instances of this component will be dropped when loading entities");
                manifest.addComponentMetadata(new ComponentMetadata(componentInfo.getId(), moduleName, name, null, fieldMappings));
            }
        }

        return manifest;
    }
}
