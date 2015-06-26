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

package org.terasology.module;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.terasology.module.dependencyResolution.OptionalResolutionStrategy;
import org.terasology.naming.Name;
import org.terasology.naming.Version;
import org.terasology.naming.VersionRange;

/**
 * Dependency Resolver determines a working set of modules for a given set of desired modules. Where multiple versions are compatible, they are resolved in favour of the
 * latest available. In particular, the latest version of the desired modules is prioritised, and in the order requested (so if using the latest version of the first
 * desired module prevents the use of latest version of the second desired module, then that is what will happen).
 * <p>
 * The algorithm used is based on Arc Consistency Algorithm #3.
 * </p>
 *
 * @author Immortius
 */
public class DependencyResolver {
    private final OptionalResolutionStrategy optionalStrategy;
    private final ModuleRegistry registry;

    /**
     * Creates a DependencyResolver using the {@link org.terasology.module.dependencyResolution.OptionalResolutionStrategy#INCLUDE_IF_REQUIRED}.
     *
     * @param registry The registry to resolve modules from
     */
    public DependencyResolver(ModuleRegistry registry) {
        this(registry, OptionalResolutionStrategy.INCLUDE_IF_REQUIRED);
    }

    /**
     * @param registry                   The registry to resolve modules from
     * @param optionalResolutionStrategy The strategy for handling optional dependencies
     */
    public DependencyResolver(ModuleRegistry registry, OptionalResolutionStrategy optionalResolutionStrategy) {
        this.registry = registry;
        this.optionalStrategy = optionalResolutionStrategy;
    }

    /**
     * @param rootModule        The first root module
     * @param additionalModules Any further root modules
     * @return A set of compatible modules based on the required modules.
     */
    public ResolutionResult resolve(Name rootModule, Name... additionalModules) {
        return builder().add(rootModule).addAll(additionalModules).build();
    }

    /**
     * @param moduleIds The set of module ids to build a set of compatible modules from
     * @return A set of compatible modules based on the required modules.
     */
    public ResolutionResult resolve(Iterable<Name> moduleIds) {
        return builder().addAll(moduleIds).build();
    }

    /**
     * @return A builder to resolve a set of compatible modules based on the required modules.
     */
    public ResolutionBuilder builder() {
        return new ResolutionBuilder();
    }

    public class ResolutionBuilder {
        private final Map<Name, Optional<VersionRange>> validVersions = new HashMap<>();

        public ResolutionBuilder add(Name moduleId) {
            validVersions.put(moduleId, Optional.empty());
            return this;
        }

        public ResolutionBuilder add(Name moduleId, Version version) {
            validVersions.put(moduleId, Optional.of(new VersionRange(version, version.getNextPatchVersion())));
            return this;
        }

        public ResolutionBuilder add(Name moduleId, VersionRange range) {
            validVersions.put(moduleId, Optional.of(range));
            return this;
        }

        public ResolutionBuilder addAll(Name[] moduleIds) {
            for (Name name : moduleIds) {
                validVersions.put(name, Optional.empty());
            }
            return this;
        }

        public ResolutionBuilder addAll(Iterable<Name> moduleIds) {
            Iterator<Name> iterator = moduleIds.iterator();
            while (iterator.hasNext()) {
                validVersions.put(iterator.next(), Optional.empty());
            }
            return this;
        }

        public ResolutionResult build() {
            ResolutionAttempt attempt = new ResolutionAttempt(registry, optionalStrategy);
            return attempt.resolve(validVersions);
        }

    }
}
