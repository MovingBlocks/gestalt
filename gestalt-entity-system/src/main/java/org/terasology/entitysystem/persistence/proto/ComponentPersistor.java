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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.component.ComponentType;
import org.terasology.entitysystem.component.PropertyAccessor;
import org.terasology.entitysystem.entity.Component;
import org.terasology.entitysystem.persistence.proto.exception.PersistenceException;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

import java.util.Objects;
import java.util.Optional;

/**
 *
 */
public class ComponentPersistor {

    private static final Logger logger = LoggerFactory.getLogger(ComponentPersistor.class);

    private final ComponentManager componentManager;
    private final ComponentManifest manifest;
    private final ProtoPersistence context;


    public ComponentPersistor(ComponentManager componentManager, ProtoPersistence context, ComponentManifest manifest) {
        this.componentManager = componentManager;
        this.manifest = manifest;
        this.context = context;
    }


    public <T extends Component> ProtoDatastore.Component.Builder serialize(T component) {
        ComponentType<T> componentType = componentManager.getType(component);
        ComponentMetadata componentMetadata = manifest.getComponentMetadata(componentType);
        ProtoDatastore.Component.Builder builder = ProtoDatastore.Component.newBuilder();
        builder.setTypeIndex(componentMetadata.getId());
        for (PropertyAccessor<T, ?> property : componentType.getPropertyInfo().getProperties().values()) {
            builder.addFieldId(componentMetadata.getFieldId(property.getName()));
            builder.addFieldValues(context.serialize(property.get(component), property.getPropertyType()));
        }
        return builder;
    }

    public <T extends Component> ProtoDatastore.Component.Builder serializeDelta(T baseComponent, T component) {
        ComponentType<T> componentType = componentManager.getType(component);
        ComponentMetadata componentMetadata = manifest.getComponentMetadata(componentType);
        ProtoDatastore.Component.Builder builder = ProtoDatastore.Component.newBuilder();
        builder.setTypeIndex(componentMetadata.getId());
        for (PropertyAccessor<T, ?> property : componentType.getPropertyInfo().getProperties().values()) {
            if (!Objects.equals(property.get(component), property.get(baseComponent))) {
                builder.addFieldId(componentMetadata.getFieldId(property.getName()));
                builder.addFieldValues(context.serialize(property.get(component), property.getPropertyType()));
            }
        }
        return builder;
    }

    public Optional<Component> deserialize(ProtoDatastore.Component componentData) {
        ComponentMetadata componentMetadata = manifest.getComponentMetadata(componentData.getTypeIndex());
        if (componentMetadata.getComponentType().isPresent()) {
            ComponentType<?> componentType = componentMetadata.getComponentType().get();
            Component comp = componentType.create();
            for (int i = 0; i < componentData.getFieldIdCount(); ++i) {
                PropertyAccessor fieldAccessor = componentMetadata.getFieldAccessor(componentData.getFieldId(i));
                fieldAccessor.set(comp, context.deserialize(componentData.getFieldValues(i), fieldAccessor.getPropertyType()));
            }
            return Optional.of(comp);
        } else {
            return Optional.empty();
        }
    }

    public <T extends Component> T deserializeOnto(ProtoDatastore.Component data, T target) {
        ComponentType<T> componentType = componentManager.getType(target);
        ComponentMetadata componentMetadata = manifest.getComponentMetadata(data.getTypeIndex());
        if (componentMetadata.getComponentType().isPresent() && componentMetadata.getComponentType().get().equals(componentType)) {
            for (int i = 0; i < data.getFieldIdCount(); ++i) {
                PropertyAccessor fieldAccessor = componentMetadata.getFieldAccessor(data.getFieldId(i));
                fieldAccessor.set(target, context.deserialize(data.getFieldValues(i), fieldAccessor.getPropertyType()));
            }
            return target;
        } else {
            throw new PersistenceException("Incorrect component data for expected type: Expected '" + target.getClass().getSimpleName() + "', found '" + componentMetadata.getName());
        }
    }
}
