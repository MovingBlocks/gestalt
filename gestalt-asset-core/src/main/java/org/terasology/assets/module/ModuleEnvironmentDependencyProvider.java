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

package org.terasology.assets.module;

import android.support.annotation.Nullable;

import org.terasology.assets.format.producer.ModuleDependencyProvider;
import org.terasology.module.ModuleEnvironment;
import org.terasology.naming.Name;

import java.util.Collections;
import java.util.List;

/**
 * A {@link ModuleDependencyProvider} that uses a module environment to obtain dependency information. This environment can be switched at runtime. It can also be null.
 */
public class ModuleEnvironmentDependencyProvider implements ModuleDependencyProvider {

    private volatile ModuleEnvironment moduleEnvironment;

    /**
     * Creates an empty {@link ModuleEnvironmentDependencyProvider} with no environment
     */
    public ModuleEnvironmentDependencyProvider() {
    }

    /**
     * Creates a {@link ModuleEnvironmentDependencyProvider} with the provided environment
     *
     * @param moduleEnvironment
     */
    public ModuleEnvironmentDependencyProvider(@Nullable ModuleEnvironment moduleEnvironment) {
        this.moduleEnvironment = moduleEnvironment;
    }

    /**
     * Sets the module environment to use to obtain dependency information
     *
     * @param moduleEnvironment
     */
    public void setModuleEnvironment(@Nullable ModuleEnvironment moduleEnvironment) {
        this.moduleEnvironment = moduleEnvironment;
    }

    /**
     * @return The module environment currently being used
     */
    public ModuleEnvironment getModuleEnvironment() {
        return moduleEnvironment;
    }

    @Override
    public boolean dependencyExists(Name fromModule, Name onModule) {
        ModuleEnvironment currentEnvironment = moduleEnvironment;
        return currentEnvironment != null && currentEnvironment.getDependencyNamesOf(fromModule).contains(onModule);
    }

    @Override
    public List<Name> getModulesOrderedByDependency() {
        ModuleEnvironment currentEnvironment = moduleEnvironment;
        if (currentEnvironment != null) {
            return currentEnvironment.getModuleIdsOrderedByDependencies();
        } else {
            return Collections.emptyList();
        }
    }

}
