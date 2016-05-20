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

package org.terasology.entitysystem.event.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitysystem.entity.Component;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.event.Event;
import org.terasology.entitysystem.event.EventHandler;
import org.terasology.entitysystem.event.EventResult;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The core event processing logic. When an event is sent against an entity, the EventProcessor propagates the event through an ordered list of relevant event handlers,
 * filtering out handlers that are not appropriate for the target entity based on the components it has.  All of this occurs within a provided transaction. If an event handler
 * returns EventResult.COMPLETE or EventResult.CANCEL the event processing is halted.
 *
 * @author Immortius
 * @see EventProcessorBuilder
 */
@ThreadSafe
public class EventProcessor {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessor.class);
    private final ListMultimap<Class<? extends Event>, EventHandlerRegistration> eventHandlers;

    /**
     * Initialises the EventProcessor with the ordered list of EventHandlers for each event type.
     *
     * @param eventHandlers A {@link ListMultimap} giving an ordered list of event handlers for each event type. Events will be propagated through the handlers in the
     *                      order they are appear in each list.
     */
    public EventProcessor(ListMultimap<Class<? extends Event>, EventHandlerRegistration> eventHandlers) {
        this.eventHandlers = ArrayListMultimap.create(eventHandlers);
    }

    /**
     * @return A new builder for constructing an EventProcessor.
     */
    public static EventProcessorBuilder newBuilder() {
        return new EventProcessorBuilder();
    }

    /**
     * Sends an event against an entity, within the given transaction.
     *
     * @param event  The event to send
     * @param entity The entity to send the event against
     * @return The result of the event. If any event handler returns EventResult.CANCEL then that is returned, otherwise the result will be EventResult.COMPLETE.
     */
    public EventResult send(Event event, EntityRef entity) {
        return send(event, entity, Collections.emptySet());
    }

    /**
     * Sends an event against an entity, within the given transaction.
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
    public EventResult send(Event event, EntityRef entity, Set<Class<? extends Component>> triggeringComponents) {
        EventResult result = EventResult.CONTINUE;
        for (EventHandlerRegistration handler : eventHandlers.get(event.getClass())) {
            if (validToInvoke(handler, entity.getComponentTypes(), triggeringComponents)) {
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

        switch (result) {
            case CONTINUE:
                return EventResult.COMPLETE;
            default:
                return result;
        }
    }

    private boolean validToInvoke(EventHandlerRegistration handler, Set<Class<? extends Component>> targetComponents, Set<Class<? extends Component>> triggeringComponents) {
        for (Class<? extends Component> component : handler.getRequiredComponents()) {
            if (!targetComponents.contains(component)) {
                return false;
            }
        }
        if (!triggeringComponents.isEmpty()) {
            for (Class<? extends Component> component : handler.getRequiredComponents()) {
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
     * A registration of an EventHandler. Includes the handler to call and the components that an entity must have for the handler to be called.
     */
    public static class EventHandlerRegistration {
        private EventHandler receiver;
        private ImmutableList<Class<? extends Component>> components;

        /**
         * @param receiver           The event handler
         * @param requiredComponents The components an entity must have for the receiver to be called.
         */
        public EventHandlerRegistration(EventHandler<?> receiver, Iterable<Class<? extends Component>> requiredComponents) {
            this.receiver = receiver;
            this.components = ImmutableList.copyOf(requiredComponents);
        }

        /**
         * @return The components required by this event handler
         */
        public List<Class<? extends Component>> getRequiredComponents() {
            return components;
        }

        /**
         * Invokes the event handler
         *
         * @param event  The event itself
         * @param entity The entity the event is being sent against
         * @return The result of running the event, indicating whether processing should continue or halt
         */
        @SuppressWarnings("unchecked")
        public EventResult invoke(Event event, EntityRef entity) {
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
