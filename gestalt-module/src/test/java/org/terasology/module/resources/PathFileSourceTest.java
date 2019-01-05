package org.terasology.module.resources;

import java.nio.file.Paths;

public class PathFileSourceTest extends BaseFileSourceTest {

    private PathFileSource source = new PathFileSource(Paths.get("src", "test", "resources", "content"));

    @Override
    public ModuleFileSource getFileSource() {
        return source;
    }
}
