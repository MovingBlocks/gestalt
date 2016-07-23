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
import org.terasology.entitysystem.entity.Component;
import org.terasology.entitysystem.entity.inmemory.InMemoryEntityManager;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;
import org.terasology.entitysystem.stubs.SampleComponent;
import org.terasology.valuetype.ImmutableCopy;
import org.terasology.valuetype.TypeHandler;
import org.terasology.valuetype.TypeLibrary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ComponentPersistorTest {

    private ComponentManager componentManager;
    private ComponentPersistor persistor;

    public ComponentPersistorTest() {
        TypeLibrary typeLibrary = new TypeLibrary();
        typeLibrary.addHandler(new TypeHandler<>(String.class, ImmutableCopy.create()));
        componentManager = new CodeGenComponentManager(typeLibrary);
    }

    @Test
    public void persistComponent() {
        SampleComponent component = componentManager.create(SampleComponent.class);
        component.setName("Test Name");
        component.setDescription("Test Description");

        ProtoDatastore.Component componentData = persistor.serialize(component);
        Component deserialized = persistor.deserialize(componentData);
        assertTrue(deserialized instanceof SampleComponent);
        SampleComponent deserializedSampleComp = (SampleComponent) deserialized;
        assertEquals(component.getName(), deserializedSampleComp.getName());
        assertEquals(component.getDescription(), deserializedSampleComp.getDescription());

    }
}
