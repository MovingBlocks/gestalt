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
import org.terasology.gestalt.entitysystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.event.EventSystem;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A manager for collating and sending Lifecycle events.
 * sendPendingEvents should be called regularly to send events.
 *
 * This event manager sends three sorts of events:
 * <ul>
 *     <li>OnAdded events, listing all components that have been added to an entity</li>
 *     <li>OnChanged events, listing all components that have been modified on an entity</li>
 *     <li>OnRemoved events, listing all components that have been removed from an entity</li>
 * </ul>
 * This events are sent in that order. If a component has been involved in multiple events since
 * last time events were processed:
 * <ul>
 *     <li>If a component was removed and then readded, a OnChange event is sent instead</li>
 *     <li>If the component was added and/or changed, then removed, only an OnRemoved event is sent</li>
 * </ul>
 *
 * This class is not thread safe. It is expected all modification and sending events happens on a single
 * thread.
 */
public class LifecycleEventManager {

    private Map<EntityRef, OnAdded> onAddedEvents = new LinkedHashMap<>();
    private Map<EntityRef, OnChanged> onChangedEvents = new LinkedHashMap<>();
    private Map<EntityRef, OnRemoved> onRemovedEvents = new LinkedHashMap<>();

    /**
     * Notifies that a component has been added to an entity.
     * @param entity The entity the component has been added to
     * @param componentType The type of component that was added
     * @param <T> The type of component that was added
     */
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

    /**
     * Notifies that a component was modified on an entity
     * @param entity The entity the component was modified on
     * @param componentType The type of component that was modified
     * @param <T> The type of component that was modified
     */
    public <T extends Component<T>> void componentChanged(EntityRef entity, Class<T> componentType) {
        OnChanged onChanged = onChangedEvents.get(entity);
        if (onChanged == null) {
            onChanged = new OnChanged(componentType);
            onChangedEvents.put(entity, onChanged);
        } else {
            onChanged.getComponentTypes().add(componentType);
        }
    }

    /**
     * Notifies that a component was removed from an entity
     * @param entity The entity the component was removed from
     * @param component The removed component
     * @param <T> The type of component that was removed
     */
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

    /**
     * Sends all pending events
     * @param eventSystem The event system to send the events to
     */
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
