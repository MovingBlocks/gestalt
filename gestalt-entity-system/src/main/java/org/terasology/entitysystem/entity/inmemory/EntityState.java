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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.NullEntityRef;
import org.terasology.entitysystem.transaction.pipeline.UpdateAction;
import org.terasology.util.collection.TypeKeyedMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Information on the state of an entity as part of a transaction.
 * <p>When an entity is brought into a transaction, the current revision of the entity is noted and a copy is made of all its components. When the transaction
 * is committed, the </p>
 */
public class EntityState {

    private long id;
    private int revision;
    private Set<Class<? extends Component>> originalComponentTypes;
    private TypeKeyedMap<Component> workingComponents;
    private TypeKeyedMap<Component> removedComponents = new TypeKeyedMap<>();

    public EntityState(long id, int revision, Collection<Component> originalComponents, Collection<Component> workingComponents) {
        this.id = id;
        this.revision = revision;
        this.workingComponents = new TypeKeyedMap<>(workingComponents.stream().collect(Collectors.toMap(Component::getClass, (x) -> x)));
        this.originalComponentTypes = ImmutableSet.copyOf(this.workingComponents.keySet());
    }

    public long getId() {
        return id;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    public <T extends Component> Optional<T> getComponent(Class<T> type) {
        return Optional.ofNullable(workingComponents.get(type));
    }

    public TypeKeyedMap<Component> getComponents() {
        return new TypeKeyedMap<>(Collections.unmodifiableMap(workingComponents.getInner()));
    }

    public Set<Class<? extends Component>> getInvolvedComponents() {
        return Sets.union(workingComponents.keySet(), removedComponents.keySet());
    }

    public void addComponent(Component component) {
        workingComponents.getInner().put(component.getClass(), component);
        removedComponents.remove(component.getClass());
    }

    public <T extends Component> T removeComponent(Class<T> type) {
        T removed = workingComponents.remove(type);
        if (removed != null && originalComponentTypes.contains(type)) {
            removedComponents.put(type, removed);
        }
        return removed;
    }

    public void delete() {
        removedComponents.putAll(workingComponents);
        removedComponents.keySet().removeIf(x -> !originalComponentTypes.contains(x));
        workingComponents.clear();
    }

    public UpdateAction getUpdateAction(Class<? extends Component> type) {
        if (removedComponents.containsKey(type)) {
            return UpdateAction.REMOVE;
        }
        else if (!originalComponentTypes.contains(type)) {
            return UpdateAction.ADD;
        } else if (workingComponents.get(type).isDirty()) {
            return UpdateAction.UPDATE;
        }
        return UpdateAction.NONE;
    }

    public TypeKeyedMap<Component> getRemovedComponents() {
        return removedComponents;
    }
}
