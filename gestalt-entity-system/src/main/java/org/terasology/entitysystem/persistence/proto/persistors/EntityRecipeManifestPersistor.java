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

package org.terasology.entitysystem.persistence.proto.persistors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.management.AssetManager;
import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.persistence.proto.EntityRecipeManifest;
import org.terasology.entitysystem.persistence.proto.EntityRecipeMetadata;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;
import org.terasology.entitysystem.prefab.Prefab;
import org.terasology.util.collection.TypeKeyedMap;

import java.util.Optional;

/**
 * Serializes and Deserializes EntityRecipeManifests.
 */
public class EntityRecipeManifestPersistor {

    private static final Logger logger = LoggerFactory.getLogger(EntityRecipeManifestPersistor.class);

    private final ComponentPersistor componentPersistor;
    private final AssetManager assetManager;

    public EntityRecipeManifestPersistor(AssetManager assetManager, ComponentPersistor componentPersistor) {
        this.assetManager = assetManager;
        this.componentPersistor = componentPersistor;
    }

    public ProtoDatastore.EntityRecipeManifestData.Builder serialize(EntityRecipeManifest manifest) {
        ProtoDatastore.EntityRecipeManifestData.Builder builder = ProtoDatastore.EntityRecipeManifestData.newBuilder();
        for (EntityRecipeMetadata entityRecipeMetadata : manifest.allEntityRecipeMetadata()) {
            builder.addRecipes(serialize(entityRecipeMetadata));
        }
        return builder;
    }

    public ProtoDatastore.EntityRecipeData.Builder serialize(EntityRecipeMetadata entityRecipeMetadata) {
        ProtoDatastore.EntityRecipeData.Builder entityRecipeBuilder = ProtoDatastore.EntityRecipeData.newBuilder();
        entityRecipeBuilder.setId(entityRecipeMetadata.getId());
        entityRecipeBuilder.setUrn(entityRecipeMetadata.getUrn().toString());
        for (Component component : entityRecipeMetadata.getComponents().values()) {
            entityRecipeBuilder.addComponent(componentPersistor.serialize(component));
        }
        return entityRecipeBuilder;
    }

    public EntityRecipeManifest deserialize(ProtoDatastore.EntityRecipeManifestData data) {
        EntityRecipeManifest manifest = new EntityRecipeManifest(assetManager);
        for (ProtoDatastore.EntityRecipeData entityRecipe : data.getRecipesList()) {
            manifest.addEntityRecipeMetadata(deserialize(entityRecipe));
        }
        return manifest;
    }

    public EntityRecipeMetadata deserialize(ProtoDatastore.EntityRecipeData entityRecipe) {
        int id = entityRecipe.getId();
        ResourceUrn urn = new ResourceUrn(entityRecipe.getUrn());
        Optional<Prefab> prefab = assetManager.getAsset(urn.getRootUrn(), Prefab.class);
        if (prefab.isPresent() && prefab.get().getEntityRecipes().get(urn) != null) {
            return new EntityRecipeMetadata(id, urn, prefab.get().getEntityRecipes().get(urn));
        } else {
            logger.info("Failed to resolve entity recipe '{}', loading preserved components instead.", urn);
            TypeKeyedMap<Component> components = new TypeKeyedMap<>();
            for (ProtoDatastore.ComponentData componentData : entityRecipe.getComponentList()) {
                componentPersistor.deserialize(componentData).ifPresent(component -> components.put((Class) component.getType(), component));
            }

            return new EntityRecipeMetadata(id, urn, components);
        }
    }
}
