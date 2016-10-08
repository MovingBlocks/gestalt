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
import org.terasology.entitysystem.persistence.proto.persistors.EntityPersistor;
import org.terasology.entitysystem.persistence.proto.persistors.SimpleEntityPersistor;
import org.terasology.entitysystem.stubs.SampleComponent;
import org.terasology.module.ModuleEnvironment;
import org.terasology.valuetype.ImmutableCopy;
import org.terasology.valuetype.TypeHandler;
import org.terasology.valuetype.TypeLibrary;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class SimpleEntityPersistorTest {

    private static final String NAME = "Name";
    private static final String DESCRIPTION = "Description";

    private EntityPersistor persistor;
    private EntityManager originalEntityManager;
    private EntityManager finalEntityManager;

    public SimpleEntityPersistorTest() throws Exception {
        ModuleEnvironment moduleEnvironment;
        VirtualModuleEnvironment virtualModuleEnvironment = new VirtualModuleEnvironment(getClass());
        moduleEnvironment = virtualModuleEnvironment.createEnvironment();

        TypeLibrary typeLibrary = new TypeLibrary();
        typeLibrary.addHandler(new TypeHandler<>(String.class, ImmutableCopy.create()));
        ComponentManager componentManager = new CodeGenComponentManager(typeLibrary);
        ProtoPersistence context = ProtoPersistence.create();
        persistor = new SimpleEntityPersistor(context, new ComponentManifest(moduleEnvironment, componentManager));

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

}
