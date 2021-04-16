// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.module;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.terasology.gestalt.i18n.I18nMap;
import org.terasology.gestalt.module.dependencyresolution.DependencyInfo;
import org.terasology.gestalt.naming.Name;
import org.terasology.gestalt.naming.Version;

import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Date;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the {@link ModuleMetadataJsonAdapter} class.
 */
public class ModuleMetadataJsonAdapterTest {

    @Test
    public void testReadWrite() {
        ModuleMetadata meta = new ModuleMetadata();
        meta.setId(new Name("ModuleNameId"));
        meta.setDisplayName(new I18nMap(ImmutableMap.of(
                Locale.ENGLISH, "englishDisplayName",
                Locale.GERMAN, "germanDisplayName")));
        meta.setDescription(new I18nMap(ImmutableMap.of(
                Locale.ENGLISH, "englishDescription",
                Locale.GERMAN, "germanDescription")));
        meta.setVersion(new Version(1, 2, 3, true));
        DependencyInfo depInfo = new DependencyInfo();
        depInfo.setId(new Name("myDependency"));
        depInfo.setMinVersion(new Version(1, 0, 0, false));
        depInfo.setMaxVersion(new Version(2, 3, 4, true));
        depInfo.setOptional(true);
        meta.getDependencies().add(depInfo);
        meta.getRequiredPermissions().add("myPermission");
        meta.setExtension("myExtension", new Date(1234567890123L));

        ModuleMetadataJsonAdapter adapter = new ModuleMetadataJsonAdapter();
        adapter.registerExtension("myExtension", Date.class);

        StringWriter writer = new StringWriter();
        adapter.write(meta, writer);
        String jsonString = writer.toString();

        ModuleMetadata parsedMeta = adapter.read(new StringReader(jsonString));

        assertEquals(meta, parsedMeta);
    }
}
