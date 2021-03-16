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

package org.terasology.gestalt.module;

import org.junit.Before;
import org.junit.Test;
import org.terasology.gestalt.module.dependencyresolution.DependencyResolver;
import org.terasology.gestalt.module.sandbox.ModuleSecurityManager;
import org.terasology.gestalt.module.sandbox.ModuleSecurityPolicy;
import org.terasology.gestalt.module.sandbox.PermissionProviderFactory;
import org.terasology.gestalt.module.sandbox.PermissionSet;
import org.terasology.gestalt.module.sandbox.StandardPermissionProviderFactory;
import org.terasology.gestalt.module.sandbox.WarnOnlyProviderFactory;
import org.terasology.gestalt.naming.Name;
import org.terasology.test.api.IOInterface;
import org.terasology.test.api.IndexForTest;

import java.io.FilePermission;
import java.nio.file.Paths;
import java.security.Policy;

/**
 * @author OvermindDL1
 */
public class PermissiveSandboxTest {

    private ModuleRegistry registry;
    private PermissionProviderFactory permissionProviderFactory;

    @Before
    public void setup() {
        registry = new TableModuleRegistry();
        new ModulePathScanner(new ModuleFactory()).scan(registry, Paths.get("test-modules").toFile());
        StandardPermissionProviderFactory standardPermissionProviderFactory = new StandardPermissionProviderFactory();

        standardPermissionProviderFactory.getBasePermissionSet().addAPIPackage("sun.reflect");
        standardPermissionProviderFactory.getBasePermissionSet().addAPIPackage("java.lang");
        standardPermissionProviderFactory.getBasePermissionSet().addAPIPackage("java.util");
        PermissionSet ioPermissionSet = new PermissionSet();
        ioPermissionSet.addAPIPackage("java.io");
        ioPermissionSet.addAPIPackage("java.nio.file");
        ioPermissionSet.addAPIPackage("java.nio.file.attribute");
        ioPermissionSet.addAPIClass(IOInterface.class);
        ioPermissionSet.grantPermission(FilePermission.class);
        standardPermissionProviderFactory.addPermissionSet("io", ioPermissionSet);
        permissionProviderFactory = new WarnOnlyProviderFactory(standardPermissionProviderFactory);

        Policy.setPolicy(new ModuleSecurityPolicy());
        System.setSecurityManager(new ModuleSecurityManager());
    }

    // Ensure a normal method using globally allowed classes works
    @Test
    public void accessToNormalMethod() throws Exception {
        DependencyResolver resolver = new DependencyResolver(registry);
        ModuleEnvironment environment =
                new ModuleEnvironment(resolver.resolve(new Name("moduleA")).getModules(), permissionProviderFactory);

        Class<?> type = findClass("ModuleAClass", environment);
        Object instance = type.newInstance();
        type.getMethod("standardMethod").invoke(instance);
    }

    // Ensure access to disallowed classes passes on permissive
    @Test
    public void deniedAccessToRestrictedClass() throws Exception {
        DependencyResolver resolver = new DependencyResolver(registry);
        ModuleEnvironment environment =
                new ModuleEnvironment(resolver.resolve(new Name("moduleA")).getModules(), permissionProviderFactory);

        Class<?> type = findClass("ModuleAClass", environment);
        Object instance = type.newInstance();
        type.getMethod("requiresIoMethod").invoke(instance);
    }

    // Ensures access to additionally required permission sets works (both classes and permissions)
    @Test
    public void allowedAccessToClassFromRequiredPermissionSet() throws Exception {
        DependencyResolver resolver = new DependencyResolver(registry);
        ModuleEnvironment environment =
                new ModuleEnvironment(resolver.resolve(new Name("moduleB")).getModules(), permissionProviderFactory);

        Class<?> type = findClass("ModuleBClass", environment);
        Object instance = type.newInstance();
        type.getMethod("requiresIoMethod").invoke(instance);
    }

    // Ensure that a module doesn't gain accesses required by the parent but not by itself, permissive
    @Test
    public void deniedAccessToClassPermittedToParent() throws Exception {
        DependencyResolver resolver = new DependencyResolver(registry);
        ModuleEnvironment environment =
                new ModuleEnvironment(resolver.resolve(new Name("moduleC")).getModules(), permissionProviderFactory);

        Class<?> type = findClass("ModuleCClass", environment);
        Object instance = type.newInstance();
        type.getMethod("requiresIoMethod").invoke(instance);
    }

    private Class<?> findClass(String name, ModuleEnvironment environment) throws ClassNotFoundException {
        for (Class<?> type : environment.getTypesAnnotatedWith(IndexForTest.class)) {
            if (type.getSimpleName().equals(name)) {
                return type;
            }
        }
        throw new ClassNotFoundException(name);
    }
}
