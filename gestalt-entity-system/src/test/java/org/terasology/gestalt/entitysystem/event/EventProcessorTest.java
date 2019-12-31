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

package org.terasology.gestalt.entitysystem.event;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.junit.Test;
import org.terasology.gestalt.entitysystem.component.management.ComponentManager;
import org.terasology.gestalt.entitysystem.component.store.ArrayComponentStore;
import org.terasology.gestalt.entitysystem.component.store.ComponentStore;
import org.terasology.gestalt.entitysystem.entity.EntityManager;
import org.terasology.gestalt.entitysystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.entity.manager.CoreEntityManager;
import org.terasology.gestalt.entitysystem.event.impl.EventProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private EntityManager entityManager;

    public EventProcessorTest() {
        ComponentManager componentManager = new ComponentManager();
        List<ComponentStore<?>> componentStores = new ArrayList<>();
        componentStores.add(new ArrayComponentStore<>(componentManager.getType(Sample.class)));
        componentStores.add(new ArrayComponentStore<>(componentManager.getType(Second.class)));
        entityManager = new CoreEntityManager(componentStores);
    }

    @org.junit.Before
    public void startup() {
        testEntity = entityManager.createEntity();
        Sample comp = new Sample();
        comp.setName(TEST_NAME);
        testEntity.setComponent(comp);
        eventProcessor = new EventProcessor();
    }

    @Test
    public void eventHandlerReceivesEvent() {
        EventHandler<TestEvent> handler = mock(EventHandler.class);
        when(handler.onEvent(event, testEntity)).thenReturn(EventResult.CONTINUE);
        eventProcessor.registerHandler(handler, EventHandler.class, Collections.emptyList(), Collections.emptyList(), ImmutableList.of(Sample.class));

        assertEquals(EventResult.COMPLETE, eventProcessor.process(event, testEntity));

        verify(handler).onEvent(event, testEntity);
    }

    @Test
    public void cancelStopsProcessingChain() {
        EventHandler<TestEvent> handler = mock(EventHandler.class);
        EventHandler<TestEvent> handler2 = mock(EventHandler.class);
        when(handler.onEvent(event, testEntity)).thenReturn(EventResult.CANCEL);
        when(handler2.onEvent(event, testEntity)).thenReturn(EventResult.CONTINUE);
        eventProcessor.registerHandler(handler, EventHandler.class, Collections.emptyList(), Collections.emptyList(), ImmutableList.of(Sample.class));
        eventProcessor.registerHandler(handler2, EventHandler.class, Collections.emptyList(), Collections.emptyList(), ImmutableList.of(Sample.class));

        assertEquals(EventResult.CANCEL, eventProcessor.process(event, testEntity));

        verify(handler).onEvent(event, testEntity);
        verifyNoMoreInteractions(handler2);
    }

    @Test
    public void completeStopsProcessingChain() {
        EventHandler<TestEvent> handler = mock(EventHandler.class);
        EventHandler<TestEvent> handler2 = mock(EventHandler.class);
        when(handler.onEvent(event, testEntity)).thenReturn(EventResult.COMPLETE);
        when(handler2.onEvent(event, testEntity)).thenReturn(EventResult.CONTINUE);
        eventProcessor.registerHandler(handler, EventHandler.class, Collections.emptyList(), Collections.emptyList(), ImmutableList.of(Sample.class));
        eventProcessor.registerHandler(handler2, EventHandler.class, Collections.emptyList(), Collections.emptyList(), ImmutableList.of(Sample.class));

        assertEquals(EventResult.COMPLETE, eventProcessor.process(event, testEntity));

        verify(handler).onEvent(event, testEntity);
        verifyNoMoreInteractions(handler2);
    }

    @Test
    public void eventHandlerSkippedIfComponentNotPresent() {
        EventHandler<TestEvent> handler = mock(EventHandler.class);
        when(handler.onEvent(event, testEntity)).thenReturn(EventResult.CONTINUE);
        eventProcessor.registerHandler(handler, EventHandler.class, Collections.emptyList(), Collections.emptyList(), ImmutableList.of(Second.class));

        assertEquals(EventResult.COMPLETE, eventProcessor.process(event, testEntity));

        verifyNoMoreInteractions(handler);
    }

    @Test
    public void triggeringComponentsRestrictsReceivers() {
        Second second = new Second();
        testEntity.setComponent(second);

        EventHandler<TestEvent> sampleHandler = mock(EventHandler.class);
        when(sampleHandler.onEvent(event, testEntity)).thenReturn(EventResult.CONTINUE);

        EventHandler<TestEvent> secondHandler = mock(EventHandler.class);
        when(secondHandler.onEvent(event, testEntity)).thenReturn(EventResult.CONTINUE);
        eventProcessor.registerHandler(sampleHandler, EventHandler.class, Collections.emptyList(), Collections.emptyList(), ImmutableList.of(Sample.class));
        eventProcessor.registerHandler(secondHandler, EventHandler.class, Collections.emptyList(), Collections.emptyList(), ImmutableList.of(Second.class));

        assertEquals(EventResult.COMPLETE, eventProcessor.process(event, testEntity, Sets.newHashSet(Sample.class)));

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
        eventProcessor.registerHandler(sampleHandler, EventHandler.class, Collections.emptyList(), Collections.emptyList(), ImmutableList.of(Sample.class));
        eventProcessor.registerHandler(secondHandler, EventHandler.class, Collections.emptyList(), Collections.emptyList(), ImmutableList.of(Second.class));

        assertEquals(EventResult.COMPLETE, eventProcessor.process(event, testEntity, Sets.newHashSet(Second.class)));

        verifyNoMoreInteractions(sampleHandler);
        verify(secondHandler).onEvent(event, testEntity);
    }

    @Test
    public void canForceEventHandlerBeforeAnotherByType() {
        EventHandler<TestEvent> handlerA = mock(EventHandlerA.class);
        EventHandler<TestEvent> handlerB = mock(EventHandlerB.class);
        when(handlerA.onEvent(event, testEntity)).thenReturn(EventResult.CANCEL);
        when(handlerB.onEvent(event, testEntity)).thenReturn(EventResult.CONTINUE);
                eventProcessor.registerHandler(handlerA, handlerA.getClass(), Collections.emptyList(), Collections.emptyList(), ImmutableList.of(Sample.class));
                eventProcessor.registerHandler(handlerB, handlerB.getClass(), ImmutableSet.of(handlerA.getClass()), Collections.emptyList(), ImmutableList.of(Sample.class));

        assertEquals(EventResult.CANCEL, eventProcessor.process(event, testEntity));

        verify(handlerB).onEvent(event, testEntity);
    }


    @Test
    public void handleExceptionWhenInvokingHandler() {
        // Errors should be logged but not stop processing
        EventHandler<TestEvent> handlerA = mock(EventHandlerA.class);
        EventHandler<TestEvent> handlerB = mock(EventHandlerB.class);
        when(handlerA.onEvent(event, testEntity)).thenThrow(new RuntimeException());
        when(handlerB.onEvent(event, testEntity)).thenReturn(EventResult.CONTINUE);
        eventProcessor.registerHandler(handlerA, handlerA.getClass(), Collections.emptyList(), Collections.emptyList(), ImmutableList.of(Sample.class));
        eventProcessor.registerHandler(handlerB, handlerB.getClass(), ImmutableList.of(handlerA.getClass()), Collections.emptyList(), ImmutableList.of(Sample.class));

        assertEquals(EventResult.COMPLETE, eventProcessor.process(event, testEntity));

        verify(handlerB).onEvent(event, testEntity);
    }

    private interface EventHandlerA<T extends Event> extends EventHandler<T> {
    }

    private interface EventHandlerB<T extends Event> extends EventHandler<T> {
    }

}
