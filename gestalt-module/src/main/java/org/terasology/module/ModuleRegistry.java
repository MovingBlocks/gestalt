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

package org.terasology.module;

import org.terasology.naming.Name;
import org.terasology.naming.Version;

import java.util.Collection;
import java.util.Set;

/**
 * ModuleRegistry is a specialised collection of modules with lookup support for latest versions, specific version or all versions of a particular module.
 * <p>
 * For add operations, if a module already exists in the registry it will not be added (similar to a set)
 * </p>
 *
 * @author Immortius
 */
public interface ModuleRegistry extends Collection<Module> {

    /**
     * @param id The name of the modules to return
     * @return A list of all versions of the module with the given id
     */
    Collection<Module> getModuleVersions(Name id);

    /**
     * @return A complete collection of all available module names
     */
    Set<Name> getModuleIds();

    /**
     * @param id The name of the module to return
     * @return The most recent version of the desired module, or null if there is no matching module
     */
    Module getLatestModuleVersion(Name id);

    /**
     * @param id         The name of the module to return
     * @param minVersion The lower bound (inclusive) on the version desired
     * @param maxVersion The upper bound (exclusive) on the version desired
     * @return The most recent version of the desired module within the bounds, or null if there is no matching module
     */
    Module getLatestModuleVersion(Name id, Version minVersion, Version maxVersion);

    /**
     * Retrieves a specific module
     *
     * @param moduleId The name of the module
     * @param version  The version of the module
     * @return The module, or null if it doesn't exist in the registry
     */
    Module getModule(Name moduleId, Version version);

}
