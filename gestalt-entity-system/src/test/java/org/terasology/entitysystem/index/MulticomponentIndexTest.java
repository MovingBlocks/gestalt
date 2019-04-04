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
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.entity.inmemory.InMemoryEntityManager;
import org.terasology.entitysystem.transaction.TransactionManager;

import java.io.IOException;

import modules.test.components.Reference;
import modules.test.components.Sample;
import modules.test.components.Second;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class MulticomponentIndexTest {
    private Index componentIndex;

    private TransactionManager transactionManager = new TransactionManager();
    private EntityManager entityManager;


    public MulticomponentIndexTest() {
        entityManager = new InMemoryEntityManager(new ComponentManager(), transactionManager);
        componentIndex = ComponentIndexes.createComponentIndex(transactionManager, entityManager, Sample.class, Second.class);
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
        entity.addComponent(Reference.class);
        transactionManager.commit();
        assertFalse(componentIndex.contains(entity));
    }

    @Test
    public void newEntityOneComponentNotInIndex() {
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(Sample.class);
        transactionManager.commit();
        assertFalse(componentIndex.contains(entity));
    }

    @Test
    public void newEntityWithBothComponentInIndex() {
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(Sample.class);
        entity.addComponent(Second.class);
        transactionManager.commit();
        assertTrue(componentIndex.contains(entity));
    }

    @Test
    public void entityWithComponentRemovedIsRemovedFromIndex() {
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(Sample.class);
        entity.addComponent(Second.class);
        transactionManager.commit();

        transactionManager.begin();
        entity.removeComponent(Sample.class);
        transactionManager.commit();
        assertFalse(componentIndex.contains(entity));
    }

    @Test
    public void entityWithComponentAddedIsAddedToIndex() {
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(Second.class);
        transactionManager.commit();

        transactionManager.begin();
        entity.addComponent(Sample.class);
        transactionManager.commit();
        assertTrue(componentIndex.contains(entity));
    }
}
