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

package org.terasology.entitysystem.event;

import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Test;
import org.terasology.entitysystem.Transaction;
import org.terasology.entitysystem.component.CodeGenComponentManager;
import org.terasology.entitysystem.entity.EntityManager;
import org.terasology.entitysystem.entity.inmemory.InMemoryEntityManager;
import org.terasology.entitysystem.stubs.SampleComponent;
import org.terasology.entitysystem.stubs.SecondComponent;
import org.terasology.entitysystem.stubs.TestChildEvent;
import org.terasology.entitysystem.stubs.TestEvent;
import org.terasology.valuetype.ImmutableCopy;
import org.terasology.valuetype.TypeHandler;
import org.terasology.valuetype.TypeLibrary;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

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

    private EventProcessor eventProcessor;
    private EntityManager entityManager;
    private URLClassLoader tempLoader;
    private long testEntity;
    private Transaction transaction;

    public EventProcessorTest() {
        TypeLibrary typeLibrary = new TypeLibrary();
        typeLibrary.addHandler(new TypeHandler<>(String.class, ImmutableCopy.create()));
        tempLoader = new URLClassLoader(new URL[0]);
        entityManager = new InMemoryEntityManager(new CodeGenComponentManager(typeLibrary, tempLoader));

        transaction = entityManager.beginTransaction();
        testEntity = transaction.createEntity();
        SampleComponent comp = transaction.addComponent(testEntity, SampleComponent.class);
        comp.setName(TEST_NAME);
        transaction.commit();

    }

    @After
    public void teardown() throws IOException {
        tempLoader.close();
    }

    @Test
    public void eventHandlerReceivesEvent() {
        TestEvent event = new TestEvent("test");
        EventHandler<TestEvent> handler = mock(EventHandler.class);
        when(handler.onEvent(event, testEntity, transaction)).thenReturn(EventResult.CONTINUE);
        eventProcessor = EventProcessor.newBuilder().addHandler(handler, TestEvent.class, SampleComponent.class).build();

        assertEquals(EventResult.COMPLETE, eventProcessor.send(testEntity, event, transaction));

        verify(handler).onEvent(event, testEntity, transaction);
    }

    @Test
    public void cancelStopsProcessingChain() {
        TestEvent event = new TestEvent("test");
        EventHandler<TestEvent> handler = mock(EventHandler.class);
        EventHandler<TestEvent> handler2 = mock(EventHandler.class);
        when(handler.onEvent(event, testEntity, transaction)).thenReturn(EventResult.CANCEL);
        when(handler2.onEvent(event, testEntity, transaction)).thenReturn(EventResult.CONTINUE);
        eventProcessor = EventProcessor.newBuilder()
                .addHandler(handler, TestEvent.class, SampleComponent.class)
                .addHandler(handler2, TestEvent.class, SampleComponent.class).build();

        assertEquals(EventResult.CANCEL, eventProcessor.send(testEntity, event, transaction));

        verify(handler).onEvent(event, testEntity, transaction);
        verifyNoMoreInteractions(handler2);
    }

    @Test
    public void completeStopsProcessingChain() {
        TestEvent event = new TestEvent("test");
        EventHandler<TestEvent> handler = mock(EventHandler.class);
        EventHandler<TestEvent> handler2 = mock(EventHandler.class);
        when(handler.onEvent(event, testEntity, transaction)).thenReturn(EventResult.COMPLETE);
        when(handler2.onEvent(event, testEntity, transaction)).thenReturn(EventResult.CONTINUE);
        eventProcessor = EventProcessor.newBuilder()
                .addHandler(handler, TestEvent.class, SampleComponent.class)
                .addHandler(handler2, TestEvent.class, SampleComponent.class).build();

        assertEquals(EventResult.COMPLETE, eventProcessor.send(testEntity, event, transaction));

        verify(handler).onEvent(event, testEntity, transaction);
        verifyNoMoreInteractions(handler2);
    }

    @Test
    public void eventHandlerSkippedIfComponentNotPresent() {
        TestEvent event = new TestEvent("test");
        EventHandler<TestEvent> handler = mock(EventHandler.class);
        when(handler.onEvent(event, testEntity, transaction)).thenReturn(EventResult.CONTINUE);
        eventProcessor = EventProcessor.newBuilder()
                .addHandler(handler, TestEvent.class, SecondComponent.class).build();

        assertEquals(EventResult.COMPLETE, eventProcessor.send(testEntity, event, transaction));

        verifyNoMoreInteractions(handler);
    }

    @Test
    public void triggeringComponentsRestrictsReceivers() {
        transaction.addComponent(testEntity, SecondComponent.class);
        transaction.commit();

        TestEvent event = new TestEvent("test");

        EventHandler<TestEvent> sampleHandler = mock(EventHandler.class);
        when(sampleHandler.onEvent(event, testEntity, transaction)).thenReturn(EventResult.CONTINUE);

        EventHandler<TestEvent> secondHandler = mock(EventHandler.class);
        when(sampleHandler.onEvent(event, testEntity, transaction)).thenReturn(EventResult.CONTINUE);
        eventProcessor = EventProcessor.newBuilder()
                .addHandler(sampleHandler, TestEvent.class, SampleComponent.class)
                .addHandler(secondHandler, TestEvent.class, SecondComponent.class).build();

        assertEquals(EventResult.COMPLETE, eventProcessor.send(testEntity, event, transaction, Sets.newHashSet(SampleComponent.class)));

        verify(sampleHandler).onEvent(event, testEntity, transaction);
        verifyNoMoreInteractions(secondHandler);
    }

    @Test
    public void canForceEventHandlerBeforeAnotherByType() {
        TestEvent event = new TestEvent("test");
        EventHandler<TestEvent> handlerA = mock(EventHandlerA.class);
        EventHandler<TestEvent> handlerB = mock(EventHandlerB.class);
        when(handlerA.onEvent(event, testEntity, transaction)).thenReturn(EventResult.CANCEL);
        when(handlerB.onEvent(event, testEntity, transaction)).thenReturn(EventResult.CONTINUE);
        eventProcessor = EventProcessor.newBuilder()
                .addHandler(handlerA, TestEvent.class, SampleComponent.class)
                .addHandler(handlerB, TestEvent.class, SampleComponent.class).orderBefore(handlerA.getClass()).build();

        assertEquals(EventResult.CANCEL, eventProcessor.send(testEntity, event, transaction));

        verify(handlerB).onEvent(event, testEntity, transaction);
    }

    @Test
    public void registeringForBaseEventReceivesChildEvent() {
        TestChildEvent event = new TestChildEvent(TEST_NAME);

        EventHandler<TestEvent> handler = mock(EventHandler.class);
        when(handler.onEvent(event, testEntity, transaction)).thenReturn(EventResult.CONTINUE);
        eventProcessor = EventProcessor.newBuilder()
                .addEventClass(TestEvent.class)
                .addEventClass(TestChildEvent.class)
                .addHandler(handler, TestEvent.class, SampleComponent.class)
                .build();
        assertEquals(EventResult.COMPLETE, eventProcessor.send(testEntity, event, transaction));

        verify(handler).onEvent(event, testEntity, transaction);
    }

    private interface EventHandlerA<T extends Event> extends EventHandler<T> {}

    private interface EventHandlerB<T extends Event> extends EventHandler<T> {}

    // TODO: (not in this class)
    // Builder and ordering
    // Sending event with no direct handlers (but handlers on parent class)
    // * Transaction handling
    // * Synchronous vs Asynchronous


}
