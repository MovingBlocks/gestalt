/*
 * Copyright 2014 MovingBlocks
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

package org.terasology.module;

import org.junit.Before;
import org.junit.Test;
import org.terasology.module.sandbox.BytecodeInjector;
import org.terasology.module.sandbox.ModuleSecurityManager;
import org.terasology.module.sandbox.ModuleSecurityPolicy;
import org.terasology.module.sandbox.PermissionSet;
import org.terasology.naming.Name;

import java.io.FilePermission;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.security.Policy;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author Immortius
 */
public class SandboxTest {

    private ModuleRegistry registry;
    private ModuleSecurityManager securityManager;

    @Before
    public void setup() {
        registry = new TableModuleRegistry();
        new ModulePathScanner().scan(registry, Paths.get("test-modules"));

        securityManager = new ModuleSecurityManager();
        securityManager.getBasePermissionSet().addAPIPackage("sun.reflect");
        securityManager.getBasePermissionSet().addAPIPackage("java.lang");
        securityManager.getBasePermissionSet().addAPIPackage("java.util");
        PermissionSet ioPermissionSet = new PermissionSet();
        ioPermissionSet.addAPIPackage("java.io");
        ioPermissionSet.addAPIPackage("java.nio.file");
        ioPermissionSet.addAPIPackage("java.nio.file.attribute");
        ioPermissionSet.grantPermission(FilePermission.class);
        securityManager.addPermissionSet("io", ioPermissionSet);

        Policy.setPolicy(new ModuleSecurityPolicy());
        System.setSecurityManager(securityManager);
    }

    // Ensure a normal method using globally allowed classes works
    @Test
    public void accessToNormalMethod() throws Exception {
        DependencyResolver resolver = new DependencyResolver(registry);
        ModuleEnvironment environment = new ModuleEnvironment(resolver.resolve(new Name("moduleA")).getModules(), securityManager, Collections.<BytecodeInjector>emptyList());

        Class<?> type = findClass("ModuleAClass", environment);
        Object instance = type.newInstance();
        type.getMethod("standardMethod").invoke(instance);
    }

    // Ensure access to disallowed classes fails
    @Test(expected = InvocationTargetException.class)
    public void deniedAccessToRestrictedClass() throws Exception {
        DependencyResolver resolver = new DependencyResolver(registry);
        ModuleEnvironment environment = new ModuleEnvironment(resolver.resolve(new Name("moduleA")).getModules(), securityManager, Collections.<BytecodeInjector>emptyList());

        Class<?> type = findClass("ModuleAClass", environment);
        Object instance = type.newInstance();
        type.getMethod("requiresIoMethod").invoke(instance);
    }

    // Ensures access to additionally required permission sets works (both classes and permissions
    @Test
    public void allowedAccessToClassFromRequiredPermissionSet() throws Exception {
        DependencyResolver resolver = new DependencyResolver(registry);
        ModuleEnvironment environment = new ModuleEnvironment(resolver.resolve(new Name("moduleB")).getModules(), securityManager, Collections.<BytecodeInjector>emptyList());

        Class<?> type = findClass("ModuleBClass", environment);
        Object instance = type.newInstance();
        type.getMethod("requiresIoMethod").invoke(instance);
    }

    // Ensure that a module doesn't gain accesses required by the parent but not by itself
    @Test(expected = InvocationTargetException.class)
    public void deniedAccessToClassPermittedToParent() throws Exception {
        DependencyResolver resolver = new DependencyResolver(registry);
        ModuleEnvironment environment = new ModuleEnvironment(resolver.resolve(new Name("moduleC")).getModules(), securityManager, Collections.<BytecodeInjector>emptyList());

        Class<?> type = findClass("ModuleCClass", environment);
        Object instance = type.newInstance();
        type.getMethod("requiresIoMethod").invoke(instance);
    }

    private Class<?> findClass(String name, ModuleEnvironment environment) {
        for (Class<?> type : environment.getSubtypesOf(Comparator.class)) {
            if (type.getSimpleName().equals(name)) {
                return type;
            }
        }
        return null;
    }
}
