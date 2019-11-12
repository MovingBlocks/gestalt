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

package org.terasology.gestalt.module.resources;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Base class for testing ModuleFileSources. The expectation is that module file source will have
 * this structure:
 *
 * <pre>
 * hidden.txt
 * content/readme.txt
 * content/fake.class
 * content/subfolder/test.resource (containing text "this space intentionally left blank")
 * content/subfolder/subpath/another.resource
 * content/folder/some.resource
 * </pre>
 */
public abstract class BaseFileSourceTest {

    public abstract ModuleFileSource getFileSource();

    @Test
    public void listFiles() {
        Set<String> expected = Sets.newHashSet("readme.txt", "test.resource", "another.resource", "some.resource");
        Set<String> actual = Sets.newLinkedHashSet();
        for (FileReference fileReference : getFileSource()) {
            actual.add(fileReference.toString());
        }
        assertEquals(expected, actual);
    }

    @Test
    public void listFilesInPath() {
        List<String> expected = Lists.newArrayList("test.resource");
        List<String> actual = Lists.newArrayList();
        for (FileReference fileReference : getFileSource().getFilesInPath(false, "subfolder")) {
            actual.add(fileReference.toString());
        }
        assertEquals(expected, actual);
    }

    @Test
    public void listFilesInPathRecursive() {
        Set<String> expected = Sets.newHashSet("test.resource", "another.resource");
        Set<String> actual = Sets.newLinkedHashSet();
        for (FileReference fileReference : getFileSource().getFilesInPath(true, "subfolder")) {
            actual.add(fileReference.toString());
        }
        assertEquals(expected, actual);
    }

    @Test
    public void listFilesInNonexistentPath() {
        Set<String> expected = Collections.emptySet();
        Set<String> actual = Sets.newLinkedHashSet();
        for (FileReference fileReference : getFileSource().getFilesInPath(true, "notafolder")) {
            actual.add(fileReference.toString());
        }
        assertEquals(expected, actual);
    }

    @Test
    public void listFilesBreakingDirectoryStructureFails() {
        List<String> expected = Lists.newArrayList();
        List<String> actual = Lists.newArrayList();
        for (FileReference fileReference : getFileSource().getFilesInPath(true, "..")) {
            actual.add(fileReference.toString());
        }
        assertEquals(expected, actual);
    }

    @Test
    public void getNonexistentFile() {
        Optional<FileReference> file = getFileSource().getFile("missing.jpg");
        assertFalse(file.isPresent());
    }

    @Test
    public void getFileBreakingDirectoryStructureFails() {
        Optional<FileReference> file = getFileSource().getFile("..", "hidden.txt");
        assertFalse(file.isPresent());
    }

    @Test
    public void getExistentFile() throws IOException {
        Optional<FileReference> file = getFileSource().getFile("subfolder", "test.resource");
        assertTrue(file.isPresent());
        FileReference fullFile = file.get();

        assertEquals(Lists.newArrayList("subfolder"), fullFile.getPath());
        try (Reader reader = new InputStreamReader(fullFile.open(), Charsets.UTF_8)) {
            String result = CharStreams.toString(reader);
            assertEquals("this space intentionally left blank", result);
        }
    }

    @Test
    public void getSubpaths() {
        assertEquals(Sets.newHashSet("subfolder", "folder"), getFileSource().getSubpaths());
    }

    @Test
    public void getSubpathsOfSubpath() {
        assertEquals(Sets.newHashSet("subpath"), getFileSource().getSubpaths("subfolder"));
    }

    @Test
    public void getSubpathsResistsBreakingStructure() {
        assertEquals(Collections.emptySet(), getFileSource().getSubpaths(".."));
    }

}
