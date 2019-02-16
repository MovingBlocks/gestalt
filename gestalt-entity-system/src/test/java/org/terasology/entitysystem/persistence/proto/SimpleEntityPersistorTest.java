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

package org.terasology.entitysystem.persistence.proto;

import org.junit.Test;
import org.terasology.entitysystem.component.CodeGenComponentManager;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.entity.inmemory.InMemoryEntityManager;
import org.terasology.entitysystem.persistence.proto.persistors.EntityPersistor;
import org.terasology.entitysystem.persistence.proto.persistors.SimpleEntityPersistor;
import org.terasology.entitysystem.transaction.TransactionManager;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleFactory;
import org.terasology.module.sandbox.PermitAllPermissionProviderFactory;
import org.terasology.valuetype.ImmutableCopy;
import org.terasology.valuetype.TypeHandler;
import org.terasology.valuetype.TypeLibrary;

import java.util.Collections;

import modules.test.components.Sample;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class SimpleEntityPersistorTest {

    private static final String NAME = "Name";
    private static final String DESCRIPTION = "Description";

    private TransactionManager originalTransaction = new TransactionManager();
    private TransactionManager finalTransaction = new TransactionManager();
    private EntityPersistor persistor;
    private InMemoryEntityManager originalEntityManager;
    private InMemoryEntityManager finalEntityManager;

    public SimpleEntityPersistorTest() throws Exception {
        ModuleFactory factory = new ModuleFactory();
        Module module = factory.createPackageModule("modules.test");
        ModuleEnvironment moduleEnvironment = new ModuleEnvironment(Collections.singletonList(module), new PermitAllPermissionProviderFactory());

        TypeLibrary typeLibrary = new TypeLibrary();
        typeLibrary.addHandler(new TypeHandler<>(String.class, ImmutableCopy.create()));
        ComponentManager componentManager = new CodeGenComponentManager(typeLibrary);
        ProtoPersistence context = ProtoPersistence.create();
        persistor = new SimpleEntityPersistor(context, new ComponentManifest(moduleEnvironment, componentManager));

        originalEntityManager = new InMemoryEntityManager(componentManager, originalTransaction);
        finalEntityManager = new InMemoryEntityManager(componentManager, finalTransaction, 5);
    }

    @Test
    public void serializeTrivialEntity() {
        originalTransaction.begin();
        EntityRef entity = originalEntityManager.createEntity();
        Sample component = entity.addComponent(Sample.class);
        component.setName(NAME);
        component.setDescription(DESCRIPTION);
        originalTransaction.commit();

        originalTransaction.begin();
        finalTransaction.begin();
        EntityRef finalEntity = persistor.deserialize(persistor.serialize(entity).build(), finalEntityManager);
        finalTransaction.commit();
        originalTransaction.rollback();

        finalTransaction.begin();
        assertEquals(entity.getId(), finalEntity.getId());

        Sample comp = finalEntity.getComponent(Sample.class).orElseThrow(RuntimeException::new);
        assertEquals(NAME, comp.getName());
        assertEquals(DESCRIPTION, comp.getDescription());
    }

}
