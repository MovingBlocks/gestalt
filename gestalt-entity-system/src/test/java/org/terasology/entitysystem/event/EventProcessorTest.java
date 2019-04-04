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

import com.google.common.collect.Sets;

import org.junit.After;
import org.junit.Test;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.entity.inmemory.InMemoryEntityManager;
import org.terasology.entitysystem.event.impl.EventProcessor;
import org.terasology.entitysystem.transaction.TransactionManager;

import java.io.IOException;

import modules.test.components.Sample;
import modules.test.components.Second;
import modules.test.TestChildEvent;
import modules.test.TestEvent;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 *
 */
public class EventProcessorTest {

    private static String TEST_NAME = "name";
    private static TestEvent event = new TestEvent(TEST_NAME);
    private static TestEvent childEvent = new TestChildEvent(TEST_NAME);

    private EventProcessor eventProcessor;
    private EntityRef testEntity;

    private TransactionManager transactionManager;
    private EntityManager entityManager;

    public EventProcessorTest() {
        transactionManager = new TransactionManager();
        entityManager = new InMemoryEntityManager(new ComponentManager(), transactionManager);
    }

    @org.junit.Before
    public void startup() {
        transactionManager.begin();
        testEntity = entityManager.createEntity();
        Sample comp = testEntity.addComponent(Sample.class);
        comp.setName(TEST_NAME);
        transactionManager.commit();

        transactionManager.begin();
    }

    @After
    public void teardown() throws IOException {
        while (transactionManager.isActive()) {
            transactionManager.rollback();
        }
    }

    @Test
    public void eventHandlerReceivesEvent() {
        EventHandler<TestEvent> handler = mock(EventHandler.class);
        when(handler.onEvent(event, testEntity)).thenReturn(EventResult.CONTINUE);
        eventProcessor = EventProcessor.newBuilder().addHandler(handler, TestEvent.class, Sample.class).build();

        assertEquals(EventResult.COMPLETE, eventProcessor.send(event, testEntity));

        verify(handler).onEvent(event, testEntity);
    }

    @Test
    public void cancelStopsProcessingChain() {
        EventHandler<TestEvent> handler = mock(EventHandler.class);
        EventHandler<TestEvent> handler2 = mock(EventHandler.class);
        when(handler.onEvent(event, testEntity)).thenReturn(EventResult.CANCEL);
        when(handler2.onEvent(event, testEntity)).thenReturn(EventResult.CONTINUE);
        eventProcessor = EventProcessor.newBuilder()
                .addHandler(handler, TestEvent.class, Sample.class)
                .addHandler(handler2, TestEvent.class, Sample.class).build();

        assertEquals(EventResult.CANCEL, eventProcessor.send(event, testEntity));

        verify(handler).onEvent(event, testEntity);
        verifyNoMoreInteractions(handler2);
    }

    @Test
    public void completeStopsProcessingChain() {
        EventHandler<TestEvent> handler = mock(EventHandler.class);
        EventHandler<TestEvent> handler2 = mock(EventHandler.class);
        when(handler.onEvent(event, testEntity)).thenReturn(EventResult.COMPLETE);
        when(handler2.onEvent(event, testEntity)).thenReturn(EventResult.CONTINUE);
        eventProcessor = EventProcessor.newBuilder()
                .addHandler(handler, TestEvent.class, Sample.class)
                .addHandler(handler2, TestEvent.class, Sample.class).build();

        assertEquals(EventResult.COMPLETE, eventProcessor.send(event, testEntity));

        verify(handler).onEvent(event, testEntity);
        verifyNoMoreInteractions(handler2);
    }

    @Test
    public void eventHandlerSkippedIfComponentNotPresent() {
        EventHandler<TestEvent> handler = mock(EventHandler.class);
        when(handler.onEvent(event, testEntity)).thenReturn(EventResult.CONTINUE);
        eventProcessor = EventProcessor.newBuilder()
                .addHandler(handler, TestEvent.class, Second.class).build();

        assertEquals(EventResult.COMPLETE, eventProcessor.send(event, testEntity));

        verifyNoMoreInteractions(handler);
    }

