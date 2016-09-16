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

package org.terasology.entitysystem.persistence.proto;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.management.AssetManager;
import org.terasology.entitysystem.component.ComponentType;
import org.terasology.entitysystem.persistence.proto.ComponentMetadata;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;
import org.terasology.entitysystem.prefab.EntityRecipe;
import org.terasology.entitysystem.prefab.Prefab;
import org.terasology.module.ModuleEnvironment;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *
 */
public class EntityRecipeManifest {
    private final AssetManager assetManager;
    private int nextRecipeId;
    private final TIntObjectMap<EntityRecipeMetadata> metadataById = new TIntObjectHashMap<>();
    private final Map<ResourceUrn, EntityRecipeMetadata> metadataByUrn = new LinkedHashMap<>();

    public EntityRecipeManifest(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public void addEntityRecipeMetadata(EntityRecipeMetadata metadata) {
        Preconditions.checkArgument(!metadataById.containsKey(metadata.getId()), "EntityRecipeMetadata with id " + metadata.getId() + " already registered");
        Preconditions.checkArgument(!metadataByUrn.containsKey(metadata.getUrn()), "EntityRecipeMetadata with urn " + metadata.getUrn() + " already registered");

        metadataById.put(metadata.getId(), metadata);
        metadataByUrn.put(metadata.getUrn(), metadata);

        if (metadata.getId() >= nextRecipeId) {
            nextRecipeId = metadata.getId() + 1;
        }
    }

    public Optional<EntityRecipeMetadata> getEntityRecipeMetadata(ResourceUrn urn) {
        EntityRecipeMetadata metadata = metadataByUrn.get(urn);
        if (metadata == null) {
            Optional<Prefab> prefab = assetManager.getAsset(urn.getRootUrn(), Prefab.class);
            if (prefab.isPresent()) {
                EntityRecipe entityRecipe = prefab.get().getEntityRecipes().get(urn);
                if (entityRecipe != null) {
                    metadata = new EntityRecipeMetadata(nextRecipeId++, urn, entityRecipe);
                    addEntityRecipeMetadata(metadata);
                }
            }
        }
        return Optional.ofNullable(metadata);
    }

    public EntityRecipeMetadata getEntityRecipeMetadata(int id) {
        return metadataById.get(id);
    }

    public Iterable<EntityRecipeMetadata> allEntityRecipeMetadata() {
        return Collections.unmodifiableCollection(metadataByUrn.values());
    }
}

