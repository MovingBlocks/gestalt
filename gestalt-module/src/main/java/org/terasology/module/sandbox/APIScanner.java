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

import com.google.common.reflect.Reflection;

import org.reflections.Reflections;

/**
 * Scans a reflections manifest for API annotated classes and packages, registering them with a {@link StandardPermissionProviderFactory}.
 *
 * @author Immortius
 */
public class APIScanner {

    private StandardPermissionProviderFactory permissionProviderFactory;
    private ClassLoader forClassLoader;

    public APIScanner(StandardPermissionProviderFactory permissionProviderFactory) {
        this(permissionProviderFactory, ClassLoader.getSystemClassLoader());
    }

    public APIScanner(StandardPermissionProviderFactory permissionProviderFactory, ClassLoader forClassLoader) {
        this.permissionProviderFactory = permissionProviderFactory;
        this.forClassLoader = forClassLoader;
    }

    /**
     * Scans a reflections manifest, adding any class or package marked with the @API annotation into appropriate permission sets. Permission sets will be created if necessary.
     *
     * @param manifest The reflections manifest to scan
     */
    public void scan(Reflections manifest) {
        for (Class<?> apiClass : manifest.getTypesAnnotatedWith(API.class, true)) {
            if (forClassLoader == apiClass.getClassLoader()) {
                for (String permissionSetId : apiClass.getAnnotation(API.class).permissionSet()) {
                    PermissionSet permissionSet = permissionProviderFactory.getPermissionSet(permissionSetId);
                    if (permissionSet == null) {
                        permissionSet = new PermissionSet();
                        permissionProviderFactory.addPermissionSet(permissionSetId, permissionSet);
                    }
                    if (apiClass.isSynthetic()) {
                        // This is a package-info
                        permissionSet.addAPIPackage(Reflection.getPackageName(apiClass));
                    } else {
                        permissionSet.addAPIClass(apiClass);
                    }
                }
            }
        }
    }

}
