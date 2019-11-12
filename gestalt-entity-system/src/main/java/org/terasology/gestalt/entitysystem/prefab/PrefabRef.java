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
 * A reference to a prefab, for use by components within a prefab. When the prefab containing the reference is instantiated, the referenced prefab will also be instantiated
 * and the reference replaced with a reference to the prefab. This allows prefabs to be created that are a composition of prefabs.
 * It otherwise behaves as a NullEntityRef
 *
 * Note: Doesn't inherit NullEntityRef because that would result in broken equals behavior.
 */
public class PrefabRef extends AbstractNOPEntityRef {

    private final Prefab prefab;

    public PrefabRef(Prefab prefab) {
        this.prefab = prefab;
    }

    public Prefab getPrefab() {
        return prefab;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof PrefabRef) {
            PrefabRef other = (PrefabRef) obj;
            return Objects.equals(other.prefab, this.prefab);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return prefab.hashCode();
    }

    @Override
    public String toString() {
        return "PrefabRef(" + prefab.getUrn() + ")";
    }
}
