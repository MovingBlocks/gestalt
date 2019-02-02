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

package org.terasology;

import org.junit.Test;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleFactory;
import org.terasology.module.ModuleMetadata;
import org.terasology.module.ModulePathScanner;
import org.terasology.module.ModuleRegistry;
import org.terasology.module.TableModuleRegistry;
import org.terasology.module.dependencyresolution.DependencyResolver;
import org.terasology.module.dependencyresolution.ResolutionResult;
import org.terasology.module.sandbox.APIScanner;
import org.terasology.module.sandbox.ModuleSecurityManager;
import org.terasology.module.sandbox.ModuleSecurityPolicy;
import org.terasology.module.sandbox.PermissionSet;
import org.terasology.module.sandbox.StandardPermissionProviderFactory;
import org.terasology.naming.Name;
import org.terasology.naming.Version;

import java.nio.file.Paths;
import java.security.Policy;
import java.util.Set;

/**
 *
 */
public class Sandbox {

    @Test
    public void test() {
        System.out.println(determineModuleSet());
    }


    public ModuleRegistry buildModuleRegistry() {
        ModuleRegistry registry = new TableModuleRegistry();
        new ModulePathScanner().scan(registry, Paths.get("test-modules"));
        return registry;
    }

    public Set<Module> determineModuleSet() {
        DependencyResolver resolver = new DependencyResolver(buildModuleRegistry());
        ResolutionResult resolutionResult = resolver.resolve(new Name("ModuleA"), new Name("ModuleC"));
        if (resolutionResult.isSuccess()) {
            return resolutionResult.getModules();
        } else {
            throw new RuntimeException("Unable to resolve compatible dependency set for ModuleA and ModuleC");
        }
    }

    public Module buildClasspathModule() {
        ModuleMetadata metadata = new ModuleMetadata();
        metadata.setId(new Name("Core"));
        metadata.setVersion(new Version("1.0.0"));
        ModuleFactory factory = new ModuleFactory();
        return factory.createFullClasspathModule(metadata);
    }

    public ModuleEnvironment establishSecureModuleEnvironment() {
        StandardPermissionProviderFactory permissionProviderFactory = new StandardPermissionProviderFactory();

        // Establish standard permissions
        permissionProviderFactory.getBasePermissionSet().addAPIPackage("com.example.api");
        permissionProviderFactory.getBasePermissionSet().addAPIPackage("sun.reflect");
        permissionProviderFactory.getBasePermissionSet().addAPIClass(String.class);

        // Add optional permission set "io"
        PermissionSet ioPermissionSet = new PermissionSet();
        ioPermissionSet.addAPIPackage("java.io");
        permissionProviderFactory.addPermissionSet("io", ioPermissionSet);
        new APIScanner(permissionProviderFactory).scan(buildModuleRegistry());

        // Installs the security manager to restrict access to permissions
        System.setSecurityManager(new ModuleSecurityManager());

        // Installs a policy that relaxes permission access for non-module code
        Policy.setPolicy(new ModuleSecurityPolicy());
        return new ModuleEnvironment(determineModuleSet(), permissionProviderFactory);


    }
}
