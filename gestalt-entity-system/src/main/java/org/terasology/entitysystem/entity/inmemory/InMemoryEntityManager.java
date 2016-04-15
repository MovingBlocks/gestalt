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

package org.terasology.entitysystem.entity.inmemory;

import com.google.common.base.Preconditions;
import gnu.trove.iterator.TLongIterator;
import org.terasology.entitysystem.entity.Component;
import org.terasology.entitysystem.entity.EntityManager;
import org.terasology.entitysystem.Transaction;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.util.Varargs;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 *
 */
public class InMemoryEntityManager implements EntityManager {

    private final ComponentManager library;
    private EntityStore entityStore;

    public InMemoryEntityManager(ComponentManager library) {
        this.library = library;
        entityStore = new ComponentTable(library);
    }

    @Override
    public <T extends Component> T createComponent(Class<T> componentClass) {
        return library.create(componentClass);
    }

    @Override
    public long createEntity(Component first, Component... additional) {
        Preconditions.checkNotNull(first);
        List<Component> components = Varargs.combineToList(first, additional);
        return createEntity(components);
    }

    @Override
    @SuppressWarnings("unchecked")
    public long createEntity(Collection<Component> components) {
        Preconditions.checkArgument(!components.isEmpty(), "Cannot create an entity without at least one component");
        long entityId = entityStore.createEntityId();
        for (Component component : components) {
            Class componentType = library.getType(component.getClass()).getInterfaceType();
            entityStore.add(entityId, componentType, component);
        }
        return entityId;
    }

    @Override
    public <T extends Component> Optional<T> getComponent(long entityId, Class<T> type) {
        return Optional.ofNullable(entityStore.get(entityId, type));
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean addComponent(long entityId, Component component) {
        Class componentType = library.getType(component.getClass()).getInterfaceType();
        return entityStore.add(entityId, componentType, component);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean updateComponent(long entityId, Component component) {
        Class componentType = library.getType(component.getClass()).getInterfaceType();
        return entityStore.update(entityId, componentType, component);
    }

    @Override
    public boolean removeComponent(long entityId, Class<? extends Component> componentClass) {
        return entityStore.remove(entityId, componentClass) != null;
    }

    @Override
    public Transaction beginTransaction() {
        return new InMemoryTransaction(entityStore, library);
    }

    @Override
    public TLongIterator findEntitiesWithComponents(Class<? extends Component> first, Class<? extends Component>... additional) {
        return findEntitiesWithComponents(Varargs.combineToSet(first, additional));
    }

    @Override
    public TLongIterator findEntitiesWithComponents(Set<Class<? extends Component>> componentTypes) {
        Preconditions.checkArgument(!componentTypes.isEmpty());
        return entityStore.findWithComponents(componentTypes);
    }
}
