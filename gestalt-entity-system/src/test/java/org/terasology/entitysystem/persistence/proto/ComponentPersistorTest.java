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

package org.terasology.entitysystem.persistence.proto;

import org.junit.Test;
import org.terasology.entitysystem.component.CodeGenComponentManager;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.persistence.proto.persistors.ComponentPersistor;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;
import org.terasology.entitysystem.stubs.SampleComponent;
import org.terasology.module.ModuleEnvironment;
import org.terasology.valuetype.ImmutableCopy;
import org.terasology.valuetype.TypeHandler;
import org.terasology.valuetype.TypeLibrary;

import virtualModules.test.VirtualModuleEnvironmentFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ComponentPersistorTest {

    private ComponentManager componentManager;
    private ComponentPersistor persistor;
    private ProtoPersistence context = ProtoPersistence.create();

    public ComponentPersistorTest() throws Exception {
        ModuleEnvironment moduleEnvironment;
        VirtualModuleEnvironmentFactory virtualModuleEnvironmentFactory = new VirtualModuleEnvironmentFactory(getClass());
        moduleEnvironment = virtualModuleEnvironmentFactory.createEnvironment();

        TypeLibrary typeLibrary = new TypeLibrary();
        typeLibrary.addHandler(new TypeHandler<>(String.class, ImmutableCopy.create()));
        componentManager = new CodeGenComponentManager(typeLibrary);
        persistor = new ComponentPersistor(context, new ComponentManifest(moduleEnvironment, componentManager));
    }

    @Test
    public void persistComponent() {
        SampleComponent component = componentManager.create(SampleComponent.class);
        component.setName("Test Name");
        component.setDescription("Test Description");

        ProtoDatastore.ComponentData componentData = persistor.serialize(component).build();
        Component deserialized = persistor.deserialize(componentData).orElseThrow(RuntimeException::new);
        assertTrue(deserialized instanceof SampleComponent);
        SampleComponent deserializedSampleComp = (SampleComponent) deserialized;
        assertEquals(component.getName(), deserializedSampleComp.getName());
        assertEquals(component.getDescription(), deserializedSampleComp.getDescription());
    }

    @Test
    public void persistComponentDelta() {
        SampleComponent baseComponent = componentManager.create(SampleComponent.class);
        baseComponent.setName("Test Name");
        baseComponent.setDescription("Test Description");

        SampleComponent component = componentManager.create(SampleComponent.class);
        component.setName("New Name");
        component.setDescription("Test Description");

        ProtoDatastore.ComponentData componentData = persistor.serializeDelta(baseComponent, component).build();
        Component deserialized = persistor.deserializeOnto(componentData, componentManager.copy(baseComponent));
        assertTrue(deserialized instanceof SampleComponent);
        SampleComponent deserializedSampleComp = (SampleComponent) deserialized;
        assertEquals(component.getName(), deserializedSampleComp.getName());
        assertEquals(component.getDescription(), deserializedSampleComp.getDescription());
    }

    @Test
    public void persistComponentDeltaWithChangedBase() {
        SampleComponent baseComponent = componentManager.create(SampleComponent.class);
        baseComponent.setName("Test Name");
        baseComponent.setDescription("Test Description");

        SampleComponent component = componentManager.create(SampleComponent.class);
        component.setName("New Name");
        component.setDescription("Test Description");

        ProtoDatastore.ComponentData componentData = persistor.serializeDelta(baseComponent, component).build();
        baseComponent.setDescription("Meow");
        Component deserialized = persistor.deserializeOnto(componentData, componentManager.copy(baseComponent));
        assertTrue(deserialized instanceof SampleComponent);
        SampleComponent deserializedSampleComp = (SampleComponent) deserialized;
        assertEquals(component.getName(), deserializedSampleComp.getName());
        assertEquals("Meow", deserializedSampleComp.getDescription());
    }
}
