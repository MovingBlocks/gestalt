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

package org.terasology.entitysystem.entity;

import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Test;
import org.terasology.entitysystem.Transaction;
import org.terasology.entitysystem.stubs.SampleComponent;
import org.terasology.entitysystem.stubs.SecondComponent;
import org.terasology.entitysystem.component.CodeGenComponentManager;
import org.terasology.entitysystem.entity.inmemory.InMemoryEntityManager;
import org.terasology.valuetype.ImmutableCopy;
import org.terasology.valuetype.TypeHandler;
import org.terasology.valuetype.TypeLibrary;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ConcurrentModificationException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TransactionTest {
    public static final String TEST_NAME = "Fred";
    public static final String TEST_NAME_2 = "Jill";
    private EntityManager entityManager;
    private URLClassLoader tempLoader;

    public TransactionTest() {
        TypeLibrary typeLibrary = new TypeLibrary();
        typeLibrary.addHandler(new TypeHandler<>(String.class, ImmutableCopy.create()));
        tempLoader = new URLClassLoader(new URL[0]);
        entityManager = new InMemoryEntityManager(new CodeGenComponentManager(typeLibrary, tempLoader));
    }

    @After
    public void teardown() throws IOException {
        tempLoader.close();
    }

    @Test
    public void createEntity() throws Exception {
        Transaction transaction = entityManager.beginTransaction();
        long entityId = transaction.createEntity();
        SampleComponent sampleComponent = transaction.addComponent(entityId, SampleComponent.class);
        sampleComponent.setName(TEST_NAME);
        transaction.commit();

        Optional<SampleComponent> finalComp = entityManager.getComponent(entityId, SampleComponent.class);
        assertTrue(finalComp.isPresent());
        assertEquals(TEST_NAME, finalComp.get().getName());
    }

    @Test
    public void addComponent() throws Exception {
        long entityId = entityManager.createEntity(entityManager.createComponent(SampleComponent.class));
        Transaction transaction = entityManager.beginTransaction();
        SecondComponent secondComponent = transaction.addComponent(entityId, SecondComponent.class);
        secondComponent.setName(TEST_NAME);
        transaction.commit();

        Optional<SecondComponent> finalComp = entityManager.getComponent(entityId, SecondComponent.class);
        assertTrue(finalComp.isPresent());
        assertEquals(TEST_NAME, finalComp.get().getName());
    }

    @Test
    public void getComponent() {
        SampleComponent original = entityManager.createComponent(SampleComponent.class);
        original.setName(TEST_NAME);
        long entityId = entityManager.createEntity(original);
        Transaction transaction = entityManager.beginTransaction();

        Optional<SampleComponent> finalComp = transaction.getComponent(entityId, SampleComponent.class);
        assertTrue(finalComp.isPresent());
        assertEquals(TEST_NAME, finalComp.get().getName());
    }

    @Test
    public void updateComponent() {
        long entityId = entityManager.createEntity(entityManager.createComponent(SampleComponent.class));
        Transaction transaction = entityManager.beginTransaction();
        Optional<SampleComponent> component = transaction.getComponent(entityId, SampleComponent.class);
        component.get().setName(TEST_NAME);
        transaction.commit();

        Optional<SampleComponent> finalComp = entityManager.getComponent(entityId, SampleComponent.class);
        assertTrue(finalComp.isPresent());
        assertEquals(TEST_NAME, finalComp.get().getName());
    }

    @Test
    public void removeComponent() throws Exception {
        long entityId = entityManager.createEntity(entityManager.createComponent(SampleComponent.class));
        Transaction transaction = entityManager.beginTransaction();
        transaction.removeComponent(entityId, SampleComponent.class);
        transaction.commit();

        Optional<SampleComponent> finalComp = entityManager.getComponent(entityId, SampleComponent.class);
        assertFalse(finalComp.isPresent());
    }

    @Test
    public void addThenRemoveComponent() throws Exception {
        long entityId = entityManager.createEntity(entityManager.createComponent(SampleComponent.class));
        Transaction transaction = entityManager.beginTransaction();
        transaction.addComponent(entityId, SecondComponent.class);
        transaction.removeComponent(entityId, SecondComponent.class);
        transaction.commit();

        Optional<SecondComponent> finalComp = entityManager.getComponent(entityId, SecondComponent.class);
        assertFalse(finalComp.isPresent());
    }

    @Test
    public void removeThenAddComponent() throws Exception {
        long entityId = entityManager.createEntity(entityManager.createComponent(SampleComponent.class));
        Transaction transaction = entityManager.beginTransaction();
        transaction.removeComponent(entityId, SampleComponent.class);
        SampleComponent comp = transaction.addComponent(entityId, SampleComponent.class);
        comp.setName(TEST_NAME);
        transaction.commit();

        Optional<SampleComponent> finalComp = entityManager.getComponent(entityId, SampleComponent.class);
        assertTrue(finalComp.isPresent());
        assertEquals(TEST_NAME, finalComp.get().getName());
    }

    @Test
    public void rollback() throws Exception {
        long entityId = entityManager.createEntity(entityManager.createComponent(SampleComponent.class));
        Transaction transaction = entityManager.beginTransaction();
        transaction.removeComponent(entityId, SampleComponent.class);
        SecondComponent comp = transaction.addComponent(entityId, SecondComponent.class);
        comp.setName(TEST_NAME);
        transaction.rollback();

        Optional<SampleComponent> finalSampleComp = entityManager.getComponent(entityId, SampleComponent.class);
        assertTrue(finalSampleComp.isPresent());
        Optional<SecondComponent> finalSecondComp = entityManager.getComponent(entityId, SecondComponent.class);
        assertFalse(finalSecondComp.isPresent());
    }

    @Test(expected = ConcurrentModificationException.class)
    public void concurrentModificationTriggersException() {
        SampleComponent originalComponent = entityManager.createComponent(SampleComponent.class);
        long entityId = entityManager.createEntity(originalComponent);

        Transaction transaction = entityManager.beginTransaction();
        SampleComponent transactionComponent = transaction.getComponent(entityId, SampleComponent.class).get();
        transactionComponent.setName(TEST_NAME);

        originalComponent.setName(TEST_NAME_2);
        entityManager.updateComponent(entityId, originalComponent);

        transaction.commit();
    }

    @Test
    public void failedCommitIsRolledBack() throws Exception {
        SampleComponent originalComponent = entityManager.createComponent(SampleComponent.class);
        long entityId = entityManager.createEntity(originalComponent);

        Transaction transaction = entityManager.beginTransaction();
        SampleComponent transactionComponent = transaction.getComponent(entityId, SampleComponent.class).get();
        transactionComponent.setName(TEST_NAME);
        transaction.addComponent(entityId, SecondComponent.class);

        originalComponent.setName(TEST_NAME_2);
        entityManager.updateComponent(entityId, originalComponent);

        try {
            transaction.commit();
        } catch (ConcurrentModificationException e) {
            assertEquals(TEST_NAME_2, entityManager.getComponent(entityId, SampleComponent.class).get().getName());
            assertFalse(entityManager.getComponent(entityId, SecondComponent.class).isPresent());
        }

    }

    @Test
    public void getCompositionOfEntity() throws Exception {
        SampleComponent originalComponent = entityManager.createComponent(SampleComponent.class);
        long entityId = entityManager.createEntity(originalComponent);
        entityManager.addComponent(entityId, entityManager.createComponent(SecondComponent.class));

        Transaction transaction = entityManager.beginTransaction();
        assertEquals(Sets.newHashSet(SampleComponent.class, SecondComponent.class), transaction.getEntityComposition(entityId));
    }

    @Test
    public void getCompositionOfEntityAccountsForModification() throws Exception {
        SampleComponent originalComponent = entityManager.createComponent(SampleComponent.class);
        long entityId = entityManager.createEntity(originalComponent);
        entityManager.addComponent(entityId, entityManager.createComponent(SecondComponent.class));

        Transaction transaction = entityManager.beginTransaction();
        transaction.removeComponent(entityId, SampleComponent.class);
        assertEquals(Sets.newHashSet(SecondComponent.class), transaction.getEntityComposition(entityId));
    }


}
