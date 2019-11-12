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

package org.terasology.gestalt.entitysystem.prefab;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.gestalt.util.collection.TypeKeyedMap;

import java.util.Optional;


/**
 * The recipe for creating an entity, as part of a prefab.
 */
public class EntityRecipe {

    private final ResourceUrn identifier;
    private final EntityRecipeRef recipeRef;
    private TypeKeyedMap<Component> componentMap = new TypeKeyedMap<>(ImmutableMap.of());

    public EntityRecipe(ResourceUrn identifier) {
        Preconditions.checkArgument(!identifier.getFragmentName().isEmpty(), "EntityRecipe identifiers must have a fragment name");
        this.identifier = identifier;
        this.recipeRef = new EntityRecipeRef(this);
    }

    public EntityRecipe(ResourceUrn prefabIdentifier, String identifier) {
        this.identifier = new ResourceUrn(prefabIdentifier, identifier);
        this.recipeRef = new EntityRecipeRef(this);
    }

    /**
     * @return A reference to this EntityRecipe
     */
    public EntityRecipeRef getReference() {
        return recipeRef;
    }

    /**
     * @return The identity of the recipe. This is the prefab's urn with a fragment name identifying this entity recipe.
     */
    public ResourceUrn getIdentifier() {
        return identifier;
    }

    /**
     * @param componentType The type of component to get
     * @param <T>           The type of component to get
     * @return A component of the requested type, or {@link Optional#empty()}.
     */
    public <T extends Component<T>> Optional<T> getComponent(Class<T> componentType) {
        return Optional.ofNullable(componentType.cast(componentMap.get(componentType)));
    }

    /**
     * @return A map of all the components of the in the recipe
     */
    public TypeKeyedMap<Component> getComponents() {
        return componentMap;
    }

    /**
     * Adds a component to the recipe.
     *
     * @param component The component
     * @param <T>       The type of the component
     */
    public synchronized <T extends Component<T>> void add(T component) {
        this.componentMap = new TypeKeyedMap<>(ImmutableMap.<Class<? extends Component>, Component>builder().putAll(componentMap.getInner()).put(component.getClass(), component).build());
    }

    /**
     * Removes a component from the recipe.
     *
     * @param componentType The type of the component to remove
     */
    public <T extends Component<T>> void remove(Class<T> componentType) {
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

    @Override
    public String toString() {
        return "EntityRecipe(" + identifier + ")";
    }


}
