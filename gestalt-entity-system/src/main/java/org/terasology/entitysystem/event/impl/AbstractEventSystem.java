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
import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.transaction.TransactionEventListener;
import org.terasology.entitysystem.event.Event;
import org.terasology.entitysystem.event.EventSystem;
import org.terasology.entitysystem.event.Synchronous;

import java.util.Deque;
import java.util.List;
import java.util.Set;

/**
 *
 */
public abstract class AbstractEventSystem implements EventSystem, TransactionEventListener {

    private ThreadLocal<TransactionState> transactionState = new ThreadLocal<TransactionState>() {
        @Override
        protected TransactionState initialValue() {
            return new TransactionState();
        }
    };

    public AbstractEventSystem(EntityManager entityManager) {
        entityManager.registerTransactionListener(this);
    }

    @Override
    public final void onBegin() {
        transactionState.get().pendingEvents.push(Lists.newArrayList());
    }

    @Override
    public final void onCommit() {
        List<PendingEventInfo> pendingEvents = transactionState.get().pendingEvents.pop();
        for (PendingEventInfo pendingEvent : pendingEvents) {
            processEvent(pendingEvent.event, pendingEvent.entity, pendingEvent.triggeringComponents);
        }
    }

    @Override
    public final void onRollback() {
        transactionState.get().pendingEvents.pop();
    }

    @Override
    public final void send(Event event, EntityRef entity) {
        send(event, entity, ImmutableSet.of());
    }

    @Override
    public final void send(Event event, EntityRef entity, Set<Class<? extends Component>> triggeringComponents) {
        if (event.getClass().isAnnotationPresent(Synchronous.class)) {
            processEvent(event, entity, triggeringComponents);
        } else {
            TransactionState state = this.transactionState.get();
            if (!state.pendingEvents.isEmpty()) {
                state.pendingEvents.peek().add(new PendingEventInfo(event, entity, triggeringComponents));
            } else {
                processEvent(event, entity, triggeringComponents);
            }
        }
    }

    protected abstract void processEvent(Event event, EntityRef entity, Set<Class<? extends Component>> triggeringComponents);

    private static class TransactionState {
        private Deque<List<PendingEventInfo>> pendingEvents = Queues.newArrayDeque();
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
    }
}
