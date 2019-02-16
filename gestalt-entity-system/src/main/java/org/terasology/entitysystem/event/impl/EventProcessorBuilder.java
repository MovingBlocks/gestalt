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

import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import org.reflections.ReflectionUtils;
import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.event.Event;
import org.terasology.entitysystem.event.EventHandler;
import org.terasology.util.collection.KahnSorter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
public class EventProcessorBuilder {

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
    public EventProcessorBuilder addEventClass(Class<? extends Event> eventClass) {
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
        return addHandler(eventHandler, provider, eventClass, Arrays.asList(requiredComponents));
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
    public <T extends Event> EventProcessorBuilder addHandler(EventHandler<? super T> eventHandler, Class<?> provider, Class<T> eventClass, Iterable<Class<? extends Component>> requiredComponents) {
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
    public EventProcessorBuilder orderBefore(Class<?> provider) {
        lastAddedInfo.getBefore().add(provider);
        return this;
    }

    /**
     * @param provider The last registered handler will be ordered after all event handlers provided by this class.
     * @return The builder for call chaining.
     */
    public EventProcessorBuilder orderAfter(Class<?> provider) {
        lastAddedInfo.getAfter().add(provider);
        return this;
    }

    /**
     * @return A new EventProcessor set up with the registered event handlers and event classes.
     */
    public EventProcessor build() {
        SetMultimap<Class<? extends Event>, Class<? extends Event>> childEventLookup = buildEventHierarchy();

        ListMultimap<Class<? extends Event>, EventHandlerInfo> handlerLists = ArrayListMultimap.create(handlersToAdd);
        registerHandlersWithChildEvents(handlerLists, childEventLookup);

        ListMultimap<Class<? extends Event>, EventProcessor.EventHandlerRegistration> orderedEventHandlers = ArrayListMultimap.create();
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
            List<? extends EventProcessor.EventHandlerRegistration> sortedHandlers = sorter.sort().stream().map(x -> new EventProcessor.EventHandlerRegistration(x.eventHandler, x.requiredComponents)).collect(Collectors.toList());
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
            for (Class parent : ReflectionUtils.getAllSuperTypes(eventClass, Predicates.subtypeOf(Event.class))) {
                if (!Event.class.equals(parent)) {
                    childEventLookup.put(parent, eventClass);
                }
            }
        }
        return childEventLookup;
    }

    public void orderBeforeAll(Set<Class<?>> providerTypes) {
        providerTypes.forEach(this::orderBefore);
    }

    public void orderAfterAll(Set<Class<?>> providerTypes) {
        providerTypes.forEach(this::orderAfter);
    }

    private static class EventHandlerInfo {
        private EventHandler<?> eventHandler;
        private Class<? extends Event> eventClass;
        private Set<Class<? extends Component>> requiredComponents;
        private Class<?> provider;
        private Set<Class<?>> before = Sets.newLinkedHashSet();
        private Set<Class<?>> after = Sets.newLinkedHashSet();

        EventHandlerInfo(EventHandler<?> handler, Class<? extends Event> eventClass, Class<?> provider, Iterable<Class<? extends Component>> requiredComponents) {
            this.eventHandler = handler;
            this.eventClass = eventClass;
            this.provider = provider;
            this.requiredComponents = ImmutableSet.copyOf(requiredComponents);
        }

        Set<Class<? extends Component>> getRequiredComponents() {
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
