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

package org.terasology.entitysystem.persistence.proto;

import com.google.common.collect.ImmutableMap;

import org.terasology.assets.ResourceUrn;
import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.prefab.EntityRecipe;
import org.terasology.util.collection.TypeKeyedMap;

import java.util.Collections;

/**
 * Metadata describing Entity Recipes. This includes the id and urn of the recipe, and the components of the recipe. These components should be serialized with the metadata,
 * so if the entity recipe is not available during deserialization the last known settings can be used.
 */
public class EntityRecipeMetadata {
    private final int id;
    private final ResourceUrn urn;
    private final EntityRecipe recipe;
    private final TypeKeyedMap<Component> components;

    /**
     * Constructs EntityRecipeMetadata for an available recipe.
     *
     * @param id     The id of the EntityRecipe
     * @param urn    The urn of the EntityRecipe
     * @param recipe The EntityRecipe
     */
    public EntityRecipeMetadata(int id, ResourceUrn urn, EntityRecipe recipe) {
        this.id = id;
        this.urn = urn;
        this.recipe = recipe;
        this.components = null;
    }

    /**
     * Constructs EntityRecipeMetadata for an unavailable recipe, with a map of components.
     *
     * @param id         The id of the EntityRecipe
     * @param urn        The urn of the EntityRecipe
     * @param components The last known components of the recipe
     */
    public EntityRecipeMetadata(int id, ResourceUrn urn, TypeKeyedMap<Component> components) {
        this.id = id;
        this.urn = urn;
        this.recipe = null;
        this.components = new TypeKeyedMap<>(ImmutableMap.copyOf(components.getInner()));
    }

    /**
     * @return The id of the EntityRecipe
     */
    public int getId() {
        return id;
    }

    /**
     * @return The urn of the EntityRecipe
     */
    public ResourceUrn getUrn() {
        return urn;
    }

    /**
     * @return A map of the components of the entity recipe
     */
    public TypeKeyedMap<Component> getComponents() {
        if (recipe != null) {
            return new TypeKeyedMap<>(Collections.unmodifiableMap(recipe.getComponents().getInner()));
        }
        return components;
    }
}
