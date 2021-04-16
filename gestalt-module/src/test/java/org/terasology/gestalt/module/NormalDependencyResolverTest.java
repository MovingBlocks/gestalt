// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.module;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.terasology.gestalt.module.dependencyresolution.DependencyResolver;
import org.terasology.gestalt.module.dependencyresolution.ResolutionResult;
import org.terasology.gestalt.naming.Name;
import org.terasology.gestalt.naming.Version;
import org.terasology.gestalt.naming.VersionRange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Immortius
 */
public class NormalDependencyResolverTest extends DependencyResolverTestBase {

    @Test
    public void singleModuleResolution() {
        ModuleRegistry registry = new TableModuleRegistry();
        Module core = createStubModule(registry, "core", "1.0.0");

        DependencyResolver resolver = new DependencyResolver(registry);
        ResolutionResult results = resolver.resolve(new Name("core"));
        assertTrue(results.isSuccess());
        assertEquals(Sets.newHashSet(core), results.getModules());
    }

    @Test
    public void missingRootModule() {
        ModuleRegistry registry = new TableModuleRegistry();

        DependencyResolver resolver = new DependencyResolver(registry);
        ResolutionResult results = resolver.resolve(new Name("core"));
        assertFalse(results.isSuccess());
    }

    @Test
    public void missingDependency() {
        ModuleRegistry registry = new TableModuleRegistry();
        Module core = createStubModule(registry, "core", "1.0.0");
        addDependency(core, "base");

        DependencyResolver resolver = new DependencyResolver(registry);
        ResolutionResult results = resolver.resolve(new Name("core"));
        assertFalse(results.isSuccess());
    }

    @Test
    public void wrongVersionDependency() {
        ModuleRegistry registry = new TableModuleRegistry();
        Module core = createStubModule(registry, "core", "1.0.0");
        addDependency(core, "base", "1.0.0", "2.0.0");
        registry.add(createStubModule(registry, "base", "2.0.0"));

        DependencyResolver resolver = new DependencyResolver(registry);
        ResolutionResult results = resolver.resolve(new Name("core"));
        assertFalse(results.isSuccess());
    }

    @Test
    public void multiplePossibilitiesShouldUseLatest() {
        ModuleRegistry registry = new TableModuleRegistry();
        Module latestCore = createStubModule(registry, "core", "2.0.0");
        registry.add(createStubModule(registry, "core", "1.0.0"));

        DependencyResolver resolver = new DependencyResolver(registry);
        ResolutionResult results = resolver.resolve(new Name("core"));
        assertTrue(results.isSuccess());
        assertEquals(Sets.newHashSet(latestCore), results.getModules());
    }

    @Test
    public void selectFromOptionsWithDependenciesSatisfied() {
        ModuleRegistry registry = new TableModuleRegistry();
        Module coreV3 = createStubModule(registry, "core", "3.0.0");
        Module coreV2 = createStubModule(registry, "core", "2.0.0");
        Module coreV1 = createStubModule(registry, "core", "1.0.0");
        Module availableDependency = createStubModule(registry, "depends", "1.0.0");
        addDependency(coreV3, "depends", "2.0.0", "3.0.0");
        addDependency(coreV2, "depends", "1.0.0", "2.0.0");
        addDependency(coreV1, "depends", "0.1.0", "1.0.0");

        DependencyResolver resolver = new DependencyResolver(registry);
        ResolutionResult results = resolver.resolve(new Name("core"));
        assertTrue(results.isSuccess());
        assertEquals(Sets.newHashSet(coreV2, availableDependency), results.getModules());
    }

    @Test
    public void invalidCrossModuleDependency() {
        ModuleRegistry registry = new TableModuleRegistry();
        Module core = createStubModule(registry, "core", "1.0.0");
        Module moduleA = createStubModule(registry, "a", "1.0.0");
        Module moduleB = createStubModule(registry, "b", "1.0.0");
        createStubModule(registry, "c", "1.0.0");
        addDependency(core, "a");
        addDependency(core, "b");
        addDependency(moduleA, "c");
        addDependency(moduleB, "c", "2.0.0", "3.0.0");

        DependencyResolver resolver = new DependencyResolver(registry);
        assertFalse(resolver.resolve(new Name("core")).isSuccess());
    }

    @Test
    public void shouldNotInvalidateResolvingIfVersionNotUsed() {
        ModuleRegistry registry = new TableModuleRegistry();
        Module core = createStubModule(registry, "core", "1.0.0");
        Module moduleA1 = createStubModule(registry, "a", "1.0.0");
        Module moduleA2 = createStubModule(registry, "a", "2.0.0");

        addDependency(core, "a", "1.0.0", "3.0.0");
        addDependency(moduleA1, "c");

        DependencyResolver resolver = new DependencyResolver(registry);
        ResolutionResult results = resolver.resolve(new Name("core"));
        assertTrue(results.isSuccess());
        assertEquals(Sets.newHashSet(core, moduleA2), results.getModules());
    }

    @Test
    public void optionalNotRequired() {
        ModuleRegistry registry = new TableModuleRegistry();
        Module core = createStubModule(registry, "core", "1.0.0");
        Module moduleA = createStubModule(registry, "a", "1.0.0");

        addDependency(core, "a");
        addDependency(core, "b", true);

        DependencyResolver resolver = new DependencyResolver(registry);
        ResolutionResult results = resolver.resolve(new Name("core"));
        assertTrue(results.isSuccess());
        assertEquals(Sets.newHashSet(core, moduleA), results.getModules());
    }

