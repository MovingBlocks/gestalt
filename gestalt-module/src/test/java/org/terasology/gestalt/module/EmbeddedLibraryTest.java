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

import com.google.common.collect.Sets;

import org.junit.Before;
import org.junit.Test;
import org.terasology.gestalt.module.dependencyresolution.DependencyResolver;
import org.terasology.gestalt.module.sandbox.ModuleSecurityManager;
import org.terasology.gestalt.module.sandbox.ModuleSecurityPolicy;
import org.terasology.gestalt.module.sandbox.PermissionSet;
import org.terasology.gestalt.module.sandbox.StandardPermissionProviderFactory;
import org.terasology.gestalt.naming.Name;
import org.terasology.test.api.IOInterface;
import org.terasology.test.api.IndexForTest;

import java.io.FilePermission;
import java.nio.file.Paths;
import java.security.Policy;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertFalse;

/**
 *
 */
public class EmbeddedLibraryTest {

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

    @Test
    public void loadedModuleContainsEmbeddedLibraryClasses() {
        DependencyResolver resolver = new DependencyResolver(registry);
        ModuleEnvironment environment = new ModuleEnvironment(resolver.resolve(new Name("moduleE")).getModules(), permissionProviderFactory);

        LinkedHashSet<Class<?>> classes = Sets.newLinkedHashSet(environment.getTypesAnnotatedWith(IndexForTest.class));

        assertFalse(classes.isEmpty());
    }
}
