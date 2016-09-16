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

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.management.AssetManager;
import org.terasology.entitysystem.entity.Component;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;
import org.terasology.entitysystem.prefab.Prefab;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public class EntityRecipeManifestPersistor {

    private static final Logger logger = LoggerFactory.getLogger(EntityRecipeManifestPersistor.class);

    private final ComponentPersistor componentPersistor;
    private final AssetManager assetManager;

    public EntityRecipeManifestPersistor(AssetManager assetManager, ComponentPersistor componentPersistor) {
        this.assetManager = assetManager;
        this.componentPersistor = componentPersistor;
    }

    public ProtoDatastore.EntityRecipeManifest.Builder serialize(EntityRecipeManifest manifest) {
        ProtoDatastore.EntityRecipeManifest.Builder builder = ProtoDatastore.EntityRecipeManifest.newBuilder();
        for (EntityRecipeMetadata entityRecipeMetadata : manifest.allEntityRecipeMetadata()) {
            builder.addRecipes(serialize(entityRecipeMetadata));
        }
        return builder;
    }

    public ProtoDatastore.EntityRecipe.Builder serialize(EntityRecipeMetadata entityRecipeMetadata) {
        ProtoDatastore.EntityRecipe.Builder entityRecipeBuilder = ProtoDatastore.EntityRecipe.newBuilder();
        entityRecipeBuilder.setId(entityRecipeMetadata.getId());
        entityRecipeBuilder.setUrn(entityRecipeMetadata.getUrn().toString());
        for (Component component : entityRecipeMetadata.getComponents()) {
            entityRecipeBuilder.addComponent(componentPersistor.serialize(component));
        }
        return entityRecipeBuilder;
    }

    public EntityRecipeManifest deserialize(ProtoDatastore.EntityRecipeManifest data) {
        EntityRecipeManifest manifest = new EntityRecipeManifest(assetManager);
        for (ProtoDatastore.EntityRecipe entityRecipe : data.getRecipesList()) {
            manifest.addEntityRecipeMetadata(deserialize(entityRecipe));
        }
        return manifest;
    }

    public EntityRecipeMetadata deserialize(ProtoDatastore.EntityRecipe entityRecipe) {
        int id = entityRecipe.getId();
        ResourceUrn urn = new ResourceUrn(entityRecipe.getUrn());
        Optional<Prefab> prefab = assetManager.getAsset(urn.getRootUrn(), Prefab.class);
        if (prefab.isPresent() && prefab.get().getEntityRecipes().get(urn) != null) {
            return new EntityRecipeMetadata(id, urn, prefab.get().getEntityRecipes().get(urn));
        } else {
            logger.info("Failed to resolve entity recipe '{}', loading preserved components instead.", urn);
            List<Component> components = Lists.newArrayListWithCapacity(entityRecipe.getComponentCount());
            for (ProtoDatastore.Component componentData : entityRecipe.getComponentList()) {
                componentPersistor.deserialize(componentData).ifPresent(components::add);
            }

            return new EntityRecipeMetadata(id, urn, components);
        }
    }
}
