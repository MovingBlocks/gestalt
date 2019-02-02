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

package org.terasology.module;

import com.google.common.collect.Sets;

import org.junit.Test;
import org.terasology.module.dependencyresolution.DependencyResolver;
import org.terasology.module.dependencyresolution.OptionalResolutionStrategy;
import org.terasology.module.dependencyresolution.ResolutionResult;
import org.terasology.naming.Name;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
