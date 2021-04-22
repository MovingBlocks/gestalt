// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.entitysystem.event;

import com.google.common.collect.ImmutableSet;
import modules.test.TestEvent;
import modules.test.TestSynchEvent;
import modules.test.components.Sample;
import modules.test.components.Second;
import org.junit.jupiter.api.Test;
import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.gestalt.entitysystem.component.store.ComponentStore;
import org.terasology.gestalt.entitysystem.entity.EntityManager;
import org.terasology.gestalt.entitysystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.entity.manager.CoreEntityManager;
import org.terasology.gestalt.entitysystem.event.impl.EventSystemImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Core tests showing correct behaviour for any EventSystem implementation.
 */
public class EventSystemTest {

    private static final String EVENT_VALUE = "Test";
    private TestSynchEvent synchEvent = new TestSynchEvent(EVENT_VALUE);
    private TestEvent asynchEvent = new TestEvent(EVENT_VALUE);
    private EntityManager entityManager;
    private EntityRef entity;
    private EventSystem eventSystem = new EventSystemImpl();
    private Set<Class<? extends Component>> triggeringComponents = ImmutableSet.of(Sample.class, Second.class);


    public EventSystemTest() {
        List<ComponentStore<?>> componentStores = new ArrayList<>();
        entityManager = new CoreEntityManager(componentStores);
        entity = entityManager.createEntity();
    }

    @Test
    public void sendAsynchEvent() throws Exception {
        EventHandler<TestEvent> eventHandler = mock(EventHandler.class);
        eventSystem.registerHandler(TestEvent.class, eventHandler);
        eventSystem.send(asynchEvent, entity);
        verifyNoMoreInteractions(eventHandler);
        eventSystem.processEvents();
        verify(eventHandler).onEvent(asynchEvent, entity);
    }

    @Test
    public void sendAsynchEventHandleException() throws Exception {
        EventHandler<TestEvent> eventHandler = mock(EventHandler.class);
        eventSystem.registerHandler(TestEvent.class, eventHandler);
        when(eventHandler.onEvent(asynchEvent, entity)).thenThrow(new RuntimeException());
        eventSystem.send(asynchEvent, entity);
        eventSystem.processEvents();
    }

    @Test
    public void sendAsynchEventWithTriggeringComponentsIgnoresHandlerNotInterestedInThoseComponents() throws Exception {
        EventHandler<TestEvent> eventHandler = mock(EventHandler.class);
        eventSystem.registerHandler(TestEvent.class, eventHandler);
        eventSystem.send(asynchEvent, entity, triggeringComponents);
        eventSystem.processEvents();
        verifyNoMoreInteractions(eventHandler);
    }

    @Test
    public void sendAsynchEventWithTriggeringComponentsTriggersInterestedHandler() throws Exception {
        EventHandler<TestEvent> eventHandler = mock(EventHandler.class);
        eventSystem.registerHandler(TestEvent.class, eventHandler, Sample.class);
        eventSystem.send(asynchEvent, entity, triggeringComponents);
        eventSystem.processEvents();
        verify(eventHandler).onEvent(asynchEvent, entity);
    }

    @Test
    public void sendSynchEvent() throws Exception {
        EventHandler<TestSynchEvent> eventHandler = mock(EventHandler.class);
        eventSystem.registerHandler(TestSynchEvent.class, eventHandler);
        eventSystem.send(synchEvent, entity);
        verify(eventHandler).onEvent(synchEvent, entity);
    }

}
