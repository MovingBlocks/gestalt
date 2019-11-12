/*
 * Copyright 2019 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.gestalt.android;

import com.google.common.base.Joiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.gestalt.module.Module;
import org.terasology.gestalt.module.sandbox.JavaModuleClassLoader;
import org.terasology.gestalt.module.sandbox.ModuleClassLoader;
import org.terasology.gestalt.module.sandbox.ObtainClassloader;
import org.terasology.gestalt.module.sandbox.PermissionProvider;
import org.terasology.gestalt.naming.Name;

import java.io.File;
import java.security.AccessController;
import java.util.List;

import dalvik.system.DexClassLoader;

/**
 * A module class loader built on top of DexClassLoader, to support loading code under Android.
 */
public class AndroidModuleClassLoader extends DexClassLoader implements ModuleClassLoader {

    private static final Logger logger = LoggerFactory.getLogger(JavaModuleClassLoader.class);
    private static final Joiner FILE_JOINER = Joiner.on(File.pathSeparatorChar);
    private final PermissionProvider permissionProvider;

    private final Name moduleId;

    /**
     * @param module             The name of the module this classloader belongs to
     * @param files              The files where the module classes can be found
     * @param codeCacheDir       The codeCacheDir, as per {@link DexClassLoader}
     * @param parent             The parent classloader, where the API classes can be found
     * @param permissionProvider The security manager that sandboxes the classes
     */
    private AndroidModuleClassLoader(Name module, List<File> files, File codeCacheDir, ClassLoader parent, PermissionProvider permissionProvider) {

        super(FILE_JOINER.join(files), codeCacheDir.toString(), null, parent);
        this.moduleId = module;
        this.permissionProvider = permissionProvider;
    }

    /**
     * @param module             The name of the module this classloader belongs to
     * @param parent             The parent classloader, where the API classes can be found
     * @param permissionProvider The security manager that sandboxes the classes
     * @param codeCacheDir       The codeCacheDir, as per {@link DexClassLoader}
     * @return An new AndroidModuleClassLoader for the module
     */
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