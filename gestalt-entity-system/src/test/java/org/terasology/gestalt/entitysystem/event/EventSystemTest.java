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

import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.gestalt.entitysystem.component.store.ArrayComponentStore;
import org.terasology.gestalt.entitysystem.component.store.ComponentStore;
import org.terasology.gestalt.entitysystem.entity.EntityManager;
import org.terasology.gestalt.entitysystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.entity.manager.CoreEntityManager;
import org.terasology.gestalt.entitysystem.event.impl.EventProcessor;
import org.terasology.gestalt.entitysystem.event.impl.EventSystemImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import modules.test.TestEvent;
import modules.test.TestSynchEvent;
import modules.test.components.Sample;
import modules.test.components.Second;

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
        eventSystem.registerHandler(TestEvent.class, eventHandler, eventHandler.getClass());
        eventSystem.send(asynchEvent, entity);
        verifyNoMoreInteractions(eventHandler);
        eventSystem.processEvents();
        verify(eventHandler).onEvent(asynchEvent, entity);
    }

    @Test
    public void sendAsynchEventHandleException() throws Exception {
        EventHandler<TestEvent> eventHandler = mock(EventHandler.class);
        eventSystem.registerHandler(TestEvent.class, eventHandler, eventHandler.getClass());
        when(eventHandler.onEvent(asynchEvent, entity)).thenThrow(new RuntimeException());
        eventSystem.send(asynchEvent, entity);
        eventSystem.processEvents();
    }

    @Test
    public void sendAsynchEventWithTriggeringComponentsIgnoresHandlerNotInterestedInThoseComponents() throws Exception {
        EventHandler<TestEvent> eventHandler = mock(EventHandler.class);
        eventSystem.registerHandler(TestEvent.class, eventHandler, eventHandler.getClass());
        eventSystem.send(asynchEvent, entity, triggeringComponents);
        eventSystem.processEvents();
        verifyNoMoreInteractions(eventHandler);
    }

    @Test
    public void sendAsynchEventWithTriggeringComponentsTriggersInterestedHandler() throws Exception {
        EventHandler<TestEvent> eventHandler = mock(EventHandler.class);
        eventSystem.registerHandler(TestEvent.class, eventHandler, eventHandler.getClass(), Sample.class);
        eventSystem.send(asynchEvent, entity, triggeringComponents);
        eventSystem.processEvents();
        verify(eventHandler).onEvent(asynchEvent, entity);
    }

    @Test
    public void sendSynchEvent() throws Exception {
        EventHandler<TestSynchEvent> eventHandler = mock(EventHandler.class);
        eventSystem.registerHandler(TestSynchEvent.class, eventHandler, eventHandler.getClass());
        eventSystem.send(synchEvent, entity);
        verify(eventHandler).onEvent(synchEvent, entity);
    }

}
