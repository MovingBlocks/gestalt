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

import org.junit.Test;
import org.terasology.entitysystem.component.CodeGenComponentManager;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.entity.inmemory.InMemoryEntityManager;
import org.terasology.valuetype.ImmutableCopy;
import org.terasology.valuetype.TypeHandler;
import org.terasology.valuetype.TypeLibrary;

import java.io.IOException;
import java.util.Optional;

import modules.test.components.Sample;
import modules.test.components.Second;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class BasicEntityTest {

    private static final String TEST_NAME = "Fred";
    private static final String TEST_NAME_2 = "Jill";

    private TransactionManager transactionManager = new TransactionManager();
    private EntityManager entityManager;


    public BasicEntityTest() {
        TypeLibrary typeLibrary = new TypeLibrary();
        typeLibrary.addHandler(new TypeHandler<>(String.class, ImmutableCopy.create()));
        entityManager = new InMemoryEntityManager(new CodeGenComponentManager(typeLibrary), transactionManager);
    }

    @org.junit.Before
    public void setup() {
        transactionManager.begin();
    }

    @org.junit.After
    public void teardown() throws IOException {
        while (transactionManager.isActive()) {
            transactionManager.rollback();
        }
    }

    @Test
    public void createEntity() {
        EntityRef entity = entityManager.createEntity();
        assertNotNull(entity);
        entity.addComponent(Sample.class);
        transactionManager.commit();
        transactionManager.begin();
        assertTrue(entity.isPresent());
    }

    @Test
    public void createEntityWithoutComponentsFails() {
        EntityRef entity = entityManager.createEntity();
        assertNotNull(entity);
        transactionManager.commit();
        transactionManager.begin();
        assertFalse(entity.isPresent());
    }

    @Test
    public void newEntityDoesExist() {
        EntityRef entity = entityManager.createEntity();
        assertTrue(entity.isPresent());
    }

    @Test
    public void retrieveComponentFromEntity() {
        EntityRef entity = entityManager.createEntity();
        Sample component = entity.addComponent(Sample.class);
        component.setName("Name");
        component.setDescription("Description");
        transactionManager.commit();

        transactionManager.begin();
        Optional<Sample> retrievedComponent = entity.getComponent(Sample.class);
        assertTrue(retrievedComponent.isPresent());
        assertEquals(component.getName(), retrievedComponent.get().getName());
        assertEquals(component.getDescription(), retrievedComponent.get().getDescription());
    }

    @Test
    public void updateComponent() {
        EntityRef entity = entityManager.createEntity();
        Sample sampleComponent = entity.addComponent(Sample.class);
        sampleComponent.setName(TEST_NAME);
        transactionManager.commit();

        transactionManager.begin();
        Optional<Sample> component = entity.getComponent(Sample.class);
        component.get().setName(TEST_NAME_2);
        transactionManager.commit();

        transactionManager.begin();
        Optional<Sample> finalComp = entity.getComponent(Sample.class);
        assertTrue(finalComp.isPresent());
        assertEquals(TEST_NAME_2, finalComp.get().getName());
    }

    @Test
    public void changesInOriginalComponentDoesNotChangeStoredComponent() {
        EntityRef entity = entityManager.createEntity();
        Sample component = entity.addComponent(Sample.class);
        transactionManager.commit();

        transactionManager.begin();
        component.setName("New Name");
        assertNotEquals(component.getName(), entity.getComponent(Sample.class).get().getName());
    }

    @Test
    public void changesInRetrievedComponentDoesNotChangeStoredComponent() {
        EntityRef entity = entityManager.createEntity();
        Sample component = entity.addComponent(Sample.class);
        transactionManager.commit();

        transactionManager.begin();
        Sample retrievedComponent = entity.getComponent(Sample.class).get();
        retrievedComponent.setName("New Name");
        assertNotEquals(component.getName(), retrievedComponent.getName());
        transactionManager.commit();
        assertNotEquals(component.getName(), retrievedComponent.getName());
    }

    @Test
    public void addComponent() throws Exception {
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(Sample.class);
        Second second = entity.addComponent(Second.class);
        second.setName(TEST_NAME);
        transactionManager.commit();

        transactionManager.begin();
        Optional<Second> finalComp = entity.getComponent(Second.class);
        assertTrue(finalComp.isPresent());
        assertEquals(TEST_NAME, finalComp.get().getName());
    }

    @Test
    public void removeComponent() {
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(Sample.class);
        entity.addComponent(Second.class);
        transactionManager.commit();

        transactionManager.begin();
        entity.removeComponent(Second.class);
        assertFalse(entity.getComponent(Second.class).isPresent());
        transactionManager.commit();
        transactionManager.begin();
        assertFalse(entity.getComponent(Second.class).isPresent());
    }

    @Test
    public void removeComponentDuringInitialTransaction() {
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(Sample.class);
        entity.addComponent(Second.class);
        entity.removeComponent(Second.class);
        assertFalse(entity.getComponent(Second.class).isPresent());
        transactionManager.commit();

        transactionManager.begin();
        assertFalse(entity.getComponent(Second.class).isPresent());
    }

    @Test
    public void deleteEntity() {
        EntityRef entity = entityManager.createEntity();
        transactionManager.commit();

        transactionManager.begin();
        entity.delete();
        assertFalse(entity.isPresent());
        transactionManager.commit();

        transactionManager.begin();
        assertFalse(entity.isPresent());
    }

}
