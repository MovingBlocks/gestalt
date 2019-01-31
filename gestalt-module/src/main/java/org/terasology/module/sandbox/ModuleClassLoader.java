package org.terasology.module.sandbox;

import org.terasology.naming.Name;

import java.io.IOException;

public interface ModuleClassLoader {

    Name getModuleId();

    ClassLoader getClassLoader();

    void close() throws IOException;

    PermissionProvider getPermissionProvider();
}
