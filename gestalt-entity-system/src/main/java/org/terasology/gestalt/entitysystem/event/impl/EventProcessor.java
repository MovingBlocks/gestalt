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

package org.terasology.gestalt.entitysystem.event.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.gestalt.entitysystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.event.Event;
import org.terasology.gestalt.entitysystem.event.EventHandler;
import org.terasology.gestalt.entitysystem.event.EventResult;
import org.terasology.gestalt.util.collection.KahnSorter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * The core event processing logic, for a single type of event. When an event is sent against an entity, the EventProcessor
 * propagates the event through an ordered list of registered event handlers,
 * filtering out handlers that are not appropriate for the target entity based on the components it
 * has.  All of this occurs within a provided transaction. If an event handler
 * returns EventResult.COMPLETE or EventResult.CANCEL the event processing is halted.
 *
 * @author Immortius
 */
public class EventProcessor {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessor.class);
    private final List<EventProcessor> children = new ArrayList<>();
    private final List<EventHandlerRegistration> eventHandlers = new CopyOnWriteArrayList<>();
    private final Multimap<Class<?>, EventHandlerRegistration> eventHandlersByProvider = ArrayListMultimap.create();

    public EventProcessor() {
        this(null);
    }

    /**
     * Initialises the EventProcessor with the ordered list of EventHandlers for each event type.
     *
     * @param parent The event processor for the parent event, if any
     */
    public EventProcessor(EventProcessor parent) {
        if (parent != null) {
            parent.children.add(this);
            this.eventHandlers.addAll(parent.eventHandlers);
            this.eventHandlersByProvider.putAll(parent.eventHandlersByProvider);
        }
    }


    /**
     * Sends an event against an entity
     *
     * @param event  The event to send
     * @param entity The entity to send the event against
     * @return The result of the event. If any event handler returns EventResult.CANCEL then that
     * is returned, otherwise the result will be EventResult.COMPLETE.
     */
    public EventResult process(Event event, EntityRef entity) {
        return process(event, entity, Collections.emptySet());
    }

    /**
     * Sends an event against an entity
     *
     * @param event                The event to send
     * @param entity               The entity to send the event against
     * @param triggeringComponents The components triggering the event, if any. If present, then only event handlers specifically interested in those components will
     *                             be notified of the event.
     *                             <p>This is specifically useful for lifecycle events - for example, it allows an ComponentAddedEvent to be sent only to event handlers
     *                             that are interested in the specific component that was added. Without a triggering component the ComponentAddedEvent would be sent to
     *                             all EventHandlers interested in components the entity has.
     * @return The result of the event. If any event handler returns EventResult.CANCEL then that is returned, otherwise the result will be EventResult.COMPLETE.
     */
    public EventResult process(Event event, EntityRef entity, Set<Class<? extends Component>> triggeringComponents) {
        EventResult result = EventResult.CONTINUE;
        Set<Class<? extends Component>> componentTypes = entity.getComponentTypes();
        for (EventHandlerRegistration handler : eventHandlers) {
            if (validToInvoke(handler, componentTypes, triggeringComponents)) {
                try {
                    result = handler.invoke(event, entity);
                    switch (result) {
                        case COMPLETE:
                        case CANCEL:
                            return result;
                        default:
                            // Continue
                    }
                } catch (RuntimeException e) {
                    logger.error("Exception thrown when processing event {}", event.getClass(), e);
                }
            }
        }

        if (result == EventResult.CONTINUE) {
            return EventResult.COMPLETE;
        }
        return result;
    }

    private boolean validToInvoke(EventHandlerRegistration handler, Set<Class<? extends Component>> targetComponents, Set<Class<? extends Component>> triggeringComponents) {
        for (Class<? extends Component> component : handler.components) {
            if (!targetComponents.contains(component) && !triggeringComponents.contains(component)) {
                return false;
            }
        }
        if (!triggeringComponents.isEmpty()) {
            for (Class<? extends Component> component : handler.components) {
                if (triggeringComponents.contains(component)) {
                    return true;
                }
            }
        } else {
            return true;
        }
        return false;
    }

    /**
     * Registers an event handler
     * @param eventHandler The handler to register
     * @param provider The class providing the handler (can be the handler's class)
     * @param before Any providers whose handlers should be invoked before eventHandler
     * @param after Any providers whose handlers should be invoked after eventHandler
     * @param requiredComponents Any components that are required for this handler to be called
     */
    public synchronized void registerHandler(EventHandler<?> eventHandler, Class<?> provider, Collection<Class<?>> before, Collection<Class<?>> after, Iterable<Class<? extends Component>> requiredComponents) {
        children.forEach(child -> child.registerHandler(eventHandler, provider, before, after, requiredComponents));
        EventHandlerRegistration eventHandlerRegistration = new EventHandlerRegistration(eventHandler, before, after, requiredComponents);
        eventHandlersByProvider.put(provider, eventHandlerRegistration);
        sortHandlers();
    }

    private void sortHandlers() {
        KahnSorter<EventHandlerRegistration> sorter = new KahnSorter<>();
        sorter.addNodes(eventHandlersByProvider.values());
        for (EventHandlerRegistration eventHandler : eventHandlersByProvider.values()) {
            for (Class<?> beforeProvider : eventHandler.before) {
                eventHandlersByProvider.get(beforeProvider).forEach(x -> sorter.addEdge(eventHandler, x));
            }
            for (Class<?> afterProvider : eventHandler.after) {
                eventHandlersByProvider.get(afterProvider).forEach(x -> sorter.addEdge(x, eventHandler));
            }
        }
        eventHandlers.clear();
        eventHandlers.addAll(sorter.sort());
    }

    /**
     * Removes all handlers provided by the given provider
     * @param provider The provider to remove handlers for
     * @return Whether any handlers were removed
     */
    public synchronized boolean removeProvider(Class<?> provider) {
        return eventHandlers.removeAll(eventHandlersByProvider.removeAll(provider));
    }

    /**
     * Removes a specific handler
     * @param handler The handler to remove
     * @return Whether the handler was removed
     */
    public synchronized boolean removeHandler(EventHandler<?> handler) {
        if (eventHandlersByProvider.values().removeIf(x -> x.receiver.equals(handler))) {
            return eventHandlers.removeIf(x -> x.receiver.equals(handler));
        }
        return false;
    }

    /**
     * A registration of an EventHandler. Includes the handler to call and the components that an entity must have for the handler to be called.
     */
    private static class EventHandlerRegistration {
        private final EventHandler receiver;
        private final ImmutableList<Class<? extends Component>> components;
        private final List<Class<?>> before;
        private final List<Class<?>> after;

        /**
         * @param receiver           The event handler
         * @param requiredComponents The components an entity must have for the receiver to be called.
         */
        EventHandlerRegistration(EventHandler<?> receiver, Iterable<Class<?>> before, Iterable<Class<?>> after, Iterable<Class<? extends Component>> requiredComponents) {
            this.receiver = receiver;
            this.components = ImmutableList.copyOf(requiredComponents);
            this.before = ImmutableList.copyOf(before);
            this.after = ImmutableList.copyOf(after);
        }

        /**
         * Invokes the event handler
         *
         * @param event  The event itself
         * @param entity The entity the event is being sent against
         * @return The result of running the event, indicating whether processing should continue or halt
         */
        @SuppressWarnings("unchecked")
        EventResult invoke(Event event, EntityRef entity) {
            return receiver.onEvent(event, entity);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof EventHandlerRegistration) {
                EventHandlerRegistration other = (EventHandlerRegistration) obj;
                return Objects.equals(receiver, other.receiver);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(receiver);
        }
    }

}
