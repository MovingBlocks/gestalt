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

package org.terasology.module;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.terasology.naming.Name;
import org.terasology.naming.Version;
import org.terasology.naming.VersionRange;
import org.terasology.util.Varargs;
import org.terasology.util.collection.UniqueQueue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Dependency Resolver determines a working set of modules for a given set of desired modules. Where multiple versions are compatible, they are resolved in favour of the
 * latest available. In particular, the latest version of the desired modules is prioritised, and in the order requested (so if using the latest version of the first
 * desired module prevents the use of latest version of the second desired module, then that is what will happen).
 * <p/>
 * The algorithm used is based on Arc Consistency Algorithm #3.
 *
 * @author Immortius
 */
public class DependencyResolver {
    private final ModuleRegistry registry;
    private Set<Name> rootModules;
    private SetMultimap<Name, Version> moduleVersionPool;
    private ListMultimap<Name, Constraint> constraints;
    private UniqueQueue<Constraint> constraintQueue;

    /**
     * @param registry The registry to resolve modules from
     */
    public DependencyResolver(ModuleRegistry registry) {
        this.registry = registry;
    }

    /**
     * @param rootModule        The first root module
     * @param additionalModules Any further root modules
     * @return A set of compatible modules based on the required modules.
     */
    public ResolutionResult resolve(Name rootModule, Name... additionalModules) {
        return resolve(Varargs.combineToSet(rootModule, additionalModules));
    }

    /**
     * @param moduleIds The set of module ids to build a set of compatible modules from
     * @return A set of compatible modules based on the required modules.
     */
    public ResolutionResult resolve(Iterable<Name> moduleIds) {
        rootModules = ImmutableSet.copyOf(moduleIds);
        populateDomains();
        populateConstraints();
        if (!includesModules(rootModules)) {
            return new ResolutionResult(false, Collections.<Module>emptySet());
        }

        constraintQueue = UniqueQueue.createWithExpectedSize(constraints.size());
        constraintQueue.addAll(constraints.values());
        processConstraints();

        if (!includesModules(rootModules)) {
            return new ResolutionResult(false, Collections.<Module>emptySet());
        }

        return new ResolutionResult(true, finaliseModules());
    }

    /**
     * Populates the domains (modules of interest) for resolution. Includes all versions of all modules depended on by any version of a module of interest, recursively.
     */
    private void populateDomains() {
        moduleVersionPool = HashMultimap.create();
        Set<Name> involvedModules = Sets.newHashSet();
        Deque<Name> moduleQueue = Queues.newArrayDeque();
        for (Name rootModule : rootModules) {
            involvedModules.add(rootModule);
            moduleQueue.push(rootModule);
        }

        while (!moduleQueue.isEmpty()) {
            Name id = moduleQueue.pop();
            for (Module version : registry.getModuleVersions(id)) {
                moduleVersionPool.put(id, version.getVersion());
                for (DependencyInfo dependency : version.getMetadata().getDependencies()) {
                    if (involvedModules.add(dependency.getId())) {
                        moduleQueue.push(dependency.getId());
                    }
                }
            }
        }
    }

    /**
     * Populates the constraints between the domains. For each module, any dependency that at least one version of the module has becomes a constraint
     * between the two, with a mapping of version to version-range.
     */
    private void populateConstraints() {
        constraints = ArrayListMultimap.create();
        for (Name name : moduleVersionPool.keySet()) {
            Set<Name> dependencies = Sets.newLinkedHashSet();
            for (Module module : registry.getModuleVersions(name)) {
                for (DependencyInfo dependency : module.getMetadata().getDependencies()) {
                    dependencies.add(dependency.getId());
                }
            }

            for (Name dependency : dependencies) {
                Map<Version, VersionRange> constraintTable = Maps.newHashMapWithExpectedSize(moduleVersionPool.get(name).size());
                for (Version version : moduleVersionPool.get(name)) {
                    Module versionedModule = registry.getModule(name, version);
                    DependencyInfo info = versionedModule.getMetadata().getDependencyInfo(dependency);
                    if (info != null) {
                        constraintTable.put(version, info.versionRange());
                    }
                }
                constraints.put(name, new Constraint(name, dependency, constraintTable));
                constraints.put(dependency, new Constraint(name, dependency, constraintTable));
            }
        }
    }

