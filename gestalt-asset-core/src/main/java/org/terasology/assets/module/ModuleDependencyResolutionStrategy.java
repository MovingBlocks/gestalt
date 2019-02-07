/*
 * Copyright 2015 MovingBlocks
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

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;

import net.jcip.annotations.ThreadSafe;

import org.terasology.assets.ResolutionStrategy;
import org.terasology.assets.format.producer.ModuleDependencyProvider;
import org.terasology.module.ModuleEnvironment;
import org.terasology.naming.Name;

import java.util.Set;

/**
 * ModuleDependencyResolutionStrategy is a resolution strategy that takes into account the dependencies of the module that is the context of resolution.
 * Firstly, if the context is one of the possible modules, just the context is returned. Otherwise, only list of possible modules
 * are filtered down to just those that are a dependency of the module context
 *
 * @author Immortius
 */
@ThreadSafe
public class ModuleDependencyResolutionStrategy implements ResolutionStrategy {

    private final ModuleDependencyProvider dependencyProvider;

    /**
     * Creates a {@link ModuleDependencyResolutionStrategy} from a ModuleEnvironment. It is recommended to use a ModuleDependencyProvider instead as this will
     * allow for switching environments without creating a new resolution strategy.
     *
     * @param environment The module environment to use to resolve an asset.
     */
    public ModuleDependencyResolutionStrategy(ModuleEnvironment environment) {
        this(new ModuleEnvironmentDependencyProvider(environment));
    }

    /**
     * @param dependencyProvider The provider of dependency information
     */
    public ModuleDependencyResolutionStrategy(ModuleDependencyProvider dependencyProvider) {
        this.dependencyProvider = dependencyProvider;
    }

    @Override
    public Set<Name> resolve(Set<Name> modules, final Name context) {
        if (modules.contains(context)) {
            return ImmutableSet.of(context);
        }

        return ImmutableSet.copyOf(Collections2.filter(modules, input -> dependencyProvider.dependencyExists(context, input)));
    }
}
