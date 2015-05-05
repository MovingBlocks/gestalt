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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import org.terasology.naming.Name;
import org.terasology.naming.Version;
import org.terasology.util.Varargs;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final ModuleRegistry registry;

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
        return resolve(true, rootModule, additionalModules);
    }

    /**
     * @param includeOptional   If optional modules should be included
     * @param rootModule        The first root module
     * @param additionalModules Any further root modules
     * @return A set of compatible modules based on the required modules.
     */
    public ResolutionResult resolve(boolean includeOptional, Name rootModule, Name... additionalModules) {
        return resolve(includeOptional, Varargs.combineToSet(rootModule, additionalModules));
    }

    /**
     * @param moduleIds The set of module ids to build a set of compatible modules from
     * @return A set of compatible modules based on the required modules.
     */
    public ResolutionResult resolve(Iterable<Name> moduleIds) {
        return resolve(true, moduleIds);
    }

    /**
     * @param includeOptional If optional modules should be included
     * @param moduleIds       The set of module ids to build a set of compatible modules from
     * @return A set of compatible modules based on the required modules.
     */
    public ResolutionResult resolve(boolean includeOptional, Iterable<Name> moduleIds) {
        Set<Name> rootModules = ImmutableSet.copyOf(moduleIds);
        Multimap<Name, Module> moduleVersionPool = populateDomains(rootModules);

        return generateResolutionResult(moduleVersionPool, moduleIds, includeOptional);
    }

    /**
     * This method generates the actual resolution result given the requiredModuleIds it has to resolve.
     * It uses breadth-first approach in traversing dependency graph, ignoring dependencies it is already following (by name).
     *
     * @param moduleVersionPool Pool of all available versions for all possible dependencies.
     * @param requiredModuleIds Modules that are required to be included in the result
     * @param includeOptional   If optional modules should be included.
     * @return Result of the dependency resolution.
     */
    private ResolutionResult generateResolutionResult(Multimap<Name, Module> moduleVersionPool, Iterable<Name> requiredModuleIds, boolean includeOptional) {
        // Create a set of latest versions of the specified required modules, we will downgrade them as needed
        // to fulfil the dependency requirements later on in the method
        Map<Name, Module> analyzedRequiredModuleSelection = constructLatestRequiredModuleSelection(moduleVersionPool, requiredModuleIds);

        // If any of the required modules are missing in the resolver, just return failure
        if (analyzedRequiredModuleSelection == null) {
            return new ResolutionResult(false, Collections.emptySet());
        }

        // Continue downgrading required module versions until we find a combination that matches our requirements or fail to find one
        while (true) {
            // Try to resolve all dependencies for currently analyzed required module version selection
            SumMap<Name, Module> matchingResult = findMatchingDependencyVersionGraph(moduleVersionPool, analyzedRequiredModuleSelection,
                    new SumMap<>(null, analyzedRequiredModuleSelection), includeOptional);
            if (matchingResult != null) {
                // It was able to successfully resolve all dependencies - return result
                Set<Module> resolvedModules = new HashSet<>();
                resolvedModules.addAll(Lists.newLinkedList(matchingResult.getValues()));
                return new ResolutionResult(true, resolvedModules);
            }

            // Create next possible required module version selection combination
            analyzedRequiredModuleSelection = constructNextPossibleRequiredModuleSelection(moduleVersionPool, requiredModuleIds, analyzedRequiredModuleSelection);
            if (analyzedRequiredModuleSelection == null) {
                // It was no longer able to create any new valid required module selection combination, so we have to return failure
                return new ResolutionResult(false, Collections.emptySet());
            }
        }
    }

    /**
     * Tries to recursively (breadth-first) go down the dependency tree and find a valid combination of module version dependencies.
     *
     * @param moduleVersionPool   Pool of all available versions for all possible dependencies.
     * @param lastDependencyLevel Last dependency level resolved with their versions. It is used to find next batch of dependencies.
     * @param alreadyMapped       Modules that have been already resolved (includes last level) in this breadth-first search.
     * @param includeOptional     If optional modules should be included.
     * @return Match found with all the modules and their versions, or <code>null</code> if match with current parameters was not possible.
     */
    private SumMap<Name, Module> findMatchingDependencyVersionGraph(Multimap<Name, Module> moduleVersionPool, Map<Name, Module> lastDependencyLevel,
                                                                    SumMap<Name, Module> alreadyMapped, boolean includeOptional) {
        // Gather non-conflicting constraints for current level of breadth-first search
        Map<Name, Constraint> constraintMap = constructConstraintMapForLevel(lastDependencyLevel.values(), alreadyMapped);
        if (constraintMap == null) {
            // No valid non-conflicting constraints has been found, finding match is not possible
            return null;
        }

        // Create a set of latest versions of the specified dependency modules, we will downgrade them as needed
        // to fulfil the dependency requirements later on in the method
        Map<Name, Module> analyzedDependentModuleSelection = constructLatestDependentModuleSelection(moduleVersionPool, includeOptional, constraintMap);
        if (analyzedDependentModuleSelection == null) {
            return null;
        }

        // If there is no dependencies to add, that means we have finished the breadth-first search, return the final result
        if (analyzedDependentModuleSelection.isEmpty()) {
            return alreadyMapped;
        }

        // Keep downgrading dependency module versions, until we find a combination that works or fail to find one
        while (true) {
            // First check if the current combination is valid at all, no reason to dig deeper in the dependency graph, if
            // we can cut the branch at this point
            SumMap<Name, Module> moduleSelection = new SumMap<>(alreadyMapped, analyzedDependentModuleSelection);
            if (isValidSelectionSoFar(moduleSelection)) {
                // Proceed to the next level of breadth-first search with current selection for this level (and all previous levels)
                SumMap<Name, Module> possibleResolvedGraph = findMatchingDependencyVersionGraph(moduleVersionPool, analyzedDependentModuleSelection,
                        moduleSelection, includeOptional);
                if (possibleResolvedGraph != null) {
                    // A match has been found - return the result
                    return possibleResolvedGraph;
                }
            }

            // We were not able to resolve with current selection, proceed to the next combination of dependencies for this level
            analyzedDependentModuleSelection = constructNextPossibleDependentModuleSelection(moduleVersionPool, includeOptional, constraintMap,
                    analyzedDependentModuleSelection);
            if (analyzedDependentModuleSelection == null) {
                // Such valid combination does not exist, so we need to fail this level
                return null;
            }
        }
    }

    /**
     * Validates that the specified module selection (so far, as some dependencies might not have been resolved yet)
     * is valid
     *
     * @param moduleSelection Module selection to check consistency of.
     * @return If the selection is valid (excluding missing dependencies)
     */
    private boolean isValidSelectionSoFar(SumMap<Name, Module> moduleSelection) {
        for (Module module : moduleSelection.getValues()) {
            for (DependencyInfo dependencyInfo : module.getMetadata().getDependencies()) {
                Name dependencyName = dependencyInfo.getId();
                if (moduleSelection.containsKey(dependencyName)) {
                    Module dependencyVersion = moduleSelection.get(dependencyName);
                    if (!isValid(dependencyInfo, dependencyVersion.getVersion())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Constructs next possible dependency version combination for current level based on the last one.
     * <p>
     * I'd like to use a metaphor to show how this method works:<br/>
     * Imagine you have a combination lock with a set of dials, one for each module, with each dial specifying valid module
     * versions of that module starting from latest all the way to eldest.<br/>
     * To find a next possible combination, you try to spin the first dial down by one, keeping rest of the dials intact.
     * However, if the dial goes back to its original position (latest version), you keep it at latest version and spin
     * the next dial (for next module), if the second one also ends up on its latest version, you continue, until you spin a dial
     * down on a module that does not reset it to its starting position.<br/>
     * If however, you moved through all the dials and all of them got reset to their starting position, that means you
     * went through all the combinations.
     *
     * @param moduleVersionPool            Pool of all available versions for all possible dependencies.
     * @param includeOptional              If optional modules should be included.
     * @param constraintMap                Constraints defined for this level of dependency resolution.
     * @param lastDependentModuleSelection Last analyzed module version selection.
     * @return Next valid combination of module versions, if possible, <code>null</code> is returned if no such combination exists
     */
    private Map<Name, Module> constructNextPossibleDependentModuleSelection(Multimap<Name, Module> moduleVersionPool, boolean includeOptional,
                                                                            Map<Name, Constraint> constraintMap, Map<Name, Module> lastDependentModuleSelection) {
        Map<Name, Module> result = Maps.newLinkedHashMap();
        // Flag if we already made change compared to last module selection
        boolean madeChange = false;
        for (Map.Entry<Name, Constraint> dependency : constraintMap.entrySet()) {
            // Exclude optional if not required
            if (includeOptional || !dependency.getValue().optional) {
                Name moduleId = dependency.getKey();
                // Get last version analyzed for this module name
                Module lastAnalyzedVersion = lastDependentModuleSelection.get(moduleId);
                if (madeChange) {
                    // If a change was already made, just copy the last one used
                    result.put(moduleId, lastAnalyzedVersion);
                } else {
                    Collection<Module> allowedVersions = moduleVersionPool.get(moduleId);
                    Module lowerVersion = findFirstMatchingLowerThan(allowedVersions, dependency.getValue(), lastAnalyzedVersion.getVersion());
                    if (lowerVersion == null) {
                        // If no lower version exists, reset the counter for this module to the start of the list (latest)
                        result.put(moduleId, findFirstMatching(allowedVersions, dependency.getValue()));
                    } else {
                        // Set the value for this module to the lower version and set the flag
                        result.put(moduleId, lowerVersion);
                        madeChange = true;
                    }
                }
            }
        }
        if (!madeChange) {
            return null;
        }
        return result;
    }

    /**
     * @param moduleVersionPool           Pool of all available versions for all possible dependencies.
     * @param requiredModuleIds           Modules that are required to be included in the result.
     * @param lastRequiredModuleSelection Last analyzed module version selection.
     * @return Next valid combination of module versions, if possible, <code>null</code> is returned if no such combination exists
     * @see #constructNextPossibleDependentModuleSelection
     */
    private Map<Name, Module> constructNextPossibleRequiredModuleSelection(Multimap<Name, Module> moduleVersionPool, Iterable<Name> requiredModuleIds,
                                                                           Map<Name, Module> lastRequiredModuleSelection) {
        Map<Name, Module> result = Maps.newLinkedHashMap();
        boolean madeChange = false;
        for (Name requiredModuleId : requiredModuleIds) {
            Module lastAnalyzedVersion = lastRequiredModuleSelection.get(requiredModuleId);
            if (madeChange) {
                result.put(requiredModuleId, lastAnalyzedVersion);
            } else {
                Collection<Module> allowedVersions = moduleVersionPool.get(requiredModuleId);
                Module lowerVersion = findFirstMatchingLowerThan(allowedVersions, null, lastAnalyzedVersion.getVersion());
                if (lowerVersion == null) {
                    result.put(requiredModuleId, allowedVersions.iterator().next());
                } else {
                    result.put(requiredModuleId, lowerVersion);
                    madeChange = true;
                }
            }
        }
        if (!madeChange) {
            return null;
        }
        return result;
    }

    /**
     * Constructs a combination of modules from the constraint map with each module set at the latest version
     *
     * @param moduleVersionPool Pool of all available versions for all possible dependencies.
     * @param includeOptional   If optional modules should be included.
     * @param constraintMap     Map with constraints for all modules that should be included (unless optional).
     * @return Combination of modules with highest possible version for each (within constraints).
     */
    private Map<Name, Module> constructLatestDependentModuleSelection(Multimap<Name, Module> moduleVersionPool, boolean includeOptional, Map<Name, Constraint> constraintMap) {
        Map<Name, Module> result = Maps.newLinkedHashMap();
        for (Map.Entry<Name, Constraint> dependency : constraintMap.entrySet()) {
            if (includeOptional || !dependency.getValue().optional) {
                Name dependencyName = dependency.getKey();
                Module firstMatching = findFirstMatching(moduleVersionPool.get(dependencyName), dependency.getValue());
                if (firstMatching == null) {
                    return null;
                }
                result.put(dependencyName, firstMatching);
            }
        }

        return result;
    }

    /**
     * Constructs a combination of modules from the required module collection, each module set at the latest version.
     *
     * @param moduleVersionPool Pool of all available versions for all possible dependencies.
     * @param requiredModuleIds Collection of modules to add to the combination.
     * @return Combination of modules with highest possible version for each (within constraints).
     */
    private Map<Name, Module> constructLatestRequiredModuleSelection(Multimap<Name, Module> moduleVersionPool, Iterable<Name> requiredModuleIds) {
        Map<Name, Module> analyzedRequiredModuleSelection = Maps.newLinkedHashMap();
        for (Name requiredModuleId : requiredModuleIds) {
            Collection<Module> allowedVersions = moduleVersionPool.get(requiredModuleId);
            if (allowedVersions.isEmpty()) {
                return null;
            }
            analyzedRequiredModuleSelection.put(requiredModuleId, allowedVersions.iterator().next());
        }
        return analyzedRequiredModuleSelection;
    }

    /**
     * Generates a constraint map for the dependencies of the specified modules.
     *
     * @param modules       Modules to analyze dependencies of.
     * @param alreadyMapped Modules that have already been mapped, so no longer need to be included, however if the already
     *                      mapped version is conflicting with the dependency, this constraint map construction should fail.
     * @return Map of constraints for the level of dependencies, or <code>null</code> if there is no valid constraints possible.
     */
    private Map<Name, Constraint> constructConstraintMapForLevel(Collection<Module> modules, SumMap<Name, Module> alreadyMapped) {
        Map<Name, Constraint> missingDirectDependencies = new HashMap<>();
        for (Module module : modules) {
            List<DependencyInfo> moduleDependencies = module.getMetadata().getDependencies();
            for (DependencyInfo moduleDependency : moduleDependencies) {
                Name dependencyId = moduleDependency.getId();
                if (alreadyMapped.containsKey(dependencyId)) {
                    Module usedVersion = alreadyMapped.get(dependencyId);
                    if (!isValid(moduleDependency, usedVersion.getVersion())) {
                        return null;
                    }
                } else if (missingDirectDependencies.containsKey(dependencyId)) {
                    Constraint currentConstraint = missingDirectDependencies.get(dependencyId);
                    Constraint dependencyConstraint = new Constraint(moduleDependency.getMinVersion(), moduleDependency.getMaxVersion(), moduleDependency.isOptional());
                    Constraint resultConstraint = currentConstraint.createValidIntersection(dependencyConstraint);
                    if (resultConstraint == null) {
                        return null;
                    }
                    missingDirectDependencies.put(dependencyId, resultConstraint);
                } else {
                    missingDirectDependencies.put(dependencyId, new Constraint(moduleDependency.getMinVersion(), moduleDependency.getMaxVersion(),
                            moduleDependency.isOptional()));
                }
            }
        }
        return missingDirectDependencies;
    }

    /**
     * If the specified version is valid for this module dependency.
     *
     * @param moduleDependency Module dependency to check.
     * @param version          Version to check.
     * @return If is valid.
     */
    private boolean isValid(DependencyInfo moduleDependency, Version version) {
        return moduleDependency.getMinVersion().compareTo(version) <= 0
                && moduleDependency.getMaxVersion().compareTo(version) > 0;
    }

    /**
     * Finds first module version that matches the constraint and is lower than the specified version.
     *
     * @param modules    Module versions to check.
     * @param constraint Constraint to match against.
     * @param version    Version to compare to.
     * @return First module that matches the constraint and is lower than the specified version or <code>null</code> if not found.
     */
    private Module findFirstMatchingLowerThan(Collection<Module> modules, Constraint constraint, Version version) {
        for (Module module : modules) {
            if (moduleMatchesConstraint(constraint, module)
                    && version.compareTo(module.getVersion()) > 0) {
                return module;
            }
        }
        return null;
    }

    /**
     * Checks if module matches the constraint (or constraint is null).
     * @param constraint Constraint to check.
     * @param module     Module to check against constraint.
     * @return If constraint is null or it matches the module version.
     */
    private boolean moduleMatchesConstraint(Constraint constraint, Module module) {
        return constraint == null
                || (constraint.minVersion.compareTo(module.getVersion()) <= 0
                && constraint.maxVersion.compareTo(module.getVersion()) > 0);
    }

    /**
     * Finds first module version that matches the constraint.
     * @param modules    Module to check against the constraint.
     * @param constraint Constraint to check.
     * @return First module that matches the constraint.
     */
    private Module findFirstMatching(Collection<Module> modules, Constraint constraint) {
        for (Module module : modules) {
            if (constraint.minVersion.compareTo(module.getVersion()) <= 0
                    && constraint.maxVersion.compareTo(module.getVersion()) > 0) {
                return module;
            }
        }
        return null;
    }

    /**
     * Populates the domains (modules of interest) for resolution. Includes all versions of all modules depended on by any version of a module of interest, recursively.
     */
    private SortedSetMultimap<Name, Module> populateDomains(Set<Name> rootModules) {
        // Create a sorted multimap, where versions are sorted from the latest to the oldest.
        SortedSetMultimap<Name, Module> result = TreeMultimap.create(Comparator.naturalOrder(),
                (o1, o2) -> o2.getVersion().compareTo(o1.getVersion()));

        Set<Name> involvedModules = Sets.newHashSet();
        Deque<Name> moduleQueue = Queues.newArrayDeque();
        for (Name rootModule : rootModules) {
            involvedModules.add(rootModule);
            moduleQueue.push(rootModule);
        }

        while (!moduleQueue.isEmpty()) {
            Name id = moduleQueue.pop();
            for (Module version : registry.getModuleVersions(id)) {
                result.put(id, version);
                for (DependencyInfo dependency : version.getMetadata().getDependencies()) {
                    if (involvedModules.add(dependency.getId())) {
                        moduleQueue.push(dependency.getId());
                    }
                }
            }
        }

        return result;
    }

    private final class Constraint {
        private Version minVersion;
        private Version maxVersion;
        private boolean optional;

        private Constraint(Version minVersion, Version maxVersion, boolean optional) {
            this.minVersion = minVersion;
            this.maxVersion = maxVersion;
            this.optional = optional;
        }

        private Constraint createValidIntersection(Constraint anotherConstraint) {
            Version newMinVersion = (anotherConstraint.minVersion.compareTo(minVersion) > 0) ? anotherConstraint.minVersion : minVersion;
            Version newMaxVersion = (anotherConstraint.maxVersion.compareTo(maxVersion) < 0) ? anotherConstraint.maxVersion : maxVersion;

            // Check if the ranges are not overlapping at all
            if (newMinVersion.compareTo(newMaxVersion) >= 0) {
                return null;
            } else {
                return new Constraint(newMinVersion, newMaxVersion, optional & anotherConstraint.optional);
            }
        }
    }

    /**
     * Primitive Map-sort-of (read-only) that allows to aggregate values of another SumMap with an actual Map implementation.
     * @param <K> Key type of the Map-sort-of
     * @param <V> Value type of the Map-sort-of
     */
    private final class SumMap<K, V> {
        private SumMap<K, V> previousMap;
        private Map<K, V> newMap;

        private SumMap(SumMap<K, V> previousMap, Map<K, V> newMap) {
            this.previousMap = previousMap;
            this.newMap = newMap;
        }

        public V get(K key) {
            V value = newMap.get(key);
            if (value != null) {
                return value;
            } else if (previousMap != null) {
                return previousMap.get(key);
            } else {
                return null;
            }
        }

        public boolean containsKey(K key) {
            return newMap.containsKey(key) || (previousMap != null && previousMap.containsKey(key));
        }

        public Iterable<K> getKeys() {
            if (previousMap != null) {
                return Iterables.concat(previousMap.getKeys(), newMap.keySet());
            } else {
                return newMap.keySet();
            }
        }

        public Iterable<V> getValues() {
            if (previousMap != null) {
                return Iterables.concat(previousMap.getValues(), newMap.values());
            } else {
                return newMap.values();
            }
        }
    }
}
