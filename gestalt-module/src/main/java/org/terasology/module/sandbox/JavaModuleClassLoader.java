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
package org.terasology.module.sandbox;

import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.module.Module;
import org.terasology.naming.Name;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

/**
 * A classloader to use when loading modules. This classloader ties into the sandboxing of modules by:
 * <ul>
 * <li>Acting as an indicator that a class belongs to a module - any class whose classloader is an instance of ModuleClassLoader comes from a module.</li>
 * <li>Restricting the classes visible to modules to those belonging those the module module has access to - as determined by the PermissionProvider.
 * Accessing any other class outside of the modules results in a ClassNotFoundException</li>
 * </ul>
 * <p>
 * Additionally, the ModuleClassLoader provides hooks for any injection that needs to be done to module classes as they are loaded, via javassist.
 * </p>
 *
 * @author Immortius
 */
public class JavaModuleClassLoader extends URLClassLoader implements ModuleClassLoader {

    private static final Logger logger = LoggerFactory.getLogger(JavaModuleClassLoader.class);
    private final PermissionProvider permissionProvider;
    private final ClassPool pool;

    private final Name moduleId;
    private final List<BytecodeInjector> bytecodeInjectors;

    /**
     * @param module             The name of the module this classloader belongs to
     * @param urls               The urls where the module classes can be found
     * @param parent             The parent classloader, where the API classes can be found
     * @param permissionProvider The security manager that sandboxes the classes
     */
    public JavaModuleClassLoader(Name module, URL[] urls, ClassLoader parent, PermissionProvider permissionProvider) {
        this(module, urls, parent, permissionProvider, Collections.emptyList());
    }

    /**
     * @param module             The name of the module this classloader belongs to
     * @param urls               The urls where the module classes can be found
     * @param parent             The parent classloader, where the API classes can be found
     * @param permissionProvider The security manager that sandboxes the classes
     * @param injectors          A collection of byte code injectors to pass all loaded module code through
     */
    public JavaModuleClassLoader(Name module, URL[] urls, ClassLoader parent, PermissionProvider permissionProvider, Iterable<BytecodeInjector> injectors) {
        super(urls, parent);
        this.moduleId = module;
        this.permissionProvider = permissionProvider;
        this.bytecodeInjectors = ImmutableList.copyOf(injectors);
        if (!bytecodeInjectors.isEmpty()) {
            pool = new ClassPool(ClassPool.getDefault());
            for (URL url : urls) {
                try {
                    logger.debug("Module path: {}", url.toURI());
                    pool.appendClassPath(new File(url.toURI()).toString());
                } catch (NotFoundException | URISyntaxException e) {
                    logger.error("Failed to process module url: {}", url);
                }
            }
        } else {
            pool = null;
        }
    }

    public static ModuleClassLoader create(Module module, ClassLoader parent, PermissionProvider permissionProvider) {
        URL[] urls = module.getClasspaths().stream().map(x -> {
            try {
                return x.toURI().toURL();
            } catch (MalformedURLException e) {
                logger.error("Failed to code location {} to URL", x, e);
                return null;
            }
        }).filter(Objects::nonNull).toArray(URL[]::new);
        return new JavaModuleClassLoader(module.getId(), urls, parent, permissionProvider);
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
            return ((JavaModuleClassLoader) getParent()).getBaseClassLoader();
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
        try {
            if (pool != null) {
                return AccessController.doPrivileged((PrivilegedExceptionAction<Class<?>>) () -> {
                    CtClass cc = pool.get(name);

                    for (BytecodeInjector injector : bytecodeInjectors) {
                        injector.inject(cc);
                    }
                    byte[] b = cc.toBytecode();
                    return defineClass(name, b, 0, b.length);
                });
            } else {
                return super.findClass(name);
            }

        } catch (PrivilegedActionException e) {
            throw new ClassNotFoundException("Failed to find or load class " + name, e.getCause());
        }
    }
}
