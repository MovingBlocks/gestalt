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

import java.util.Collections;
import java.util.Set;

/**
 * An EventSystem manages the sending of events against entities.
 * <p>
 * Events annotated as {@link Synchronous} will always be run immediately. Otherwise the events will
 * be run later.
 *
 * <p>The internal mechanics of determining what event handlers will be sent each event is handled by
 * an EventProcessor - EventSystem deals with the higher level concern of managing how events are queued
 * and run.
 */
public interface EventSystem {

    /**
     * Sends an event against an entity. This event will be processed immediately if annotated as {@link Synchronous}, otherwise it will be processed at some future point.
     *
     * @param event  The event to send
     * @param entity The entity to send the event against.
     */
    default void send(Event event, EntityRef entity) {
        send(event, entity, Collections.emptySet());
    }

    /**
     * Sends an event against an entity. This event will be processed immediately if annotated as {@link Synchronous}, otherwise it will be processed at some future point.
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
     * Clears all pending events and blocks for currently processing events to finish (in necessary)
     */
    void clearPendingEvents();


}
