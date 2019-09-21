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
import org.terasology.entitysystem.component.Component;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.event.impl.DelayedEventSystem;
import org.terasology.entitysystem.event.impl.EventProcessor;

import java.util.Collections;
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
public abstract class EventSystemTest {

    private static final String EVENT_VALUE = "Test";
    TestSynchEvent synchEvent = new TestSynchEvent(EVENT_VALUE);
    TestEvent asynchEvent = new TestEvent(EVENT_VALUE);
    private EntityRef entity = mock(EntityRef.class);
    private EventProcessor eventProcessor = mock(EventProcessor.class);
    private EventSystem eventSystem;
    private Set<Class<? extends Component>> triggeringComponents = ImmutableSet.of(Sample.class, Second.class);

    public EventSystemTest() {
        eventSystem = new DelayedEventSystem(eventProcessor);
    }

    @Test
    public void sendAsynchEvent() throws Exception {
        eventSystem.send(asynchEvent, entity);
        verifyNoMoreInteractions(eventProcessor);
        eventSystem.processEvents();
        verify(eventProcessor).send(asynchEvent, entity, Collections.emptySet());
    }

    @Test
    public void sendAsynchEventHandleException() throws Exception {
        when(eventProcessor.send(asynchEvent, entity, Collections.emptySet())).thenThrow(new RuntimeException());

        eventSystem.send(asynchEvent, entity);
        eventSystem.processEvents();
    }

    @Test
    public void sendAsynchEventWithTriggeringComponents() throws Exception {
        eventSystem.send(asynchEvent, entity, triggeringComponents);
        eventSystem.processEvents();
        verify(eventProcessor).send(asynchEvent, entity, triggeringComponents);
    }

    @Test
    public void sendSynchEvent() throws Exception {
        when(eventProcessor.send(synchEvent, entity, Collections.emptySet())).thenThrow(new RuntimeException());

        eventSystem.send(synchEvent, entity);
    }


    @Test
    public void sendSynchEventWithTriggeringComponents() throws Exception {
        eventSystem.send(synchEvent, entity, triggeringComponents);

        verify(eventProcessor).send(synchEvent, entity, triggeringComponents);
    }
}
