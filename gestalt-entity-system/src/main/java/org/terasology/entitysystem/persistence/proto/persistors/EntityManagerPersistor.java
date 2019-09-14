/*
 * Copyright 2019 MovingBlocks
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

import org.terasology.entitysystem.component.management.ComponentManager;
import org.terasology.entitysystem.entity.EntityManager;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.persistence.proto.ComponentManifest;
import org.terasology.entitysystem.persistence.proto.ProtoPersistence;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;
import org.terasology.entitysystem.transaction.TransactionManager;
import org.terasology.module.ModuleEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Serializes and Deserializes EntityManagers.
 */
public class EntityManagerPersistor {

    private final ComponentManager componentManager;
    private final ModuleEnvironment moduleEnvironment;
    private final ComponentManifestPersistor componentManifestPersistor;
    private final ProtoPersistence context;

    public EntityManagerPersistor(ModuleEnvironment moduleEnvironment, ComponentManager componentManager, ProtoPersistence context) {
        this.moduleEnvironment = moduleEnvironment;
        this.componentManager = componentManager;
        this.context = context;
        this.componentManifestPersistor = new ComponentManifestPersistor(moduleEnvironment, componentManager);
    }

    public void serialize(EntityManager entityManager, TransactionManager transactionManager, Path file) throws IOException {
        try (OutputStream stream = Files.newOutputStream(file)) {
            serialize(entityManager, transactionManager).writeTo(stream);
        }
    }

    public EntityManager deserialize(Path file, TransactionManager transactionManager) throws IOException {
        try (InputStream stream = Files.newInputStream(file)) {
            return deserialize(ProtoDatastore.Store.parseFrom(stream), transactionManager);
        }
    }

    public ProtoDatastore.Store serialize(EntityManager entityManager, TransactionManager transactionManager) {
        ComponentManifest componentManifest = new ComponentManifest(moduleEnvironment, componentManager);

        EntityPersistor entityPersistor = new SimpleEntityPersistor(context, componentManifest);
        ProtoDatastore.Store.Builder builder = serializeEntities(entityManager, transactionManager, entityPersistor);
        builder.setComponentManifest(componentManifestPersistor.serialize(componentManifest));
        builder.setNextEntityId(entityManager.getNextId());

        return builder.build();
    }

    public EntityManager deserialize(ProtoDatastore.Store entityManagerData, TransactionManager transactionManager) {
        EntityManager entityManager = new InMemoryEntityManager(componentManager, transactionManager, entityManagerData.getNextEntityId());

        ComponentManifest componentManifest = componentManifestPersistor.deserialize(entityManagerData.getComponentManifest());
        EntityPersistor entityPersistor = new SimpleEntityPersistor(context, componentManifest);
        deserializeEntities(entityManagerData, entityManager, transactionManager, entityPersistor);
        return entityManager;
    }

    private ProtoDatastore.Store.Builder serializeEntities(EntityManager entityManager, TransactionManager transactionManager, EntityPersistor entityPersistor) {
        ProtoDatastore.Store.Builder builder = ProtoDatastore.Store.newBuilder();
        Iterator<EntityRef> i = entityManager.allEntities();
        while (i.hasNext()) {
            EntityRef entity = i.next();
            transactionManager.begin();
            if (entity.isPresent()) {
                builder.addEntity(entityPersistor.serialize(entity));
            }
            transactionManager.rollback();
        }

        return builder;
    }

    private void deserializeEntities(ProtoDatastore.Store data, EntityManager entityManager, TransactionManager transactionManager, EntityPersistor entityPersistor) {
        for (ProtoDatastore.EntityData entityData : data.getEntityList()) {
            transactionManager.begin();
            entityPersistor.deserialize(entityData, entityManager);
            transactionManager.commit();
        }
    }
}
