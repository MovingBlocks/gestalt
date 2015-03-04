/*
 * Copyright 2014 MovingBlocks
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Immortius
 */
public class FindFilesTest {

    @Test
    public void findFilesInClasspathModule() throws Exception {
        ModuleMetadata metadata = new ModuleMetadata();
        metadata.setId(new Name("test"));
        metadata.setVersion(new Version("1.0.0"));
        Module module = ClasspathModule.create(metadata, true, getClass());
        List<Path> paths = module.findFiles("glob:**/*.resource");
        assertEquals(1, paths.size());
        assertTrue(paths.get(0).endsWith(module.getFileSystem().getPath("subfolder", "test.resource")));
    }

    @Test
    public void findFilesInArchiveModule() throws Exception {
        ModuleMetadata metadata = new ModuleMetadata();
        metadata.setId(new Name("test"));
        metadata.setVersion(new Version("1.0.0"));
        Module module = new ArchiveModule(Paths.get("test-modules", "moduleA.jar"), metadata);
        List<Path> paths = module.findFiles("glob:**/*.info");
        assertEquals(1, paths.size());
        assertTrue(paths.get(0).endsWith("module.info"));
    }
}
