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

package org.terasology.entitysystem.prefab;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import org.terasology.assets.ResourceUrn;
import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.util.collection.TypeKeyedMap;

import java.util.Optional;
import java.util.Set;

/**
 * The recipe for creating an entity, intended for use as part of a prefab. This is an {@link EntityRef}, to allow it to be used in the components of other EntityRecipes
 * that are part of the same prefab. When the prefab is instantiated, these references will be replaced with references to the actual entities.
 */
public class EntityRecipe implements EntityRef {

    private TypeKeyedMap<Component> componentMap = new TypeKeyedMap<>(ImmutableMap.of());
    private ResourceUrn identifier;

    public EntityRecipe(ResourceUrn identifier) {
        Preconditions.checkArgument(!identifier.getFragmentName().isEmpty(), "EntityRecipe identifiers must have a fragment name");
        this.identifier = identifier;
    }

    public EntityRecipe(ResourceUrn prefabIdentifier, String identifier) {
        this.identifier = new ResourceUrn(prefabIdentifier, identifier);
    }

    /**
     * @return The identity of the recipe. This is the prefab's urn with a fragment name identifying this entity recipe.
     */
    public ResourceUrn getIdentifier() {
        return identifier;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public long getRevision() {
        return 0;
    }

    @Override
    public boolean isPresent() {
        return true;
    }

    @Override
    public <T extends Component> Optional<T> getComponent(Class<T> componentType) {
        return Optional.ofNullable(componentType.cast(componentMap.get(componentType)));
    }

    @Override
    public Set<Class<? extends Component>> getComponentTypes() {
        return componentMap.keySet();
    }

    /**
     * Adds a component to the recipe.
     *
     * @param componentType The type of the component
     * @param component     The component
     * @param <T>           The type of the component
     */
    public synchronized <T extends Component> void add(Class<T> componentType, T component) {
        this.componentMap = new TypeKeyedMap<>(ImmutableMap.<Class<? extends Component>, Component>builder().putAll(componentMap.getInner()).put(componentType, component).build());
    }

    /**
     * Removes a component from the recipe.
     *
     * @param componentType The type of the component to remove
     */
    public synchronized void remove(Class<? extends Component> componentType) {
        ImmutableMap.Builder<Class<? extends Component>, Component> builder = ImmutableMap.builder();
        componentMap.forEach(new TypeKeyedMap.EntryConsumer<Component>() {
            @Override
            public <U extends Component> void accept(Class<U> type, U value) {
                if (type != componentType) {
                    builder.put(type, value);
                }
            }
        });

        this.componentMap = new TypeKeyedMap<>(builder.build());
    }

    // AddComponent is unsupported, to avoid accidentally modifying an EntityPrefab in core entity processing. Also because EntityPrefab has no way to instantiate a
    // component by class
    @Override
    public <T extends Component> T addComponent(Class<T> componentType) {
        throw new UnsupportedOperationException("Components cannot be added to an entity prefab in this way - use EntityRecipe::add instead");
    }

    @Override
    public <T extends Component> T addComponent(T component) {
        throw new UnsupportedOperationException("Components cannot be added to an entity prefab in this way - use EntityRecipe::add instead");
    }

    // RemoveComponent is unsupported, to avoid accidentally modifying an EntityPrefab in core entity processing.
    @Override
    public <T extends Component> void removeComponent(Class<T> componentType) {
        throw new UnsupportedOperationException("Components cannot be removed from an entity prefab in this way - use EntityRecipe::remove instead");
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException("EntityRecipes cannot be deleted");
    }

    @Override
    public String toString() {
        return "EntityRecipe(" + identifier + ")";
    }

    /**
     * @return A map of all the components of the in the recipe
     */
    @Override
    public TypeKeyedMap<Component> getComponents() {
        return componentMap;
    }
}
