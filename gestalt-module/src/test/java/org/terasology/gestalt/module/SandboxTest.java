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

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.terasology.gestalt.module.dependencyresolution.DependencyResolver;
import org.terasology.gestalt.module.resources.EmptyFileSource;
import org.terasology.gestalt.module.sandbox.ModuleSecurityManager;
import org.terasology.gestalt.module.sandbox.ModuleSecurityPolicy;
import org.terasology.gestalt.module.sandbox.PermissionSet;
import org.terasology.gestalt.module.sandbox.StandardPermissionProviderFactory;
import org.terasology.gestalt.naming.Name;
import org.terasology.gestalt.naming.Version;
import org.terasology.test.api.IOInterface;
import org.terasology.test.api.IndexForTest;

import java.io.FilePermission;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.security.Policy;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Immortius
 */
public class SandboxTest {

    private ModuleRegistry registry;
    private StandardPermissionProviderFactory permissionProviderFactory = new StandardPermissionProviderFactory();

    @Before
    public void setup() {
        registry = new TableModuleRegistry();
        new ModulePathScanner().scan(registry, Paths.get("test-modules").toFile());

        permissionProviderFactory.getBasePermissionSet().addAPIPackage("sun.reflect");
        permissionProviderFactory.getBasePermissionSet().addAPIPackage("java.lang");
        permissionProviderFactory.getBasePermissionSet().addAPIPackage("java.util");
        PermissionSet ioPermissionSet = new PermissionSet();
        ioPermissionSet.addAPIPackage("java.io");
        ioPermissionSet.addAPIPackage("java.nio.file");
        ioPermissionSet.addAPIPackage("java.nio.file.attribute");
        ioPermissionSet.addAPIClass(IOInterface.class);
        ioPermissionSet.grantPermission(FilePermission.class);
        permissionProviderFactory.addPermissionSet("io", ioPermissionSet);


        Policy.setPolicy(new ModuleSecurityPolicy());
        System.setSecurityManager(new ModuleSecurityManager());
    }

    // Ensure a normal method using globally allowed classes works
    @Test
    public void accessToNormalMethod() throws Exception {
        DependencyResolver resolver = new DependencyResolver(registry);
        ModuleEnvironment environment = new ModuleEnvironment(resolver.resolve(new Name("moduleA")).getModules(), permissionProviderFactory);

        Class<?> type = findClass("ModuleAClass", environment);
        Object instance = type.newInstance();
        type.getMethod("standardMethod").invoke(instance);
    }

    // Ensure access to disallowed classes fails
    @Test(expected = InvocationTargetException.class)
    public void deniedAccessToRestrictedClassInMethod() throws Exception {
        DependencyResolver resolver = new DependencyResolver(registry);
        ModuleEnvironment environment = new ModuleEnvironment(resolver.resolve(new Name("moduleB")).getModules(), permissionProviderFactory);

        Class<?> type = findClass("ModuleBClass", environment);
        Object instance = type.newInstance();
        type.getMethod("illegalMethod").invoke(instance);
    }

    @Test(expected = ClassNotFoundException.class)
    public void deniedAccessToClassImplementingRestrictedInterface() throws Exception {
        DependencyResolver resolver = new DependencyResolver(registry);
        ModuleEnvironment environment = new ModuleEnvironment(resolver.resolve(new Name("moduleD")).getModules(), permissionProviderFactory);

        findClass("ModuleDRestrictedClass", environment);
    }

    @Test
    public void allowedAccessToClassImplementingPermissionSetInterface() throws Exception {
        DependencyResolver resolver = new DependencyResolver(registry);
        ModuleEnvironment environment = new ModuleEnvironment(resolver.resolve(new Name("moduleB")).getModules(), permissionProviderFactory);

        Class<?> type = findClass("ModuleBPermittedClass", environment);
        assertNotNull(type);
        assertTrue(IOInterface.class.isAssignableFrom(type));
    }

    // Ensures access to additionally required permission sets works (both classes and permissions)
    @Test
    public void allowedAccessToClassFromRequiredPermissionSet() throws Exception {
        DependencyResolver resolver = new DependencyResolver(registry);
        ModuleEnvironment environment = new ModuleEnvironment(resolver.resolve(new Name("moduleB")).getModules(), permissionProviderFactory);

        Class<?> type = findClass("ModuleBClass", environment);
        Object instance = type.newInstance();
        type.getMethod("requiresIoMethod").invoke(instance);
    }

    // Ensure that a module doesn't gain accesses required by the parent but not by itself
    @Test(expected = InvocationTargetException.class)
    public void deniedAccessToClassPermittedToParent() throws Exception {
        DependencyResolver resolver = new DependencyResolver(registry);
        ModuleEnvironment environment = new ModuleEnvironment(resolver.resolve(new Name("moduleC")).getModules(), permissionProviderFactory);

        Class<?> type = findClass("ModuleCClass", environment);
        Object instance = type.newInstance();
        type.getMethod("requiresIoMethod").invoke(instance);
    }

    @Test
    public void allowedAccessToPackageModuleClassViaModuleClassloader() throws Exception {
        DependencyResolver resolver = new DependencyResolver(registry);
        ModuleMetadata metadata = new ModuleMetadata();
        metadata.setId(new Name("PackageModule"));
        metadata.setVersion(Version.DEFAULT);

        Reflections reflections = new Reflections(new ConfigurationBuilder().forPackages("org.terasology.gestalt.module.packageModule").addClassLoader(ClasspathHelper.contextClassLoader()).addScanners(new SubTypesScanner()));
        Module module = new Module(metadata, new EmptyFileSource(), Collections.emptyList(), reflections, x -> com.google.common.reflect.Reflection.getPackageName(x).startsWith("org.terasology.gestalt.module.packageModule"));
        List<Module> modules = Lists.newArrayList(module);
        modules.addAll(resolver.resolve(new Name("moduleC")).getModules());
        ModuleEnvironment environment = new ModuleEnvironment(modules, permissionProviderFactory);
        Object result = findClass("StandaloneClass", environment).newInstance();
        assertNotNull(result);
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
