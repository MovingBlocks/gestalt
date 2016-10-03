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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.event.Event;
import org.terasology.entitysystem.event.EventResult;
import org.terasology.entitysystem.event.Synchronous;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;

/**
 * A basic implementation of EventSystem. {@link Synchronous} events are processed immediately, other events are only processed when processEvents is called.
 */
@ThreadSafe
public class DelayedEventSystem extends AbstractEventSystem {
    private static final Logger logger = LoggerFactory.getLogger(ImmediateEventSystem.class);

    private transient EventProcessor eventProcessor;
    private final EntityManager entityManager;
    private final BlockingDeque<PendingEventInfo> pendingEvents = Queues.newLinkedBlockingDeque();

    public DelayedEventSystem(EventProcessor eventProcessor, EntityManager entityManager) {
        super(entityManager);
        this.eventProcessor = eventProcessor;
        this.entityManager = entityManager;
    }

    @Override
    public void setEventProcessor(EventProcessor processor) {
        this.eventProcessor = processor;
    }

    @Override
    public void processEvents() throws InterruptedException {
        List<PendingEventInfo> events = Lists.newArrayListWithExpectedSize(pendingEvents.size());
        while (!pendingEvents.isEmpty()) {
            pendingEvents.drainTo(events);
            for (PendingEventInfo eventInfo : events) {
                doEvent(eventInfo.getEvent(), eventInfo.getEntity(), eventInfo.triggeringComponents);
            }
        }
    }

    @Override
    protected void processEvent(Event event, EntityRef entity, Set<Class<? extends Component>> triggeringComponents) {
        if (event.getClass().isAnnotationPresent(Synchronous.class)) {
            doEvent(event, entity, triggeringComponents);
        } else {
            pendingEvents.addLast(new PendingEventInfo(event, entity, triggeringComponents));
        }
    }

    private void doEvent(Event event, EntityRef entity, Set<Class<? extends Component>> triggeringComponents) {
        if (event.getClass().isAnnotationPresent(Synchronous.class) && entityManager.isTransactionActive()) {
            eventProcessor.send(event, entity, triggeringComponents);
        } else {

            boolean completed = false;
            while (!completed) {
                try {
                    entityManager.beginTransaction();
                    if (eventProcessor.send(event, entity, triggeringComponents) == EventResult.CANCEL) {
                        entityManager.rollback();
                    } else {
                        entityManager.commit();
                    }
                    completed = true;
                } catch (ConcurrentModificationException e) {
                    logger.debug("Concurrency failure processing event {}, retrying", event, e);
                }
            }
        }
    }

    @Override
    public void clearPendingEvents() throws InterruptedException {
        pendingEvents.clear();
    }

    private static class PendingEventInfo {
        private Event event;
        private EntityRef entity;
        private Set<Class<? extends Component>> triggeringComponents;

        public PendingEventInfo(Event event, EntityRef entity, Set<Class<? extends Component>> triggeringComponents) {
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
