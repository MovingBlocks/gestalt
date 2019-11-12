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

package org.terasology.gestalt.entitysystem.event.lifecycle;

import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.gestalt.entitysystem.component.ComponentIterator;
import org.terasology.gestalt.entitysystem.component.store.ComponentStore;
import org.terasology.gestalt.entitysystem.component.management.ComponentType;
import org.terasology.gestalt.entitysystem.entity.EntityManager;

public class LifecycleAwareComponentStore<T extends Component<T>> implements ComponentStore<T> {

    private final LifecycleEventManager lifecycleEventManager;
    private final ComponentStore<T> inner;
    private EntityManager entityManager;

    public LifecycleAwareComponentStore(LifecycleEventManager lifecycleEventManager, ComponentStore<T> inner) {
        this.inner = inner;
        this.lifecycleEventManager = lifecycleEventManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean set(int entityId, T component) {
        if (inner.set(entityId, component)) {
            lifecycleEventManager.componentAdded(entityManager.getEntity(entityId), component.getClass());
            return true;
        } else {
            lifecycleEventManager.componentChanged(entityManager.getEntity(entityId), component.getClass());
            return false;
        }
    }

    @Override
    public T remove(int entityId) {
        T result = inner.remove(entityId);
        if (result != null) {
            lifecycleEventManager.componentRemoved(entityManager.getEntity(entityId), result);
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
