package org.terasology.module.resources;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DirectoryFileSourceTest {

    private DirectoryFileSource source = new DirectoryFileSource(new File("src/test/resources"));

    @Test
    public void listFiles() {
        List<String> expected = Lists.newArrayList("test.resource");
        List<String> actual = Lists.newArrayList();
        for (ModuleFile moduleFile : source) {
            actual.add(moduleFile.toString());
        }

        assertEquals(expected, actual);
    }

    @Test
    public void listFilesInPath() {
        List<String> expected = Lists.newArrayList("test.resource");
        List<String> actual = Lists.newArrayList();
        for (ModuleFile moduleFile : source.getFilesInPath("subfolder")) {
            actual.add(moduleFile.toString());
        }

        assertEquals(expected, actual);
    }

    @Test
    public void listFilesInNonexistentPath() {
        List<String> expected = Lists.newArrayList();
        List<String> actual = Lists.newArrayList();
        for (ModuleFile moduleFile : source.getFilesInPath("grumbleface")) {
            actual.add(moduleFile.toString());
        }

        assertEquals(expected, actual);
    }

    @Test
    public void getNonexistentFile() {
        Optional<ModuleFile> file = source.getFile("test", "file.jpg");
        assertFalse(file.isPresent());
    }

    @Test
    public void getExistentFile() throws IOException {
        Optional<ModuleFile> file = source.getFile("subfolder", "test.resource");
        assertTrue(file.isPresent());
        ModuleFile fullFile = file.get();

        assertEquals(Lists.newArrayList("subfolder"), fullFile.getPath());
        try (Reader reader = new InputStreamReader(fullFile.open(), Charsets.UTF_8)) {
            String result = CharStreams.toString(reader);
            assertEquals("this space intentionally left blank", result);
        }
    }

    @Test
    public void breakDirectoryStructureFails() {
        List<String> expected = Lists.newArrayList();
        List<String> actual = Lists.newArrayList();
        for (ModuleFile moduleFile : source.getFilesInPath("..")) {
            actual.add(moduleFile.toString());
        }
        assertEquals(expected, actual);
    }
}
