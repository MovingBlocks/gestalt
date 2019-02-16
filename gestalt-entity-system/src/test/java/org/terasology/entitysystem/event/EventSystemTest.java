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

package org.terasology.entitysystem.event;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.event.impl.EventProcessor;
import org.terasology.entitysystem.transaction.TransactionManager;
import org.terasology.entitysystem.transaction.pipeline.TransactionContext;
import org.terasology.entitysystem.transaction.pipeline.TransactionPipeline;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Set;

import modules.test.components.Sample;
import modules.test.components.Second;
import modules.test.TestEvent;
import modules.test.TestSynchEvent;

import static org.mockito.Mockito.atLeastOnce;
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

    private static final String EVENT_VALUE = "Test";
    TestSynchEvent synchEvent = new TestSynchEvent(EVENT_VALUE);
    TestEvent asynchEvent = new TestEvent(EVENT_VALUE);
    private EntityRef entity = mock(EntityRef.class);
    private TransactionManager transactionManager = mock(TransactionManager.class);
    private EventProcessor eventProcessor = mock(EventProcessor.class);
    private EventSystem eventSystem;
    private TransactionPipeline pipeline = new TransactionPipeline();
    private Set<Class<? extends Component>> triggeringComponents = ImmutableSet.of(Sample.class, Second.class);

    public EventSystemTest() {
        when(transactionManager.getPipeline()).thenReturn(pipeline);
        eventSystem = createEventSystem(transactionManager, eventProcessor);
        verify(transactionManager, atLeastOnce()).getPipeline();
    }

    @Test
    public void sendAsynchEventOutsideTransaction() throws Exception {
        eventSystem.send(asynchEvent, entity);
        eventSystem.processEvents();
        verify(transactionManager).begin();
        verify(eventProcessor).send(asynchEvent, entity, Collections.emptySet());
        verify(transactionManager).commit();
    }

    @Test
    public void sendAsynchEventRollbackIfCancelled() throws Exception {
        when(eventProcessor.send(asynchEvent, entity, Collections.emptySet())).thenReturn(EventResult.CANCEL);
        eventSystem.send(asynchEvent, entity);
        eventSystem.processEvents();
        verify(transactionManager).begin();
        verify(eventProcessor).send(asynchEvent, entity, Collections.emptySet());
        verify(transactionManager).rollback();
    }

    @Test
    public void sendAsynchEventHandleCommitFailure() throws Exception {
        doThrow(new ConcurrentModificationException()).doNothing().when(transactionManager).commit();

        eventSystem.send(asynchEvent, entity);
        eventSystem.processEvents();

        verify(eventProcessor, times(2)).send(asynchEvent, entity, Collections.emptySet());
        verify(transactionManager, times(2)).commit();
    }

    @Test
    public void sendAsynchEventWithTriggeringComponents() throws Exception {
        eventSystem.send(asynchEvent, entity, triggeringComponents);
        eventSystem.processEvents();
        verify(eventProcessor).send(asynchEvent, entity, triggeringComponents);
        verify(transactionManager).commit();
    }

    @Test
    public void sendSynchEventWithNoRunningTransaction() throws Exception {
        when(transactionManager.isActive()).thenReturn(false);
        eventSystem.send(synchEvent, entity);

        verify(transactionManager).isActive();
        verify(transactionManager).begin();
        verify(eventProcessor).send(synchEvent, entity, Collections.emptySet());
        verify(transactionManager).commit();
        verifyNoMoreInteractions(transactionManager);
    }

    @Test
    public void sendSynchEventNoRunningTransactionCommitFailure() throws Exception {
        doThrow(new ConcurrentModificationException()).doNothing().when(transactionManager).commit();

        eventSystem.send(synchEvent, entity);
        eventSystem.processEvents();

        verify(eventProcessor, times(2)).send(synchEvent, entity, Collections.emptySet());
        verify(transactionManager, times(2)).commit();
    }

    @Test
    public void sendSynchEventNoRunningTransactionWithTriggeringComponents() throws Exception {
        eventSystem.send(synchEvent, entity, triggeringComponents);

        verify(transactionManager).isActive();
        verify(transactionManager).begin();
        verify(eventProcessor).send(synchEvent, entity, triggeringComponents);
        verify(transactionManager).commit();
        verifyNoMoreInteractions(transactionManager);
    }

    @Test
    public void sendSynchEventWithRunningTransaction() throws Exception {
        // Should be processed within current transaction
        when(transactionManager.isActive()).thenReturn(true);
        pipeline.begin(new TransactionContext());
        eventSystem.send(synchEvent, entity);
        verify(transactionManager).isActive();
        verify(eventProcessor).send(synchEvent, entity, Collections.emptySet());
        verifyNoMoreInteractions(transactionManager);
    }

    @Test
    public void sendSynchEventWithRunningTransactionAndTriggeringComponents() throws Exception {
        when(transactionManager.isActive()).thenReturn(true);
        pipeline.begin(new TransactionContext());
        eventSystem.send(synchEvent, entity, triggeringComponents);

        verify(transactionManager).isActive();
        verify(eventProcessor).send(synchEvent, entity, triggeringComponents);
        verifyNoMoreInteractions(transactionManager);
    }

    @Test
    public void sendAsyncEventHeldUntilCurrentTransactionCommitted() throws Exception {
        when(transactionManager.isActive()).thenReturn(true);

        TransactionContext context = new TransactionContext();
        when(transactionManager.getContext()).thenReturn(context);
        pipeline.begin(context);
        eventSystem.send(asynchEvent, entity);
        eventSystem.processEvents();


        verify(transactionManager, atLeastOnce()).isActive();
        verify(transactionManager, atLeastOnce()).getContext();
        verifyNoMoreInteractions(transactionManager);
        verifyNoMoreInteractions(eventProcessor);

        pipeline.commit(context);
        eventSystem.processEvents();

        verify(transactionManager).begin();
        verify(eventProcessor).send(asynchEvent, entity, Collections.emptySet());
        verify(transactionManager).commit();
        verifyNoMoreInteractions(transactionManager);
    }

    @Test
    public void sendAsyncEventDiscardedIfTransactionRolledBack() throws Exception {
        when(transactionManager.isActive()).thenReturn(true);
        TransactionContext context = new TransactionContext();
        when(transactionManager.getContext()).thenReturn(context);
        pipeline.begin(context);
        eventSystem.send(asynchEvent, entity);
        eventSystem.processEvents();

        verify(transactionManager, atLeastOnce()).isActive();
        verify(transactionManager, atLeastOnce()).getContext();
        verifyNoMoreInteractions(transactionManager);
        verifyNoMoreInteractions(eventProcessor);

        pipeline.rollback(context);
        context = new TransactionContext();
        when(transactionManager.getContext()).thenReturn(context);
        pipeline.begin(context);
        pipeline.commit(context);
        eventSystem.processEvents();

        verifyNoMoreInteractions(transactionManager);
        verifyNoMoreInteractions(eventProcessor);
    }

    @Test
    public void sendAsyncEventPreservedButNotSentOverDurationOfInnerTransaction() throws Exception {
        when(transactionManager.isActive()).thenReturn(true);
        TransactionContext outerContext = new TransactionContext();
        when(transactionManager.getContext()).thenReturn(outerContext);
        pipeline.begin(outerContext);
        eventSystem.send(asynchEvent, entity);
        eventSystem.processEvents();

        verify(transactionManager, atLeastOnce()).isActive();
        verify(transactionManager, atLeastOnce()).getContext();
        verifyNoMoreInteractions(transactionManager);
        verifyNoMoreInteractions(eventProcessor);

        TransactionContext innerContext = new TransactionContext();
        when(transactionManager.getContext()).thenReturn(innerContext);
        pipeline.begin(innerContext);
        pipeline.commit(innerContext);
        eventSystem.processEvents();

        verify(transactionManager, atLeastOnce()).isActive();
        verify(transactionManager, atLeastOnce()).getContext();
        verifyNoMoreInteractions(transactionManager);
        verifyNoMoreInteractions(eventProcessor);

        pipeline.commit(outerContext);
        eventSystem.processEvents();

        verify(transactionManager).begin();
        verify(eventProcessor).send(asynchEvent, entity, Collections.emptySet());
        verify(transactionManager).commit();
        verifyNoMoreInteractions(transactionManager);
    }


    protected abstract EventSystem createEventSystem(TransactionManager transactionManager, EventProcessor eventProcessor);
}
