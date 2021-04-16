// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.module;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.terasology.gestalt.module.dependencyresolution.DependencyResolver;
import org.terasology.gestalt.module.dependencyresolution.OptionalResolutionStrategy;
import org.terasology.gestalt.module.dependencyresolution.ResolutionResult;
import org.terasology.gestalt.naming.Name;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Immortius
 */
public class UseOptionalIfAvailableDependencyResolverTest extends DependencyResolverTestBase {

    @Test
    public void optionalNotRequired() {
        ModuleRegistry registry = new TableModuleRegistry();
        Module core = createStubModule(registry, "core", "1.0.0");
        Module moduleA = createStubModule(registry, "a", "1.0.0");

        addDependency(core, "a");
        addDependency(core, "b", true);

        DependencyResolver resolver = new DependencyResolver(registry, OptionalResolutionStrategy.INCLUDE_IF_AVAILABLE);
        ResolutionResult results = resolver.resolve(new Name("core"));
        assertTrue(results.isSuccess());
        assertEquals(Sets.newHashSet(core, moduleA), results.getModules());
    }

    @Test
    public void optionalUsedIfAvailable() {
        ModuleRegistry registry = new TableModuleRegistry();
        Module core = createStubModule(registry, "core", "1.0.0");
        Module moduleA = createStubModule(registry, "a", "1.0.0");
        Module moduleB = createStubModule(registry, "b", "1.0.0");

        addDependency(core, "a");
        addDependency(core, "b", true);

        DependencyResolver resolver = new DependencyResolver(registry, OptionalResolutionStrategy.INCLUDE_IF_AVAILABLE);
        ResolutionResult results = resolver.resolve(new Name("core"));
        assertTrue(results.isSuccess());
        assertEquals(Sets.newHashSet(core, moduleA, moduleB), results.getModules());
    }

}
