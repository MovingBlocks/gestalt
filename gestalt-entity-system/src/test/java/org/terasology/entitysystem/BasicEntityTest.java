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

package org.terasology.entitysystem;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Test;
import org.terasology.entitysystem.component.CodeGenComponentManager;
import org.terasology.entitysystem.inmemory.InMemoryEntityManager;
import org.terasology.valuetype.ImmutableCopy;
import org.terasology.valuetype.TypeHandler;
import org.terasology.valuetype.TypeLibrary;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class BasicEntityTest {

    private EntityManager entityManager;
    private URLClassLoader tempLoader;


    public BasicEntityTest() {
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
    public void createEntity() {
        entityManager.createEntity(entityManager.createComponent(SampleComponent.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createEntityWithoutComponentsFails() {
        entityManager.createEntity(Lists.newArrayList());
    }

    @Test
    public void retrieveComponentFromEntity() {
        SampleComponent component = entityManager.createComponent(SampleComponent.class);
        component.setName("Name");
        component.setDescription("Description");
        long entity = entityManager.createEntity(component);

        Optional<SampleComponent> retrievedComponent = entityManager.getComponent(entity, SampleComponent.class);
        assertTrue(retrievedComponent.isPresent());
        assertEquals(component.getName(), retrievedComponent.get().getName());
        assertEquals(component.getDescription(), retrievedComponent.get().getDescription());
    }

    @Test
    public void addComponentToEntity() {
        long entity = entityManager.createEntity(entityManager.createComponent(SampleComponent.class));

        SecondComponent secondComponent = entityManager.createComponent(SecondComponent.class);
        assertTrue(entityManager.addComponent(entity, secondComponent));
        assertTrue(entityManager.getComponent(entity, SecondComponent.class).isPresent());
    }

    @Test
    public void addComponentToEntityThatAlreadyHasIt() {
        SampleComponent component = entityManager.createComponent(SampleComponent.class);
        long entity = entityManager.createEntity(component);
        assertFalse(entityManager.addComponent(entity, component));
    }

    @Test
    public void updateComponent() {
        SampleComponent component = entityManager.createComponent(SampleComponent.class);
        long entity = entityManager.createEntity(component);

        SampleComponent updatedComponent = entityManager.getComponent(entity, SampleComponent.class).get();
        updatedComponent.setName("Mooooo");
        assertTrue(entityManager.updateComponent(entity, updatedComponent));
        assertEquals(updatedComponent.getName(), entityManager.getComponent(entity, SampleComponent.class).get().getName());
    }

    @Test
    public void updateComponentFailsIfComponentNotPresent() {
        SampleComponent component = entityManager.createComponent(SampleComponent.class);
        long entityOne = entityManager.createEntity(component);
        long entityTwo = entityManager.createEntity(component);

        SecondComponent secondComponent = entityManager.createComponent(SecondComponent.class);

        entityManager.addComponent(entityOne, secondComponent);
        secondComponent.setName("Mooooo");
        assertFalse(entityManager.updateComponent(entityTwo, secondComponent));
        assertFalse(entityManager.getComponent(entityTwo, SecondComponent.class).isPresent());
    }

    @Test
    public void removeComponent() {
        SampleComponent component = entityManager.createComponent(SampleComponent.class);
        long entity = entityManager.createEntity(component);

        assertTrue(entityManager.removeComponent(entity, SampleComponent.class));
        assertFalse(entityManager.getComponent(entity, SampleComponent.class).isPresent());
    }

    @Test
    public void removeComponentFailsIfNotPresent() {
        SampleComponent component = entityManager.createComponent(SampleComponent.class);
        long entity = entityManager.createEntity(component);

        assertFalse(entityManager.removeComponent(entity, SecondComponent.class));
    }

    @Test
    public void changesInOriginalComponentDoesNotChangeStoredComponent() {
        SampleComponent component = entityManager.createComponent(SampleComponent.class);
        long entity = entityManager.createEntity(component);

        component.setName("New Name");
        assertNotEquals(component.getName(), entityManager.getComponent(entity, SampleComponent.class).get().getName());
    }

    @Test
    public void changesInRetrievedComponentDoesNotChangeStoredComponent() {
        SampleComponent component = entityManager.createComponent(SampleComponent.class);
        long entity = entityManager.createEntity(component);

        component = entityManager.getComponent(entity, SampleComponent.class).get();
        component.setName("New Name");
        assertNotEquals(component.getName(), entityManager.getComponent(entity, SampleComponent.class).get().getName());
    }

}
