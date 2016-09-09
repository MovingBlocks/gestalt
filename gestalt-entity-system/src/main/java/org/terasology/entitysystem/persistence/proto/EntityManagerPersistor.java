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
import org.terasology.entitysystem.entity.EntityManager;
import org.terasology.entitysystem.entity.inmemory.InMemoryEntityManager;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;
import org.terasology.module.ModuleEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
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

    public void serialize(EntityManager entityManager, Path file) throws IOException {
        try (OutputStream stream = Files.newOutputStream(file)) {
            serialize(entityManager).writeTo(stream);
        }
    }

    public EntityManager deserialize(Path file) throws IOException {
        try (InputStream stream = Files.newInputStream(file)) {
            return deserialize(ProtoDatastore.Store.parseFrom(stream));
        }
    }

    public ProtoDatastore.Store serialize(EntityManager entityManager) {
        ComponentManifest componentManifest = new ComponentManifest(moduleEnvironment);

        EntityPersistor entityPersistor = new EntityPersistor(componentManager, context, componentManifest);
        ProtoDatastore.Store.Builder builder = entityPersistor.serializeEntities(entityManager);
        builder.setComponentManifest(componentManifestPersistor.serialize(componentManifest));
        builder.setNextEntityId(entityManager.getNextId());

        return builder.build();
    }

    public EntityManager deserialize(ProtoDatastore.Store entityManagerData) {
        EntityManager entityManager = new InMemoryEntityManager(componentManager, entityManagerData.getNextEntityId());

        ComponentManifest componentManifest = componentManifestPersistor.deserialize(entityManagerData.getComponentManifest());
        EntityPersistor entityPersistor = new EntityPersistor(componentManager, context, componentManifest);
        entityPersistor.deserializeEntities(entityManagerData, entityManager);
        return entityManager;
    }
}
