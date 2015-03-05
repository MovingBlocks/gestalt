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

package org.terasology.module.filesystem;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.terasology.module.ClasspathModule;
import org.terasology.module.Module;
import org.terasology.module.ModuleMetadata;
import org.terasology.module.TableModuleRegistry;
import org.terasology.naming.Name;
import org.terasology.naming.Version;

import java.io.BufferedReader;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Immortius
 */
public class ModulePathTest {

    private ModuleFileSystem fileSystem;

    public ModulePathTest() throws Exception {
        ModuleMetadata metadata = new ModuleMetadata();
        metadata.setId(new Name("test"));
        metadata.setVersion(new Version("1.0.0"));
        Module module = ClasspathModule.create(metadata, true, getClass());

        fileSystem = new ModuleFileSystemProvider(new TableModuleRegistry()).newFileSystem(module);
    }

    @Test
    public void absolutePath() {
        ModulePath path = fileSystem.getPath("/");
        assertTrue(path.isAbsolute());
    }

    @Test
    public void relativePath() {
        ModulePath path = fileSystem.getPath("");
        assertFalse(path.isAbsolute());
    }

    @Test
    public void joinPathParts() {
        assertEquals("", fileSystem.getPath("").toString());
        assertEquals("hello", fileSystem.getPath("hello").toString());
        assertEquals("hello/world", fileSystem.getPath("hello", "world").toString());
    }

    @Test
    public void joinPathPartsIgnoreEmptyParts() {
        assertEquals("", fileSystem.getPath("", "").toString());
        assertEquals("hello", fileSystem.getPath("hello", "").toString());
        assertEquals("hello", fileSystem.getPath("", "hello").toString());
        assertEquals("hello", fileSystem.getPath("", "hello", "").toString());
    }

    @Test
    public void joinPathPartsDoNotDoubleUpOnPathSeparators() {
        assertEquals("/", fileSystem.getPath("/", "/").toString());
        assertEquals("hello", fileSystem.getPath("hello", "/").toString());
        assertEquals("/hello", fileSystem.getPath("/", "hello").toString());
        assertEquals("/hello", fileSystem.getPath("/", "hello", "/").toString());
        assertEquals("/hello", fileSystem.getPath("//hello////").toString());
    }

    @Test
    public void getRootForAbsolutePath() {
        ModulePath path = fileSystem.getPath("/hello", "world");
        assertEquals(fileSystem.getPath("/"), path.getRoot());
    }

    @Test
    public void getRootForRelativePath() {
        ModulePath path = fileSystem.getPath("hello", "world");
        assertNull(path.getRoot());
    }

    @Test
    public void getFilename() {
        assertEquals(fileSystem.getPath(""), fileSystem.getPath("").getFileName());
        assertEquals(null, fileSystem.getPath("/").getFileName());
        assertEquals(fileSystem.getPath("hello"), fileSystem.getPath("hello").getFileName());
        assertEquals(fileSystem.getPath("world"), fileSystem.getPath("hello", "world").getFileName());
    }

    @Test
    public void getParent() {
        assertEquals(null, fileSystem.getPath("").getParent());
        assertEquals(null, fileSystem.getPath("/").getParent());
        assertEquals(null, fileSystem.getPath("hello").getParent());
        assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/hello").getParent());
        assertEquals(fileSystem.getPath("hello"), fileSystem.getPath("hello", "world").getParent());
        assertEquals(fileSystem.getPath("/hello"), fileSystem.getPath("/hello", "world").getParent());
    }

    @Test
    public void getNameCount() {
        assertEquals(1, fileSystem.getPath("").getNameCount());
        assertEquals(0, fileSystem.getPath("/").getNameCount());
        assertEquals(1, fileSystem.getPath("hello").getNameCount());
        assertEquals(1, fileSystem.getPath("/hello").getNameCount());
        assertEquals(2, fileSystem.getPath("hello", "world").getNameCount());
        assertEquals(2, fileSystem.getPath("/hello", "world").getNameCount());
    }

    @Test
    public void iterateRelativePath() {
        Iterable<Path> i = fileSystem.getPath("hello", "world");
        assertEquals(Lists.<Path>newArrayList(fileSystem.getPath("hello"), fileSystem.getPath("world")), Lists.newArrayList(i));
    }

