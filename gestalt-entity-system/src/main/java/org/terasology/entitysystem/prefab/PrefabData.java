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
import com.google.common.collect.Maps;

import org.terasology.assets.AssetData;
import org.terasology.assets.ResourceUrn;

import java.util.Collections;
import java.util.Map;

/**
 * PrefabData is the asset data used when creating or reloading {@link Prefab} asset.
 */
public class PrefabData implements AssetData {
    private ResourceUrn rootEntityId;
    private Map<ResourceUrn, EntityRecipe> recipes = Maps.newLinkedHashMap();

    /**
     * @return The identity of the root entity recipe
     */
    public ResourceUrn getRootEntityId() {
        return rootEntityId;
    }

    /**
     * Sets the identity of the root entity id.
     *
     * @param rootEntityId The new root entity id. This must correspond to a recipe that is part of the prefab data.
     */
    public void setRootEntityId(ResourceUrn rootEntityId) {
        Preconditions.checkArgument(recipes.containsKey(rootEntityId));
        this.rootEntityId = rootEntityId;
    }

    /**
     * @return The unmodified collection of all the entity recipes composing the PrefabData
     */
    public Map<ResourceUrn, EntityRecipe> getRecipes() {
        return Collections.unmodifiableMap(recipes);
    }

    /**
     * @param recipe The recipe to add to the PrefabData
     */
    public void addEntityRecipe(EntityRecipe recipe) {
        recipes.put(recipe.getIdentifier(), recipe);
        if (rootEntityId == null) {
            rootEntityId = recipe.getIdentifier();
        }
    }

    /**
     * @param recipe The recipe to remove from the PrefabData
     */
    public void removeEntityPrefab(EntityRecipe recipe) {
        recipes.remove(recipe.getIdentifier());
    }
}
