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

package org.terasology.module.filesystem;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.module.ClasspathModule;
import org.terasology.module.Module;
import org.terasology.module.ModuleMetadata;
import org.terasology.module.TableModuleRegistry;
import org.terasology.naming.Name;
import org.terasology.naming.Version;


import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * @author Immortius
 */
@RunWith(Parameterized.class)
public class ModuleFileSystemPathMatcherGlobTest {

    private static final Logger logger = LoggerFactory.getLogger(ModuleFileSystemPathMatcherGlobTest.class);

    @Parameterized.Parameter(0)
    public String glob;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameter(2)
    public boolean expectedResult;

    private ModuleFileSystem fileSystem;

    public ModuleFileSystemPathMatcherGlobTest() throws Exception {
        ModuleMetadata metadata = new ModuleMetadata();
        metadata.setId(new Name("test"));
        metadata.setVersion(new Version("1.0.0"));
        Module module = ClasspathModule.create(metadata, getClass());

        this.fileSystem = new ModuleFileSystemProvider(new TableModuleRegistry()).newFileSystem(module);
    }

    @Parameterized.Parameters
    public static Collection testCases() {
        return Arrays.asList(new Object[][]{
                {"hello.txt", "hello.txt", true},
                {"hello.txt", "wrong.txt", false},
                {"*", "hello.txt", true},
                {"*", "hello/world.txt", false},
                {"*.txt", "hello.txt", true},
                {"*.txt", "wrong.java", false},
                {"*.txt", "helloatxt", false},
                {"*.*", "hello.txt", true},
                {"*.*", "hello", false},
                {"**", "hello/world.txt", true},
                {"**/world.txt", "hello/world.txt", true},
                {"**/world.txt", "hello/moo.txt", false},
                {"**/world.txt", "hello/another/world.txt", true},
                {"**/world.txt", "hello/another/boom.txt", false},
                {"wor?d.txt", "world.txt", true},
                {"wor?d.txt", "word.txt", false},
                {"wor?d.txt", "worlld.txt", false},
                {"\\{.txt", "{.txt", true},
                {"\\?.txt", "m.txt", false},
                {"].txt", "].txt", true},
                {"*.{java,class}", "hello.java", true},
                {"*.{java,class}", "hello.class", true},
                {"*.{java,class}", "hello.txt", false},
                {"*.{java}", "hello.java", true},
                {"*.{}", "hello.", true},
                {"a?*", "a/moo", false},
                {"[abc]", "a", true},
                {"[abc]", "c", true},
                {"[abc]", "d", false},
                {"[a-c]", "b", true},
                {"[a-c]", "-", false},
                {"[-a-c]", "-", true},
                {"[!a-c]", "d", true},
                {"[!a-c]", "b", false},
                {"[!-a]", "-", false},
                {"[!a[b-c]]", "-", false}
        });
    }

    @Test
    public void test() {
        logger.info("{} -> {}", glob, ModuleFileSystemUtils.globToRegex(glob));
        assertEquals(expectedResult, fileSystem.getPathMatcher("glob:" + glob).matches(fileSystem.getPath(path)));
    }

    @Test
    public void windowsTest() {
        assertEquals(expectedResult, FileSystems.getDefault().getPathMatcher("glob:" + glob).matches(FileSystems.getDefault().getPath(path)));
    }
}
