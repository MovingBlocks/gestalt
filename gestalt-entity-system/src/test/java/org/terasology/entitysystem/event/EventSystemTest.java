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
import org.mockito.ArgumentCaptor;
import org.terasology.entitysystem.entity.Component;
import org.terasology.entitysystem.entity.EntityManager;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.entity.TransactionEventListener;
import org.terasology.entitysystem.event.impl.EventProcessor;
import org.terasology.entitysystem.stubs.SampleComponent;
import org.terasology.entitysystem.stubs.SecondComponent;
import org.terasology.entitysystem.stubs.TestEvent;
import org.terasology.entitysystem.stubs.TestSynchEvent;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Set;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Core tests showing correct behaviour for any EventSystem implementation.
 */
public abstract class EventSystemTest {

    public static final String EVENT_VALUE = "Test";

    private EntityRef entity = mock(EntityRef.class);
    private EntityManager entityManager = mock(EntityManager.class);
    private EventProcessor eventProcessor = mock(EventProcessor.class);
    private EventSystem eventSystem;
    private TransactionEventListener transactionEventListener;

    private Set<Class<? extends Component>> triggeringComponents = ImmutableSet.of(SampleComponent.class, SecondComponent.class);
    TestSynchEvent synchEvent = new TestSynchEvent(EVENT_VALUE);
    TestEvent asynchEvent = new TestEvent(EVENT_VALUE);

    public EventSystemTest() {
        final ArgumentCaptor<TransactionEventListener> captor = ArgumentCaptor.forClass(TransactionEventListener.class);
        eventSystem = createEventSystem(entityManager, eventProcessor);
        verify(entityManager).registerTransactionListener(captor.capture());
        transactionEventListener = captor.getValue();
    }

    @Test
    public void sendAsynchEvent() throws Exception {
        eventSystem.send(asynchEvent, entity);
        eventSystem.processEvents();
        verify(entityManager).beginTransaction();
        verify(eventProcessor).send(asynchEvent, entity, Collections.emptySet());
        verify(entityManager).commit();
    }

    @Test
    public void sendAsynchEventRollbackIfCancelled() throws Exception {
        when(eventProcessor.send(asynchEvent, entity, Collections.emptySet())).thenReturn(EventResult.CANCEL);
        eventSystem.send(asynchEvent, entity);
        eventSystem.processEvents();
        verify(entityManager).beginTransaction();
        verify(eventProcessor).send(asynchEvent, entity, Collections.emptySet());
        verify(entityManager).rollback();
    }

    @Test
    public void sendAsynchEventHandleCommitFailure() throws Exception {
        doThrow(new ConcurrentModificationException()).doNothing().when(entityManager).commit();

        eventSystem.send(asynchEvent, entity);
        eventSystem.processEvents();

        verify(eventProcessor, times(2)).send(asynchEvent, entity, Collections.emptySet());
        verify(entityManager, times(2)).commit();
    }

    @Test
    public void sendAsynchEventWithTriggeringComponents() throws Exception {
        eventSystem.send(asynchEvent, entity, triggeringComponents);
        eventSystem.processEvents();
        verify(eventProcessor).send(asynchEvent, entity, triggeringComponents);
        verify(entityManager).commit();
    }

    @Test
    public void sendSynchEventWithNoRunningTransaction() throws Exception {
        when(entityManager.isTransactionActive()).thenReturn(false);
        eventSystem.send(synchEvent, entity);

        verify(entityManager).isTransactionActive();
        verify(entityManager).beginTransaction();
        verify(eventProcessor).send(synchEvent, entity, Collections.emptySet());
        verify(entityManager).commit();
        verifyNoMoreInteractions(entityManager);
    }

    @Test
    public void sendSynchEventNoRunningTransactionCommitFailure() throws Exception {
        doThrow(new ConcurrentModificationException()).doNothing().when(entityManager).commit();

        eventSystem.send(synchEvent, entity);
        eventSystem.processEvents();

        verify(eventProcessor, times(2)).send(synchEvent, entity, Collections.emptySet());
        verify(entityManager, times(2)).commit();
    }

    @Test
    public void sendSynchEventNoRunningTransactionWithTriggeringComponents() throws Exception {
        eventSystem.send(synchEvent, entity, triggeringComponents);

        verify(entityManager).isTransactionActive();
        verify(entityManager).beginTransaction();
        verify(eventProcessor).send(synchEvent, entity, triggeringComponents);
        verify(entityManager).commit();
        verifyNoMoreInteractions(entityManager);
    }

    @Test
    public void sendSynchEventWithRunningTransaction() throws Exception {
        when(entityManager.isTransactionActive()).thenReturn(true);
        transactionEventListener.onBegin();
        eventSystem.send(synchEvent, entity);
        verify(entityManager).isTransactionActive();
        verify(eventProcessor).send(synchEvent, entity, Collections.emptySet());
        verifyNoMoreInteractions(entityManager);
    }

    @Test
    public void sendSynchEventWithRunningTransactionAndTriggeringComponents() throws Exception {
        when(entityManager.isTransactionActive()).thenReturn(true);
        transactionEventListener.onBegin();
        eventSystem.send(synchEvent, entity, triggeringComponents);

        verify(entityManager).isTransactionActive();
        verify(eventProcessor).send(synchEvent, entity, triggeringComponents);
        verifyNoMoreInteractions(entityManager);
    }

    @Test
    public void sendAsyncEventHeldUntilCurrentTransactionCommitted() throws Exception {
        transactionEventListener.onBegin();
        eventSystem.send(asynchEvent, entity);
        eventSystem.processEvents();

        verifyNoMoreInteractions(entityManager);
        verifyNoMoreInteractions(eventProcessor);

        transactionEventListener.onCommit();
        eventSystem.processEvents();

        verify(entityManager).beginTransaction();
        verify(eventProcessor).send(asynchEvent, entity, Collections.emptySet());
        verify(entityManager).commit();
        verifyNoMoreInteractions(entityManager);
    }

    @Test
    public void sendAsyncEventDiscardedIfTransactionRolledBack() throws Exception {
        transactionEventListener.onBegin();
        eventSystem.send(asynchEvent, entity);
        eventSystem.processEvents();

        verifyNoMoreInteractions(entityManager);
        verifyNoMoreInteractions(eventProcessor);

        transactionEventListener.onRollback();
        transactionEventListener.onBegin();
        transactionEventListener.onCommit();
        eventSystem.processEvents();

        verifyNoMoreInteractions(entityManager);
        verifyNoMoreInteractions(eventProcessor);
    }

    @Test
    public void sendAsyncEventPreservedButNotSentOverDurationOfInnerTransaction() throws Exception {
        transactionEventListener.onBegin();
        eventSystem.send(asynchEvent, entity);
        eventSystem.processEvents();

        verifyNoMoreInteractions(entityManager);
        verifyNoMoreInteractions(eventProcessor);

        transactionEventListener.onBegin();
        transactionEventListener.onCommit();
        eventSystem.processEvents();

        verifyNoMoreInteractions(entityManager);
        verifyNoMoreInteractions(eventProcessor);

        transactionEventListener.onCommit();
        eventSystem.processEvents();

        verify(entityManager).beginTransaction();
        verify(eventProcessor).send(asynchEvent, entity, Collections.emptySet());
        verify(entityManager).commit();
        verifyNoMoreInteractions(entityManager);
    }



    protected abstract EventSystem createEventSystem(EntityManager entityManager, EventProcessor eventProcessor);
}
