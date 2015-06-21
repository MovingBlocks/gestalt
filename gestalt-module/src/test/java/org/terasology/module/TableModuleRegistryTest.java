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

package org.terasology.module;

import org.junit.Test;
import org.terasology.naming.Name;
import org.terasology.naming.Version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Immortius
 */
public class TableModuleRegistryTest {

    private static final Name MODULE_NAME = new Name("ModuleA");

    @Test
    public void nonSnapshotOverridesShapshot() {
        TableModuleRegistry registry = new TableModuleRegistry();
        Module moduleSnapshot = mock(Module.class);
        when(moduleSnapshot.getId()).thenReturn(MODULE_NAME);
        when(moduleSnapshot.getVersion()).thenReturn(new Version(1, 0, 0, true));
        registry.add(moduleSnapshot);
        Module module = mock(Module.class);
        when(module.getId()).thenReturn(MODULE_NAME);
        when(module.getVersion()).thenReturn(new Version(1, 0, 0, false));
        registry.add(module);
        assertFalse(registry.getLatestModuleVersion(MODULE_NAME).getVersion().isSnapshot());
    }

    @Test
    public void snapshotDoesNotOverrideNonSnapshot() {
        TableModuleRegistry registry = new TableModuleRegistry();
        Module module = mock(Module.class);
        when(module.getId()).thenReturn(MODULE_NAME);
        when(module.getVersion()).thenReturn(new Version(1, 0, 0, false));
        registry.add(module);
        Module moduleSnapshot = mock(Module.class);
        when(moduleSnapshot.getId()).thenReturn(MODULE_NAME);
        when(moduleSnapshot.getVersion()).thenReturn(new Version(1, 0, 0, true));
        registry.add(moduleSnapshot);
        assertFalse(registry.getLatestModuleVersion(MODULE_NAME).getVersion().isSnapshot());
    }

    @Test
    public void iteratorRemove() {
        TableModuleRegistry registry = new TableModuleRegistry();
        Module moduleV1 = mock(Module.class);
        when(moduleV1.getId()).thenReturn(MODULE_NAME);
        when(moduleV1.getVersion()).thenReturn(new Version(1, 0, 0));
        registry.add(moduleV1);
        Module moduleV2 = mock(Module.class);
        when(moduleV2.getId()).thenReturn(MODULE_NAME);
        when(moduleV2.getVersion()).thenReturn(new Version(2, 0, 0));
        registry.add(moduleV2);

        // remove entries based on their version
        registry.removeIf(mod -> mod.getVersion().compareTo(new Version(1, 5, 0)) > 0);

        assertEquals(new Version(1, 0, 0), registry.getLatestModuleVersion(MODULE_NAME).getVersion());

        registry.removeIf(mod -> mod.getId().equals(MODULE_NAME));

        assertTrue(registry.isEmpty());
    }
}
