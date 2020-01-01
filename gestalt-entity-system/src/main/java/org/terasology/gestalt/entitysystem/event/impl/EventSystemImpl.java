package org.terasology.gestalt.entitysystem.event.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.gestalt.entitysystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.event.Event;
import org.terasology.gestalt.entitysystem.event.EventHandler;
import org.terasology.gestalt.entitysystem.event.EventSystem;
import org.terasology.gestalt.entitysystem.event.Synchronous;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Threadsafe event system implementation. Events are queued up from any thread (run immediately if {@link Synchronous}, and then processed
 * when processEvents is called. processEvents can only be called on a single thread at a time and is blocking until completed.
 */
@ThreadSafe
public class EventSystemImpl implements EventSystem {

    private static final Logger logger = LoggerFactory.getLogger(EventSystemImpl.class);

    private final BlockingQueue<PendingEventInfo> pendingEvents = new LinkedBlockingQueue<>();
    private final Map<Class<? extends Event>, EventProcessor> eventProcessorLookup = new LinkedHashMap<>();

    @Override
    public void send(Event event, EntityRef entity, Set<Class<? extends Component>> triggeringComponents) {
        if (event.getClass().isAnnotationPresent(Synchronous.class)) {
            processEvent(event, entity, triggeringComponents);
        } else {
            pendingEvents.add(new PendingEventInfo(event, entity, triggeringComponents));
        }
    }

    @Override
    public synchronized void processEvents() {
        List<PendingEventInfo> events = Lists.newArrayListWithExpectedSize(pendingEvents.size());
        while (!pendingEvents.isEmpty()) {
            pendingEvents.drainTo(events);
            for (PendingEventInfo eventInfo : events) {
                processEvent(eventInfo.getEvent(), eventInfo.getEntity(), eventInfo.triggeringComponents);
            }
        }
    }

    @Override
    public void clearPendingEvents() {
        pendingEvents.clear();
    }

    private synchronized void processEvent(Event event, EntityRef entity, Set<Class<? extends Component>> triggeringComponents) {
        if (entity.exists()) {
            EventProcessor eventProcessor = getEventProcessor(event.getClass());
            eventProcessor.process(event, entity, triggeringComponents);
        }
    }

    @Override
    public synchronized <T extends Event> void registerHandler(Class<T> eventClass, EventHandler<? super T> eventHandler, Class<?> provider, Collection<Class<?>> before, Collection<Class<?>> after, Iterable<Class<? extends Component>> requiredComponents) {
        EventProcessor eventProcessor = getEventProcessor(eventClass);
        eventProcessor.registerHandler(eventHandler, provider, before, after, requiredComponents);
    }

    @Override
    public synchronized boolean removeHandlers(Class<?> provider) {
        boolean result = false;
        for (EventProcessor processor : eventProcessorLookup.values()) {
            result |= processor.removeProvider(provider);
        }
        return result;
    }

    @Override
    public synchronized boolean removeHandler(EventHandler<?> handler) {
        boolean result = false;
        for (EventProcessor processor : eventProcessorLookup.values()) {
            result |= processor.removeHandler(handler);
        }
        return result;
    }

    private synchronized EventProcessor getEventProcessor(Class<? extends Event> eventClass) {
        EventProcessor eventProcessor = eventProcessorLookup.get(eventClass);
        if (eventProcessor == null) {
            eventProcessor = createEventProcessor(eventClass);
        }
        return eventProcessor;
    }

    private EventProcessor createEventProcessor(Class<? extends Event> eventClass) {
        EventProcessor processor;
        if (Event.class.isAssignableFrom(eventClass.getSuperclass())) {
            EventProcessor parentProcessor = getEventProcessor((Class<? extends Event>) eventClass.getSuperclass());
            processor = new EventProcessor(parentProcessor);
        } else {
            processor = new EventProcessor();
        }
        eventProcessorLookup.put(eventClass, processor);
        return processor;
    }

    private static class PendingEventInfo {
        private final Event event;
        private final EntityRef entity;
        private final Set<Class<? extends Component>> triggeringComponents;

        private PendingEventInfo(Event event, EntityRef entity, Set<Class<? extends Component>> triggeringComponents) {
            this.event = event;
            this.entity = entity;
            this.triggeringComponents = ImmutableSet.copyOf(triggeringComponents);
        }

        public EntityRef getEntity() {
            return entity;
        }

        public Event getEvent() {
            return event;
        }

        public Set<Class<? extends Component>> getTriggeringComponents() {
            return triggeringComponents;
        }
    }
}
