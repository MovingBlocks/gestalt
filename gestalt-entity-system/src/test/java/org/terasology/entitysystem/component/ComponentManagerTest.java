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

package org.terasology.entitysystem.component;

import org.junit.Test;

import modules.test.components.BasicComponent;
import modules.test.components.Empty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 *
 */
public class ComponentManagerTest {

    @Test
    public void constructComponent() {
        StandardComponentManager library = new StandardComponentManager();
        BasicComponent instance = library.create(BasicComponent.class);
        assertNotNull(instance);
        instance.setName("World");
        assertEquals("World", instance.getName());
        ComponentType<BasicComponent> typeInfo = library.getType(BasicComponent.class);
        PropertyAccessor<BasicComponent, String> property = (PropertyAccessor<BasicComponent, String>) typeInfo.getPropertyInfo().getProperty("name").get();
        property.set(instance, "Blue");
        assertEquals("Blue", property.get(instance));
    }

    @Test
    public void constructComponentGetType() {
        StandardComponentManager library = new StandardComponentManager();
        BasicComponent instance = library.create(BasicComponent.class);
        assertNotNull(instance);
        assertEquals(BasicComponent.class, instance.getClass());
    }

    @Test
    public void constructEmptyComponent() {
        StandardComponentManager library = new StandardComponentManager();
        Empty instance = library.create(Empty.class);
        assertNotNull(instance);
    }

    @Test
    public void emptyComponentsSingletons() {
        StandardComponentManager library = new StandardComponentManager();
        Empty instance = library.create(Empty.class);
        Empty instance2 = library.create(Empty.class);
        assertSame(instance, instance2);
    }
}