    private boolean includesModules(Set<Name> modules) {
        for (Name module : modules) {
            if (moduleVersionPool.get(module).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Processes queued constraints, until the queue is exhausted.
     */
    private void processConstraints() {
        while (!constraintQueue.isEmpty() && includesModules(rootModules)) {
            Constraint constraint = constraintQueue.remove();

            if (applyConstraintToDependency(constraint)) {
                for (Constraint relatedConstraint : constraints.get(constraint.to)) {
                    if (!Objects.equals(relatedConstraint, constraint)) {
                        constraintQueue.add(relatedConstraint);
                    }
                }
            }

            if (applyConstraintToDependant(constraint)) {
                for (Constraint relatedConstraint : constraints.get(constraint.from)) {
                    constraintQueue.add(relatedConstraint);
                }
            }
        }
    }

    /**
     * Applies a constraint on dependencies based on the available versions of the dependant. A dependency version is removed if there is
     * no dependant that it is compatible with.
     * <p/>
     * Example: if core-1.0.0 depends on child [1.0.0-2.0.0), then child-3.0.0 will be removed unless there is either another version of core that it is compatible with,
     * or a version of core with no dependency on it at all.
     *
     * @param constraint The constraint to process
     * @return Whether a change was applied the "to" domain of the constraint.
     */
    private boolean applyConstraintToDependency(Constraint constraint) {
        boolean changed = false;
        Iterator<Version> dependencyVersions = moduleVersionPool.get(constraint.to).iterator();
        while (dependencyVersions.hasNext()) {
            Version dependencyVersion = dependencyVersions.next();
            boolean valid = false;
            for (Version version : moduleVersionPool.get(constraint.from)) {
                VersionRange versionRange = constraint.getVersionCompatibilities().get(version);
                if (versionRange == null || versionRange.contains(dependencyVersion)) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                dependencyVersions.remove();
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Applies a constraint on a dependant based on available versions of the dependencies. A dependant version is removed if there is no compatible dependency version
     * available to support it.
     *
     * @param constraint The constraint to process
     * @return Whether a change was applied to the "from" domain of the constraint
     */
    private boolean applyConstraintToDependant(Constraint constraint) {
        boolean changed = false;
        Iterator<Version> validVersions = moduleVersionPool.get(constraint.from).iterator();
        while (validVersions.hasNext()) {
            Version version = validVersions.next();
            VersionRange versionRange = constraint.getVersionCompatibilities().get(version);
            if (versionRange != null) {
                boolean valid = false;
                for (Version dependencyVersion : moduleVersionPool.get(constraint.to)) {
                    if (versionRange.contains(dependencyVersion)) {
                        valid = true;
                        break;
                    }
                }
                if (!valid) {
                    validVersions.remove();
                    changed = true;
                }
            }
        }
        return changed;
    }

    /**
     * Taking the already constrained moduleVersionPool, works through the remaining possibilities restricting down to the latest possible versions.
     * <p/>
     * Root modules are restricted first and in order, to keep their versions as recent as possible.
     * Dependencies are then followed, restricted them to latest as needed.
     * As dependencies are followed, any modules that aren't required by the finally selected versions will not be present in the final result.
     *
     * @return The final set of compatible modules.
     */
    private Set<Module> finaliseModules() {
        Set<Module> finalModuleSet = Sets.newLinkedHashSetWithExpectedSize(moduleVersionPool.keySet().size());
        Deque<Module> moduleQueue = Queues.newArrayDeque();
        for (Name rootModule : rootModules) {
            Module module = registry.getModule(rootModule, reduceToLatestVersion(rootModule));
            finalModuleSet.add(module);
            moduleQueue.push(module);
        }

        while (!moduleQueue.isEmpty()) {
            Module module = moduleQueue.pop();
            for (DependencyInfo dependency : module.getMetadata().getDependencies()) {
                Module dependencyModule = registry.getModule(dependency.getId(), reduceToLatestVersion(dependency.getId()));
                if (finalModuleSet.add(dependencyModule)) {
                    moduleQueue.push(dependencyModule);
                }
            }
        }
        return finalModuleSet;
    }

    /**
     * Reduces the available versions of the given module to just the latest remaining version,
     * and then processes constraints affected by this reduction. Should only be called of there is at least
     * one version available.
     *
     * @param module The module to limit to the latest version
     * @return The latest version of the module.
     */
    private Version reduceToLatestVersion(Name module) {
        if (moduleVersionPool.get(module).size() > 1) {
            Iterator<Version> versions = moduleVersionPool.get(module).iterator();
            Version latest = versions.next();
            while (versions.hasNext()) {
                Version version = versions.next();
                if (version.compareTo(latest) > 0) {
                    latest = version;
                }
            }
            moduleVersionPool.replaceValues(module, Arrays.asList(latest));
            constraintQueue.addAll(constraints.get(module));
            processConstraints();
            return latest;
        } else {
            Iterator<Version> iterator = moduleVersionPool.get(module).iterator();
            assert iterator.hasNext();
            return iterator.next();
        }
    }

    /**
     * Describes a constraint, in the form of a mapping of Versions of the "from" module to allowed ranges of the "to" modules.
     */
    private static final class Constraint {
        private final Name from;
        private final Name to;
        private final Map<Version, VersionRange> versionCompatibilities;

        private Constraint(Name from, Name to, Map<Version, VersionRange> versionCompatibilities) {
            this.from = from;
            this.to = to;
            this.versionCompatibilities = versionCompatibilities;
        }

        public Map<Version, VersionRange> getVersionCompatibilities() {
            return versionCompatibilities;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Constraint) {
                Constraint other = (Constraint) obj;
                return Objects.equals(from, other.from) && Objects.equals(to, other.to);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }

        @Override
        public String toString() {
            return from + "==>" + to;
        }
    }

}
