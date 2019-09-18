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

import org.terasology.entitysystem.component.Component;
import org.terasology.entitysystem.component.management.ComponentType;
import org.terasology.util.collection.TypeKeyedMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class OnAdded implements LifecycleEvent {
    private Set<Class<? extends Component>> componentTypes = new HashSet<>();

    public OnAdded(Class<? extends Component> componentType) {
        componentTypes.add(componentType);
    }

    public OnAdded(Collection<Class<? extends Component>> components) {
        componentTypes.addAll(components);
    }

    @Override
    public Set<Class<? extends Component>> getComponentTypes() {
        return componentTypes;
    }

}
