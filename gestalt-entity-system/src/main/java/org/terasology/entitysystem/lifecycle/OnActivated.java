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

package org.terasology.entitysystem.lifecycle;

import com.google.common.collect.ImmutableMap;

import org.terasology.entitysystem.core.Component;
import org.terasology.util.collection.TypeKeyedMap;

import java.util.Collection;
import java.util.Collections;

/**
 *
 */
public class OnActivated extends LifecycleEvent {
    private TypeKeyedMap<Component> components;

    public OnActivated(int revision, TypeKeyedMap<Component> components) {
        super(revision);
        this.components = new TypeKeyedMap<>(Collections.unmodifiableMap(components.getInner()));
    }

    public OnActivated(int revision, Collection<Component> components) {
        super(revision);
        ImmutableMap.Builder<Class<? extends Component>, Component> builder = ImmutableMap.builder();
        for (Component component : components) {
            builder.put(component.getType(), component);
        }
        this.components = new TypeKeyedMap<>(builder.build());
    }

    public TypeKeyedMap<Component> getComponents() {
        return components;
    }

    public <T extends Component> T getComponent(Class<T> type) {
        return components.get(type);
    }
}
