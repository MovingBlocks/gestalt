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

package org.terasology.entitysystem.persistence.proto.typehandlers;

import org.junit.Test;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.component.StandardComponentManager;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.core.NullEntityRef;
import org.terasology.entitysystem.core.ProxyEntityRef;
import org.terasology.entitysystem.entity.inmemory.InMemoryEntityManager;
import org.terasology.entitysystem.persistence.proto.ProtoPersistence;
import org.terasology.entitysystem.transaction.TransactionManager;
import org.terasology.valuetype.TypeLibrary;

import modules.test.components.Sample;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class PrefabEntityRefHandlerTest {

    private TransactionManager transactionManager;
    private EntityManager entityManager;
    private ComponentManager componentManager;
    private ProtoPersistence context = ProtoPersistence.create();

    public PrefabEntityRefHandlerTest() {
        transactionManager = new TransactionManager();
        componentManager = new StandardComponentManager();
        entityManager = new InMemoryEntityManager(componentManager, transactionManager);
        context.addTypeHandler(new EntityRefHandler(entityManager), EntityRef.class);
    }

    @Test
    public void handleNullEntityRef() {
        EntityRef ref = NullEntityRef.get();
        assertEquals(ref, context.deserialize(context.serialize(ref, EntityRef.class).build(), EntityRef.class));
    }

    @Test
    public void handleCoreEntityRef() {
        transactionManager.begin();
        EntityRef ref = entityManager.createEntity();
        ref.addComponent(Sample.class);
        transactionManager.commit();

        ref = ((ProxyEntityRef) ref).getActualRef();

        assertEquals(ref, context.deserialize(context.serialize(ref, EntityRef.class).build(), EntityRef.class));
    }

    @Test
    public void handleNewEntityRef() {
        transactionManager.begin();
        EntityRef ref = entityManager.createEntity();
        ref.addComponent(Sample.class);
        transactionManager.commit();

        assertEquals(((ProxyEntityRef) ref).getActualRef(), context.deserialize(context.serialize(ref, EntityRef.class).build(), EntityRef.class));
    }


}
