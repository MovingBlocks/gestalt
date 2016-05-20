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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.terasology.entitysystem.component.CodeGenComponentManager;
import org.terasology.entitysystem.entity.inmemory.InMemoryEntityManager;
import org.terasology.entitysystem.entity.references.NewEntityRef;
import org.terasology.entitysystem.stubs.SampleComponent;
import org.terasology.entitysystem.stubs.SecondComponent;
import org.terasology.valuetype.ImmutableCopy;
import org.terasology.valuetype.TypeHandler;
import org.terasology.valuetype.TypeLibrary;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class BasicEntityTest {

    public static final String TEST_NAME = "Fred";
    public static final String TEST_NAME_2 = "Jill";

    private EntityManager entityManager;
    private URLClassLoader tempLoader;


    public BasicEntityTest() {
        TypeLibrary typeLibrary = new TypeLibrary();
        typeLibrary.addHandler(new TypeHandler<>(String.class, ImmutableCopy.create()));
        tempLoader = new URLClassLoader(new URL[0]);
        entityManager = new InMemoryEntityManager(new CodeGenComponentManager(typeLibrary, tempLoader));
    }

    @org.junit.Before
    public void setup() {
        entityManager.beginTransaction();
    }

    @org.junit.After
    public void teardown() throws IOException {
        while (entityManager.isTransactionActive()) {
            entityManager.rollback();
        }
        tempLoader.close();
    }

    @Test
    public void createEntity() {
        EntityRef entity = entityManager.createEntity();
        assertNotNull(entity);
        entity.addComponent(SampleComponent.class);
        entityManager.commit();
        entityManager.beginTransaction();
        assertTrue(entity.isPresent());
    }

    @Test
    public void createEntityWithoutComponentsFails() {
        EntityRef entity = entityManager.createEntity();
        assertNotNull(entity);
        entityManager.commit();
        entityManager.beginTransaction();
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
        SampleComponent component = entity.addComponent(SampleComponent.class);
        component.setName("Name");
        component.setDescription("Description");
        entityManager.commit();

        entityManager.beginTransaction();
        Optional<SampleComponent> retrievedComponent = entity.getComponent(SampleComponent.class);
        assertTrue(retrievedComponent.isPresent());
        assertEquals(component.getName(), retrievedComponent.get().getName());
        assertEquals(component.getDescription(), retrievedComponent.get().getDescription());
    }

    @Test
    public void updateComponent() {
        EntityRef entity = entityManager.createEntity();
        SampleComponent sampleComponent = entity.addComponent(SampleComponent.class);
        sampleComponent.setName(TEST_NAME);
        entityManager.commit();

        entityManager.beginTransaction();
        Optional<SampleComponent> component = entity.getComponent(SampleComponent.class);
        component.get().setName(TEST_NAME_2);
        entityManager.commit();

        entityManager.beginTransaction();
        Optional<SampleComponent> finalComp = entity.getComponent(SampleComponent.class);
        assertTrue(finalComp.isPresent());
        assertEquals(TEST_NAME_2, finalComp.get().getName());
    }

    @Test
    public void changesInOriginalComponentDoesNotChangeStoredComponent() {
        EntityRef entity = entityManager.createEntity();
        SampleComponent component = entity.addComponent(SampleComponent.class);
        entityManager.commit();

        entityManager.beginTransaction();
        component.setName("New Name");
        assertNotEquals(component.getName(), entity.getComponent(SampleComponent.class).get().getName());
    }

    @Test
    public void changesInRetrievedComponentDoesNotChangeStoredComponent() {
        EntityRef entity = entityManager.createEntity();
        SampleComponent component = entity.addComponent(SampleComponent.class);
        entityManager.commit();

        entityManager.beginTransaction();
        SampleComponent retrievedComponent = entity.getComponent(SampleComponent.class).get();
        retrievedComponent.setName("New Name");
        assertNotEquals(component.getName(), retrievedComponent.getName());
        entityManager.commit();
        assertNotEquals(component.getName(), retrievedComponent.getName());
    }

    @Test
    public void addComponent() throws Exception {
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(SampleComponent.class);
        SecondComponent secondComponent = entity.addComponent(SecondComponent.class);
        secondComponent.setName(TEST_NAME);
        entityManager.commit();

        entityManager.beginTransaction();
        Optional<SecondComponent> finalComp = entity.getComponent(SecondComponent.class);
        assertTrue(finalComp.isPresent());
        assertEquals(TEST_NAME, finalComp.get().getName());
    }

    @Test
    public void removeComponent() {
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(SampleComponent.class);
        entity.addComponent(SecondComponent.class);
        entityManager.commit();

        entityManager.beginTransaction();
        entity.removeComponent(SecondComponent.class);
        assertFalse(entity.getComponent(SecondComponent.class).isPresent());
        entityManager.commit();
        entityManager.beginTransaction();
        assertFalse(entity.getComponent(SecondComponent.class).isPresent());
    }

    @Test
    public void removeComponentDuringInitialTransaction() {
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(SampleComponent.class);
        entity.addComponent(SecondComponent.class);
        entity.removeComponent(SecondComponent.class);
        assertFalse(entity.getComponent(SecondComponent.class).isPresent());
        entityManager.commit();

        entityManager.beginTransaction();
        assertFalse(entity.getComponent(SecondComponent.class).isPresent());
    }

    @Test
    public void deleteEntity() {
        EntityRef entity = entityManager.createEntity();
        entityManager.commit();

        entityManager.beginTransaction();
        entity.delete();
        assertFalse(entity.isPresent());
        entityManager.commit();

        entityManager.beginTransaction();
        assertFalse(entity.isPresent());
    }

    @Test
    public void findEntitiesWithSingleComponent() {
        List<EntityRef> sampleEntities = Lists.newArrayList();
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(SampleComponent.class);
        sampleEntities.add(entity);

        entity = entityManager.createEntity();
        entity.addComponent(SampleComponent.class);
        entity.addComponent(SecondComponent.class);
        sampleEntities.add(entity);

        entity = entityManager.createEntity();
        entity.addComponent(SecondComponent.class);
        entityManager.commit();

        Iterator<EntityRef> iterator = entityManager.findEntitiesWithComponents(SampleComponent.class);
        Set<EntityRef> actualEntities = Sets.newHashSet();
        while (iterator.hasNext()) {
            actualEntities.add(iterator.next());
        }

        assertEquals(listActualEntities(sampleEntities), actualEntities);
    }

    @Test
    public void findEntitiesWithMultipleComponent() {
        List<EntityRef> sampleAndSecondEntities = Lists.newArrayList();
        EntityRef entity = entityManager.createEntity();
        entity.addComponent(SampleComponent.class);
        entity = entityManager.createEntity();
        entity.addComponent(SampleComponent.class);
        entity.addComponent(SecondComponent.class);
        sampleAndSecondEntities.add(entity);
        entity = entityManager.createEntity();
        entity.addComponent(SecondComponent.class);
        entityManager.commit();

        Iterator<EntityRef> iterator = entityManager.findEntitiesWithComponents(SampleComponent.class, SecondComponent.class);
        Set<EntityRef> actualEntities = Sets.newHashSet();
        while (iterator.hasNext()) {
            actualEntities.add(iterator.next());
        }

        assertEquals(listActualEntities(sampleAndSecondEntities), actualEntities);
    }

    private Set<EntityRef> listActualEntities(List<EntityRef> newEntities) {
        Set<EntityRef> result = Sets.newLinkedHashSet();
        for (EntityRef entity : newEntities) {
            if (entity instanceof NewEntityRef) {
                result.add(((NewEntityRef) entity).getInnerEntityRef().get());
            } else {
                result.add(entity);
            }
        }
        return result;
    }

}
