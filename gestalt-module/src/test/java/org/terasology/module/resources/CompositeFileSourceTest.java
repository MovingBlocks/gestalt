package org.terasology.module.resources;

import java.io.File;
import java.io.IOException;

public class CompositeFileSourceTest extends BaseFileSourceTest {

    private CompositeFileSource source;

    public CompositeFileSourceTest() throws IOException {
        source = new CompositeFileSource(new DirectoryFileSource(new File("src/test/resources/halfone/content")), new ArchiveFileSource(new File("src/test/resources/halftwo.zip"), "content"));
    }

    @Override
    public ModuleFileSource getFileSource() {
        return source;
    }
}
