package org.terasology.gestalt.android;

import com.google.common.base.Joiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.module.Module;
import org.terasology.module.sandbox.JavaModuleClassLoader;
import org.terasology.module.sandbox.ModuleClassLoader;
import org.terasology.module.sandbox.ObtainClassloader;
import org.terasology.module.sandbox.PermissionProvider;
import org.terasology.naming.Name;

import java.io.File;
import java.security.AccessController;
import java.util.List;

import dalvik.system.DexClassLoader;


public class AndroidModuleClassLoader extends DexClassLoader implements ModuleClassLoader {

    private static final Logger logger = LoggerFactory.getLogger(JavaModuleClassLoader.class);
    private static final Joiner FILE_JOINER = Joiner.on(File.pathSeparatorChar);
    private final PermissionProvider permissionProvider;

    private final Name moduleId;

    /**
     * @param module      The name of the module this classloader belongs to
     * @param files       The files where the module classes can be found
     * @param parent      The parent classloader, where the API classes can be found
     * @param permissionProvider The security manager that sandboxes the classes
     */
    private AndroidModuleClassLoader(Name module, List<File> files, File codeCacheDir, ClassLoader parent, PermissionProvider permissionProvider) {

        super(FILE_JOINER.join(files), codeCacheDir.toString(), null, parent);
        this.moduleId = module;
        this.permissionProvider = permissionProvider;
    }

    public static ModuleClassLoader create(Module module, ClassLoader parent, PermissionProvider permissionProvider, File codeCacheDir) {
        return new AndroidModuleClassLoader(module.getId(), module.getClasspaths(), codeCacheDir, parent, permissionProvider);
    }

    /**
     * @return The id of the module this ClassLoader belongs to
     */
    @Override
    public Name getModuleId() {
        return moduleId;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this;
    }

    @Override
    public void close() {
    }

    /**
     * @return The permission provider for this ModuleClassLoader
     */
    @Override
    public PermissionProvider getPermissionProvider() {
        return permissionProvider;
    }

    /**
     * @return The non-ModuleClassLoader that the module classloader chain is based on
     */
    private ClassLoader getBaseClassLoader() {
        if (getParent() instanceof ModuleClassLoader) {
            return ((AndroidModuleClassLoader) getParent()).getBaseClassLoader();
        }
        return getParent();
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz;
        try {
            clazz = getBaseClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            clazz = super.loadClass(name, resolve);
        }

        ClassLoader parentLoader = AccessController.doPrivileged(new ObtainClassloader(clazz));
        if (parentLoader != this && !(parentLoader instanceof ModuleClassLoader)) {
            if (permissionProvider.isPermitted(clazz)) {
                return clazz;
            } else {
                logger.error("Denied access to class (not allowed with this module's permissions): {}", name);
                return null;
            }
        }
        return clazz;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        if (name.startsWith("java.")) {
            return null;
        }
        return super.findClass(name);
    }
}