    @Test
    public void iterateAbsolutePath() {
        Iterable<Path> i = fileSystem.getPath("/", "hello", "world");
        assertEquals(Lists.<Path>newArrayList(fileSystem.getPath("hello"), fileSystem.getPath("world")), Lists.newArrayList(i));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNameWithNegativeIndex() {
        fileSystem.getPath("hello", "world").getName(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNameHigherThanNameCount() {
        fileSystem.getPath("hello", "world").getName(2);
    }

    @Test
    public void getNameRelative() {
        Path path = fileSystem.getPath("hello", "world");
        assertEquals(fileSystem.getPath("hello"), path.getName(0));
        assertEquals(fileSystem.getPath("world"), path.getName(1));
    }

    @Test
    public void getNameAbsolute() {
        Path path = fileSystem.getPath("/", "hello", "world");
        assertEquals(fileSystem.getPath("hello"), path.getName(0));
        assertEquals(fileSystem.getPath("world"), path.getName(1));
    }

    @Test
    public void subpathWithFullRange() {
        Path path = fileSystem.getPath("/", "hello", "world", "and", "goodnight");
        assertEquals(fileSystem.getPath("hello", "world", "and", "goodnight"), path.subpath(0, 4));
    }

    @Test
    public void subpathWithPartialRanges() {
        Path path = fileSystem.getPath("/", "hello", "world", "and", "goodnight");
        assertEquals(fileSystem.getPath("hello"), path.subpath(0, 1));
        assertEquals(fileSystem.getPath("world"), path.subpath(1, 2));
        assertEquals(fileSystem.getPath("world", "and"), path.subpath(1, 3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void subpathWithBeginIndexTooLow() {
        fileSystem.getPath("/", "hello", "world", "and", "goodnight").subpath(-1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void subpathWithBeginIndexTooHigh() {
        fileSystem.getPath("/", "hello", "world", "and", "goodnight").subpath(4, 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void subpathWithEndIndexTooLow() {
        fileSystem.getPath("/", "hello", "world", "and", "goodnight").subpath(1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void subpathWithEndIndexTooHigh() {
        fileSystem.getPath("/", "hello", "world", "and", "goodnight").subpath(1, 5);
    }

    @Test
    public void startsWithPathAbsolute() {
        Path path = fileSystem.getPath("/", "hello", "world");
        assertTrue(path.startsWith(fileSystem.getPath("/")));
        assertTrue(path.startsWith(fileSystem.getPath("/", "hello")));
        assertTrue(path.startsWith(fileSystem.getPath("/", "hello", "world")));
    }

    @Test
    public void startsWithPathRelative() {
        Path path = fileSystem.getPath("hello", "world");
        assertFalse(path.startsWith(fileSystem.getPath("")));
        assertTrue(path.startsWith(fileSystem.getPath("hello")));
        assertTrue(path.startsWith(fileSystem.getPath("hello", "world")));
    }

    @Test
    public void startsWithPathWithMixedRelativeAndAbsolute() {
        Path relative = fileSystem.getPath("hello", "world");
        Path absolute = fileSystem.getPath("/", "hello", "world");
        assertFalse(relative.startsWith(absolute));
        assertFalse(absolute.startsWith(relative));
    }

    @Test
    public void startsWithPathAcrossFileSystems() {
        Path path = fileSystem.getPath("hello", "world");
        assertFalse(path.startsWith(FileSystems.getDefault().getPath("hello")));
    }

    @Test
    public void endsWithPath() {
        Path path = fileSystem.getPath("/", "hello", "world");
        assertTrue(path.endsWith(fileSystem.getPath("world")));
        assertTrue(path.endsWith(fileSystem.getPath("hello", "world")));
        assertTrue(path.endsWith(fileSystem.getPath("/", "hello", "world")));
        assertFalse(path.endsWith(fileSystem.getPath("hello")));
        assertFalse(path.endsWith(fileSystem.getPath("/", "world")));
    }

    @Test
    public void normaliseNoActionRequired() {
        assertEquals(fileSystem.getPath("hello", "world"), fileSystem.getPath("hello", "world").normalize());
    }

    @Test
    public void normaliseRemoveSameDirIndicator() {
        assertEquals(fileSystem.getPath("hello", "world"), fileSystem.getPath("hello", ".", "world").normalize());
        assertEquals(fileSystem.getPath("hello", "world"), fileSystem.getPath(".", "hello", "world").normalize());
        assertEquals(fileSystem.getPath("hello", "world"), fileSystem.getPath(".", ".", "hello", "world").normalize());
        assertEquals(fileSystem.getPath("/", "hello", "world"), fileSystem.getPath("/", ".", "hello", "world", ".").normalize());
    }

    @Test
    public void normaliseApplyPreviousDirectoryIndicator() {
        assertEquals(fileSystem.getPath("world"), fileSystem.getPath("hello", "..", "world").normalize());
        assertEquals(fileSystem.getPath(""), fileSystem.getPath("hello", "world", "..", "..").normalize());
        assertEquals(fileSystem.getPath(".."), fileSystem.getPath("hello", "world", "..", "..", "..").normalize());
        assertEquals(fileSystem.getPath("..", ".."), fileSystem.getPath("hello", "..", "..", "..").normalize());
        assertEquals(fileSystem.getPath("..", "hello", "world"), fileSystem.getPath("..", "hello", "world").normalize());
        assertEquals(fileSystem.getPath("/", "hello", "world"), fileSystem.getPath("/", "..", "hello", "world").normalize());
    }

    @Test
    public void resolveReturnsAbsolutePathAsIs() {
        assertEquals(fileSystem.getPath("/", "world"), fileSystem.getPath("/", "hello").resolve(fileSystem.getPath("/", "world")));
    }

    @Test
    public void resolve() {
        assertEquals(fileSystem.getPath("/", "hello"), fileSystem.getPath("/", "hello").resolve(fileSystem.getPath("")));
        assertEquals(fileSystem.getPath("/", "hello", "world"), fileSystem.getPath("/", "hello").resolve(fileSystem.getPath("world")));
    }

    @Test
    public void relativise() {
        Path path = fileSystem.getPath("/", "hello", "world");
        assertEquals(fileSystem.getPath(""), path.relativize(path));
        assertEquals(fileSystem.getPath("wooo"), path.relativize(path.resolve("wooo")));
        assertEquals(fileSystem.getPath(".."), path.relativize(path.getParent()));
        assertEquals(fileSystem.getPath("../woo"), path.relativize(path.getParent().resolve("woo")));
    }

    @Test
    public void relativiseDenormalizedPaths() {
        Path path = fileSystem.getPath("/", "hello", ".");
        assertEquals(fileSystem.getPath(""), path.relativize(path));
        assertEquals(fileSystem.getPath("wooo"), path.relativize(path.resolve("wooo")));
        assertEquals(fileSystem.getPath(""), path.relativize(path.resolve(".")));
    }

    @Test
    public void pathToUri() throws Exception {
        Path path = fileSystem.getPath("/", "hello", "world");
        assertEquals(new URI(String.format("%s://%s:%s/hello/world", "module", "test", "1.0.0")), path.toUri());
    }

    @Test
    public void readFile() throws Exception {
        Path path = fileSystem.getPath("/", "subfolder", "test.resource");
        try (BufferedReader reader = Files.newBufferedReader(path, Charsets.UTF_8)) {
            assertEquals("this space intentionally left blank", reader.readLine());
        }
    }

    @Test
    public void exists() throws Exception {
        assertTrue(Files.exists(fileSystem.getPath("/", "subfolder")));
        assertFalse(Files.exists(fileSystem.getPath("/", "makebelieverubbish")));
    }

    @Test
    public void isDirectory() throws Exception {
        Path path = fileSystem.getPath("/");
        assertTrue(Files.isDirectory(path));
        assertFalse(Files.isRegularFile(path));
    }

    @Test
    public void getContents() throws Exception {
        Path path = fileSystem.getPath("/", "subfolder");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path);) {
            List<Path> contents = Lists.newArrayList(stream.iterator());
            assertEquals(1, contents.size());
            assertEquals(fileSystem.getPath("/", "subfolder", "test.resource"), contents.get(0));
        }
    }
}
