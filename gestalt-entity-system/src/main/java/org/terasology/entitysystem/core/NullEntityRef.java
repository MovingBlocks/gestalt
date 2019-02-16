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

package org.terasology.entitysystem.core;

import org.terasology.util.collection.TypeKeyedMap;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * A NullEntityRef represents a reference to no entity. This can be used instead of a straight null to avoid NullPointerExceptions (following the Null Object pattern).
 * Note that attempting to add a component to a NullEntityRef will evoke an exception.
 */
public final class NullEntityRef implements EntityRef {

    public static final NullEntityRef INSTANCE = new NullEntityRef();

    private NullEntityRef() {

    }

    public static NullEntityRef get() {
        return INSTANCE;
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
        return false;
    }

    @Override
    public <T extends Component> Optional<T> getComponent(Class<T> componentType) {
        return Optional.empty();
    }

    @Override
    public Set<Class<? extends Component>> getComponentTypes() {
        return Collections.emptySet();
    }

    @Override
    public TypeKeyedMap<Component> getComponents() {
        return new TypeKeyedMap<>();
    }

    @Override
    public <T extends Component> T addComponent(Class<T> componentType) {
        throw new IllegalStateException("Cannot add component as referenced entity does not exist");
    }

    @Override
    public <T extends Component> void removeComponent(Class<T> componentType) {
    }

    @Override
    public void delete() {
    }

    @Override
    public String toString() {
        return "EntityRef( null )";
    }
}
