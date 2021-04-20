// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.module.dependencyresolution;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import org.terasology.gestalt.module.Module;
import org.terasology.gestalt.module.ModuleRegistry;
import org.terasology.gestalt.naming.Name;
import org.terasology.gestalt.naming.Version;
import org.terasology.gestalt.util.collection.UniqueQueue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class ResolutionAttempt {
    private final OptionalResolutionStrategy optionalStrategy;
    private final ModuleRegistry registry;

    private Set<Name> rootModules;
    private SetMultimap<Name, PossibleVersion> moduleVersionPool;
    private ListMultimap<Name, Constraint> constraints;
    private UniqueQueue<Constraint> constraintQueue;

    ResolutionAttempt(ModuleRegistry registry, OptionalResolutionStrategy optionalStrategy) {
        this.registry = registry;
        this.optionalStrategy = optionalStrategy;
    }

    ResolutionResult resolve(Map<Name, Optional<Predicate<Version>>> validVersions) {
        rootModules = ImmutableSet.copyOf(validVersions.keySet());
        populateDomains(validVersions);
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
    private void populateDomains(Map<Name, Optional<Predicate<Version>>> validVersions) {
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
                Optional<Predicate<Version>> range = validVersions.getOrDefault(version.getId(), Optional.empty());
                if (!range.isPresent() || range.get().test(version.getVersion())) {
                    moduleVersionPool.put(id, new PossibleVersion(version.getVersion()));
                    for (DependencyInfo dependency : version.getMetadata().getDependencies()) {
                        if (involvedModules.add(dependency.getId())) {
                            moduleQueue.push(dependency.getId());
                            moduleVersionPool.put(dependency.getId(), PossibleVersion.OPTIONAL_VERSION);
                        }
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
                dependencies.addAll(module.getMetadata().getDependencies().stream().map(DependencyInfo::getId).collect(Collectors.toList()));
            }

            for (Name dependency : dependencies) {
                Map<Version, CompatibleVersions> constraintTable = Maps.newHashMapWithExpectedSize(moduleVersionPool.get(name).size());
                for (PossibleVersion version : moduleVersionPool.get(name)) {
                    if (version.getVersion().isPresent()) {
                        Module versionedModule = registry.getModule(name, version.getVersion().get());
                        DependencyInfo info = versionedModule.getMetadata().getDependencyInfo(dependency);
                        if (info != null) {
                            constraintTable.put(version.getVersion().get(), new CompatibleVersions(info.versionPredicate(), info.isOptional() && !optionalStrategy.isRequired()));
                        }
                    }
                }
                Constraint constraint = new VersionConstraint(name, dependency, constraintTable);
                constraints.put(name, constraint);
                constraints.put(dependency, constraint);
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
                for (Constraint relatedConstraint : constraints.get(constraint.getTo())) {
                    if (!Objects.equals(relatedConstraint, constraint)) {
                        constraintQueue.add(relatedConstraint);
                    }
                }
            }

            if (applyConstraintToDependant(constraint)) {
                for (Constraint relatedConstraint : constraints.get(constraint.getFrom())) {
                    constraintQueue.add(relatedConstraint);
                }
            }
        }
    }

    /**
     * Applies a constraint on dependencies based on the available versions of the dependant. A dependency version is removed if there is
     * no dependant that it is compatible with.
     * <p>
     * Example: if core-1.0.0 depends on child [1.0.0-2.0.0), then child-3.0.0 will be removed unless there is either another version of core that it is compatible with,
     * or a version of core with no dependency on it at all.
     * </p>
     *
     * @param constraint The constraint to process
     * @return Whether a change was applied the "to" domain of the constraint.
     */
    private boolean applyConstraintToDependency(Constraint constraint) {
        return constraint.constrainTo(Collections.unmodifiableSet(moduleVersionPool.get(constraint.getFrom())), moduleVersionPool.get(constraint.getTo()));
    }

    /**
     * Applies a constraint on a dependant based on available versions of the dependencies. A dependant version is removed if there is no compatible dependency version
     * available to support it.
     *
     * @param constraint The constraint to process
     * @return Whether a change was applied to the "from" domain of the constraint
     */
    private boolean applyConstraintToDependant(Constraint constraint) {
        return constraint.constrainFrom(moduleVersionPool.get(constraint.getFrom()), Collections.unmodifiableSet(moduleVersionPool.get(constraint.getTo())));
    }

    /**
     * Taking the already constrained moduleVersionPool, works through the remaining possibilities restricting down to the latest possible versions.
     * <p>
     * Root modules are restricted first and in order, to keep their versions as recent as possible.
     * Dependencies are then followed, restricted them to latest as needed.
     * As dependencies are followed, any modules that aren't required by the finally selected versions will not be present in the final result.
     * </p>
     *
     * @return The final set of compatible modules.
     */
    private Set<Module> finaliseModules() {
        Set<Module> finalModuleSet = Sets.newLinkedHashSetWithExpectedSize(moduleVersionPool.keySet().size());
        Deque<Module> moduleQueue = Queues.newArrayDeque();
        for (Name rootModule : rootModules) {
            Version latestVersion = reduceToFinalVersion(rootModule, true).get();
            Module module = registry.getModule(rootModule, latestVersion);
            finalModuleSet.add(module);
            moduleQueue.push(module);
        }

        while (!moduleQueue.isEmpty()) {
            Module module = moduleQueue.pop();
            for (DependencyInfo dependency : module.getMetadata().getDependencies()) {
                Optional<Version> latestVersion = reduceToFinalVersion(dependency.getId(), optionalStrategy.isDesired());
                if (latestVersion.isPresent()) {
                    Module dependencyModule = registry.getModule(dependency.getId(), latestVersion.get());
                    if (finalModuleSet.add(dependencyModule)) {
                        moduleQueue.push(dependencyModule);
                    }
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
    private Optional<Version> reduceToFinalVersion(Name module, boolean includeIfOptional) {
        switch (moduleVersionPool.get(module).size()) {
            case 0:
                return Optional.empty();
            case 1:
                return moduleVersionPool.get(module).iterator().next().getVersion();
            default:
                PossibleVersion version;
                if (!includeIfOptional && moduleVersionPool.get(module).contains(PossibleVersion.OPTIONAL_VERSION)) {
                    version = PossibleVersion.OPTIONAL_VERSION;
                } else {
                    List<PossibleVersion> versions = Lists.newArrayList(moduleVersionPool.get(module));
                    Collections.sort(versions);
                    version = versions.get(versions.size() - 1);
                }
                moduleVersionPool.replaceValues(module, Arrays.asList(version));
                constraintQueue.addAll(constraints.get(module));
                processConstraints();
                return version.getVersion();
        }
    }

    /**
     * Describes a constraint, in the form of a mapping of Versions of the "from" module to allowed ranges of the "to" modules.
     */
    private interface Constraint {

        Name getFrom();

        Name getTo();

        boolean constrainFrom(Set<PossibleVersion> fromVersions, Set<PossibleVersion> toVersions);

        boolean constrainTo(Set<PossibleVersion> fromVersions, Set<PossibleVersion> toVersions);

    }

    /**
     * Describes a constraint, in the form of a mapping of Versions of the "from" module to allowed ranges of the "to" modules.
     */
    private static final class VersionConstraint implements Constraint {
        private final Name from;
        private final Name to;
        private final Map<Version, CompatibleVersions> versionCompatibilities;

        private VersionConstraint(Name from, Name to, Map<Version, CompatibleVersions> versionCompatibilities) {
            this.from = from;
            this.to = to;
            this.versionCompatibilities = versionCompatibilities;
        }

        @Override
        public Name getFrom() {
            return from;
        }

        @Override
        public Name getTo() {
            return to;
        }

        @Override
        public boolean constrainFrom(Set<PossibleVersion> fromVersions, Set<PossibleVersion> toVersions) {
            boolean changed = false;
            Iterator<PossibleVersion> validVersions = fromVersions.iterator();
            while (validVersions.hasNext()) {
                PossibleVersion version = validVersions.next();
                if (version.getVersion().isPresent()) {
                    CompatibleVersions compatibility = versionCompatibilities.get(version.getVersion().get());
                    if (compatibility != null) {
                        boolean valid = false;
                        for (PossibleVersion dependencyVersion : toVersions) {
                            if (compatibility.isCompatible(dependencyVersion)) {
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
            }
            return changed;
        }

        @Override
        public boolean constrainTo(Set<PossibleVersion> fromVersions, Set<PossibleVersion> toVersions) {
            boolean changed = false;
            Iterator<PossibleVersion> dependencyVersions = toVersions.iterator();
            while (dependencyVersions.hasNext()) {
                PossibleVersion dependencyVersion = dependencyVersions.next();
                boolean valid = false;
                for (PossibleVersion version : fromVersions) {
                    if (version.getVersion().isPresent()) {
                        CompatibleVersions compatibility = versionCompatibilities.get(version.getVersion().get());
                        if (compatibility == null || compatibility.isCompatible(dependencyVersion)) {
                            valid = true;
                            break;
                        }
                    } else {
                        valid = true;
                    }
                }
                if (!valid) {
                    dependencyVersions.remove();
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof VersionConstraint) {
                VersionConstraint other = (VersionConstraint) obj;
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

    private static class CompatibleVersions {
        private final Predicate<Version> versionRange;
        private final boolean missingAllowed;

        public CompatibleVersions(Predicate<Version> versionRange, boolean missingAllowed) {
            this.versionRange = versionRange;
            this.missingAllowed = missingAllowed;
        }

        public boolean isCompatible(PossibleVersion version) {
            if (version.getVersion().isPresent()) {
                return versionRange.test(version.getVersion().get());
            } else {
                return missingAllowed;
            }
        }
    }

    private static class PossibleVersion implements Comparable<PossibleVersion> {
        public static final PossibleVersion OPTIONAL_VERSION = new PossibleVersion();
        private final Optional<Version> version;

        private PossibleVersion() {
            version = Optional.empty();
        }

        public PossibleVersion(Version version) {
            this.version = Optional.of(version);
        }

        public Optional<Version> getVersion() {
            return version;
        }

        @Override
        public int compareTo(PossibleVersion o) {
            if (!version.isPresent()) {
                if (o.version.isPresent()) {
                    return -1;
                }
                return 0;
            } else {
                if (o.version.isPresent()) {
                    return version.get().compareTo(o.version.get());
                }
                return 1;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof PossibleVersion) {
                return compareTo((PossibleVersion) obj) == 0;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(version);
        }
    }

}
