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

package org.terasology.entitysystem.entity;

import org.terasology.entitysystem.component.Component;
import org.terasology.util.collection.TypeKeyedMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public abstract class AbstractNOPEntityRef implements EntityRef {

    public static final int NON_EXISTENT_ENTITY_ID = -1;

    @Override
    public int getId() {
        return NON_EXISTENT_ENTITY_ID;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public <T extends Component<T>> boolean getComponent(T component) {
        return false;
    }

    @Override
    public <T extends Component<T>> Optional<T> getComponent(Class<T> componentType) {
        return Optional.empty();
    }

    @Override
    public Set<Class<? extends Component>> getComponentTypes() {
        return Collections.emptySet();
    }

    @Override
    public TypeKeyedMap<Component> getAllComponents() {
        return new TypeKeyedMap<>();
    }

    @Override
    public <T extends Component<T>> boolean setComponent(T component) {
        return false;
    }

    @Override
    public void setComponents(Collection<Component> components) {
    }

    @Override
    public <T extends Component<T>> T removeComponent(Class<T> componentType) {
        return null;
    }

    @Override
    public Set<Component<?>> removeComponents(Collection<Class<? extends Component>> componentTypes) {
        return Collections.emptySet();
    }

    @Override
    public Set<Component<?>> delete() {
        return Collections.emptySet();
    }

    @Override
    public <T extends Component<T>> boolean hasComponent(Class<T> type) {
        return false;
    }
}
