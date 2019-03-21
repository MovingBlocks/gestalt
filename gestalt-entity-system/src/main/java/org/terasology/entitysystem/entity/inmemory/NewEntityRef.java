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

package org.terasology.entitysystem.entity.inmemory;

import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.transaction.exception.ComponentAlreadyExistsException;
import org.terasology.entitysystem.transaction.exception.ComponentDoesNotExistException;
import org.terasology.util.collection.TypeKeyedMap;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * An entity ref to a new entity being created as part of a transaction. Once the entity is created it acts as a wrapper around a EntityRefImpl.
 */
public class NewEntityRef implements EntityRef {

    private ComponentManager componentManager;
    private NewEntityState state;

    public NewEntityRef(ComponentManager componentManager, NewEntityState state) {
        this.componentManager = componentManager;
        this.state = state;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public long getRevision() {
        return 0;
    }

    @Override
    public boolean isPresent() {
        return true;
    }

    @Override
    public <T extends Component> Optional<T> getComponent(Class<T> componentType) {
        return Optional.ofNullable(state.getComponents().get(componentType));
    }

    @Override
    public Set<Class<? extends Component>> getComponentTypes() {
        return Collections.unmodifiableSet(state.getComponents().keySet());
    }

    @Override
    public TypeKeyedMap<Component> getComponents() {
        return new TypeKeyedMap<>(Collections.unmodifiableMap(state.getComponents().getInner()));
    }

    @Override
    public <T extends Component> T addComponent(Class<T> componentType) {
        if (state.getComponents().get(componentType) != null) {
            throw new ComponentAlreadyExistsException("Entity already has a component of type " + componentType.getSimpleName());
        }
        T component = componentManager.create(componentType);
        state.getComponents().put(componentType, component);
        return component;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> T addComponent(T component) {
        if (state.getComponents().get(component.getClass()) != null) {
            throw new ComponentAlreadyExistsException("Entity already has a component of type " + component.getClass().getSimpleName());
        }
        T componentCopy = componentManager.copy(component);
        state.getComponents().put(componentCopy);
        return componentCopy;
    }

    @Override
    public <T extends Component> void removeComponent(Class<T> componentType) {
        if (state.getComponents().remove(componentType) == null) {
            throw new ComponentDoesNotExistException("Entity does not have a component of type " + componentType.getSimpleName());
        }
    }

    @Override
    public void delete() {
        state.getComponents().clear();
    }

    @Override
    public String toString() {
        return "EntityRef( new )";
    }
}
