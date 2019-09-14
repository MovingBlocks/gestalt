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

package org.terasology.entitysystem.transaction;

import com.google.common.collect.Sets;

import org.junit.Test;
import org.terasology.entitysystem.component.management.ComponentManager;
import org.terasology.entitysystem.entity.EntityManager;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.entity.NullEntityRef;
import org.terasology.entitysystem.transaction.exception.RollbackException;

import java.util.ConcurrentModificationException;
import java.util.Optional;

import modules.test.components.Sample;
import modules.test.components.Second;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class InMemoryEntityManagerTest {
    private static final String TEST_NAME = "Fred";
    private static final String TEST_NAME_2 = "Jill";
    private TransactionManager transactionManager;
    private EntityManager entityManager;

    public InMemoryEntityManagerTest() {
        transactionManager = new TransactionManager();
        entityManager = new InMemoryEntityManager(new ComponentManager(), transactionManager);
    }

    @Test
    public void createEntityAddsCoreEntityRefOnCommit() throws Exception {
        transactionManager.begin();
        EntityRef entity = entityManager.createEntity();
        Sample sampleComponent = entity.addComponent(Sample.class);
        sampleComponent.setName(TEST_NAME);
        transactionManager.commit();

        assertTrue(entity instanceof ProxyEntityRef);
        ProxyEntityRef newEntityRef = (ProxyEntityRef) entity;
        assertTrue(newEntityRef.getActualRef() instanceof CoreEntityRef);
    }

    @Test
    public void createEmptyEntityAddsNullEntityRefOnCommit() throws Exception {
        transactionManager.begin();
        EntityRef entity = entityManager.createEntity();
        transactionManager.commit();

        assertTrue(entity instanceof ProxyEntityRef);
        ProxyEntityRef newEntityRef = (ProxyEntityRef) entity;
        assertTrue(newEntityRef.getActualRef() instanceof NullEntityRef);
    }

    @Test
    public void createEntitySwitchesToNullEntityRefOnRollback() throws Exception {
        transactionManager.begin();
        EntityRef entity = entityManager.createEntity();
        Sample sampleComponent = entity.addComponent(Sample.class);
        sampleComponent.setName(TEST_NAME);
        transactionManager.rollback();

        assertTrue(entity instanceof ProxyEntityRef);
        ProxyEntityRef newEntityRef = (ProxyEntityRef) entity;
        assertEquals(NullEntityRef.get(), newEntityRef.getActualRef());
    }

    @Test(expected = ConcurrentModificationException.class)
    public void createEntitySwitchesToNullEntityRefOnFailedCommit() throws Exception {
        transactionManager.begin();
        EntityRef initialEntity = entityManager.createEntity();
        initialEntity.addComponent(Sample.class);
        transactionManager.commit();

        transactionManager.begin();
        initialEntity.delete();
        EntityRef entity = entityManager.createEntity();
        Sample sampleComponent = entity.addComponent(Sample.class);
        sampleComponent.setName(TEST_NAME);
        transactionManager.begin();
        initialEntity.addComponent(Second.class);
        transactionManager.commit();
        try {
            transactionManager.commit();
        } finally {
            assertTrue(entity instanceof ProxyEntityRef);
            ProxyEntityRef newEntityRef = (ProxyEntityRef) entity;
            assertEquals(NullEntityRef.get(), newEntityRef.getActualRef());
        }
    }

    @Test
    public void addThenRemoveComponent() throws Exception {
        transactionManager.begin();
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(Sample.class);
        transactionManager.commit();

        transactionManager.begin();
        entity.addComponent(Second.class);
        entity.removeComponent(Second.class);
        transactionManager.commit();

        transactionManager.begin();
        Optional<Second> finalComp = entity.getComponent(Second.class);
        assertFalse(finalComp.isPresent());
    }

    @Test
    public void removeThenAddComponent() throws Exception {
        transactionManager.begin();
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(Sample.class);
        transactionManager.commit();

        transactionManager.begin();
        entity.removeComponent(Sample.class);
        Sample comp = entity.addComponent(Sample.class);
        comp.setName(TEST_NAME);
        transactionManager.commit();

        transactionManager.begin();
        Optional<Sample> finalComp = entity.getComponent(Sample.class);
        assertTrue(finalComp.isPresent());
        assertEquals(TEST_NAME, finalComp.get().getName());
    }

    @Test
    public void rollback() throws Exception {
        transactionManager.begin();
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(Sample.class);
        transactionManager.commit();

        transactionManager.begin();
        entity.removeComponent(Sample.class);
        Second comp = entity.addComponent(Second.class);
        comp.setName(TEST_NAME);
        transactionManager.rollback();

        transactionManager.begin();
        Optional<Sample> finalSampleComp = entity.getComponent(Sample.class);
        assertTrue(finalSampleComp.isPresent());
        Optional<Second> finalSecondComp = entity.getComponent(Second.class);
        assertFalse(finalSecondComp.isPresent());
    }

    @Test(expected = RollbackException.class)
    public void concurrentModificationTriggersException() {
        transactionManager.begin();
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(Sample.class);
        transactionManager.commit();

        transactionManager.begin();
        Sample transactionComponent = entity.getComponent(Sample.class).get();
        transactionComponent.setName(TEST_NAME);

        transactionManager.begin();
        entity.getComponent(Sample.class).get().setName(TEST_NAME_2);
        transactionManager.commit();

        transactionManager.commit();
    }

    @Test(expected = RollbackException.class)
    public void failedCommitIsRolledBack() throws Exception {
        transactionManager.begin();
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(Sample.class);
        transactionManager.commit();

        transactionManager.begin();
        Sample transactionComponent = entity.getComponent(Sample.class).get();
        transactionComponent.setName(TEST_NAME);
        entity.addComponent(Second.class);

        transactionManager.begin();
        entity.getComponent(Sample.class).get().setName(TEST_NAME_2);
        transactionManager.commit();

        try {
            transactionManager.commit();
        } finally {
            transactionManager.begin();
            assertEquals(TEST_NAME_2, entity.getComponent(Sample.class).get().getName());
            assertFalse(entity.getComponent(Second.class).isPresent());
        }

    }

    @Test
    public void getCompositionOfEntity() throws Exception {
        transactionManager.begin();
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(Sample.class);
        entity.addComponent(Second.class);
        transactionManager.commit();

        transactionManager.begin();
        assertEquals(Sets.newHashSet(Sample.class, Second.class), entity.getComponentTypes());
    }

    @Test
    public void getCompositionOfEntityAccountsForLocalModification() throws Exception {
        transactionManager.begin();
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(Second.class);
        entity.addComponent(Sample.class);
        transactionManager.commit();

        transactionManager.begin();
        entity.removeComponent(Sample.class);
        assertEquals(Sets.newHashSet(Second.class), entity.getComponentTypes());
    }

    @Test
    public void transactionInactiveIfNotStarted() {
        assertFalse(transactionManager.isActive());
    }


}
