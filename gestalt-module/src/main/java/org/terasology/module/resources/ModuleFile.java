package org.terasology.module.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * A handle describing and for accessing a
 */
public interface ModuleFile {

    /**
     * @return The name of the file
     */
    String getName();

    /**
     * @return The path to the file
     */
    List<String> getPath();

    /**
     * @return A stream for reading the file. Closing the stream is the duty of the caller
     * @throws IOException If there is an exception opening the file
     */
    InputStream open() throws IOException;

}
