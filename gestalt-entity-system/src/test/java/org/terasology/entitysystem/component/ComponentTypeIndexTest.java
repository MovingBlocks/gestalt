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
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.test.VirtualModuleEnvironmentFactory;
import org.terasology.entitysystem.component.module.ComponentTypeIndex;
import org.terasology.entitysystem.stubs.SampleComponent;
import org.terasology.module.ModuleEnvironment;
import org.terasology.naming.Name;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class ComponentTypeIndexTest {

    private ComponentTypeIndex index;

    public ComponentTypeIndexTest() throws Exception {
        VirtualModuleEnvironmentFactory virtualModuleEnvironmentFactory = new VirtualModuleEnvironmentFactory(getClass());
        ModuleEnvironment moduleEnvironment = virtualModuleEnvironmentFactory.createEnvironment();
        index = new ComponentTypeIndex(moduleEnvironment);
    }

    @Test
    public void findComponentWithFullUrn() {
        assertEquals(Optional.of(SampleComponent.class), index.find(new ResourceUrn("Test:SampleComponent")));
    }

    @Test
    public void findComponentWithStringUrn() {
        assertEquals(Optional.of(SampleComponent.class), index.find("Test:SampleComponent"));
    }

    @Test
    public void findComponentWithNameAlone() {
        assertEquals(Optional.of(SampleComponent.class), index.find(new Name("SampleComponent")));
    }

    @Test
    public void findComponentWithStringName() {
        assertEquals(Optional.of(SampleComponent.class), index.find("SampleComponent"));
    }

    @Test
    public void findComponentWithNameWithoutSuffix() {
        assertEquals(Optional.of(SampleComponent.class), index.find("Sample"));
    }
}
