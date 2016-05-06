/*
 * Copyright 2016 MovingBlocks
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

package org.terasology.entitysystem;

import com.google.common.base.Preconditions;
import gnu.trove.iterator.TLongIterator;
import org.terasology.entitysystem.entity.Component;
import org.terasology.entitysystem.entity.EntityManager;
import org.terasology.entitysystem.event.EventSystem;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * EntitySystem is an extension of EntityManager with support for events.
 * <p>
 * EntitySystem provides all the functionality that EntityManager supports. Additionally, EntitySystem provides {@link Transaction} rather than {@link org.terasology.entitysystem.entity.EntityTransaction},
 * which provides event support.
 */
public class EntitySystem implements EntityManager {

    private final EntityManager entityManager;
    private final EventSystem eventSystem;

    /**
     * Creates the EntitySystem wrapping the provided entityManager and entitySystem
     * @param entityManager
     * @param eventSystem
     */
    public EntitySystem(EntityManager entityManager, EventSystem eventSystem) {
        Preconditions.checkNotNull(entityManager);
        Preconditions.checkNotNull(eventSystem);
        this.entityManager = entityManager;
        this.eventSystem = eventSystem;
    }

    /**
     * @return the underlying EntityManager
     */
    public EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * @return the underlying EventSystem
     */
    public EventSystem getEventSystem() {
        return eventSystem;
    }

    @Override
    public <T extends Component> T createComponent(Class<T> componentClass) {
        return entityManager.createComponent(componentClass);
    }

    @Override
    public long createEntity(Component first, Component... additional) {
        return entityManager.createEntity(first, additional);
    }

    @Override
    public long createEntity(Collection<Component> components) {
        return entityManager.createEntity(components);
    }

    @Override
    public boolean addComponent(long entityId, Component component) {
        return entityManager.addComponent(entityId, component);
    }

    @Override
    public <T extends Component> Optional<T> getComponent(long entityId, Class<T> componentClass) {
        return entityManager.getComponent(entityId, componentClass);
    }

    @Override
    public boolean updateComponent(long entityId, Component component) {
        return entityManager.updateComponent(entityId, component);
    }

    @Override
    public boolean removeComponent(long entityId, Class<? extends Component> componentClass) {
        return entityManager.removeComponent(entityId, componentClass);
    }

    @Override
    public Transaction beginTransaction() {
        return eventSystem.beginTransaction();
    }

    @Override
    public TLongIterator findEntitiesWithComponents(Class<? extends Component> first, Class<? extends Component>... additional) {
        return entityManager.findEntitiesWithComponents(first, additional);
    }

    @Override
    public TLongIterator findEntitiesWithComponents(Set<Class<? extends Component>> componentTypes) {
        return entityManager.findEntitiesWithComponents(componentTypes);
    }
}
