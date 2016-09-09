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

import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.entity.Component;
import org.terasology.entitysystem.entity.EntityManager;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.entity.references.CoreEntityRef;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

import java.util.Iterator;

/**
 *
 */
public class EntityPersistor {

    private final ComponentManager componentManager;
    private final ComponentPersistor componentPersistor;
    private final ProtoPersistence context;
    private final ComponentManifest manifest;

    public EntityPersistor(ComponentManager componentManager, ProtoPersistence context, ComponentManifest manifest) {
        this.componentManager = componentManager;
        this.context = context;
        this.manifest = manifest;
        this.componentPersistor = new ComponentPersistor(componentManager, context, manifest);
    }

    public ProtoDatastore.Entity.Builder serialize(EntityRef entity) {
        ProtoDatastore.Entity.Builder builder = ProtoDatastore.Entity.newBuilder();
        builder.setId(entity.getId());
        for (Component component : entity.getComponents().values()) {
            builder.addComponent(componentPersistor.serialize(component));
        }
        return builder;
    }

    public EntityRef deserialize(ProtoDatastore.Entity data, EntityManager entityManager) {
        EntityRef entity = new CoreEntityRef(entityManager, data.getId());
        for (ProtoDatastore.Component componentData : data.getComponentList()) {
            ComponentMetadata componentMetadata = manifest.getComponentMetadata(componentData.getTypeIndex());
            if (componentMetadata.getComponentType().isPresent()) {
                Component c = entity.addComponent(componentMetadata.getComponentType().get().getInterfaceType());
                componentPersistor.deserializeOnto(componentData, c);
            }
        }
        return entity;
    }

    public ProtoDatastore.Store.Builder serializeEntities(EntityManager entityManager) {
        EntityPersistor entityPersistor = new EntityPersistor(componentManager, context, manifest);

        ProtoDatastore.Store.Builder builder = ProtoDatastore.Store.newBuilder();
        Iterator<EntityRef> i = entityManager.allEntities();
        while (i.hasNext()) {
            EntityRef entity = i.next();
            entityManager.beginTransaction();
            if (entity.isPresent()) {
                builder.addEntity(entityPersistor.serialize(entity));
            }
            entityManager.rollback();
        }

        return builder;
    }

    public void deserializeEntities(ProtoDatastore.Store data, EntityManager manager) {
        EntityPersistor entityPersistor = new EntityPersistor(componentManager, context, manifest);
        for (ProtoDatastore.Entity entityData : data.getEntityList()) {
            manager.beginTransaction();
            entityPersistor.deserialize(entityData, manager);
            manager.commit();
        }
    }
}
