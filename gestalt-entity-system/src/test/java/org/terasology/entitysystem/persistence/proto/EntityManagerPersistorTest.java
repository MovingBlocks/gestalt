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

import org.junit.Test;
import org.terasology.assets.test.VirtualModuleEnvironment;
import org.terasology.entitysystem.component.CodeGenComponentManager;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.transaction.inmemory.InMemoryEntityManager;
import org.terasology.entitysystem.transaction.references.CoreEntityRef;
import org.terasology.entitysystem.persistence.proto.persistors.EntityManagerPersistor;
import org.terasology.entitysystem.stubs.SampleComponent;
import org.terasology.module.ModuleEnvironment;
import org.terasology.valuetype.ImmutableCopy;
import org.terasology.valuetype.TypeHandler;
import org.terasology.valuetype.TypeLibrary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class EntityManagerPersistorTest {

    private static final String NAME = "Name";
    private static final String DESCRIPTION = "Description";

    private ComponentManager componentManager;
    private EntityManagerPersistor persistor;

    public EntityManagerPersistorTest() throws Exception {
        ModuleEnvironment moduleEnvironment;
        VirtualModuleEnvironment virtualModuleEnvironment = new VirtualModuleEnvironment(getClass());
        moduleEnvironment = virtualModuleEnvironment.createEnvironment();

        TypeLibrary typeLibrary = new TypeLibrary();
        typeLibrary.addHandler(new TypeHandler<>(String.class, ImmutableCopy.create()));
        componentManager = new CodeGenComponentManager(typeLibrary);
        ProtoPersistence context = ProtoPersistence.create();
        persistor = new EntityManagerPersistor(moduleEnvironment, componentManager, context);
    }

    @Test
    public void persistEntityManager() {
        EntityManager entityManager = new InMemoryEntityManager(componentManager);
        entityManager.beginTransaction();
        EntityRef entity = entityManager.createEntity();
        SampleComponent sampleComponent = entity.addComponent(SampleComponent.class);
        sampleComponent.setName(NAME);
        sampleComponent.setDescription(DESCRIPTION);
        entityManager.commit();

        EntityManager newEntityManager = persistor.deserialize(persistor.serialize(entityManager));
        assertNotNull(newEntityManager);
        assertEquals(entityManager.getNextId(), newEntityManager.getNextId());
        EntityRef newEntity = new CoreEntityRef(newEntityManager, entity.getId());
        newEntityManager.beginTransaction();
        assertTrue(newEntity.isPresent());
        assertTrue(newEntity.getComponent(SampleComponent.class).isPresent());
        assertEquals(NAME, newEntity.getComponent(SampleComponent.class).get().getName());
        assertEquals(DESCRIPTION, newEntity.getComponent(SampleComponent.class).get().getDescription());
    }
}
