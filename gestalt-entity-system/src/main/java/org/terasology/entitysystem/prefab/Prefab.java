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

import com.google.common.collect.ImmutableMap;

import org.terasology.assets.Asset;
import org.terasology.assets.AssetType;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.exceptions.InvalidAssetDataException;

import java.util.Map;

/**
 * A prefab is the template for generating one or more entities, that may reference each other.
 * Each prefab has a root entity recipe - this is the entity that will be returned when instantiating the prefab. Also, if one prefab references another in a EntityRef property,
 * that prefab will be instantiated and the reference linked to the root entity from that prefab when the referencing prefab is instantiated.
 */
public class Prefab extends Asset<PrefabData> {

    private ResourceUrn rootEntityId;
    private Map<ResourceUrn, EntityRecipe> recipes;

    /**
     * The constructor for an asset. It is suggested that implementing classes provide a constructor taking both the urn, and an initial AssetData to load.
     *
     * @param urn       The urn identifying the asset.
     * @param assetType The asset type this asset belongs to.
     */
    public Prefab(ResourceUrn urn, AssetType<?, PrefabData> assetType, PrefabData data) {
        super(urn, assetType);
        reload(data);
    }

    @Override
    protected void doReload(PrefabData data) {
        for (ResourceUrn recipeUrn : data.getRecipes().keySet()) {
            if (!getUrn().equals(recipeUrn.getRootUrn())) {
                throw new InvalidAssetDataException("PrefabData contains recipe that does not belong to this prefab - " + recipeUrn.toString());
            }
        }
        this.rootEntityId = data.getRootEntityId();
        this.recipes = ImmutableMap.copyOf(data.getRecipes());
    }

    /**
     * @return The urn of the root entity recipe.
     */
    public ResourceUrn getRootEntityUrn() {
        return rootEntityId;
    }

    /**
     * @return The root entity recipe
     */
    public EntityRecipe getRootEntity() {
        return recipes.get(rootEntityId);
    }

    /**
     * @return The map of all the entity recipes in the prefab. This is immutable.
     */
    public Map<ResourceUrn, EntityRecipe> getEntityRecipes() {
        return recipes;
    }
}
