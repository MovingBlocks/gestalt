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

package org.terasology.module.sandbox;

import org.terasology.module.Module;
import org.terasology.module.ModuleRegistry;

/**
 * Scans classpath modules for API annotated classes and packages, registering them with a ModuleSecurityManager.
 *
 * @author Immortius
 */
public class APIScanner {

    private ModuleSecurityManager securityManager;

    public APIScanner(ModuleSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    /**
     * Scans all modules in a registry that are on the class path, adding all packages and classes marked with the @API annotation to the securityManager's api
     *
     * @param registry The registry of modules to scan
     */
    public void scan(ModuleRegistry registry) {
        for (Module module : registry) {
            if (module.isOnClasspath()) {
                scan(module);
            }
        }
    }

    /**
     * Scans a module, adding any class or package marked with the @API annotation to the securityManager's api.
     *
     * @param module The module to scan
     */
    public void scan(Module module) {
        for (Class<?> apiClass : module.getReflectionsFragment().getTypesAnnotatedWith(API.class)) {
            if (apiClass.isSynthetic()) {
                // This is a package-info
                securityManager.addAPIPackage(apiClass.getPackage().getName());
            } else {
                securityManager.addAPIClass(apiClass);
            }
        }
    }

}
