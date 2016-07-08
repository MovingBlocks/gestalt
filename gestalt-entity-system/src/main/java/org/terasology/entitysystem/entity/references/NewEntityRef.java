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

package org.terasology.entitysystem.entity.references;

import com.google.common.collect.Maps;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.entity.Component;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.entity.exception.ComponentAlreadyExistsException;
import org.terasology.entitysystem.entity.exception.ComponentDoesNotExistException;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * An entity ref to a new entity being created as part of a transaction. Once the entity is created it acts as a wrapper around a EntityRefImpl.
 */
public class NewEntityRef implements EntityRef {

    private EntityRef innerEntityRef = null;
    private boolean useInnerRef = false;

    private ComponentManager componentManager;
    private Map<Class<? extends Component>, Component> components = Maps.newLinkedHashMap();

    public NewEntityRef(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    public void setInnerEntityRef(EntityRef entityRef) {
        if (innerEntityRef == null) {
            innerEntityRef = entityRef;
        }
    }

    public void activateInnerRef() {
        useInnerRef = true;
        components = null;
        componentManager = null;
    }

    public Optional<EntityRef> getInnerEntityRef() {
        return Optional.ofNullable(innerEntityRef);
    }

    @Override
    public boolean isPresent() {
        return innerEntityRef == null || innerEntityRef.isPresent();
    }

    @Override
    public <T extends Component> Optional<T> getComponent(Class<T> componentType) {
        if (useInnerRef) {
            return innerEntityRef.getComponent(componentType);
        }
        return Optional.ofNullable((T) components.get(componentType));
    }

    @Override
    public Set<Class<? extends Component>> getComponentTypes() {
        if (useInnerRef) {
            return innerEntityRef.getComponentTypes();
        }
        return Collections.unmodifiableSet(components.keySet());
    }

    @Override
    public <T extends Component> T addComponent(Class<T> componentType) {
        if (useInnerRef) {
            return innerEntityRef.addComponent(componentType);
        }
        if (components.get(componentType) != null) {
            throw new ComponentAlreadyExistsException("Entity already has a component of type " + componentType.getSimpleName());
        }
        T component = componentManager.create(componentType);
        components.put(componentType, component);
        return component;
    }

    @Override
    public <T extends Component> void removeComponent(Class<T> componentType) {
        if (useInnerRef) {
            innerEntityRef.removeComponent(componentType);
        } else if (components.remove(componentType) == null) {
            throw new ComponentDoesNotExistException("Entity does not have a component of type " + componentType.getSimpleName());
        }
    }

    @Override
    public void delete() {
        if (useInnerRef) {
            innerEntityRef.delete();
        } else {
            components.clear();
        }
    }

    @Override
    public String toString() {
        if (useInnerRef) {
            return "New" + innerEntityRef.toString();
        }
        return "EntityRef( new )";
    }
}
