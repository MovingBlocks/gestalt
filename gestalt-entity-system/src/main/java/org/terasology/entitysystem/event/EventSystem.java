/*
 * Copyright 2016 MovingBlocks
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

import java.util.Collections;
import java.util.Set;

/**
 * An EventSystem manages the sending of events against entities. The internal mechanics of determining what event handlers will be sent each event is handled by
 * an EventProcessor - EventSystem deals with the higher level concern of managing the transactions used to process each event, how events are queued or processes
 * across threads and allowing a thread to wait for the completion of all pending events (and events they may send).
 * <p>
 * Events annotated as {@link Synchronous} will always be run immediately, either on the transaction of origin or a fresh transaction if no transaction is provided. Otherwise
 * it is up to the EventSystem implementation when events will be run - this may be immediately or when processEvents is called, and may be on the calling thread or one or
 * more background threads.
 */
public interface EventSystem {

    /**
     * @return A new transaction with event sending support
     */
    Transaction beginTransaction();

    /**
     * Updates the event processor. All events processed after the call is complete will use the new {@link EventProcessor}
     * @param processor The new EventProcessor for the EventSystem to use to process events.
     */
    void setEventProcessor(EventProcessor processor);

    /**
     * Sends an event against an entity. This event will be processed immediately if annotated as {@link Synchronous}, otherwise it will be processed at some future point.
     * A new transaction will be created to process the event.
     * @param event The event to send
     * @param entityId The entity to send the event against.
     */
    default void send(Event event, long entityId) {
        send(event, entityId, Collections.emptySet());
    }

    /**
     * Sends an event against an entity. This event will be processed immediately using the transaction of origin if annotated as {@link Synchronous}.
     * Otherwise it will be processed at some future point with a new transaction.
     * @param event The event to send.
     * @param entityId The entity to send the event against.
     * @param origin The transaction sending the event.
     */
    default void send(Event event, long entityId, Transaction origin) {
        send(event, entityId, origin, Collections.emptySet());
    }

    /**
     * Sends an event against an entity. This event will be processed immediately if annotated as {@link Synchronous}, otherwise it will be processed at some future point.
     * A new transaction will be created to process the event.
     * @param event The event to send.
     * @param entityId The entity to send the event against.
     * @param triggeringComponents The components triggering the event if any - only event handlers interested in these components will be notified.
     */
    void send(Event event, long entityId, Set<Class<? extends Component>> triggeringComponents);

    /**
     * Sends an event against an entity. This event will be processed immediately using the transaction of origin if annotated as {@link Synchronous}.
     * Otherwise it will be processed at some future point with a new transaction.
     * @param event The event to send.
     * @param entityId The entity to send the event against.
     * @param origin The transaction sending the event.
     * @param triggeringComponents The components triggering the event if any - only event handlers interested in these components will be notified.
     */
    void send(Event event, long entityId, Transaction origin, Set<Class<? extends Component>> triggeringComponents);

    /**
     * Blocks until all pending events and events sent by those events have been processed.
     * @throws InterruptedException
     */
    void processEvents() throws InterruptedException;

    /**
     * Clears all pending events and blocks for currently processing events to finish.
     * @throws InterruptedException
     */
    void clearPendingEvents() throws InterruptedException;


}
