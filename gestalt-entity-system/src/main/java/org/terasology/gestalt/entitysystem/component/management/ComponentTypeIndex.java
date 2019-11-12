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

package org.terasology.gestalt.entitysystem.component.management;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;

import org.terasology.gestalt.assets.ResolutionStrategy;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.gestalt.assets.management.ContextManager;
import org.terasology.gestalt.assets.module.ModuleDependencyResolutionStrategy;
import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.gestalt.module.ModuleEnvironment;
import org.terasology.gestalt.naming.Name;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Lookup components via a combination of their module name and simple class name
 * as a ResourceUrn (fragment names and instance flags ignored).
 */
public class ComponentTypeIndex {

    private final Map<ResourceUrn, Class<? extends Component>> componentIndexByUrn;
    private final SetMultimap<Name, Name> modulesProvidingComponentsIndex;
    private final ResolutionStrategy resolutionStrategy;

    /**
     * @param environment The module environment providing components
     */
    public ComponentTypeIndex(ModuleEnvironment environment) {
        this(environment, new ModuleDependencyResolutionStrategy(environment));
    }

    /**
     * @param environment        The module environment providing components
     * @param resolutionStrategy The strategy for resolving the module to use when multiple module options are available.
     */
    public ComponentTypeIndex(ModuleEnvironment environment, ResolutionStrategy resolutionStrategy) {
        this.resolutionStrategy = resolutionStrategy;
        ImmutableMap.Builder<ResourceUrn, Class<? extends Component>> componentByUrnBuilder = ImmutableMap.builder();
        ImmutableSetMultimap.Builder<Name, Name> modulesProvidingComponentsBuilder = ImmutableSetMultimap.builder();
        for (Class<? extends Component> componentType : environment.getSubtypesOf(Component.class, (x) -> (x != Component.class && !x.isInterface()))) {
            Name module = environment.getModuleProviding(componentType);
            indexUrn(module, componentType.getSimpleName(), componentType, componentByUrnBuilder, modulesProvidingComponentsBuilder);
        }
        this.componentIndexByUrn = componentByUrnBuilder.build();
        this.modulesProvidingComponentsIndex = modulesProvidingComponentsBuilder.build();
    }

    private static void indexUrn(Name moduleName, String typeName, Class<? extends Component> componentType, ImmutableMap.Builder<ResourceUrn, Class<? extends Component>> componentByUrnBuilder, ImmutableSetMultimap.Builder<Name, Name> modulesProvidingComponentsBuilder) {
        ResourceUrn urn = new ResourceUrn(moduleName, new Name(typeName));
        componentByUrnBuilder.put(urn, componentType);
        modulesProvidingComponentsBuilder.put(urn.getResourceName(), urn.getModuleName());
    }

    /**
     * @param urn The urn to find the component type for
     * @return The component type, or Optional#empty
     */
    public Optional<Class<? extends Component>> find(ResourceUrn urn) {
        return Optional.ofNullable(componentIndexByUrn.get(urn));
    }

    /**
     * @param name          The name of the component
     * @param moduleContext The module context to resolve within, or Name#EMPTY
     * @return The component type, or Optional#empty
     */
    public Optional<Class<? extends Component>> find(Name name, Name moduleContext) {
        Set<Name> possibleModules = modulesProvidingComponentsIndex.get(name);
        if (!moduleContext.isEmpty()) {
            possibleModules = resolutionStrategy.resolve(possibleModules, moduleContext);
        }
        if (possibleModules.size() == 1) {
            return find(new ResourceUrn(possibleModules.iterator().next(), name));
        }
        return Optional.empty();
    }

    /**
     * Finds a component, using the current context from {@link ContextManager} if any
     *
     * @param name The name of the component
     * @return The component type, or Optional#empty
     */
    public Optional<Class<? extends Component>> find(Name name) {
        return find(name, ContextManager.getCurrentContext());
    }

    /**
     * Resolves a string to a component type. The string may be a resource urn, or just the name of a component. If it is the name of a component
     * then it will be resolved in the current context from {@link ContextManager} if any
     *
     * @param identity Either a ResourceUrn or just the name of a component
     * @return The component type, or Optional#empty
     */
    public Optional<Class<? extends Component>> find(String identity) {
        if (ResourceUrn.isValid(identity)) {
            return find(new ResourceUrn(identity));
        } else {
            return find(new Name(identity));
        }
    }
}
