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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Immortius
 */
public class ForceIncludeOptionalDependencyResolverTest extends DependencyResolverTestBase {

    @Test
    public void optionalRequired() {
        ModuleRegistry registry = new TableModuleRegistry();
        Module core = createStubModule(registry, "core", "1.0.0");
        createStubModule(registry, "a", "1.0.0");

        addDependency(core, "a");
        addDependency(core, "b", true);

        DependencyResolver resolver = new DependencyResolver(registry, OptionalResolutionStrategy.FORCE_INCLUDE);
        ResolutionResult results = resolver.resolve(new Name("core"));
        assertFalse(results.isSuccess());
    }

    @Test
    public void optionalIncluded() {
        ModuleRegistry registry = new TableModuleRegistry();
        Module core = createStubModule(registry, "core", "1.0.0");
        Module moduleA = createStubModule(registry, "a", "1.0.0");
        Module moduleB = createStubModule(registry, "b", "1.0.0");

        addDependency(core, "a");
        addDependency(core, "b", true);

        DependencyResolver resolver = new DependencyResolver(registry, OptionalResolutionStrategy.FORCE_INCLUDE);
        ResolutionResult results = resolver.resolve(new Name("core"));
        assertTrue(results.isSuccess());
        assertEquals(Sets.newHashSet(core, moduleA, moduleB), results.getModules());
    }

}
