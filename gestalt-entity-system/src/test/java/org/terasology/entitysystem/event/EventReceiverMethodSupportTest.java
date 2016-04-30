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
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.terasology.entitysystem.Transaction;
import org.terasology.entitysystem.stubs.SampleComponent;
import org.terasology.entitysystem.stubs.SecondComponent;
import org.terasology.entitysystem.stubs.TestEvent;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class EventReceiverMethodSupportTest {


    @Test
    public void registerEventReceiverMethod() {
        TrivialEventReceiver receiver = new TrivialEventReceiver();
        EventProcessorBuilder builder = mock(EventProcessorBuilder.class);
        EventReceiverMethodSupport.register(receiver, builder);

        final ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(builder).addHandler(captor.capture(), eq(TrivialEventReceiver.class), eq(TestEvent.class), eq(Collections.emptySet()));

        TestEvent event = new TestEvent("test");
        Transaction transaction = mock(Transaction.class);
        captor.getValue().onEvent(event, 1, transaction);
        assertEquals(event, receiver.lastEvent);
        assertEquals(1, receiver.lastId);
        assertEquals(transaction, receiver.lastTransaction);
    }

    @Test
    public void registerWithFilterComponentFromAnnotation() {
        FilteredEventReceiver receiver = new FilteredEventReceiver();
        EventProcessorBuilder builder = mock(EventProcessorBuilder.class);
        EventReceiverMethodSupport.register(receiver, builder);

        final ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(builder).addHandler(captor.capture(), eq(FilteredEventReceiver.class), eq(TestEvent.class), eq(Sets.newHashSet(SampleComponent.class)));

        TestEvent event = new TestEvent("test");
        Transaction transaction = mock(Transaction.class);
        captor.getValue().onEvent(event, 1, transaction);
        assertEquals(event, receiver.lastEvent);
        assertEquals(1, receiver.lastId);
        assertEquals(transaction, receiver.lastTransaction);
    }

    @Test
    public void registerWithMultipleFilterComponentFromAnnotation() {
        MultiFilteredEventReceiver receiver = new MultiFilteredEventReceiver();
        EventProcessorBuilder builder = mock(EventProcessorBuilder.class);
        EventReceiverMethodSupport.register(receiver, builder);

        final ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(builder).addHandler(captor.capture(), eq(MultiFilteredEventReceiver.class), eq(TestEvent.class), eq(Sets.newHashSet(SampleComponent.class, SecondComponent.class)));

        TestEvent event = new TestEvent("test");
        Transaction transaction = mock(Transaction.class);
        captor.getValue().onEvent(event, 1, transaction);
        assertEquals(event, receiver.lastEvent);
        assertEquals(1, receiver.lastId);
        assertEquals(transaction, receiver.lastTransaction);
    }

    @Test
    public void registerWithComponentArg() {
        MixedFilteredEventReceiver receiver = new MixedFilteredEventReceiver();
        EventProcessorBuilder builder = mock(EventProcessorBuilder.class);
        EventReceiverMethodSupport.register(receiver, builder);

        final ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(builder).addHandler(captor.capture(), eq(MixedFilteredEventReceiver.class), eq(TestEvent.class), eq(Sets.newHashSet(SampleComponent.class, SecondComponent.class)));

        TestEvent event = new TestEvent("test");

        Transaction transaction = mock(Transaction.class);
        SecondComponent comp = mock(SecondComponent.class);
        when(transaction.getComponent(1, SecondComponent.class)).thenReturn(Optional.of(comp));
        captor.getValue().onEvent(event, 1, transaction);
        assertEquals(event, receiver.lastEvent);
        assertEquals(1, receiver.lastId);
        assertEquals(transaction, receiver.lastTransaction);
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
        public long lastId;
        public Transaction lastTransaction;

        @ReceiveEvent
        public EventResult onEvent(TestEvent event, long entityId, Transaction transaction) {
            this.lastEvent = event;
            this.lastId = entityId;
            this.lastTransaction = transaction;
            return EventResult.CONTINUE;
        }
    }

    @Before(TrivialEventReceiver.class)
    public static class GlobalBeforeEventReceiver {

        public TestEvent lastEvent;
        public long lastId;
        public Transaction lastTransaction;

        @ReceiveEvent
        public EventResult onEvent(TestEvent event, long entityId, Transaction transaction) {
            this.lastEvent = event;
            this.lastId = entityId;
            this.lastTransaction = transaction;
            return EventResult.CONTINUE;
        }
    }

    @After(TrivialEventReceiver.class)
    public static class GlobalAfterEventReceiver {

        public TestEvent lastEvent;
        public long lastId;
        public Transaction lastTransaction;

        @ReceiveEvent
        public EventResult onEvent(TestEvent event, long entityId, Transaction transaction) {
            this.lastEvent = event;
            this.lastId = entityId;
            this.lastTransaction = transaction;
            return EventResult.CONTINUE;
        }
    }

    public static class LocalBeforeEventReceiver {

        public TestEvent lastEvent;
        public long lastId;
        public Transaction lastTransaction;

        @Before(TrivialEventReceiver.class)
        @ReceiveEvent
        public EventResult onEvent(TestEvent event, long entityId, Transaction transaction) {
            this.lastEvent = event;
            this.lastId = entityId;
            this.lastTransaction = transaction;
            return EventResult.CONTINUE;
        }
    }

    public static class LocalAfterEventReceiver {

        public TestEvent lastEvent;
        public long lastId;
        public Transaction lastTransaction;

        @After(TrivialEventReceiver.class)
        @ReceiveEvent
        public EventResult onEvent(TestEvent event, long entityId, Transaction transaction) {
            this.lastEvent = event;
            this.lastId = entityId;
            this.lastTransaction = transaction;
            return EventResult.CONTINUE;
        }
    }

    public static class FilteredEventReceiver {

        public TestEvent lastEvent;
        public long lastId;
        public Transaction lastTransaction;

        @ReceiveEvent(components = SampleComponent.class)
        public EventResult onEvent(TestEvent event, long entityId, Transaction transaction) {
            this.lastEvent = event;
            this.lastId = entityId;
            this.lastTransaction = transaction;
            return EventResult.CONTINUE;
        }
    }

    public static class MultiFilteredEventReceiver {

        public TestEvent lastEvent;
        public long lastId;
        public Transaction lastTransaction;

        @ReceiveEvent(components = {SampleComponent.class, SecondComponent.class})
        public EventResult onEvent(TestEvent event, long entityId, Transaction transaction) {
            this.lastEvent = event;
            this.lastId = entityId;
            this.lastTransaction = transaction;
            return EventResult.CONTINUE;
        }
    }

    public static class MixedFilteredEventReceiver {

        public TestEvent lastEvent;
        public long lastId;
        public Transaction lastTransaction;
        public SecondComponent comp;

        @ReceiveEvent(components = {SampleComponent.class})
        public EventResult onEvent(TestEvent event, long entityId, Transaction transaction, SecondComponent comp) {
            this.lastEvent = event;
            this.lastId = entityId;
            this.lastTransaction = transaction;
            this.comp = comp;
            return EventResult.CONTINUE;
        }
    }

}
