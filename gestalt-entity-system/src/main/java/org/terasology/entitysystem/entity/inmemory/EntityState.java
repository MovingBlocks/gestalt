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

package org.terasology.entitysystem.entity.inmemory;

import com.google.common.collect.Sets;

import org.terasology.entitysystem.core.Component;
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
    private Set<Class<? extends Component>> involvedComponents;
    private TypeKeyedMap<Component> originalComponents;
    private TypeKeyedMap<Component> workingComponents;

    public EntityState(long id, int revision, Collection<Component> originalComponents, Collection<Component> workingComponents) {
        this.id = id;
        this.revision = revision;
        this.originalComponents = new TypeKeyedMap<>(originalComponents.stream().collect(Collectors.toMap(Component::getType, (x) -> x)));
        this.workingComponents = new TypeKeyedMap<>(workingComponents.stream().collect(Collectors.toMap(Component::getType, (x) -> x)));
        this.involvedComponents = Sets.newLinkedHashSetWithExpectedSize(originalComponents.size());
        this.involvedComponents.addAll(this.originalComponents.keySet());
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

    public <T extends Component> Optional<T> getOriginalComponent(Class<T> type) {
        return Optional.ofNullable(originalComponents.get(type));
    }

    public <T extends Component> Optional<T> getComponent(Class<T> type) {
        return Optional.ofNullable(workingComponents.get(type));
    }

    public TypeKeyedMap<Component> getComponents() {
        return new TypeKeyedMap<>(Collections.unmodifiableMap(workingComponents.getInner()));
    }

    public Set<Class<? extends Component>> getInvolvedComponents() {
        return Collections.unmodifiableSet(involvedComponents);
    }

    public void addComponent(Component component) {
        workingComponents.getInner().put(component.getType(), component);
        involvedComponents.add(component.getType());
    }

    public <T extends Component> T removeComponent(Class<T> type) {
        return workingComponents.remove(type);
    }

    public void delete() {
        workingComponents.clear();
    }

    public UpdateAction getUpdateAction(Class<? extends Component> type) {
        Component original = originalComponents.get(type);
        Component working = workingComponents.get(type);
        if (original == null) {
            if (working == null) {
                return UpdateAction.NONE;
            } else {
                return UpdateAction.ADD;
            }
        } else if (working == null) {
            return UpdateAction.REMOVE;
        } else if (Objects.equals(original, working)) {
            return UpdateAction.NONE;
        } else {
            return UpdateAction.UPDATE;
        }
    }
}
