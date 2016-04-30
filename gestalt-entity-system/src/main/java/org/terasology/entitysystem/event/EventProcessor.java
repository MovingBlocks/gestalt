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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import org.terasology.entitysystem.Transaction;
import org.terasology.entitysystem.entity.Component;

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
     * @param entityId    The id of the entity to send the event against
     * @param event       The event to send
     * @param transaction The transaction to process the event within.
     * @return The result of the event. If any event handler returns EventResult.CANCEL then that is returned, otherwise the result will be EventResult.COMPLETE.
     */
    public EventResult send(long entityId, Event event, Transaction transaction) {
        return send(entityId, event, transaction, Collections.emptySet());
    }

    /**
     * Sends an event against an entity, within the given transaction.
     *
     * @param entityId             The id of the entity to send the event against
     * @param event                The event to send
     * @param transaction          The transaction to process the event within.
     * @param triggeringComponents The components triggering the event, if any. If present, then only event handlers specifically interested in those components will
     *                             be notified of the event.
     *                             <p>This is specifically useful for lifecycle events - for example, it allows an ComponentAddedEvent to be sent only to event handlers
     *                             that are interested in the specific component that was added. Without a triggering component the ComponentAddedEvent would be sent to
     *                             all EventHandlers interested in components the entity has.
     * @return The result of the event. If any event handler returns EventResult.CANCEL then that is returned, otherwise the result will be EventResult.COMPLETE.
     */
    public EventResult send(long entityId, Event event, Transaction transaction, Set<Class<? extends Component>> triggeringComponents) {
        EventResult result = EventResult.CONTINUE;
        for (EventHandlerRegistration handler : eventHandlers.get(event.getClass())) {
            if (validToInvoke(handler, transaction.getEntityComposition(entityId), triggeringComponents)) {
                result = handler.invoke(entityId, event, transaction);
                switch (result) {
                    case COMPLETE:
                    case CANCEL:
                        return result;
                    default:
                        // Continue
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
         * @param entity      The entity the event is being sent against
         * @param event       The event itself
         * @param transaction The transaction to process the event within
         * @return The result of running the event, indicating whether processing should continue or halt
         */
        @SuppressWarnings("unchecked")
        public EventResult invoke(long entity, Event event, Transaction transaction) {
            return receiver.onEvent(event, entity, transaction);
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
