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

import com.google.common.collect.Sets;
import org.junit.Test;
import org.terasology.naming.Name;
import org.terasology.naming.Version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Immortius
 */
public class DependencyResolverTest {

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

    private void addDependency(Module dependant, String dependencyId) {
        DependencyInfo dependencyInfo = new DependencyInfo();
        dependencyInfo.setId(new Name(dependencyId));
        dependant.getMetadata().getDependencies().add(dependencyInfo);
    }

    private void addDependency(Module dependant, String dependencyId, String lowerbound, String upperbound) {
        DependencyInfo dependencyInfo = new DependencyInfo();
        dependencyInfo.setId(new Name(dependencyId));
        dependencyInfo.setMinVersion(new Version(lowerbound));
        dependencyInfo.setMaxVersion(new Version(upperbound));
        dependant.getMetadata().getDependencies().add(dependencyInfo);
    }

    private Module createStubModule(ModuleRegistry forRegistry, String id, String version) {
        Module module = mock(Module.class);
        ModuleMetadata metadata = new ModuleMetadata();
        when(module.getMetadata()).thenReturn(metadata);
        when(module.getId()).thenReturn(new Name(id));
        when(module.getVersion()).thenReturn(new Version(version));
        forRegistry.add(module);
        return module;
    }
}
