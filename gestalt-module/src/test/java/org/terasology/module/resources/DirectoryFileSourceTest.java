package org.terasology.module.resources;

import java.io.File;

public class DirectoryFileSourceTest extends BaseFileSourceTest {

    private DirectoryFileSource source = new DirectoryFileSource(new File("src/test/resources/content"));

    @Override
    public ModuleFileSource getFileSource() {
        return source;
    }
}