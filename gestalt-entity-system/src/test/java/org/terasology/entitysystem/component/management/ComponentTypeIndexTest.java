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

package org.terasology.entitysystem.component.management;

import org.junit.Test;
import org.reflections.util.ClasspathHelper;
import org.terasology.assets.ResourceUrn;
import org.terasology.entitysystem.component.management.ComponentTypeIndex;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleFactory;
import org.terasology.module.sandbox.PermitAllPermissionProviderFactory;
import org.terasology.naming.Name;

import java.util.Collections;
import java.util.Optional;

import modules.test.components.Sample;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class ComponentTypeIndexTest {

    private ComponentTypeIndex index;

    public ComponentTypeIndexTest() throws Exception {
        ModuleFactory factory = new ModuleFactory(ClasspathHelper.staticClassLoader());
        Module module = factory.createPackageModule("modules.test");
        ModuleEnvironment moduleEnvironment = new ModuleEnvironment(Collections.singletonList(module), new PermitAllPermissionProviderFactory());
        index = new ComponentTypeIndex(moduleEnvironment);
    }

    @Test
    public void findComponentWithFullUrn() {
        assertEquals(Optional.of(Sample.class), index.find(new ResourceUrn("Test:Sample")));
    }

    @Test
    public void findComponentWithStringUrn() {
        assertEquals(Optional.of(Sample.class), index.find("Test:Sample"));
    }

    @Test
    public void findComponentWithNameAlone() {
        assertEquals(Optional.of(Sample.class), index.find(new Name("Sample")));
    }

    @Test
    public void findComponentWithStringName() {
        assertEquals(Optional.of(Sample.class), index.find("Sample"));
    }

}
