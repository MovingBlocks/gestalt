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

import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.reflections.ReflectionUtils;
import org.terasology.entitysystem.Transaction;
import org.terasology.entitysystem.entity.Component;
import org.terasology.util.collection.KahnSorter;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The core event processing logic. When an event is sent against an entity, the EventProcessor propagates the event through an ordered list of relevant event handlers,
 * filtering out handlers that are not appropriate for the target entity based on the components it has.  All of this occurs within a provided transaction. If an event handler
 * returns EventResult.COMPLETE or EventResult.CANCEL the event processing is halted.
 * @see EventProcessorBuilder
 *
 * @author Immortius
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
         * @param receiver The event handler
         * @param requiredComponents The components an entity must have for the receiver to be called.
         */
        @SafeVarargs
        public EventHandlerRegistration(EventHandler<?> receiver, Class<? extends Component>... requiredComponents) {
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
         * @param entity The entity the event is being sent against
         * @param event The event itself
         * @param transaction The transaction to process the event within
         * @return
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

    /**
     * A builder/factory for EventProcessor. This builder provides provides convenience by:
     * <ul>
     * <li>Automatically adds sets up event handlers to process child event classes for their types they are registered for (e.g. If an event handler is registered to
     * handle ParentEvent class, then it will also handle ChildEvent class, where ChildEvent inherits ParentEvent).  This requires all event types to be registered to the
     * builder before building the EventProcessor.</li>
     * <li>Sorts event handlers based on their "providing" classes and any ordering constraints specified. Sorting, and thus building the EventProcessor will fail if there
     * are circular dependencies in the ordering constraints (e.g. A must be before B, B must be before C, C must be before A).</li>
     * </ul>
     */
    public static final class EventProcessorBuilder {

        private Set<Class<? extends Event>> eventClasses = Sets.newLinkedHashSet();
        private ListMultimap<Class<? extends Event>, EventHandlerInfo> handlersToAdd = ArrayListMultimap.create();
        private ListMultimap<Class<?>, EventHandlerInfo> handlersByProvider = ArrayListMultimap.create();
        private EventHandlerInfo lastAddedInfo;

        /**
         * Registers an event class. This allows it to be considered for the purposes of inheriting event handlers from parent event classes.
         * <p>All event classes which have events explicitly registered against them are automatically registered - so this is only needed for any event classes which do not
         * have specific event handlers, but instead are handled only by handlers on parent classes. If an event classes is not registered via either mechanism, then it will
         * not be received by any event handlers.
         *
         * @param eventClass The eventClass to register
         * @return The builder for call chaining.
         */
        public final EventProcessorBuilder addEventClass(Class<? extends Event> eventClass) {
            eventClasses.add(eventClass);
            return this;
        }

        /**
         * Registers an event handler to receive events. The event handler will be marked as being provided by the class of the event handler itself.
         *
         * @param eventHandler       The handler to register
         * @param eventClass         The type of event to register it to receive
         * @param requiredComponents The components an entity must have for the event handler to receive the event
         * @param <T>                The type of the event
         * @return The builder for call chaining.
         */
        @SafeVarargs
        public final <T extends Event> EventProcessorBuilder addHandler(EventHandler<? super T> eventHandler, Class<T> eventClass, Class<? extends Component>... requiredComponents) {
            return addHandler(eventHandler, eventHandler.getClass(), eventClass, requiredComponents);
        }

        /**
         * Registers an event handler to receive events
         *
         * @param eventHandler       The handler to register
         * @param provider           The class "providing" the event handler. This can be used constrain the ordering of handlers.
         * @param eventClass         The type of event to register it to receive
         * @param requiredComponents The components an entity must have for the event handler to receive the event
         * @param <T>                The type of the event
         * @return The builder for call chaining.
         */
        @SafeVarargs
        public final <T extends Event> EventProcessorBuilder addHandler(EventHandler<? super T> eventHandler, Class<?> provider, Class<T> eventClass, Class<? extends Component>... requiredComponents) {
            addEventClass(eventClass);
            lastAddedInfo = new EventHandlerInfo(eventHandler, eventClass, provider, requiredComponents);
            handlersToAdd.put(eventClass, lastAddedInfo);
            handlersByProvider.put(provider, lastAddedInfo);
            return this;
        }

        /**
         * @param provider The last registered handler will be ordered before all event handlers provided by this class.
         * @return The builder for call chaining.
         */
        public final EventProcessorBuilder orderBefore(Class<?> provider) {
            lastAddedInfo.getBefore().add(provider);
            return this;
        }

        /**
         * @param provider The last registered handler will be ordered after all event handlers provided by this class.
         * @return The builder for call chaining.
         */
        public final EventProcessorBuilder orderAfter(Class<?> provider) {
            lastAddedInfo.getAfter().add(provider);
            return this;
        }

        /**
         * @return A new EventProcessor set up with the registered event handlers and event classes.
         */
        public final EventProcessor build() {
            SetMultimap<Class<? extends Event>, Class<? extends Event>> childEventLookup = buildEventHierarchy();

            ListMultimap<Class<? extends Event>, EventHandlerInfo> handlerLists = ArrayListMultimap.create(handlersToAdd);
            registerHandlersWithChildEvents(handlerLists, childEventLookup);

            ListMultimap<Class<? extends Event>, EventHandlerRegistration> orderedEventHandlers = ArrayListMultimap.create();
            for (Class<? extends Event> eventClass : eventClasses) {
                KahnSorter<EventHandlerInfo> sorter = new KahnSorter<>();
                sorter.addNodes(handlerLists.get(eventClass));
                for (EventHandlerInfo info : handlerLists.get(eventClass)) {
                    for (Class<?> provider : info.getBefore()) {
                        handlersByProvider.get(provider).stream().filter(beforeHandler -> beforeHandler.getEventClass() == eventClass).forEach(beforeHandler -> {
                            sorter.addEdge(info, beforeHandler);
                        });
                    }
                    for (Class<?> provider : info.getAfter()) {
                        handlersByProvider.get(provider).stream().filter(afterHandler -> afterHandler.getEventClass() == eventClass).forEach(afterHandler -> {
                            sorter.addEdge(afterHandler, info);
                        });
                    }
                }
                List<? extends EventHandlerRegistration> sortedHandlers = sorter.sort().stream().map(x -> new EventHandlerRegistration(x.eventHandler, x.requiredComponents)).collect(Collectors.toList());
                orderedEventHandlers.putAll(eventClass, sortedHandlers);
            }

            return new EventProcessor(orderedEventHandlers);
        }

        private void registerHandlersWithChildEvents(ListMultimap<Class<? extends Event>, EventHandlerInfo> handlerLists, SetMultimap<Class<? extends Event>, Class<? extends Event>> childEventLookup) {
            for (Class<? extends Event> eventClass : childEventLookup.keySet()) {
                for (Class<? extends Event> childEventClass : childEventLookup.get(eventClass)) {
                    handlerLists.putAll(childEventClass, handlerLists.get(eventClass));
                }
            }
        }

        @SuppressWarnings("unchecked")
        private SetMultimap<Class<? extends Event>, Class<? extends Event>> buildEventHierarchy() {
            SetMultimap<Class<? extends Event>, Class<? extends Event>> childEventLookup = HashMultimap.create();
            for (Class eventClass : eventClasses) {
                for (Class parent : ReflectionUtils.getAllSuperTypes(eventClass, Predicates.assignableFrom(Event.class))) {
                    if (!Event.class.equals(parent)) {
                        childEventLookup.put(parent, eventClass);
                    }
                }
            }
            return childEventLookup;
        }

        private static class EventHandlerInfo {
            private EventHandler<?> eventHandler;
            private Class<? extends Event> eventClass;
            private Class<? extends Component>[] requiredComponents;
            private Class<?> provider;
            private Set<Class<?>> before = Sets.newLinkedHashSet();
            private Set<Class<?>> after = Sets.newLinkedHashSet();

            EventHandlerInfo(EventHandler<?> handler, Class<? extends Event> eventClass, Class<?> provider, Class<? extends Component>[] requiredComponents) {
                this.eventHandler = handler;
                this.eventClass = eventClass;
                this.provider = provider;
                this.requiredComponents = requiredComponents;
            }

            Class<? extends Component>[] getRequiredComponents() {
                return requiredComponents;
            }

            Class<? extends Event> getEventClass() {
                return eventClass;
            }

            EventHandler<?> getEventHandler() {
                return eventHandler;
            }

            Class<?> getProvider() {
                return provider;
            }

            Collection<Class<?>> getBefore() {
                return before;
            }

            Collection<Class<?>> getAfter() {
                return after;
            }
        }

    }
}
