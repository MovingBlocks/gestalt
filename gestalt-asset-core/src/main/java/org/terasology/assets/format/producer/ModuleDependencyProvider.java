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

package org.terasology.assets.format.producer;

import org.terasology.naming.Name;

import java.util.List;

/**
 * Interface for a provider of information on the dependencies between modules.
 */
public interface ModuleDependencyProvider {

    /**
     * @param fromModule The proposed module with a dependency
     * @param onModule   The proposed module that is depended on
     * @return Whether fromModule has a dependency on onModule
     */
    boolean dependencyExists(Name fromModule, Name onModule);

    /**
     * @return A list of all modules, ordered so that no module is listed before a module it depends on
     */
    List<Name> getModulesOrderedByDependency();
}
