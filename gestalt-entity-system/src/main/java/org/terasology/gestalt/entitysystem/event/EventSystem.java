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

package org.terasology.gestalt.entitysystem.event;

import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.gestalt.entitysystem.entity.EntityRef;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * An EventSystem manages the sending of events against entities.
 * <p>
 * Events annotated as {@link Synchronous} will always be run immediately. Otherwise the events will
 * be later - when later is depends on the implementation, but all queued events will be run after a call to {@link EventSystem#processEvents}.
 */
public interface EventSystem {

    /**
     * Sends an event against an entity. This event will be processed immediately (and on the same thread) if annotated as {@link Synchronous}, otherwise it will be processed at some future point.
     *
     * @param event  The event to send
     * @param entity The entity to send the event against.
     */
    default void send(Event event, EntityRef entity) {
        send(event, entity, Collections.emptySet());
    }

    /**
     * Sends an event against an entity. This event will be processed immediately (and on the same thread) if annotated as {@link Synchronous}, otherwise it will be processed at some future point.
     *
     * @param event                The event to send.
     * @param entity               The entity to send the event against.
     * @param triggeringComponents The components triggering the event if any - only event handlers interested in these components will be notified.
     */
    void send(Event event, EntityRef entity, Set<Class<? extends Component>> triggeringComponents);


    /**
     * Blocks until all pending events and events sent by those events have been processed.
     */
    void processEvents();

    /**
     * Clears all pending events and blocks for currently processing events to finish (if necessary)
     */
    void clearPendingEvents();

    /**
     * Registers an event handler, with its own class as its provider
     * @param eventClass The class of event to handle
     * @param eventHandler The handler for the event
     * @param requiredComponents Zero or more components that an entity must have for this handler to be relevant for it
     * @param <T> The class of event to handle
     */
    default <T extends Event> void registerHandler(Class<T> eventClass, EventHandler<? super T> eventHandler, Class<? extends Component> ... requiredComponents) {
        registerHandler(eventClass, eventHandler, eventHandler.getClass(), Collections.emptyList(), Collections.emptyList(), Arrays.asList(requiredComponents));
    }

    /**
     * Registers an event handler with its own class as its provider
     * @param eventClass The class of event to handle
     * @param eventHandler The handler for the event
     * @param requiredComponents Zero or more components that an entity must have for this handler to be relevant for it
     * @param <T> The class of event to handle
     */
    default <T extends Event> void registerHandler(Class<T> eventClass, EventHandler<? super T> eventHandler, Iterable<Class<? extends Component>> requiredComponents) {
        registerHandler(eventClass, eventHandler, eventHandler.getClass(), Collections.emptyList(), Collections.emptyList(), requiredComponents);
    }

    /**
     * Registers an event handler
     * @param eventClass The class of event to handle
     * @param eventHandler The handler for the event
     * @param before A collection of provider classes this handler should be processed before
     * @param after A collection of provider classes this handler should be processed after
     * @param requiredComponents Zero or more components that an entity must have for this handler to be relevant for it
     * @param <T> The class of event to handle
     */
    default <T extends Event> void registerHandler(Class<T> eventClass, EventHandler<? super T> eventHandler, Collection<Class<?>> before, Collection<Class<?>> after, Class<? extends Component> ... requiredComponents) {
        registerHandler(eventClass, eventHandler, eventHandler.getClass(), before, after, Arrays.asList(requiredComponents));
    }

    /**
     * Registers an event handler
     * @param eventClass The class of event to handle
     * @param eventHandler The handler for the event
     * @param before A collection of provider classes this handler should be processed before
     * @param after A collection of provider classes this handler should be processed after
     * @param requiredComponents Zero or more components that an entity must have for this handler to be relevant for it
     * @param <T> The class of event to handle
     */
    default <T extends Event> void registerHandler(Class<T> eventClass, EventHandler<? super T> eventHandler, Collection<Class<?>> before, Collection<Class<?>> after, Iterable<Class<? extends Component>> requiredComponents) {
        registerHandler(eventClass, eventHandler, eventHandler.getClass(), before, after, requiredComponents);
    }


    /**
     * Registers an event handler
     * @param eventClass The class of event to handle
     * @param eventHandler The handler for the event
     * @param provider A class that is "providing" the handler - this is used to sort handlers before or after other handlers.
     * @param before A collection of provider classes this handler should be processed before
     * @param after A collection of provider classes this handler should be processed after
     * @param requiredComponents Zero or more components that an entity must have for this handler to be relevant for it
     * @param <T> The class of event to handle
     */
    default <T extends Event> void registerHandler(Class<T> eventClass, EventHandler<? super T> eventHandler, Class<?> provider, Collection<Class<?>> before, Collection<Class<?>> after, Class<? extends Component> ... requiredComponents) {
        registerHandler(eventClass, eventHandler, provider, before, after, Arrays.asList(requiredComponents));
    }

    /**
     * Registers an event handler
     * @param eventClass The class of event to handle
     * @param eventHandler The handler for the event
     * @param provider A class that is "providing" the handler - this is used to sort handlers before or after other handlers.
     * @param before A collection of provider classes this handler should be processed before
     * @param after A collection of provider classes this handler should be processed after
     * @param requiredComponents Zero or more components that an entity must have for this handler to be relevant for it
     * @param <T> The class of event to handle
     */
    <T extends Event> void registerHandler(Class<T> eventClass, EventHandler<? super T> eventHandler, Class<?> provider, Collection<Class<?>> before, Collection<Class<?>> after, Iterable<Class<? extends Component>> requiredComponents);

    /**
     * Removes all handlers registered with the given provider class
     * @param provider The provider to remove handlers for
     * @return Whether any handlers were removed
     */
    boolean removeHandlers(Class<?> provider);

    /**
     * Removes the given handler
     * @param handler The handler to remove
     * @return Whether any handlers were removed
     */
    boolean removeHandler(EventHandler<?> handler);
}
