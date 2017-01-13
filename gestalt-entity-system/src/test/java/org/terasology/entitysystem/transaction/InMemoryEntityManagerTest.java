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

package org.terasology.entitysystem.transaction;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.mockito.internal.matchers.Null;
import org.terasology.entitysystem.component.CodeGenComponentManager;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.core.ProxyEntityRef;
import org.terasology.entitysystem.transaction.exception.EntitySystemException;
import org.terasology.entitysystem.entity.inmemory.InMemoryEntityManager;
import org.terasology.entitysystem.entity.inmemory.CoreEntityRef;
import org.terasology.entitysystem.entity.inmemory.NewEntityRef;
import org.terasology.entitysystem.core.NullEntityRef;
import org.terasology.entitysystem.stubs.SampleComponent;
import org.terasology.entitysystem.stubs.SecondComponent;
import org.terasology.entitysystem.transaction.exception.RollbackException;
import org.terasology.valuetype.ImmutableCopy;
import org.terasology.valuetype.TypeHandler;
import org.terasology.valuetype.TypeLibrary;

import java.util.ConcurrentModificationException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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
        TypeLibrary typeLibrary = new TypeLibrary();
        typeLibrary.addHandler(new TypeHandler<>(String.class, ImmutableCopy.create()));
        transactionManager = new TransactionManager();
        entityManager = new InMemoryEntityManager(new CodeGenComponentManager(typeLibrary), transactionManager);
    }

    @Test
    public void createEntityAddsCoreEntityRefOnCommit() throws Exception {
        transactionManager.begin();
        EntityRef entity = entityManager.createEntity();
        SampleComponent sampleComponent = entity.addComponent(SampleComponent.class);
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
        SampleComponent sampleComponent = entity.addComponent(SampleComponent.class);
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
        initialEntity.addComponent(SampleComponent.class);
        transactionManager.commit();

        transactionManager.begin();
        initialEntity.delete();
        EntityRef entity = entityManager.createEntity();
        SampleComponent sampleComponent = entity.addComponent(SampleComponent.class);
        sampleComponent.setName(TEST_NAME);
        transactionManager.begin();
        initialEntity.addComponent(SecondComponent.class);
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
        entity.addComponent(SampleComponent.class);
        transactionManager.commit();

        transactionManager.begin();
        entity.addComponent(SecondComponent.class);
        entity.removeComponent(SecondComponent.class);
        transactionManager.commit();

        transactionManager.begin();
        Optional<SecondComponent> finalComp = entity.getComponent(SecondComponent.class);
        assertFalse(finalComp.isPresent());
    }

    @Test
    public void removeThenAddComponent() throws Exception {
        transactionManager.begin();
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(SampleComponent.class);
        transactionManager.commit();

        transactionManager.begin();
        entity.removeComponent(SampleComponent.class);
        SampleComponent comp = entity.addComponent(SampleComponent.class);
        comp.setName(TEST_NAME);
        transactionManager.commit();

        transactionManager.begin();
        Optional<SampleComponent> finalComp = entity.getComponent(SampleComponent.class);
        assertTrue(finalComp.isPresent());
        assertEquals(TEST_NAME, finalComp.get().getName());
    }

    @Test
    public void rollback() throws Exception {
        transactionManager.begin();
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(SampleComponent.class);
        transactionManager.commit();

        transactionManager.begin();
        entity.removeComponent(SampleComponent.class);
        SecondComponent comp = entity.addComponent(SecondComponent.class);
        comp.setName(TEST_NAME);
        transactionManager.rollback();

        transactionManager.begin();
        Optional<SampleComponent> finalSampleComp = entity.getComponent(SampleComponent.class);
        assertTrue(finalSampleComp.isPresent());
        Optional<SecondComponent> finalSecondComp = entity.getComponent(SecondComponent.class);
        assertFalse(finalSecondComp.isPresent());
    }

    @Test(expected = RollbackException.class)
    public void concurrentModificationTriggersException() {
        transactionManager.begin();
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(SampleComponent.class);
        transactionManager.commit();

        transactionManager.begin();
        SampleComponent transactionComponent = entity.getComponent(SampleComponent.class).get();
        transactionComponent.setName(TEST_NAME);

        transactionManager.begin();
        entity.getComponent(SampleComponent.class).get().setName(TEST_NAME_2);
        transactionManager.commit();

        transactionManager.commit();
    }

    @Test(expected = RollbackException.class)
    public void failedCommitIsRolledBack() throws Exception {
        transactionManager.begin();
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(SampleComponent.class);
        transactionManager.commit();

        transactionManager.begin();
        SampleComponent transactionComponent = entity.getComponent(SampleComponent.class).get();
        transactionComponent.setName(TEST_NAME);
        entity.addComponent(SecondComponent.class);

        transactionManager.begin();
        entity.getComponent(SampleComponent.class).get().setName(TEST_NAME_2);
        transactionManager.commit();

        try {
            transactionManager.commit();
        } finally {
            transactionManager.begin();
            assertEquals(TEST_NAME_2, entity.getComponent(SampleComponent.class).get().getName());
            assertFalse(entity.getComponent(SecondComponent.class).isPresent());
        }

    }

    @Test
    public void getCompositionOfEntity() throws Exception {
        transactionManager.begin();
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(SampleComponent.class);
        entity.addComponent(SecondComponent.class);
        transactionManager.commit();

        transactionManager.begin();
        assertEquals(Sets.newHashSet(SampleComponent.class, SecondComponent.class), entity.getComponentTypes());
    }

    @Test
    public void getCompositionOfEntityAccountsForLocalModification() throws Exception {
        transactionManager.begin();
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(SecondComponent.class);
        entity.addComponent(SampleComponent.class);
        transactionManager.commit();

        transactionManager.begin();
        entity.removeComponent(SampleComponent.class);
        assertEquals(Sets.newHashSet(SecondComponent.class), entity.getComponentTypes());
    }

    @Test
    public void transactionInactiveIfNotStarted() {
        assertFalse(transactionManager.isActive());
    }


}
