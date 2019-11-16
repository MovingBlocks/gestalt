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

package org.terasology.gestalt.entitysystem.event.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.gestalt.entitysystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.event.Event;
import org.terasology.gestalt.entitysystem.event.EventSystem;
import org.terasology.gestalt.entitysystem.event.Synchronous;

import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;

/**
 * A basic implementation of EventSystem. {@link org.terasology.gestalt.entitysystem.event.Synchronous} events are processed immediately, other events are only processed when processEvents is called.
 */
@ThreadSafe
public class DelayedEventSystem implements EventSystem {
    private static final Logger logger = LoggerFactory.getLogger(DelayedEventSystem.class);

    private final EventProcessor eventProcessor;
    private final BlockingDeque<PendingEventInfo> pendingEvents = Queues.newLinkedBlockingDeque();

    public DelayedEventSystem(EventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    @Override
    public void send(Event event, EntityRef entity, Set<Class<? extends Component>> triggeringComponents) {
        if (event.getClass().isAnnotationPresent(Synchronous.class)) {
            processEvent(event, entity, triggeringComponents);
        } else {
            synchronized (this) {
                pendingEvents.addLast(new PendingEventInfo(event, entity, triggeringComponents));
            }
        }
    }

    @Override
    public void processEvents() {
        List<PendingEventInfo> events = Lists.newArrayListWithExpectedSize(pendingEvents.size());
        while (!pendingEvents.isEmpty()) {
            synchronized (this) {
                pendingEvents.drainTo(events);
            }
            for (PendingEventInfo eventInfo : events) {
                processEvent(eventInfo.getEvent(), eventInfo.getEntity(), eventInfo.triggeringComponents);
            }
        }
    }

    private void processEvent(Event event, EntityRef entity, Set<Class<? extends Component>> triggeringComponents) {
        if (entity.exists()) {
            eventProcessor.send(event, entity, triggeringComponents);
        }
    }

    @Override
    public void clearPendingEvents() {
        pendingEvents.clear();
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
