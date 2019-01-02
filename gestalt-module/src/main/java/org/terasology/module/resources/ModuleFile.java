package org.terasology.module.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface ModuleFile {

    String getName();

    List<String> getPath();

    InputStream open() throws IOException;

    long getSize();
}
