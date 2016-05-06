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

package org.terasology.entitysystem;

import com.google.common.collect.Lists;
import org.terasology.entitysystem.entity.Component;
import org.terasology.entitysystem.entity.EntityTransaction;
import org.terasology.entitysystem.event.Event;
import org.terasology.entitysystem.event.EventSystem;
import org.terasology.entitysystem.event.Synchronous;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * An entity system transaction. This builds upon an entity transaction, adding support for sending events.
 * <p>
 * Transactions allow for one or more entities to be updated consistently in the presence of multiple threads. Once a transaction is started, each component accessed
 * is cached in the transaction, and can be updated freely. When the transaction is committed, the changes are applied to the entity system and available for other threads.
 * If a thread has made a change to an involved entity in the meantime, then a ConcurrentModificationException is thrown and no change occurs. A transaction can also be rolled
 * back, throwing away all local modifications.
 * <p>
 * Events can also be sent through a transaction. Events annotated with {@link Synchronous} are run immediately within the transaction, while other events are only run
 * after the transaction is successfully committed.
 * <p>
 * After commit or rollback a transaction is ready for reuse.
 * <p>
 * A transaction is not threadsafe - it should be used only by a single thread (at a time at least).
 */
public class Transaction implements EntityTransaction {

    private EntityTransaction entityTransaction;
    private EventSystem eventSystem;
    private List<QueuedEventInfo> queuedEvents = Lists.newArrayList();

    /**
     * @param entityTransaction The entity transaction this transaction wraps
     * @param eventSystem The event system to process events with
     */
    public Transaction(EntityTransaction entityTransaction, EventSystem eventSystem) {
        this.entityTransaction = entityTransaction;
        this.eventSystem = eventSystem;
    }

    /**
     * Sends an event against an entity. If the event is synchronous this is done immediately within using the current transaction - otherwise the event will be processed
     * after the current transaction is successfully committed.
     *
     * @param event    The event to send
     * @param entityId The entity to send the event against
     */
    public void send(Event event, long entityId) {
        if (event.getClass().isAnnotationPresent(Synchronous.class)) {
            eventSystem.send(event, entityId, this);
        } else {
            queuedEvents.add(new QueuedEventInfo(event, entityId));
        }
    }

    @Override
    public long createEntity() {
        return entityTransaction.createEntity();
    }

    @Override
    public <T extends Component> Optional<T> getComponent(long entityId, Class<T> componentType) {
        return entityTransaction.getComponent(entityId, componentType);
    }

    @Override
    public Set<Class<? extends Component>> getEntityComposition(long entityId) {
        return entityTransaction.getEntityComposition(entityId);
    }

    @Override
    public <T extends Component> T addComponent(long entityId, Class<T> componentType) {
        return entityTransaction.addComponent(entityId, componentType);
    }

    @Override
    public <T extends Component> void removeComponent(long entityId, Class<T> componentType) {
        entityTransaction.removeComponent(entityId, componentType);
    }

    @Override
    public void commit() throws ConcurrentModificationException {
        entityTransaction.commit();
        queuedEvents.stream().forEach(info -> eventSystem.send(info.getEvent(), info.getEntityId()));
        queuedEvents.clear();
    }

    @Override
    public void rollback() {
        entityTransaction.rollback();
        queuedEvents.clear();
    }

    /**
     * Details on a queued event
     */
    private static class QueuedEventInfo {
        private Event event;
        private long entityId;

        public QueuedEventInfo(Event event, long entityId) {
            this.event = event;
            this.entityId = entityId;
        }

        public Event getEvent() {
            return event;
        }

        public long getEntityId() {
            return entityId;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Transaction) {
            Transaction other = (Transaction) obj;
            return Objects.equals(other.entityTransaction, entityTransaction);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityTransaction);
    }
}
