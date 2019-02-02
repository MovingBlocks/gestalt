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

import org.terasology.naming.Name;

import java.io.IOException;

/**
 * Interface for ModuleClassLoader. This allows for different base class loaders (for example
 * on jre and android).
 */
public interface ModuleClassLoader {

    /**
     * @return The id of the module producing this class loader
     */
    Name getModuleId();

    /**
     * @return The class loader itself
     */
    ClassLoader getClassLoader();

    /**
     * Closes this class loader (when relevant)
     *
     * @throws IOException If an exception occurs closing the classloader
     */
    void close() throws IOException;

    /**
     * @return The PermissionProvider determining what classes from this module
     * are allowed to do and access
     */
    PermissionProvider getPermissionProvider();
}
