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

package org.terasology.entitysystem.entity;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.terasology.entitysystem.component.Component;
import org.terasology.entitysystem.component.management.ComponentManager;
import org.terasology.entitysystem.component.store.ArrayComponentStore;
import org.terasology.entitysystem.component.store.ComponentStore;
import org.terasology.entitysystem.component.store.ConcurrentComponentStore;
import org.terasology.entitysystem.component.store.SparseComponentStore;
import org.terasology.entitysystem.entity.manager.CoreEntityManager;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleFactory;
import org.terasology.module.ModuleMetadata;
import org.terasology.module.ModulePathScanner;
import org.terasology.module.ModuleRegistry;
import org.terasology.module.TableModuleRegistry;
import org.terasology.module.sandbox.JavaModuleClassLoader;
import org.terasology.module.sandbox.StandardPermissionProviderFactory;
import org.terasology.module.sandbox.WarnOnlyProviderFactory;
import org.terasology.naming.Name;
import org.terasology.naming.Version;

import java.util.List;

import modules.test.components.BasicComponent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FullSetupExample {

    public static final String ORIGINAL_NAME = "Fred";
    public static final String ORIGINAL_DESCRIPTION = "The Indominable";
    public static final String NEW_NAME = "Jerry";
    private EntityManager entityManager;

    @Before
    public void setup() {
        // Define/obtain modules
        ModuleRegistry moduleRegistry = new TableModuleRegistry();
        ModuleFactory factory = new ModuleFactory();
        factory.setScanningForClasses(true); // false for android

        ModuleMetadata testModule = new ModuleMetadata();
        testModule.setId(new Name("Test"));
        testModule.setVersion(new Version(1, 0, 0));
        moduleRegistry.add(factory.createPackageModule(testModule, "modules.test"));

        StandardPermissionProviderFactory permissionProviderFactory = new StandardPermissionProviderFactory();
        permissionProviderFactory.getBasePermissionSet().addAPIPackage("java.lang");
        permissionProviderFactory.getBasePermissionSet().addAPIPackage("org.terasology");

        ModuleEnvironment environment = new ModuleEnvironment(moduleRegistry, new WarnOnlyProviderFactory(permissionProviderFactory), JavaModuleClassLoader::create);

        // Create component stores. This gives an opportunity to set some component types to use ArrayComponentStore
        ComponentManager componentManager = new ComponentManager();

        List<ComponentStore<?>> stores = Lists.newArrayList();
        for (Class<? extends Component> componentType : environment.getSubtypesOf(Component.class)) {
            stores.add(new ConcurrentComponentStore(new ArrayComponentStore(componentManager.getType(componentType))));
        }

        // EntityManager
        entityManager = new CoreEntityManager(stores);
    }

    @Test
    public void createAnEntity() {
        BasicComponent component = new BasicComponent();
        component.setName(ORIGINAL_NAME);
        component.setDescription(ORIGINAL_DESCRIPTION);
        component.setCount(1);
        EntityRef newEntity = entityManager.createEntity(component);
        assertTrue(newEntity.hasComponent(BasicComponent.class));
        BasicComponent retrievedComponent = new BasicComponent();
        assertTrue(newEntity.getComponent(retrievedComponent));
        assertEquals(component, retrievedComponent);
        assertEquals(ORIGINAL_NAME, retrievedComponent.getName());
        assertEquals(ORIGINAL_DESCRIPTION, retrievedComponent.getDescription());
        assertEquals(1, retrievedComponent.getCount());
        assertTrue(newEntity.exists());
    }

    @Test
    public void addAComponentToAnEntity() {
        EntityRef entity = entityManager.createEntity();
        assertFalse(entity.hasComponent(BasicComponent.class));

        BasicComponent newComponent = new BasicComponent();
        newComponent.setName(ORIGINAL_NAME);
        newComponent.setDescription(ORIGINAL_DESCRIPTION);
        assertTrue(entity.setComponent(newComponent));
        BasicComponent retrievedComponent = new BasicComponent();
        assertTrue(entity.getComponent(retrievedComponent));
        assertEquals(newComponent, retrievedComponent);
    }

    @Test
    public void removeAComponentFromAnEntity() {
        EntityRef entity = entityManager.createEntity();
        BasicComponent component = new BasicComponent();
        component.setName(ORIGINAL_NAME);
        entity.setComponent(component);

        BasicComponent removedComponent = entity.removeComponent(BasicComponent.class);
        assertEquals(component, removedComponent);
        assertFalse(entity.getComponent(removedComponent));
        assertFalse(entity.hasComponent(BasicComponent.class));
        assertFalse(entity.getComponent(BasicComponent.class).isPresent());
    }

    @Test
    public void updateAComponent() {
        EntityRef entity = entityManager.createEntity();
        BasicComponent component = new BasicComponent();
        component.setName(ORIGINAL_NAME);
        entity.setComponent(component);

        BasicComponent retrievedComponent = new BasicComponent();
        entity.getComponent(retrievedComponent);
        retrievedComponent.setName(NEW_NAME);
        entity.setComponent(retrievedComponent);

        BasicComponent checkComponent = entity.getComponent(BasicComponent.class).orElseThrow(() -> new RuntimeException("Missing component"));
        assertNotNull(checkComponent);
        assertEquals(NEW_NAME, checkComponent.getName());
        assertEquals(retrievedComponent, checkComponent);
    }

    @Test
    public void storedComponentsUnaffectedByChangesToRetrieved() {
        EntityRef entity = entityManager.createEntity();
        BasicComponent component = new BasicComponent();
        component.setName(ORIGINAL_NAME);
        entity.setComponent(component);

        BasicComponent retrievedComponent = new BasicComponent();
        entity.getComponent(retrievedComponent);
        retrievedComponent.setName(NEW_NAME);

        assertNotEquals(component, retrievedComponent);
        BasicComponent checkComponent = entity.getComponent(BasicComponent.class).orElseThrow(() -> new RuntimeException("Missing component"));
        assertNotEquals(retrievedComponent, checkComponent);
    }


    @Test
    public void deleteAnEntity() {
        EntityRef entity = entityManager.createEntity();
        BasicComponent component = new BasicComponent();
        component.setName(ORIGINAL_NAME);
        entity.setComponent(component);

        entity.delete();

        assertFalse(entity.hasComponent(BasicComponent.class));
        assertFalse(entity.exists());
    }

}
