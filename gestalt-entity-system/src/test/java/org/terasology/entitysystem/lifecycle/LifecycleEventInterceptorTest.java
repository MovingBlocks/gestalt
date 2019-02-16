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

package org.terasology.entitysystem.lifecycle;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.terasology.entitysystem.component.CodeGenComponentManager;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.entity.inmemory.CoreEntityRef;
import org.terasology.entitysystem.entity.inmemory.EntityState;
import org.terasology.entitysystem.entity.inmemory.EntitySystemState;
import org.terasology.entitysystem.event.Event;
import org.terasology.entitysystem.event.EventSystem;
import org.terasology.entitysystem.transaction.pipeline.TransactionContext;
import org.terasology.valuetype.TypeLibrary;

import java.util.Collections;

import modules.test.SampleComponent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class LifecycleEventInterceptorTest {

    private EntityManager mockEntityManager = mock(EntityManager.class);
    private EventSystem mockEventSystem = mock(EventSystem.class);
    private ComponentManager componentManager;
    private TransactionContext context = new TransactionContext();
    private EntitySystemState systemState = new EntitySystemState();

    public LifecycleEventInterceptorTest() {
        TypeLibrary typeLibrary = new TypeLibrary();
        componentManager = new CodeGenComponentManager(typeLibrary);
        context.attach(EntitySystemState.class, systemState);
    }

    @Test
    public void sendOnAddedEventForNewComponents() {
        EntityState entityState = new EntityState(1, 1, Collections.emptyList(), Collections.emptyList());
        systemState.addState(entityState);
        SampleComponent comp = componentManager.create(SampleComponent.class);
        comp.setName("Name");
        entityState.addComponent(comp);

        EntityRef entityRef = new CoreEntityRef(null, 1);
        when(mockEntityManager.getEntity(1)).thenReturn(entityRef);

        LifecycleEventInterceptor lifecycleEventInterceptor = new LifecycleEventInterceptor(mockEntityManager, mockEventSystem);
        lifecycleEventInterceptor.handle(context);

        ArgumentCaptor<Event> eventCapturer = ArgumentCaptor.forClass(Event.class);
        verify(mockEventSystem).send(eventCapturer.capture(), eq(entityRef), eq(Sets.newHashSet(SampleComponent.class)));

        assertTrue(eventCapturer.getValue() instanceof OnAdded);
        OnAdded onAddedEvent = (OnAdded) eventCapturer.getValue();
        assertEquals(comp, onAddedEvent.getComponent(SampleComponent.class));
    }

    @Test
    public void sendOnRemovedEventForRemovedComponents() {
        SampleComponent originalComp = componentManager.create(SampleComponent.class);
        EntityState entityState = new EntityState(1, 1, Lists.newArrayList(originalComp), Lists.newArrayList(originalComp));
        systemState.addState(entityState);
        entityState.removeComponent(SampleComponent.class);

        EntityRef entityRef = new CoreEntityRef(null, 1);
        when(mockEntityManager.getEntity(1)).thenReturn(entityRef);

        LifecycleEventInterceptor lifecycleEventInterceptor = new LifecycleEventInterceptor(mockEntityManager, mockEventSystem);
        lifecycleEventInterceptor.handle(context);

        ArgumentCaptor<Event> eventCapturer = ArgumentCaptor.forClass(Event.class);
        verify(mockEventSystem).send(eventCapturer.capture(), eq(entityRef), eq(Sets.newHashSet(SampleComponent.class)));

        assertTrue(eventCapturer.getValue() instanceof OnRemoved);
        OnRemoved onRemovedEvent = (OnRemoved) eventCapturer.getValue();
        assertEquals(originalComp, onRemovedEvent.getComponent(SampleComponent.class));
    }

    @Test
    public void sendOnChangedEventForUpdatedComponents() {
        SampleComponent originalComp = componentManager.create(SampleComponent.class);
        originalComp.setName("Name");
        SampleComponent workingComp = componentManager.create(SampleComponent.class);
        workingComp.setName("NewName");
        EntityState entityState = new EntityState(1, 1, Lists.newArrayList(originalComp), Lists.newArrayList(workingComp));
        systemState.addState(entityState);

        EntityRef entityRef = new CoreEntityRef(null, 1);
        when(mockEntityManager.getEntity(1)).thenReturn(entityRef);

        LifecycleEventInterceptor lifecycleEventInterceptor = new LifecycleEventInterceptor(mockEntityManager, mockEventSystem);
        lifecycleEventInterceptor.handle(context);

        ArgumentCaptor<Event> eventCapturer = ArgumentCaptor.forClass(Event.class);
        verify(mockEventSystem).send(eventCapturer.capture(), eq(entityRef), eq(Sets.newHashSet(SampleComponent.class)));

        assertTrue(eventCapturer.getValue() instanceof OnChanged);
        OnChanged onChangedEvent = (OnChanged) eventCapturer.getValue();
        assertEquals(originalComp, onChangedEvent.getBeforeComponent(SampleComponent.class));
        assertEquals(workingComp, onChangedEvent.getAfterComponent(SampleComponent.class));
    }
}
