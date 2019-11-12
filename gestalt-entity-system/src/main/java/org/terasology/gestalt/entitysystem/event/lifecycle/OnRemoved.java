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
import org.terasology.gestalt.util.collection.TypeKeyedMap;

import java.util.Collection;
import java.util.Set;

/**
 *
 */
public class OnRemoved implements LifecycleEvent {
    private TypeKeyedMap<Component> components = new TypeKeyedMap<>();

    public OnRemoved(Component component) {
        this.components.put(component);
    }

    public OnRemoved(TypeKeyedMap<Component> components) {
        this.components.putAll(components);
    }

    public OnRemoved(Collection<Component> components) {
        this.components.putAll(components);
    }

    public TypeKeyedMap<Component> getComponents() {
        return components;
    }

    public <T extends Component> T getComponent(Class<T> type) {
        return components.get(type);
    }

    @Override
    public Set<Class<? extends Component>> getComponentTypes() {
        return components.keySet();
    }
}
