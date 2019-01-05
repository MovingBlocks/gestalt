package org.terasology.module.resources;

import org.junit.BeforeClass;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

public class ClasspathFileSourceTest extends BaseFileSourceTest {

    private static ClasspathFileSource source;

    @BeforeClass
    public static void setup() {
        ResourcesScanner resourcesScanner = new ResourcesScanner();
        resourcesScanner.setResultFilter(x -> false);
        Reflections manifest = new Reflections("content", resourcesScanner);
        source = new ClasspathFileSource(manifest, "content");
    }

    @Override
    public ModuleFileSource getFileSource() {
        return source;
    }
}
