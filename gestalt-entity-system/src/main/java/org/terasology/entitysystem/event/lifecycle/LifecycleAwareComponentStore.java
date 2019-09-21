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

package org.terasology.entitysystem.event.lifecycle;

import org.terasology.entitysystem.component.Component;
import org.terasology.entitysystem.component.ComponentIterator;
import org.terasology.entitysystem.component.store.ComponentStore;
import org.terasology.entitysystem.component.management.ComponentType;
import org.terasology.entitysystem.entity.EntityRef;

public class LifecycleAwareComponentStore<T extends Component<T>> implements ComponentStore<T> {

    private final LifecycleEventManager lifecycleEventManager;
    private final ComponentStore<T> inner;

    public LifecycleAwareComponentStore(LifecycleEventManager lifecycleEventManager, ComponentStore<T> inner) {
        this.inner = inner;
        this.lifecycleEventManager = lifecycleEventManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean set(EntityRef entity, T component) {
        if (inner.set(entity, component)) {
            lifecycleEventManager.componentAdded(entity, component.getClass());
            return true;
        } else {
            lifecycleEventManager.componentChanged(entity, component.getClass());
            return false;
        }
    }

    @Override
    public T remove(EntityRef entity) {
        T result = inner.remove(entity);
        if (result != null) {
            lifecycleEventManager.componentRemoved(entity, result);
        }
        return result;
    }

    @Override
    public ComponentType<T> getType() {
        return inner.getType();
    }

    @Override
    public boolean has(int entityId) {
        return inner.has(entityId);
    }

    @Override
    public boolean get(int entityId, T into) {
        return inner.get(entityId, into);
    }

    @Override
    public int iterationCost() {
        return inner.iterationCost();
    }

    @Override
    public void extend(int capacity) {
        inner.extend(capacity);
    }

    @Override
    public ComponentIterator<T> iterate() {
        return inner.iterate();
    }
}
