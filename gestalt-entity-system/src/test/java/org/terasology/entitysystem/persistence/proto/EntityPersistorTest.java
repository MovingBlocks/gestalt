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

import com.google.common.collect.Lists;
import org.junit.Test;
import org.terasology.assets.test.VirtualModuleEnvironment;
import org.terasology.entitysystem.component.CodeGenComponentManager;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.entity.EntityManager;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.entity.inmemory.InMemoryEntityManager;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;
import org.terasology.entitysystem.stubs.SampleComponent;
import org.terasology.module.ModuleEnvironment;
import org.terasology.valuetype.ImmutableCopy;
import org.terasology.valuetype.TypeHandler;
import org.terasology.valuetype.TypeLibrary;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class EntityPersistorTest {

    private static final String NAME = "Name";
    private static final String DESCRIPTION = "Description";

    private EntityPersistor persistor;
    private EntityManager originalEntityManager;
    private EntityManager finalEntityManager;

    public EntityPersistorTest() throws Exception {
        ModuleEnvironment moduleEnvironment;
        VirtualModuleEnvironment virtualModuleEnvironment = new VirtualModuleEnvironment(getClass());
        moduleEnvironment = virtualModuleEnvironment.createEnvironment();

        TypeLibrary typeLibrary = new TypeLibrary();
        typeLibrary.addHandler(new TypeHandler<>(String.class, ImmutableCopy.create()));
        ComponentManager componentManager = new CodeGenComponentManager(typeLibrary);
        ProtoPersistence context = new ProtoPersistence();
        persistor = new EntityPersistor(componentManager, context, new ComponentManifest(moduleEnvironment));

        originalEntityManager = new InMemoryEntityManager(componentManager);
        finalEntityManager = new InMemoryEntityManager(componentManager, 5);
    }

    @Test
    public void serializeTrivialEntity() {
        originalEntityManager.beginTransaction();
        EntityRef entity = originalEntityManager.createEntity();
        SampleComponent component = entity.addComponent(SampleComponent.class);
        component.setName(NAME);
        component.setDescription(DESCRIPTION);
        originalEntityManager.commit();

        originalEntityManager.beginTransaction();
        finalEntityManager.beginTransaction();
        EntityRef finalEntity = persistor.deserialize(persistor.serialize(entity).build(), finalEntityManager);
        originalEntityManager.rollback();
        finalEntityManager.commit();

        finalEntityManager.beginTransaction();
        assertEquals(entity.getId(), finalEntity.getId());

        SampleComponent comp = finalEntity.getComponent(SampleComponent.class).orElseThrow(RuntimeException::new);
        assertEquals(NAME, comp.getName());
        assertEquals(DESCRIPTION, comp.getDescription());
    }


    @Test
    public void persistEntitiesEmptyManager() {
        ProtoDatastore.Store entityStore = persistor.serializeEntities(originalEntityManager).build();
        persistor.deserializeEntities(entityStore, finalEntityManager);

        List<EntityRef> entities = Lists.newArrayList(finalEntityManager.allEntities());
        assertTrue(entities.isEmpty());
    }

    @Test
    public void persistEntities() {
        originalEntityManager.beginTransaction();
        EntityRef entity = originalEntityManager.createEntity();
        SampleComponent sampleComponent = entity.addComponent(SampleComponent.class);
        sampleComponent.setName(NAME);
        sampleComponent.setDescription(DESCRIPTION);
        originalEntityManager.commit();

        ProtoDatastore.Store entityStore = persistor.serializeEntities(originalEntityManager).build();
        persistor.deserializeEntities(entityStore, finalEntityManager);

        List<EntityRef> entities = Lists.newArrayList(finalEntityManager.allEntities());
        assertFalse(entities.isEmpty());
        finalEntityManager.beginTransaction();
        Optional<SampleComponent> component = entities.get(0).getComponent(SampleComponent.class);
        assertTrue(component.isPresent());
        assertEquals(NAME, component.get().getName());
        assertEquals(DESCRIPTION, component.get().getDescription());
        finalEntityManager.rollback();
    }
}