    @Test
    public void optionalNotUsedIfAvailableButNotRequired() {
        ModuleRegistry registry = new TableModuleRegistry();
        Module core = createStubModule(registry, "core", "1.0.0");
        Module moduleA = createStubModule(registry, "a", "1.0.0");
        createStubModule(registry, "b", "1.0.0");

        addDependency(core, "a");
        addDependency(core, "b", true);

        DependencyResolver resolver = new DependencyResolver(registry);
        ResolutionResult results = resolver.resolve(new Name("core"));
        assertTrue(results.isSuccess());
        assertEquals(Sets.newHashSet(core, moduleA), results.getModules());
    }

    @Test
    public void optionalVersionConstraintsAppliedWhenIncluded() {
        ModuleRegistry registry = new TableModuleRegistry();
        Module core = createStubModule(registry, "core", "1.0.0");
        Module moduleA = createStubModule(registry, "a", "1.0.0");
        Module moduleB = createStubModule(registry, "b", "1.0.0");
        createStubModule(registry, "b", "2.0.0");

        addDependency(core, "a");
        addDependency(core, "b", "1.0.0", "3.0.0");
        addDependency(moduleA, "b", "1.0.0", "2.0.0", true);

        DependencyResolver resolver = new DependencyResolver(registry);
        ResolutionResult results = resolver.resolve(new Name("core"));
        assertTrue(results.isSuccess());
        assertEquals(Sets.newHashSet(core, moduleA, moduleB), results.getModules());
    }

    @Test
    public void optionalVersionConstraintsNotAppliedWhenNotIncluded() {
        ModuleRegistry registry = new TableModuleRegistry();
        Module core = createStubModule(registry, "core", "1.0.0");
        Module moduleA = createStubModule(registry, "a", "1.0.0");
        createStubModule(registry, "b", "1.0.0");
        Module moduleC = createStubModule(registry, "c", "1.0.0");
        Module moduleBLatest = createStubModule(registry, "b", "2.0.0");

        addDependency(core, "a");
        addDependency(core, "b", "1.0.0", "3.0.0");
        addDependency(core, "c", true);
        addDependency(moduleC, "b", "1.0.0", "2.0.0");

        DependencyResolver resolver = new DependencyResolver(registry);
        ResolutionResult results = resolver.resolve(new Name("core"));
        assertTrue(results.isSuccess());
        assertEquals(Sets.newHashSet(core, moduleA, moduleBLatest), results.getModules());
    }

    @Test
    public void optionalConstraintAppliedIfUsed() {
        ModuleRegistry registry = new TableModuleRegistry();
        Module core = createStubModule(registry, "core", "1.0.0");
        Module moduleA = createStubModule(registry, "a", "1.0.0");
        Module moduleB = createStubModule(registry, "b", "1.0.0");
        Module moduleC = createStubModule(registry, "c", "1.0.0");
        createStubModule(registry, "c", "2.0.0");

        addDependency(core, "a");
        addDependency(core, "b");
        addDependency(moduleA, "c", "1.0.0", "3.0.0");
        addDependency(moduleB, "c", "1.0.0", "2.0.0", true);

        DependencyResolver resolver = new DependencyResolver(registry);
        ResolutionResult results = resolver.resolve(new Name("core"));
        assertTrue(results.isSuccess());
        assertEquals(Sets.newHashSet(core, moduleA, moduleB, moduleC), results.getModules());
    }

    @Test
    public void optionalConstraintCanCauseFailuresDueToConflict() {
        ModuleRegistry registry = new TableModuleRegistry();
        Module core = createStubModule(registry, "core", "1.0.0");
        Module moduleA = createStubModule(registry, "a", "1.0.0");
        Module moduleB = createStubModule(registry, "b", "1.0.0");
        createStubModule(registry, "c", "1.0.0");
        createStubModule(registry, "c", "2.0.0");

        addDependency(core, "a");
        addDependency(core, "b");
        addDependency(moduleA, "c", "1.0.0", "2.0.0");
        addDependency(moduleB, "c", "2.0.0", "3.0.0", true);

        DependencyResolver resolver = new DependencyResolver(registry);
        ResolutionResult results = resolver.resolve(new Name("core"));
        assertFalse(results.isSuccess());
    }

    @Test
    public void resolveExplicitVersion() {
        ModuleRegistry registry = new TableModuleRegistry();
        createStubModule(registry, "a", "1.0.0");
        createStubModule(registry, "a", "2.0.0");
        Version targetVersion = new Version(1, 0, 0);

        DependencyResolver resolver = new DependencyResolver(registry);
        ResolutionResult result = resolver.builder().requireVersion(new Name("a"), targetVersion).build();
        assertEquals(targetVersion, result.getModules().iterator().next().getVersion());
    }

    @Test
    public void resolveExplicitVersionRange() {
        ModuleRegistry registry = new TableModuleRegistry();
        createStubModule(registry, "a", "1.0.0");
        createStubModule(registry, "a", "2.0.0");
        Version targetVersion = new Version(1, 0, 0);

        DependencyResolver resolver = new DependencyResolver(registry);
        ResolutionResult result = resolver.builder().requireVersionRange(new Name("a"),
                new VersionRange(new Version(1, 0, 0), new Version(2, 0, 0))).build();
        assertEquals(targetVersion, result.getModules().iterator().next().getVersion());
    }
}
