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

package org.terasology.entitysystem.prefab;

import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.util.collection.TypeKeyedMap;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A reference to a prefab, for use by components within a prefab. When the prefab containing the reference is instantiated, the referenced prefab will also be instantiated
 * and the reference replaced with a reference to the prefab. This allows prefabs to be created that are a composition of prefabs.
 */
public class PrefabRef implements EntityRef {

    private Prefab prefab;

    public PrefabRef(Prefab prefab) {
        this.prefab = prefab;
    }

    public Prefab getPrefab() {
        return prefab;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public boolean isPresent() {
        return true;
    }

    @Override
    public <T extends Component> Optional<T> getComponent(Class<T> componentType) {
        return prefab.getRootEntity().getComponent(componentType);
    }

    @Override
    public Set<Class<? extends Component>> getComponentTypes() {
        return Collections.unmodifiableSet(prefab.getRootEntity().getComponents().keySet());
    }

    @Override
    public TypeKeyedMap<Component> getComponents() {
        return new TypeKeyedMap<>(Collections.unmodifiableMap(prefab.getRootEntity().getComponents().getInner()));
    }

    @Override
    public <T extends Component> T addComponent(Class<T> componentType) {
        throw new UnsupportedOperationException("Cannot add components to a PrefabRef");
    }

    @Override
    public <T extends Component> void removeComponent(Class<T> componentType) {
        throw new UnsupportedOperationException("Cannot remove components from a PrefabRef");
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException("Cannot delete a PrefabRef");
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
        return "PrefabEntityRef(" + prefab.getUrn() + ")";
    }
}
