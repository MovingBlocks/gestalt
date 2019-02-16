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

package org.terasology.entitysystem.index;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.terasology.entitysystem.component.CodeGenComponentManager;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.entity.inmemory.InMemoryEntityManager;
import org.terasology.entitysystem.transaction.TransactionManager;
import org.terasology.valuetype.ImmutableCopy;
import org.terasology.valuetype.TypeHandler;
import org.terasology.valuetype.TypeLibrary;

import java.io.IOException;

import modules.test.SampleComponent;
import modules.test.SecondComponent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ComponentIndexTest {
    private Index componentIndex;

    private TransactionManager transactionManager = new TransactionManager();
    private EntityManager entityManager;


    public ComponentIndexTest() {
        TypeLibrary typeLibrary = new TypeLibrary();
        typeLibrary.addHandler(new TypeHandler<>(String.class, ImmutableCopy.create()));
        entityManager = new InMemoryEntityManager(new CodeGenComponentManager(typeLibrary), transactionManager);
        componentIndex = ComponentIndexes.createComponentIndex(transactionManager, entityManager, SampleComponent.class);
    }

    @Before
    public void setup() {
        transactionManager.begin();
    }

    @After
    public void teardown() throws IOException {
        while (transactionManager.isActive()) {
            transactionManager.rollback();
        }
    }

    @Test
    public void newEntityNoComponentNotInIndex() {
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(SecondComponent.class);
        transactionManager.commit();
        assertFalse(componentIndex.contains(entity));
    }

    @Test
    public void newEntityWithComponentInIndex() {
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(SampleComponent.class);
        transactionManager.commit();
        assertTrue(componentIndex.contains(entity));
    }

    @Test
    public void entityWithComponentRemovedIsRemovedFromIndex() {
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(SampleComponent.class);
        transactionManager.commit();

        transactionManager.begin();
        entity.addComponent(SecondComponent.class);
        entity.removeComponent(SampleComponent.class);
        transactionManager.commit();
        assertFalse(componentIndex.contains(entity));
    }

    @Test
    public void entityWithComponentAddedIsAddedToIndex() {
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(SecondComponent.class);
        transactionManager.commit();

        transactionManager.begin();
        entity.addComponent(SampleComponent.class);
        transactionManager.commit();
        assertTrue(componentIndex.contains(entity));
    }
}
