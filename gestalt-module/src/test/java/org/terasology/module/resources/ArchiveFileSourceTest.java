package org.terasology.module.resources;

import java.io.File;
import java.io.IOException;

public class ArchiveFileSourceTest extends BaseFileSourceTest {

    private ArchiveFileSource source;

    public ArchiveFileSourceTest() throws IOException {
        source = new ArchiveFileSource(new File("src/test/resources/archive.zip"), "content");
    }

    @Override
    public ModuleFileSource getFileSource() {
        return source;
    }
}