    @Test
    public void triggeringComponentsRestrictsReceivers() {
        testEntity.addComponent(Second.class);

        EventHandler<TestEvent> sampleHandler = mock(EventHandler.class);
        when(sampleHandler.onEvent(event, testEntity)).thenReturn(EventResult.CONTINUE);

        EventHandler<TestEvent> secondHandler = mock(EventHandler.class);
        when(secondHandler.onEvent(event, testEntity)).thenReturn(EventResult.CONTINUE);
        eventProcessor = EventProcessor.newBuilder()
                .addHandler(sampleHandler, TestEvent.class, Sample.class)
                .addHandler(secondHandler, TestEvent.class, Second.class).build();

        assertEquals(EventResult.COMPLETE, eventProcessor.send(event, testEntity, Sets.newHashSet(Sample.class)));

        verify(sampleHandler).onEvent(event, testEntity);
        verifyNoMoreInteractions(secondHandler);
    }

    @Test
    public void triggeringComponentsCombineWithActualWhenFilteringReceivers() {
        // This is needed for OnRemoved event - the triggering component(s) will not be on the entity
        EventHandler<TestEvent> sampleHandler = mock(EventHandler.class);
        when(sampleHandler.onEvent(event, testEntity)).thenReturn(EventResult.CONTINUE);

        EventHandler<TestEvent> secondHandler = mock(EventHandler.class);
        when(secondHandler.onEvent(event, testEntity)).thenReturn(EventResult.CONTINUE);
        eventProcessor = EventProcessor.newBuilder()
                .addHandler(sampleHandler, TestEvent.class, Sample.class)
                .addHandler(secondHandler, TestEvent.class, Second.class).build();

        assertEquals(EventResult.COMPLETE, eventProcessor.send(event, testEntity, Sets.newHashSet(Second.class)));

        verifyNoMoreInteractions(sampleHandler);
        verify(secondHandler).onEvent(event, testEntity);
    }

    @Test
    public void canForceEventHandlerBeforeAnotherByType() {
        EventHandler<TestEvent> handlerA = mock(EventHandlerA.class);
        EventHandler<TestEvent> handlerB = mock(EventHandlerB.class);
        when(handlerA.onEvent(event, testEntity)).thenReturn(EventResult.CANCEL);
        when(handlerB.onEvent(event, testEntity)).thenReturn(EventResult.CONTINUE);
        eventProcessor = EventProcessor.newBuilder()
                .addHandler(handlerA, TestEvent.class, Sample.class)
                .addHandler(handlerB, TestEvent.class, Sample.class).orderBefore(handlerA.getClass()).build();

        assertEquals(EventResult.CANCEL, eventProcessor.send(event, testEntity));

        verify(handlerB).onEvent(event, testEntity);
    }

    @Test
    public void registeringForBaseEventReceivesChildEvent() {
        EventHandler<TestEvent> handler = mock(EventHandler.class);
        when(handler.onEvent(childEvent, testEntity)).thenReturn(EventResult.CONTINUE);
        eventProcessor = EventProcessor.newBuilder()
                .addEventClass(TestEvent.class)
                .addEventClass(TestChildEvent.class)
                .addHandler(handler, TestEvent.class, Sample.class)
                .build();
        assertEquals(EventResult.COMPLETE, eventProcessor.send(childEvent, testEntity));

        verify(handler).onEvent(childEvent, testEntity);
    }

    @Test
    public void handleExceptionWhenInvokingHandler() {
        // Errors should be logged but not stop processing
        EventHandler<TestEvent> handlerA = mock(EventHandlerA.class);
        EventHandler<TestEvent> handlerB = mock(EventHandlerB.class);
        when(handlerA.onEvent(event, testEntity)).thenThrow(new RuntimeException());
        when(handlerB.onEvent(event, testEntity)).thenReturn(EventResult.CONTINUE);
        eventProcessor = EventProcessor.newBuilder()
                .addHandler(handlerA, TestEvent.class, Sample.class)
                .addHandler(handlerB, TestEvent.class, Sample.class).orderBefore(handlerA.getClass()).build();

        assertEquals(EventResult.COMPLETE, eventProcessor.send(event, testEntity));

        verify(handlerB).onEvent(event, testEntity);
    }

    private interface EventHandlerA<T extends Event> extends EventHandler<T> {
    }

    private interface EventHandlerB<T extends Event> extends EventHandler<T> {
    }

}
