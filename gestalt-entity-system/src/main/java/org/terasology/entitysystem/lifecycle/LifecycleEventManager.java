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

import org.terasology.entitysystem.component.Component;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.event.EventSystem;

import java.util.LinkedHashMap;
import java.util.Map;

public class LifecycleEventManager {

    private Map<EntityRef, OnAdded> onAddedEvents = new LinkedHashMap<>();
    private Map<EntityRef, OnChanged> onChangedEvents = new LinkedHashMap<>();
    private Map<EntityRef, OnRemoved> onRemovedEvents = new LinkedHashMap<>();

    public <T extends Component<T>> void componentAdded(EntityRef entity, Class<T> componentType) {
        // Create or add to an existing OnAdded event
        OnAdded onAdded = onAddedEvents.get(entity);
        if (onAdded == null) {
            onAdded = new OnAdded(componentType);
            onAddedEvents.put(entity, onAdded);
        } else {
            onAdded.getComponentTypes().add(componentType);
        }

        // If the component was previously removed, then remove it and add componentChanged instead
        OnRemoved onRemoved = onRemovedEvents.get(entity);
        if (onRemoved != null) {
            if (onRemoved.getComponentTypes().remove(componentType)) {
                componentChanged(entity, componentType);
            }
        }
    }

    public <T extends Component<T>> void componentChanged(EntityRef entity, Class<T> componentType) {
        OnChanged onChanged = onChangedEvents.get(entity);
        if (onChanged == null) {
            onChanged = new OnChanged(componentType);
            onChangedEvents.put(entity, onChanged);
        } else {
            onChanged.getComponentTypes().add(componentType);
        }
    }

    public <T extends Component<T>> void componentRemoved(EntityRef entity, T component) {
        OnRemoved onRemoved = onRemovedEvents.get(entity);
        if (onRemoved == null) {
            onRemoved = new OnRemoved(component);
            onRemovedEvents.put(entity, onRemoved);
        } else {
            onRemoved.getComponents().put(component);
        }

        OnAdded onAdded = onAddedEvents.get(entity);
        if (onAdded != null) {
            onAdded.getComponentTypes().remove(component.getClass());
        }

        OnChanged onChanged = onChangedEvents.get(entity);
        if (onChanged != null) {
            onChanged.getComponentTypes().remove(component.getClass());
        }
    }

    public void sendPendingEvents(EventSystem eventSystem) {
        sendEvents(eventSystem, onAddedEvents);
        sendEvents(eventSystem, onChangedEvents);
        sendEvents(eventSystem, onRemovedEvents);
    }

    private void sendEvents(EventSystem eventSystem, Map<EntityRef, ? extends LifecycleEvent> events) {
        events.forEach((ref, event) -> {
            if (!event.getComponentTypes().isEmpty()) {
                eventSystem.send(event, ref, event.getComponentTypes());
            }
        });
        events.clear();
    }


}
