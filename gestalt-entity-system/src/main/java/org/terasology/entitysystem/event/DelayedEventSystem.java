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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitysystem.Transaction;
import org.terasology.entitysystem.entity.Component;
import org.terasology.entitysystem.entity.EntityTransaction;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.function.Supplier;

/**
 * A basic implementation of EventSystem. {@link Synchronous} events are processed immediately, other events are only processed when processEvents is called.
 */
@ThreadSafe
public class DelayedEventSystem implements EventSystem {
    private static final Logger logger = LoggerFactory.getLogger(ImmediateEventSystem.class);

    private transient EventProcessor eventProcessor;
    private final Supplier<EntityTransaction> transactionSupplier;
    private final BlockingDeque<PendingEventInfo> pendingEvents = Queues.newLinkedBlockingDeque();

    public DelayedEventSystem(EventProcessor eventProcessor, Supplier<EntityTransaction> transactionSupplier) {
        this.eventProcessor = eventProcessor;
        this.transactionSupplier = transactionSupplier;
    }

    @Override
    public Transaction beginTransaction() {
        return new Transaction(transactionSupplier.get(), this);
    }

    @Override
    public void setEventProcessor(EventProcessor processor) {
        this.eventProcessor = processor;
    }

    @Override
    public void send(Event event, long entityId, Set<Class<? extends Component>> triggeringComponents) {
        if (event.getClass().isAnnotationPresent(Synchronous.class)) {
            processEvent(event, entityId, triggeringComponents);
        } else {
            pendingEvents.addLast(new PendingEventInfo(event, entityId, triggeringComponents));
        }
    }

    @Override
    public void send(Event event, long entityId, Transaction origin, Set<Class<? extends Component>> triggeringComponents) {
        if (event.getClass().isAnnotationPresent(Synchronous.class) && origin != null) {
            eventProcessor.send(event, entityId, origin, triggeringComponents);
        } else {
            send(event, entityId, triggeringComponents);
        }
    }

    @Override
    public void processEvents() throws InterruptedException {
        List<PendingEventInfo> events = Lists.newArrayListWithExpectedSize(pendingEvents.size());
        while (!pendingEvents.isEmpty()) {
            pendingEvents.drainTo(events);
            for (PendingEventInfo eventInfo : events) {
                processEvent(eventInfo.getEvent(), eventInfo.getEntityId(), eventInfo.triggeringComponents);
            }
        }
    }

    private void processEvent(Event event, long entityId, Set<Class<? extends Component>> triggeringComponents) {
        boolean committed = false;
        while (!committed) {
            try {
                Transaction transaction = beginTransaction();
                eventProcessor.send(event, entityId, transaction, triggeringComponents);
                transaction.commit();
                committed = true;
            } catch (ConcurrentModificationException e) {
                logger.debug("Concurrency failure processing event {}, retrying", event, e);
            }
        }
    }

    @Override
    public void clearPendingEvents() throws InterruptedException {
        pendingEvents.clear();
    }

    private static class PendingEventInfo {
        private Event event;
        private long entityId;
        private Set<Class<? extends Component>> triggeringComponents;

        public PendingEventInfo(Event event, long entityId, Set<Class<? extends Component>> triggeringComponents) {
            this.event = event;
            this.entityId = entityId;
            this.triggeringComponents = ImmutableSet.copyOf(triggeringComponents);
        }

        public long getEntityId() {
            return entityId;
        }

        public Event getEvent() {
            return event;
        }

        public Set<Class<? extends Component>> getTriggeringComponents() {
            return triggeringComponents;
        }
    }
}
