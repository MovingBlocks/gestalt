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
package org.terasology.entitysystem.event.internal;

import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitysystem.Transaction;
import org.terasology.entitysystem.entity.Component;
import org.terasology.entitysystem.event.Event;
import org.terasology.entitysystem.event.EventHandler;
import org.terasology.entitysystem.event.EventProcessor;
import org.terasology.entitysystem.event.EventResult;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An implementation of the EventSystem.
 *
 * @author Immortius
 */
public class EventProcessorImpl implements EventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(EventProcessorImpl.class);
    private ListMultimap<Class<? extends Event>, EventHandlerInfo> eventHandlers = ArrayListMultimap.create();
    private SetMultimap<Class<? extends Event>, Class<? extends Event>> childEvents = HashMultimap.create();

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    @SafeVarargs
    @Override
    public final <T extends Event> void register(EventHandler<? super T> eventHandler, Class<T> eventClass, Class<? extends Component>... requiredComponents) {
        register(eventHandler, eventHandler, eventClass, requiredComponents);
    }

    @Override
    public <T extends Event> void register(EventHandler<? super T> eventHandler, Object eventHandlerProvider, Class<T> eventClass, Class<? extends Component>... requiredComponents) {
        register(eventHandler, eventHandler, eventClass, Collections.emptyList(), requiredComponents);
    }

    @Override
    public <T extends Event> void register(EventHandler<? super T> eventHandler, Class<T> eventClass, List<Class<?>> insertBefore, Class<? extends Component>... requiredComponents) {
        register(eventHandler, eventHandler, eventClass, insertBefore, requiredComponents);
    }

    @Override
    public <T extends Event> void register(EventHandler<? super T> eventHandler, Object eventHandlerProvider, Class<T> eventClass, List<Class<?>> insertBefore, Class<? extends Component>... requiredComponents) {
        addEventHandler(eventClass, new EventHandlerInfo<T>(eventHandler, requiredComponents), insertBefore);
    }

    private void addEventHandler(Class<? extends Event> type, EventHandlerInfo handler, List<Class<?>> before) {
        readWriteLock.writeLock().lock();
        try {
            insertBefore(eventHandlers.get(type), handler, before);
            for (Class<? extends Event> childType : childEvents.get(type)) {
                insertBefore(eventHandlers.get(childType), handler, before);
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }

    }

    private void insertBefore(List<EventHandlerInfo> handlerList, EventHandlerInfo handler, List<Class<?>> beforeClasses) {
        if (!beforeClasses.isEmpty()) {
            for (int i = 0; i < handlerList.size(); ++i) {
                EventHandlerInfo eventHandlerInfo = handlerList.get(i);
                for (Class<?> beforeClass : beforeClasses) {
                    if (beforeClass.isAssignableFrom(eventHandlerInfo.getProvider().getClass())) {
                        handlerList.add(i, handler);
                        return;
                    }
                }
            }
        }
        handlerList.add(handler);
    }


    @Override
    public <T extends Event> void unregister(Object provider, Class<T> eventClass) {
        readWriteLock.writeLock().lock();
        try {
            removeHandlerFrom(provider, eventHandlers.get(eventClass));
            for (Class<? extends Event> childClass : childEvents.get(eventClass)) {
                removeHandlerFrom(provider, eventHandlers.get(childClass));
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private void removeHandlerFrom(Object provider, List<EventHandlerInfo> list) {
        Iterator<EventHandlerInfo> iter = list.iterator();
        while (iter.hasNext()) {
            if (iter.next().getProvider().equals(provider)) {
                iter.remove();
                break;
            }
        }
    }

    @Override
    public EventResult send(long entityId, Event event, Transaction transaction) {
        return send(entityId, event, transaction, Collections.emptySet());
    }

    @Override
    public EventResult send(long entityId, Event event, Transaction transaction, Set<Class<? extends Component>> triggeringComponents) {
        readWriteLock.readLock().lock();
        try {
            EventResult result = EventResult.CONTINUE;
            for (EventHandlerInfo handler : eventHandlers.get(event.getClass())) {
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
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    private boolean validToInvoke(EventHandlerInfo<?> handler, Set<Class<? extends Component>> targetComponents, Set<Class<? extends Component>> triggeringComponents) {
        for (Class<? extends Component> component : handler.getFilterComponents()) {
            if (!targetComponents.contains(component)) {
                return false;
            }
        }
        if (!triggeringComponents.isEmpty()) {
            for (Class<? extends Component> component : handler.getFilterComponents()) {
                if (triggeringComponents.contains(component)) {
                    return true;
                }
            }
        } else {
            return true;
        }
        return false;
    }


    @Override
    @SuppressWarnings("unchecked")
    public void registerEventClass(Class<? extends Event> eventClass) {
        readWriteLock.writeLock().lock();
        try {
            logger.debug("Registering event {}", eventClass.getSimpleName());
            for (Class parent : ReflectionUtils.getAllSuperTypes(eventClass, Predicates.assignableFrom(Event.class))) {
                if (!Event.class.equals(parent)) {
                    childEvents.put(parent, eventClass);
                }
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private static class EventHandlerInfo<T extends Event> {
        private Object eventHandlerProvider;
        private EventHandler<? super T> receiver;
        private ImmutableList<Class<? extends Component>> components;

        @SafeVarargs
        public EventHandlerInfo(EventHandler<? super T> receiver, Class<? extends Component>... components) {
            this(receiver, receiver, components);
        }

        @SafeVarargs
        public EventHandlerInfo(EventHandler<? super T> receiver, Object eventHandlerProvider, Class<? extends Component>... components) {
            this.receiver = receiver;
            this.eventHandlerProvider = eventHandlerProvider;
            this.components = ImmutableList.copyOf(components);
        }

        public List<Class<? extends Component>> getFilterComponents() {
            return components;
        }

        @SuppressWarnings("unchecked")
        public EventResult invoke(long entity, Event event, Transaction transaction) {
            return receiver.onEvent((T) event, entity, transaction);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof EventHandlerInfo) {
                EventHandlerInfo other = (EventHandlerInfo) obj;
                return Objects.equals(receiver, other.receiver);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(receiver);
        }

        public Object getProvider() {
            return eventHandlerProvider;
        }
    }
}
