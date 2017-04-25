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

import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.persistence.proto.ComponentManifest;
import org.terasology.entitysystem.persistence.proto.ComponentMetadata;
import org.terasology.entitysystem.persistence.proto.ProtoPersistence;
import org.terasology.entitysystem.persistence.proto.exception.PersistenceException;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

/**
 * Serializes and Deserializes entities.
 */
public class SimpleEntityPersistor implements EntityPersistor {

    private final ComponentPersistor componentPersistor;
    private final ComponentManifest componentManifest;

    public SimpleEntityPersistor(ProtoPersistence context, ComponentManifest componentManifest) {
        this.componentManifest = componentManifest;
        this.componentPersistor = new ComponentPersistor(context, componentManifest);
    }

    @Override
    public ProtoDatastore.EntityData.Builder serialize(EntityRef entity) {
        ProtoDatastore.EntityData.Builder builder = ProtoDatastore.EntityData.newBuilder();
        builder.setId(entity.getId());
        for (Component component : entity.getComponents().values()) {
            builder.addComponent(componentPersistor.serialize(component));
        }
        return builder;
    }

    @Override
    public EntityRef deserialize(ProtoDatastore.EntityData data, EntityManager entityManager) {
        EntityRef entity = entityManager.getEntity(data.getId());
        for (ProtoDatastore.ComponentData componentData : data.getComponentList()) {
            ComponentMetadata<?> componentMetadata = componentManifest.getComponentMetadata(componentData.getTypeIndex()).orElseThrow(() -> new PersistenceException("No information found for component with index '" + componentData.getTypeIndex() + "'"));
            if (componentMetadata.getComponentType().isPresent()) {
                Component c = entity.addComponent(componentMetadata.getComponentType().get().getComponentClass());
                componentPersistor.deserializeOnto(componentData, c);
            }
        }
        return entity;
    }


}
