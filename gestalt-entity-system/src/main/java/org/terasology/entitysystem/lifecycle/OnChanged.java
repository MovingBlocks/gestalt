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

package org.terasology.entitysystem.lifecycle;

import com.google.common.collect.ImmutableMap;

import org.terasology.entitysystem.core.Component;
import org.terasology.util.collection.TypeKeyedMap;

import java.util.Collection;
import java.util.Collections;

/**
 *
 */
public class OnChanged extends LifecycleEvent {
    private TypeKeyedMap<Component> beforeComponents;
    private TypeKeyedMap<Component> afterComponents;

    public OnChanged(int revision, Collection<Component> beforeComponents, Collection<Component> afterComponents) {
        super(revision);
        ImmutableMap.Builder<Class<? extends Component>, Component> beforeBuilder = ImmutableMap.builder();
        for (Component component : beforeComponents) {
            beforeBuilder.put(component.getClass(), component);
        }
        this.beforeComponents = new TypeKeyedMap<>(beforeBuilder.build());

        ImmutableMap.Builder<Class<? extends Component>, Component> afterBuilder = ImmutableMap.builder();
        for (Component component : afterComponents) {
            afterBuilder.put(component.getClass(), component);
        }
        this.afterComponents = new TypeKeyedMap<>(afterBuilder.build());
    }

    public OnChanged(int revision, TypeKeyedMap<Component> beforeComponents, TypeKeyedMap<Component> afterComponents) {
        super(revision);
        this.beforeComponents = new TypeKeyedMap<>(Collections.unmodifiableMap(beforeComponents.getInner()));
        this.afterComponents = new TypeKeyedMap<>(Collections.unmodifiableMap(afterComponents.getInner()));
    }

    public TypeKeyedMap<Component> getBeforeComponents() {
        return beforeComponents;
    }

    public <T extends Component> T getBeforeComponent(Class<T> type) {
        return beforeComponents.get(type);
    }

    public TypeKeyedMap<Component> getAfterComponents() {
        return afterComponents;
    }

    public <T extends Component> T getAfterComponent(Class<T> type) {
        return afterComponents.get(type);
    }
}
