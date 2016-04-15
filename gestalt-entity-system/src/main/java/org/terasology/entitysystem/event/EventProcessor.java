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

package org.terasology.entitysystem.event;

import org.terasology.entitysystem.Transaction;
import org.terasology.entitysystem.entity.Component;

import java.util.List;
import java.util.Set;

/**
 * Event system propagates events to registered handlers
 *
 * @author Immortius
 */
public interface EventProcessor {

    /**
     * @param eventClass
     */
    void registerEventClass(Class<? extends Event> eventClass);

    /**
     * Registers an event handler to receive events
     *
     * @param eventHandler
     * @param eventClass
     * @param requiredComponents
     * @param <T>
     */
    <T extends Event> void register(EventHandler<? super T> eventHandler, Class<T> eventClass, Class<? extends Component>... requiredComponents);

    /**
     * Registers an event handler to receive events
     *
     * @param eventHandler
     * @param eventHandlerProvider
     * @param eventClass
     * @param requiredComponents
     * @param <T>
     */
    <T extends Event> void register(EventHandler<? super T> eventHandler, Object eventHandlerProvider, Class<T> eventClass, Class<? extends Component>... requiredComponents);

    /**
     * Registers an event handler to receive events
     *
     * @param eventHandler
     * @param eventClass
     * @param insertBefore
     * @param requiredComponents
     * @param <T>
     */
    <T extends Event> void register(EventHandler<? super T> eventHandler, Class<T> eventClass, List<Class<?>> insertBefore, Class<? extends Component>... requiredComponents);

    /**
     * Registers an event handler to receive events
     *
     * @param eventHandler
     * @param eventHandlerProvider
     * @param eventClass
     * @param insertBefore
     * @param requiredComponents
     * @param <T>
     */
    <T extends Event> void register(EventHandler<? super T> eventHandler, Object eventHandlerProvider, Class<T> eventClass, List<Class<?>> insertBefore, Class<? extends Component>... requiredComponents);

    /**
     * Unregister an event handler
     *
     * @param eventHandlerProvider
     * @param eventClass
     * @param <T>
     */
    <T extends Event> void unregister(Object eventHandlerProvider, Class<T> eventClass);

    /**
     * Sends an event to all handlers
     *
     * @param entityId
     * @param event
     */
    EventResult send(long entityId, Event event, Transaction transaction);

    /**
     * Sends an event to event handlers that are interested in the particular triggering components. Any event handlers registered for the event that don't require one or more of the triggering components will be skipped.
     *
     * @param entityId
     * @param event
     * @param triggeringComponents
     */
    EventResult send(long entityId, Event event, Transaction transaction, Set<Class<? extends Component>> triggeringComponents);
}
