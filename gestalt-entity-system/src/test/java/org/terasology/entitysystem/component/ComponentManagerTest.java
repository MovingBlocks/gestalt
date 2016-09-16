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

package org.terasology.entitysystem.component;

import org.junit.Test;
import org.terasology.entitysystem.stubs.ComponentInterface;
import org.terasology.valuetype.TypeLibrary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class ComponentManagerTest {

    @Test
    public void constructComponent() {
        TypeLibrary typeLibrary = new TypeLibrary();
        CodeGenComponentManager library = new CodeGenComponentManager(typeLibrary);
        ComponentInterface instance = library.create(ComponentInterface.class);
        assertNotNull(instance);
        instance.setName("World");
        assertEquals("World", instance.getName());
        ComponentType<ComponentInterface> typeInfo = library.getType(ComponentInterface.class);
        PropertyAccessor<ComponentInterface, String> property = (PropertyAccessor<ComponentInterface, String>) typeInfo.getPropertyInfo().getProperty("name").get();
        property.set(instance, "Blue");
        assertEquals("Blue", property.get(instance));
    }

    @Test
    public void constructComponentEquals() {
        TypeLibrary typeLibrary = new TypeLibrary();
        CodeGenComponentManager library = new CodeGenComponentManager(typeLibrary);
        ComponentInterface instance = library.create(ComponentInterface.class);
        assertNotNull(instance);
        instance.setName("World");

        ComponentInterface instance2 = library.create(ComponentInterface.class);
        instance2.setName("World");

        assertEquals(instance, instance2);
    }

    @Test
    public void constructComponentHashCode() {
        TypeLibrary typeLibrary = new TypeLibrary();
        CodeGenComponentManager library = new CodeGenComponentManager(typeLibrary);
        ComponentInterface instance = library.create(ComponentInterface.class);
        assertNotNull(instance);
        instance.setName("World");

        ComponentInterface instance2 = library.create(ComponentInterface.class);
        instance2.setName("World");

        assertEquals(instance.hashCode(), instance2.hashCode());
    }
}
