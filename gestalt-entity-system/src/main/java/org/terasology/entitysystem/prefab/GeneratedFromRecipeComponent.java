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

import org.terasology.assets.ResourceUrn;
import org.terasology.entitysystem.component.Component;

/**
 * Component indicating an entity was generated from an entity recipe
 */
public final class GeneratedFromRecipeComponent implements Component<GeneratedFromRecipeComponent> {

    private ResourceUrn entityRecipe;

    /**
     * @return The URN of the EntityRecipe that this entity was generated from
     */
    public ResourceUrn getEntityRecipe() {
        return entityRecipe;
    }

    /**
     * Sets the URN of the recipe this entity was generated from
     * @param entityRecipe
     */
    public void setEntityRecipe(ResourceUrn entityRecipe) {
        this.entityRecipe = entityRecipe;
    }

    @Override
    public void copy(GeneratedFromRecipeComponent other) {
        this.entityRecipe = other.entityRecipe;
    }
}
