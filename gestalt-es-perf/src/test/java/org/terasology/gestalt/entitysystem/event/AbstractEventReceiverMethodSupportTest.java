// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.entitysystem.event;

import com.google.common.collect.Sets;
import modules.test.TestEvent;
import modules.test.components.Sample;
import modules.test.components.Second;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.terasology.gestalt.entitysystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.event.impl.EventReceiverMethodSupport;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public abstract class AbstractEventReceiverMethodSupportTest {

    private EntityRef entity = mock(EntityRef.class);

    protected abstract EventHandlerFactory getEventHandlerFactory();
    private EventReceiverMethodSupport eventReceiverMethodSupport;

    @BeforeEach
    public void setup() {
        eventReceiverMethodSupport = new EventReceiverMethodSupport(getEventHandlerFactory());
    }

    @Test
    public void registerEventReceiverMethod() {
        TrivialEventReceiver receiver = new TrivialEventReceiver();
        EventSystem eventSystem = mock(EventSystem.class);
        eventReceiverMethodSupport.register(receiver, eventSystem);

        final ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(eventSystem).registerHandler(eq(TestEvent.class), captor.capture(), eq(TrivialEventReceiver.class), eq(Collections.emptySet()), eq(Collections.emptySet()), eq(Collections.emptySet()));

        TestEvent event = new TestEvent("test");

        captor.getValue().onEvent(event, entity);
        assertEquals(event, receiver.lastEvent);
        assertEquals(entity, receiver.lastEntity);
    }

    @Test
    public void registerWithFilterComponentFromAnnotation() {
        FilteredEventReceiver receiver = new FilteredEventReceiver();
        EventSystem system = mock(EventSystem.class);
        eventReceiverMethodSupport.register(receiver, system);

        final ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(system).registerHandler(eq(TestEvent.class), captor.capture(), eq(FilteredEventReceiver.class), eq(Collections.emptySet()), eq(Collections.emptySet()), eq(Sets.newHashSet(Sample.class)));

        TestEvent event = new TestEvent("test");
        captor.getValue().onEvent(event, entity);
        assertEquals(event, receiver.lastEvent);
        assertEquals(entity, receiver.lastEntity);
    }

    @Test
    public void registerWithMultipleFilterComponentFromAnnotation() {
        MultiFilteredEventReceiver receiver = new MultiFilteredEventReceiver();
        EventSystem builder = mock(EventSystem.class);
        eventReceiverMethodSupport.register(receiver, builder);

        final ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(builder).registerHandler(eq(TestEvent.class), captor.capture(), eq(MultiFilteredEventReceiver.class), eq(Collections.emptySet()), eq(Collections.emptySet()), eq(Sets.newHashSet(Sample.class, Second.class)));

        TestEvent event = new TestEvent("test");
        captor.getValue().onEvent(event, entity);
        assertEquals(event, receiver.lastEvent);
        assertEquals(entity, receiver.lastEntity);
    }

    @Test
    public void registerWithComponentArg() {
        MixedFilteredEventReceiver receiver = new MixedFilteredEventReceiver();
        EventSystem system = mock(EventSystem.class);
        eventReceiverMethodSupport.register(receiver, system);

        final ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(system).registerHandler(eq(TestEvent.class), captor.capture(), eq(MixedFilteredEventReceiver.class), eq(Collections.emptySet()), eq(Collections.emptySet()), eq(Sets.newHashSet(Sample.class, Second.class)));

        TestEvent event = new TestEvent("test");

        Second comp = new Second();
        when(entity.getComponent(Second.class)).thenReturn(Optional.of(comp));
        captor.getValue().onEvent(event, entity);
        assertEquals(event, receiver.lastEvent);
        assertEquals(entity, receiver.lastEntity);
        assertEquals(comp, receiver.comp);
    }

    @Test
    public void orderUsingClassBeforeAnnotation() {
        GlobalBeforeEventReceiver receiver = new GlobalBeforeEventReceiver();
        EventSystem system = mock(EventSystem.class);
        eventReceiverMethodSupport.register(receiver, system);

        verify(system).registerHandler(eq(TestEvent.class), any(EventHandler.class), eq(GlobalBeforeEventReceiver.class), eq(Sets.newHashSet(TrivialEventReceiver.class)), eq(Collections.emptySet()), eq(Collections.emptySet()));
    }


    @Test
    public void orderUsingClassAfterAnnotation() {
        GlobalAfterEventReceiver receiver = new GlobalAfterEventReceiver();
        EventSystem system = mock(EventSystem.class);
        eventReceiverMethodSupport.register(receiver, system);

        verify(system).registerHandler(eq(TestEvent.class), any(EventHandler.class), eq(GlobalAfterEventReceiver.class), eq(Collections.emptySet()), eq(Sets.newHashSet(TrivialEventReceiver.class)), eq(Collections.emptySet()));
    }

    @Test
    public void orderUsingMethodBeforeAnnotation() {
        LocalBeforeEventReceiver receiver = new LocalBeforeEventReceiver();
        EventSystem system = mock(EventSystem.class);
        eventReceiverMethodSupport.register(receiver, system);

        verify(system).registerHandler(eq(TestEvent.class), any(EventHandler.class), eq(LocalBeforeEventReceiver.class), eq(Sets.newHashSet(TrivialEventReceiver.class)), eq(Collections.emptySet()), eq(Collections.emptySet()));
    }

    @Test
    public void orderUsingMethodAfterAnnotation() {
        LocalAfterEventReceiver receiver = new LocalAfterEventReceiver();
        EventSystem system = mock(EventSystem.class);
        eventReceiverMethodSupport.register(receiver, system);

        verify(system).registerHandler(eq(TestEvent.class), any(EventHandler.class), eq(LocalAfterEventReceiver.class), eq(Collections.emptySet()), eq(Sets.newHashSet(TrivialEventReceiver.class)), eq(Collections.emptySet()));
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
