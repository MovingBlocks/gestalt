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
import org.terasology.module.ClasspathModule;
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

import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Permission;
import java.util.Collections;

/**
 * This environment loads multiple modules off of the classpath, discovered within a 'virtualModules' package.
 * This can be used to test more complex asset loading scenarios involving multiple modules.
 * <p>
 * Each virtual module should be in a directory under the virtualModules package, with the same name as the module's id. The directory should include a module.info file.
 * The internal structure of each virtual module is otherwise the standard module structure.
 *
 * @author Immortius
 */
public class VirtualModuleEnvironment {

    protected ModuleRegistry moduleRegistry;

    public VirtualModuleEnvironment() throws Exception {
        this(VirtualModuleEnvironment.class);
    }

    public VirtualModuleEnvironment(Class classpathClass) throws Exception {
        moduleRegistry = new TableModuleRegistry();
        ModuleMetadata testModuleMetadata = new ModuleMetadata();
        testModuleMetadata.setId(new Name("test"));
        testModuleMetadata.setVersion(new Version("1.0.0"));
        Module testModule = ClasspathModule.create(testModuleMetadata, true, classpathClass);
        moduleRegistry.add(testModule);

        ModulePathScanner scanner = new ModulePathScanner();
        for (Path path : testModule.getLocations()) {
            Path virtualPath;
            if (Files.isRegularFile(path)) {
                FileSystem jarFileSystem = FileSystems.newFileSystem(path, null);
                virtualPath = jarFileSystem.getPath("virtualModules");
            } else {
                virtualPath = path.resolve("virtualModules");
            }
            if (Files.isDirectory(virtualPath)) {
                scanner.scan(moduleRegistry, virtualPath);
            }
        }
    }

    public ModuleRegistry getRegistry() {
        return moduleRegistry;
    }

    public ModuleEnvironment createEnvironment(Module... modules) throws URISyntaxException {
        return new ModuleEnvironment(Lists.newArrayList(modules), new PermissionProviderFactory() {
            @Override
            public PermissionProvider createPermissionProviderFor(Module module) {
                return new PermissionProvider() {
                    @Override
                    public boolean isPermitted(Class aClass) {
                        return true;
                    }

                    @Override
                    public boolean isPermitted(Permission permission, Class<?> aClass) {
                        return false;
                    }
                };
            }
        }, Collections.<BytecodeInjector>emptyList());
    }

    public ModuleEnvironment createEmptyEnvironment() {
        return new ModuleEnvironment(Lists.<Module>newArrayList(), new PermissionProviderFactory() {
            @Override
            public PermissionProvider createPermissionProviderFor(Module module) {
                return new PermissionProvider() {
                    @Override
                    public boolean isPermitted(Class aClass) {
                        return true;
                    }

                    @Override
                    public boolean isPermitted(Permission permission, Class<?> aClass) {
                        return false;
                    }
                };
            }
        }, Collections.<BytecodeInjector>emptyList());
    }

    public ModuleEnvironment createEnvironment() throws URISyntaxException {
        return new ModuleEnvironment(Lists.newArrayList(moduleRegistry.getLatestModuleVersion(new Name("test"))), new PermissionProviderFactory() {
            @Override
            public PermissionProvider createPermissionProviderFor(Module module) {
                return new PermissionProvider() {
                    @Override
                    public boolean isPermitted(Class aClass) {
                        return true;
                    }

                    @Override
                    public boolean isPermitted(Permission permission, Class<?> aClass) {
                        return false;
                    }
                };
            }
        }, Collections.<BytecodeInjector>emptyList());
    }
}
