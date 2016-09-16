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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.terasology.assets.ResourceUrn;
import org.terasology.entitysystem.entity.Component;
import org.terasology.entitysystem.prefab.EntityRecipe;
import org.terasology.util.collection.TypeKeyedMap;

import java.util.List;

/**
 *
 */
public class EntityRecipeMetadata {
    private final int id;
    private final ResourceUrn urn;
    private final EntityRecipe recipe;
    private final List<Component> components;

    public EntityRecipeMetadata(int id, ResourceUrn urn, EntityRecipe recipe) {
        this.id = id;
        this.urn = urn;
        this.recipe = recipe;
        this.components = null;
    }

    public EntityRecipeMetadata(int id, ResourceUrn urn, Iterable<Component> components) {
        this.id = id;
        this.urn = urn;
        this.recipe = null;
        this.components = ImmutableList.copyOf(components);
    }

    public int getId() {
        return id;
    }

    public ResourceUrn getUrn() {
        return urn;
    }

    public Iterable<Component> getComponents() {
        if (recipe != null) {
            return recipe.getComponents().values();
        }
        return components;
    }
}
