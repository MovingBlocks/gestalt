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

import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.terasology.module.dependencyresolution.DependencyInfo;
import org.terasology.module.resources.EmptyFileSource;
import org.terasology.naming.Name;
import org.terasology.naming.Version;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Immortius
 */
public class DependencyResolverTestBase {

    protected void addDependency(Module dependant, String dependencyId) {
        addDependency(dependant, dependencyId, false);
    }

    protected void addDependency(Module dependant, String dependencyId, boolean optional) {
        DependencyInfo dependencyInfo = new DependencyInfo();
        dependencyInfo.setId(new Name(dependencyId));
        dependencyInfo.setOptional(optional);
        dependant.getMetadata().getDependencies().add(dependencyInfo);
    }

    protected void addDependency(Module dependant, String dependencyId, String lowerbound, String upperbound, boolean optional) {
        DependencyInfo dependencyInfo = new DependencyInfo();
        dependencyInfo.setId(new Name(dependencyId));
        dependencyInfo.setOptional(optional);
        dependencyInfo.setMinVersion(new Version(lowerbound));
        dependencyInfo.setMaxVersion(new Version(upperbound));
        dependant.getMetadata().getDependencies().add(dependencyInfo);
    }

    protected void addDependency(Module dependant, String dependencyId, String lowerbound, String upperbound) {
        DependencyInfo dependencyInfo = new DependencyInfo();
        dependencyInfo.setId(new Name(dependencyId));
        dependencyInfo.setMinVersion(new Version(lowerbound));
        dependencyInfo.setMaxVersion(new Version(upperbound));
        dependant.getMetadata().getDependencies().add(dependencyInfo);
    }

    protected Module createStubModule(ModuleRegistry forRegistry, String id, String version) {
        ModuleMetadata metadata = new ModuleMetadata();
        metadata.setId(new Name(id));
        metadata.setVersion(new Version(version));
        Configuration config = new ConfigurationBuilder();
        Module module = new Module(metadata, new EmptyFileSource(), Collections.emptyList(), new Reflections(config), x -> false);
        forRegistry.add(module);
        return module;
    }
}
