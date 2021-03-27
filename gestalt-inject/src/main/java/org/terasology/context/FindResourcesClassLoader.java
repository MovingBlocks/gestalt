package org.terasology.context;

import java.net.URL;
import java.util.Enumeration;

public interface FindResourcesClassLoader {
    Enumeration<URL> findResources(String name);
}
