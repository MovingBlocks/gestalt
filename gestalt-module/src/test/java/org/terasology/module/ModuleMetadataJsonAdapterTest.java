/*
 * Copyright 2015 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.module;

import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Date;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.terasology.i18n.I18nMap;
import org.terasology.module.dependencyresolution.DependencyInfo;
import org.terasology.naming.Name;
import org.terasology.naming.Version;

import com.google.common.collect.ImmutableMap;

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

        Assert.assertEquals(meta, parsedMeta);
    }
}
