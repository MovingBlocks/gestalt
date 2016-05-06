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
import org.junit.Test;
import org.terasology.entitysystem.Transaction;
import org.terasology.entitysystem.entity.Component;
import org.terasology.entitysystem.entity.EntityTransaction;
import org.terasology.entitysystem.stubs.SampleComponent;
import org.terasology.entitysystem.stubs.SecondComponent;
import org.terasology.entitysystem.stubs.TestEvent;
import org.terasology.entitysystem.stubs.TestSynchEvent;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Set;
import java.util.function.Supplier;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Core tests showing correct behaviour for any EventSystem implementation.
 */
public abstract class EventSystemTest {

    public static final String EVENT_VALUE = "Test";
    public static final int ENTITY_ID = 2;

    private EntityTransaction entityTransaction = mock(EntityTransaction.class);
    private EventProcessor eventProcessor = mock(EventProcessor.class);
    private EventSystem eventSystem = createEventSystem(() -> entityTransaction, eventProcessor);

    private Transaction expectedTransaction = new Transaction(entityTransaction, eventSystem);

    private Set<Class<? extends Component>> triggeringComponents = ImmutableSet.of(SampleComponent.class, SecondComponent.class);
    private Transaction originTransaction = mock(Transaction.class);
    TestSynchEvent synchEvent = new TestSynchEvent(EVENT_VALUE);
    TestEvent asynchEvent = new TestEvent(EVENT_VALUE);

    public EventSystemTest() {
    }

    @Test
    public void sendAsynchEvent() throws Exception {
        eventSystem.send(asynchEvent, ENTITY_ID);
        eventSystem.processEvents();
        verify(eventProcessor).send(asynchEvent, ENTITY_ID, expectedTransaction, Collections.emptySet());
        verify(entityTransaction).commit();
    }

    @Test
    public void sendAsynchEventRollbackIfCancelled() throws Exception {
        eventSystem.send(asynchEvent, ENTITY_ID);
        eventSystem.processEvents();
        verify(eventProcessor).send(asynchEvent, ENTITY_ID, expectedTransaction, Collections.emptySet());
        verify(entityTransaction).commit();
    }

    @Test
    public void sendAsynchEventWithOriginTransaction() throws Exception {
        eventSystem.send(asynchEvent, ENTITY_ID, originTransaction);
        eventSystem.processEvents();
        verify(eventProcessor).send(asynchEvent, ENTITY_ID, expectedTransaction, Collections.emptySet());
        verify(entityTransaction).commit();
    }

    @Test
    public void sendAsynchEventHandleCommitFailure() throws Exception {
        doThrow(new ConcurrentModificationException()).doNothing().when(entityTransaction).commit();

        eventSystem.send(asynchEvent, ENTITY_ID);
        eventSystem.processEvents();

        verify(eventProcessor, times(2)).send(asynchEvent, ENTITY_ID, expectedTransaction, Collections.emptySet());
        verify(entityTransaction, times(2)).commit();
    }

    @Test
    public void sendAsynchEventWithTriggeringComponents() throws Exception {
        eventSystem.send(asynchEvent, ENTITY_ID, triggeringComponents);
        eventSystem.processEvents();
        verify(eventProcessor).send(asynchEvent, ENTITY_ID, expectedTransaction, triggeringComponents);
        verify(entityTransaction).commit();
    }

    @Test
    public void sendAsynchEventWithOriginAndTriggeringComponents() throws Exception {
        eventSystem.send(asynchEvent, ENTITY_ID, originTransaction, triggeringComponents);
        eventSystem.processEvents();
        verify(eventProcessor).send(asynchEvent, ENTITY_ID, expectedTransaction, triggeringComponents);
        verifyNoMoreInteractions(originTransaction);
        verify(entityTransaction).commit();
    }

    @Test
    public void sendSynchEventWithoutOrigin() throws Exception {
        eventSystem.send(synchEvent, ENTITY_ID);
        verify(eventProcessor).send(synchEvent, ENTITY_ID, expectedTransaction, Collections.emptySet());
        verifyNoMoreInteractions(originTransaction);
        verify(entityTransaction).commit();
    }

    @Test
    public void sendSynchEventNoOriginHandleCommitFailure() throws Exception {
        doThrow(new ConcurrentModificationException()).doNothing().when(entityTransaction).commit();

        eventSystem.send(synchEvent, ENTITY_ID);
        eventSystem.processEvents();

        verify(eventProcessor, times(2)).send(synchEvent, ENTITY_ID, expectedTransaction, Collections.emptySet());
        verify(entityTransaction, times(2)).commit();
    }

    @Test
    public void sendSynchEventWithOriginTransaction() throws Exception {
        eventSystem.send(synchEvent, ENTITY_ID, originTransaction);
        verify(eventProcessor).send(synchEvent, ENTITY_ID, originTransaction, Collections.emptySet());
        verifyNoMoreInteractions(originTransaction);
        verifyNoMoreInteractions(entityTransaction);
    }

    @Test
    public void sendSynchEventWithoutOriginWithTriggeringComponents() throws Exception {
        eventSystem.send(synchEvent, ENTITY_ID, triggeringComponents);
        verify(eventProcessor).send(synchEvent, ENTITY_ID, expectedTransaction, triggeringComponents);
        verifyNoMoreInteractions(originTransaction);
        verify(entityTransaction).commit();
    }

    @Test
    public void sendSynchEventWithOriginAndTriggeringComponents() throws Exception {
        eventSystem.send(synchEvent, ENTITY_ID, originTransaction, triggeringComponents);
        verify(eventProcessor).send(synchEvent, ENTITY_ID, originTransaction, triggeringComponents);
        verifyNoMoreInteractions(originTransaction);
        verifyNoMoreInteractions(entityTransaction);
    }

    protected abstract EventSystem createEventSystem(Supplier<EntityTransaction> transactionFactory, EventProcessor eventProcessor);
}
