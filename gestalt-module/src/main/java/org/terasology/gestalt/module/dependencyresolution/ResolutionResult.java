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

package org.terasology.gestalt.module.dependencyresolution;

import com.google.common.base.Preconditions;

import org.terasology.gestalt.module.Module;

import java.util.Set;

/**
 * Provides the results of dependency resolution - whether it succeeded or not, and if it succeeded the set of compatible modules that were resolved.
 *
 * @author Immortius
 */
public class ResolutionResult {
    private final boolean success;
    private final Set<Module> modules;

    /**
     * Constructs a new ResolutionResult instance with given success value and modules.
     * @param success   true if resolution succeeded.
     * @param modules   set of compatible modules.
     */
    public ResolutionResult(boolean success, Set<Module> modules) {
        this.success = success;
        this.modules = modules;
    }

    /**
     * Whether resolution succeeded
     * @return true if resolution succeeded
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Gets compatible modules with all dependencies resolved
     * @return A set of compatible modules
     */
    public Set<Module> getModules() {
        Preconditions.checkState(success, "Modules only available if resolution was successful");
        return modules;
    }
}
