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

package org.terasology.gestalt.entitysystem.event.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.gestalt.entitysystem.event.After;
import org.terasology.gestalt.entitysystem.event.Before;
import org.terasology.gestalt.entitysystem.event.Event;
import org.terasology.gestalt.entitysystem.event.EventHandler;
import org.terasology.gestalt.entitysystem.event.EventHandlerFactory;
import org.terasology.gestalt.entitysystem.event.EventSystem;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.gestalt.entitysystem.event.exception.EventSystemException;
import org.terasology.gestalt.entitysystem.event.exception.InvalidEventReceiverObjectException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Helper to discover and register all methods annotated with {@link ReceiveEvent} in an object, into an {@link EventSystem}.
 * Also processes {@link Before} and {@link After} annotations to order the methods.
 */
public final class EventReceiverMethodSupport {

    public static final int FIXED_PARAM_COUNT = 2;

    private static final Logger logger = LoggerFactory.getLogger(EventReceiverMethodSupport.class);

    private EventHandlerFactory eventHandlerFactory;

    /**
     * Creates the EventReceiverMethodSupport with the default EventHandler factory that generates {@link ReflectionEventHandler}s.
     * It is recommended that in environments where they are supported that the high-performance alternative in gestalt-es-perf is used instead.
     */
    public EventReceiverMethodSupport() {
        this(ReflectionEventHandler::new);
    }

    /**
     * Creates the EventReceiverMethodSupport using the provided eventHandlerFactory to generate event handlers.
     *
     * @param eventHandlerFactory Factory for creating event handlers
     */
    public EventReceiverMethodSupport(EventHandlerFactory eventHandlerFactory) {
        this.eventHandlerFactory = eventHandlerFactory;
    }

    /**
     * Scans for and registers all methods annotated with {@link ReceiveEvent} with the provided {@link org.terasology.gestalt.entitysystem.event.EventSystem}.
     *
     * @param eventReceiverObject The object to scan for methods
     * @param eventSystem         The EventSystem to register the methods with as event handlers.
     * @throws org.terasology.gestalt.entitysystem.event.exception.InvalidEventReceiverObjectException if the eventReceiverObject is not a public class
     */
    @SuppressWarnings("unchecked")
    public void register(Object eventReceiverObject, EventSystem eventSystem) {
        Class<?> handlerClass = eventReceiverObject.getClass();
        if (!Modifier.isPublic(handlerClass.getModifiers())) {
            throw new InvalidEventReceiverObjectException("Cannot register handler " + handlerClass.getName() + ", must be public");
        }

        Set<Class<?>> globalBefore;
        if (handlerClass.isAnnotationPresent(Before.class)) {
            globalBefore = ImmutableSet.copyOf(handlerClass.getAnnotation(Before.class).value());
        } else {
            globalBefore = Collections.emptySet();
        }

        Set<Class<?>> globalAfter;
        if (handlerClass.isAnnotationPresent(After.class)) {
            globalAfter = ImmutableSet.copyOf(handlerClass.getAnnotation(After.class).value());
        } else {
            globalAfter = Collections.emptySet();
        }

        logger.debug("Registering event handler " + handlerClass.getName());
        for (Method method : handlerClass.getMethods()) {
            ReceiveEvent receiveEventAnnotation = method.getAnnotation(ReceiveEvent.class);
            if (receiveEventAnnotation != null) {
                logger.debug("Found method: " + method.toString());

                method.setAccessible(true);
                Class<?>[] types = method.getParameterTypes();

                if (!methodParametersValid(types)) {
                    logger.error("Invalid event handler method: {}::{}", handlerClass.getSimpleName(), method.getName());
                    continue;
                }

                Set<Class<? extends Component>> requiredComponents = Sets.newLinkedHashSet(Arrays.asList(receiveEventAnnotation.components()));
                List<Class<? extends Component>> componentParams = gatherComponentParameters(types);
                requiredComponents.addAll(componentParams);

                registerEventHandler(eventHandlerFactory.create(eventReceiverObject, method, componentParams), eventSystem, handlerClass, globalBefore, globalAfter, method, (Class<? extends Event>) types[0], requiredComponents);
            }
        }
    }

    private <T extends Event> void registerEventHandler(EventHandler<T> eventHandler, EventSystem eventSystem, Class<?> handlerClass, Set<Class<?>> globalBefore, Set<Class<?>> globalAfter, Method method, Class<? extends T> type, Set<Class<? extends Component>> requiredComponents) {
        Set<Class<?>> beforeUnion = globalBefore;
        if (method.isAnnotationPresent(Before.class)) {
            beforeUnion = ImmutableSet.<Class<?>>builder().addAll(globalBefore).addAll(Arrays.asList(method.getAnnotation(Before.class).value())).build();
        }

        Set<Class<?>> afterUnion = globalAfter;
        if (method.isAnnotationPresent(After.class)) {
            afterUnion = ImmutableSet.<Class<?>>builder().addAll(globalAfter).addAll(Arrays.asList(method.getAnnotation(After.class).value())).build();
        }

        eventSystem.registerHandler(type, eventHandler, handlerClass, beforeUnion, afterUnion, requiredComponents);
    }

    @SuppressWarnings("unchecked")
    private List<Class<? extends Component>> gatherComponentParameters(Class<?>[] types) {
        List<Class<? extends Component>> componentParams = Lists.newArrayList();
        for (int i = FIXED_PARAM_COUNT; i < types.length; ++i) {
            componentParams.add((Class<? extends Component>) types[i]);
        }
        return componentParams;
    }

    private boolean methodParametersValid(Class<?>[] types) {
        if (!Event.class.isAssignableFrom(types[0]) && Long.TYPE.isAssignableFrom(types[1])) {
            return false;
        }

        for (int i = FIXED_PARAM_COUNT; i < types.length; ++i) {
            if (!Component.class.isAssignableFrom(types[i])) {
                return false;
            }
        }
        return true;
    }

}
