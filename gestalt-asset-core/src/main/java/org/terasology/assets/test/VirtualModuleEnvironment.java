/*
 * Copyright 2015 MovingBlocks
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

package org.terasology.assets.test;

import com.google.common.collect.Lists;
import org.terasology.module.*;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleMetadata;
import org.terasology.module.ModulePathScanner;
import org.terasology.module.ModuleRegistry;
import org.terasology.module.TableModuleRegistry;
import org.terasology.module.sandbox.BytecodeInjector;
import org.terasology.module.sandbox.PermissionProvider;
import org.terasology.module.sandbox.PermissionProviderFactory;
import org.terasology.naming.Name;
import org.terasology.naming.Version;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Permission;
import java.util.Collections;

/**
 * This environment loads multiple modules off of the classpath, discovered within a package.
 * This can be used to test more complex asset loading scenarios involving multiple modules.
 * <p>
 * Each virtual module should be in a directory under the virtualModules package, with the same name as the module's id. The directory should include a module.info file.
 * The internal structure of each virtual module is otherwise the standard module structure.
 * <p>
 * This environment will also load the classpath as a module, with the identifier 'test' version 1.0.0
 *
 * @author Immortius
 */
public class VirtualModuleEnvironment {

    public static final Name TEST_MODULE_ID = new Name("test");
    public static final Version TEST_MODULE_VERSION = new Version("1.0.0");
    protected ModuleRegistry moduleRegistry;

    /**
     * @param modulePackage The package that contains the modules to load
     * @throws IOException If there is a problem constructing the module file system
     * @throws URISyntaxException If a source location cannot be converted to a proper URI (typically because the path to the source includes an invalid character).
     */
    public VirtualModuleEnvironment(String modulePackage) throws IOException, URISyntaxException {
        this(modulePackage, VirtualModuleEnvironment.class);
    }

    /**
     * @param modulePackage The package that contains the modules to load
     * @param classpathClass A class used to determine the source to use as the classpath module.
     * @throws IOException If there is a problem constructing the module file system
     * @throws URISyntaxException If a source location cannot be converted to a proper URI (typically because the path to the source includes an invalid character).
     */
    public VirtualModuleEnvironment(String modulePackage, Class classpathClass) throws IOException, URISyntaxException {
        ModuleFactory moduleFactory = new ModuleFactory();
        moduleRegistry = new TableModuleRegistry();
        ModuleMetadata testModuleMetadata = new ModuleMetadata();
        testModuleMetadata.setId(TEST_MODULE_ID);
        testModuleMetadata.setVersion(TEST_MODULE_VERSION);
        Module testModule = moduleFactory.createClasspathModule(testModuleMetadata, true, classpathClass);
        moduleRegistry.add(testModule);

        ModulePathScanner scanner = new ModulePathScanner();
        for (Path path : testModule.getLocations()) {
            Path virtualPath;
            if (Files.isRegularFile(path)) {
                FileSystem jarFileSystem = FileSystems.newFileSystem(path, null);
                virtualPath = jarFileSystem.getPath(modulePackage);
            } else {
                virtualPath = path.resolve(modulePackage);
            }
            if (Files.isDirectory(virtualPath)) {
                scanner.scan(moduleRegistry, virtualPath);
            }
        }
    }

    /**
     * @return The module registry containing the virtual modules
     */
    public ModuleRegistry getRegistry() {
        return moduleRegistry;
    }

    /**
     * Creates a module environment including the specified modules
     * @param modules The modules to include in the environment
     * @return A new module environment
     * @throws URISyntaxException
     */
    public ModuleEnvironment createEnvironment(Module... modules) throws URISyntaxException {
        return new ModuleEnvironment(Lists.newArrayList(modules), module -> new PermissionProvider() {
            @Override
            public boolean isPermitted(Class aClass) {
                return true;
            }

            @Override
            public boolean isPermitted(Permission permission, Class<?> aClass) {
                return false;
            }
        }, Collections.<BytecodeInjector>emptyList());
    }

    /**
     * @return A new module environment containing no modules
     */
    public ModuleEnvironment createEmptyEnvironment() {
        return new ModuleEnvironment(Lists.<Module>newArrayList(), module -> new PermissionProvider() {
            @Override
            public boolean isPermitted(Class aClass) {
                return true;
            }

            @Override
            public boolean isPermitted(Permission permission, Class<?> aClass) {
                return false;
            }
        }, Collections.<BytecodeInjector>emptyList());
    }

    /**
     * @return A new module environment containing just the classpath module
     * @throws URISyntaxException
     */
    public ModuleEnvironment createEnvironment() throws URISyntaxException {
        return new ModuleEnvironment(Lists.newArrayList(moduleRegistry.getLatestModuleVersion(TEST_MODULE_ID)), module -> new PermissionProvider() {
            @Override
            public boolean isPermitted(Class aClass) {
                return true;
            }

            @Override
            public boolean isPermitted(Permission permission, Class<?> aClass) {
                return false;
            }
        }, Collections.<BytecodeInjector>emptyList());
    }
}
