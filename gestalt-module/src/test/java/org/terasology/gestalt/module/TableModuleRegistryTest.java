// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.module;

import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.terasology.gestalt.module.resources.EmptyFileSource;
import org.terasology.gestalt.naming.Name;
import org.terasology.gestalt.naming.Version;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Immortius
 */
public class TableModuleRegistryTest {

    private static final Name MODULE_NAME = new Name("ModuleA");

    @Test
    public void nonSnapshotOverridesShapshot() {
        TableModuleRegistry registry = new TableModuleRegistry();
        Module moduleSnapshot = createModule(MODULE_NAME, new Version(1, 0, 0, true));
        registry.add(moduleSnapshot);
        Module module = createModule(MODULE_NAME, new Version(1, 0, 0, false));
        registry.add(module);
        assertFalse(registry.getLatestModuleVersion(MODULE_NAME).getVersion().isSnapshot());
    }

    @Test
    public void snapshotDoesNotOverrideNonSnapshot() {
        TableModuleRegistry registry = new TableModuleRegistry();
        Module module = createModule(MODULE_NAME, new Version(1, 0, 0, false));
        registry.add(module);
        Module moduleSnapshot = createModule(MODULE_NAME, new Version(1, 0, 0, true));
        registry.add(moduleSnapshot);
        assertFalse(registry.getLatestModuleVersion(MODULE_NAME).getVersion().isSnapshot());
    }

    @Test
    public void iteratorRemove() {
        TableModuleRegistry registry = new TableModuleRegistry();
        Module moduleV1 = createModule(MODULE_NAME, new Version(1, 0, 0));
        Module moduleV2 = createModule(MODULE_NAME, new Version(2, 0, 0));
        registry.add(moduleV1);
        registry.add(moduleV2);

        // remove entries based on their version
        registry.removeIf(mod -> mod.getVersion().compareTo(new Version(1, 5, 0)) > 0);

        assertEquals(new Version(1, 0, 0), registry.getLatestModuleVersion(MODULE_NAME).getVersion());

        registry.removeIf(mod -> mod.getId().equals(MODULE_NAME));

        assertTrue(registry.isEmpty());
    }

    private Module createModule(Name name, Version version) {
        ModuleMetadata metadata = new ModuleMetadata();
        metadata.setId(name);
        metadata.setVersion(version);
        return new Module(metadata, new EmptyFileSource(), Collections.emptyList(), new Reflections(new ConfigurationBuilder()), x -> false);
    }
}
