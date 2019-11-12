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

import org.terasology.gestalt.entitysystem.entity.AbstractNOPEntityRef;

import java.util.Objects;


/**
 * A reference to an entity recipe, for use by components within a prefab. When the prefab containing the EntityRecipeRef is instantiated, the EntityRecipeRef will be replaced with a reference to the now instantiated entity from the prefab.
 * This allows a prefab to be a structure of entities that reference each other.
 * It otherwise behaves as a NullEntityRef (in the situation it somehow leaks into a live entity system)
 * Note: Doesn't inherit NullEntityRef because that would result in broken equals behavior.
 */
public class EntityRecipeRef extends AbstractNOPEntityRef {

    private final EntityRecipe recipe;

    public EntityRecipeRef(EntityRecipe recipe) {
        this.recipe = recipe;
    }

    public EntityRecipe getRecipe() {
        return recipe;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof EntityRecipeRef) {
            EntityRecipeRef other = (EntityRecipeRef) obj;
            return Objects.equals(other.recipe, this.recipe);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return recipe.hashCode();
    }

    @Override
    public String toString() {
        return "EntityRecipeRef(" + recipe.getIdentifier() + ")";
    }
}
