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

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.event.impl.EventProcessorBuilder;
import org.terasology.entitysystem.event.impl.EventReceiverMethodSupport;

import java.util.Collections;
import java.util.Optional;

import modules.test.components.Sample;
import modules.test.components.Second;
import modules.test.TestEvent;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class EventReceiverMethodSupportTest {

    private EntityRef entity = mock(EntityRef.class);

    @Test
    public void registerEventReceiverMethod() {
        TrivialEventReceiver receiver = new TrivialEventReceiver();
        EventProcessorBuilder builder = mock(EventProcessorBuilder.class);
        EventReceiverMethodSupport.register(receiver, builder);

        final ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(builder).addHandler(captor.capture(), eq(TrivialEventReceiver.class), eq(TestEvent.class), eq(Collections.emptySet()));

        TestEvent event = new TestEvent("test");

        captor.getValue().onEvent(event, entity);
        assertEquals(event, receiver.lastEvent);
        assertEquals(entity, receiver.lastEntity);
    }

    @Test
    public void registerWithFilterComponentFromAnnotation() {
        FilteredEventReceiver receiver = new FilteredEventReceiver();
        EventProcessorBuilder builder = mock(EventProcessorBuilder.class);
        EventReceiverMethodSupport.register(receiver, builder);

        final ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(builder).addHandler(captor.capture(), eq(FilteredEventReceiver.class), eq(TestEvent.class), eq(Sets.newHashSet(Sample.class)));

        TestEvent event = new TestEvent("test");
        captor.getValue().onEvent(event, entity);
        assertEquals(event, receiver.lastEvent);
        assertEquals(entity, receiver.lastEntity);
    }

    @Test
    public void registerWithMultipleFilterComponentFromAnnotation() {
        MultiFilteredEventReceiver receiver = new MultiFilteredEventReceiver();
        EventProcessorBuilder builder = mock(EventProcessorBuilder.class);
        EventReceiverMethodSupport.register(receiver, builder);

        final ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(builder).addHandler(captor.capture(), eq(MultiFilteredEventReceiver.class), eq(TestEvent.class), eq(Sets.newHashSet(Sample.class, Second.class)));

        TestEvent event = new TestEvent("test");
        captor.getValue().onEvent(event, entity);
        assertEquals(event, receiver.lastEvent);
        assertEquals(entity, receiver.lastEntity);
    }

    @Test
    public void registerWithComponentArg() {
        MixedFilteredEventReceiver receiver = new MixedFilteredEventReceiver();
        EventProcessorBuilder builder = mock(EventProcessorBuilder.class);
        EventReceiverMethodSupport.register(receiver, builder);

        final ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(builder).addHandler(captor.capture(), eq(MixedFilteredEventReceiver.class), eq(TestEvent.class), eq(Sets.newHashSet(Sample.class, Second.class)));

        TestEvent event = new TestEvent("test");

        Second comp = mock(Second.class);
        when(entity.getComponent(Second.class)).thenReturn(Optional.of(comp));
        captor.getValue().onEvent(event, entity);
        assertEquals(event, receiver.lastEvent);
        assertEquals(entity, receiver.lastEntity);
        assertEquals(comp, receiver.comp);
    }

    @Test
    public void orderUsingClassBeforeAnnotation() {
        GlobalBeforeEventReceiver receiver = new GlobalBeforeEventReceiver();
        EventProcessorBuilder builder = mock(EventProcessorBuilder.class);
        EventReceiverMethodSupport.register(receiver, builder);

        verify(builder).addHandler(any(EventHandler.class), eq(GlobalBeforeEventReceiver.class), eq(TestEvent.class), eq(Collections.emptySet()));
        verify(builder).orderBeforeAll(Sets.newHashSet(TrivialEventReceiver.class));
    }


    @Test
    public void orderUsingClassAfterAnnotation() {
        GlobalAfterEventReceiver receiver = new GlobalAfterEventReceiver();
        EventProcessorBuilder builder = mock(EventProcessorBuilder.class);
        EventReceiverMethodSupport.register(receiver, builder);

        verify(builder).addHandler(any(EventHandler.class), eq(GlobalAfterEventReceiver.class), eq(TestEvent.class), eq(Collections.emptySet()));
        verify(builder).orderAfterAll(Sets.newHashSet(TrivialEventReceiver.class));
    }

    @Test
    public void orderUsingMethodBeforeAnnotation() {
        LocalBeforeEventReceiver receiver = new LocalBeforeEventReceiver();
        EventProcessorBuilder builder = mock(EventProcessorBuilder.class);
        EventReceiverMethodSupport.register(receiver, builder);

        verify(builder).addHandler(any(EventHandler.class), eq(LocalBeforeEventReceiver.class), eq(TestEvent.class), eq(Collections.emptySet()));
        verify(builder).orderBeforeAll(Sets.newHashSet(TrivialEventReceiver.class));
    }

    @Test
    public void orderUsingMethodAfterAnnotation() {
        LocalAfterEventReceiver receiver = new LocalAfterEventReceiver();
        EventProcessorBuilder builder = mock(EventProcessorBuilder.class);
        EventReceiverMethodSupport.register(receiver, builder);

        verify(builder).addHandler(any(EventHandler.class), eq(LocalAfterEventReceiver.class), eq(TestEvent.class), eq(Collections.emptySet()));
        verify(builder).orderAfterAll(Sets.newHashSet(TrivialEventReceiver.class));
    }

    public static class TrivialEventReceiver {

        public TestEvent lastEvent;
        public EntityRef lastEntity;

        @ReceiveEvent
        public EventResult onEvent(TestEvent event, EntityRef entity) {
            this.lastEvent = event;
            this.lastEntity = entity;
            return EventResult.CONTINUE;
        }
    }

    @Before(TrivialEventReceiver.class)
    public static class GlobalBeforeEventReceiver {

        public TestEvent lastEvent;
        public EntityRef lastEntity;

        @ReceiveEvent
        public EventResult onEvent(TestEvent event, EntityRef entity) {
            this.lastEvent = event;
            this.lastEntity = entity;
            return EventResult.CONTINUE;
        }
    }

    @After(TrivialEventReceiver.class)
    public static class GlobalAfterEventReceiver {

        public TestEvent lastEvent;
        public EntityRef lastEntity;

        @ReceiveEvent
        public EventResult onEvent(TestEvent event, EntityRef entity) {
            this.lastEvent = event;
            this.lastEntity = entity;
            return EventResult.CONTINUE;
        }
    }

    public static class LocalBeforeEventReceiver {

        public TestEvent lastEvent;
        public EntityRef lastEntity;

        @Before(TrivialEventReceiver.class)
        @ReceiveEvent
        public EventResult onEvent(TestEvent event, EntityRef entity) {
            this.lastEvent = event;
            this.lastEntity = entity;
            return EventResult.CONTINUE;
        }
    }

    public static class LocalAfterEventReceiver {

        public TestEvent lastEvent;
        public EntityRef lastEntity;

        @After(TrivialEventReceiver.class)
        @ReceiveEvent
        public EventResult onEvent(TestEvent event, EntityRef entity) {
            this.lastEvent = event;
            this.lastEntity = entity;
            return EventResult.CONTINUE;
        }
    }

    public static class FilteredEventReceiver {

        public TestEvent lastEvent;
        public EntityRef lastEntity;

        @ReceiveEvent(components = Sample.class)
        public EventResult onEvent(TestEvent event, EntityRef entity) {
            this.lastEvent = event;
            this.lastEntity = entity;
            return EventResult.CONTINUE;
        }
    }

    public static class MultiFilteredEventReceiver {

        public TestEvent lastEvent;
        public EntityRef lastEntity;

        @ReceiveEvent(components = {Sample.class, Second.class})
        public EventResult onEvent(TestEvent event, EntityRef entity) {
            this.lastEvent = event;
            this.lastEntity = entity;
            return EventResult.CONTINUE;
        }
    }

    public static class MixedFilteredEventReceiver {

        public TestEvent lastEvent;
        public EntityRef lastEntity;
        public Second comp;

        @ReceiveEvent(components = {Sample.class})
        public EventResult onEvent(TestEvent event, EntityRef entity, Second comp) {
            this.lastEvent = event;
            this.lastEntity = entity;
            this.comp = comp;
            return EventResult.CONTINUE;
        }
    }

}
