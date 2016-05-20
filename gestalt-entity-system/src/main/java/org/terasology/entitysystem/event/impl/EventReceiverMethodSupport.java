/*
 * Copyright 2016 MovingBlocks
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

package org.terasology.entitysystem.event.impl;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitysystem.entity.Component;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.event.After;
import org.terasology.entitysystem.event.Before;
import org.terasology.entitysystem.event.Event;
import org.terasology.entitysystem.event.EventHandler;
import org.terasology.entitysystem.event.EventResult;
import org.terasology.entitysystem.event.ReceiveEvent;
import org.terasology.entitysystem.event.exception.EventSystemException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Helper to discover and register all methods annotated with {@link ReceiveEvent} in an object, into an EventProcessorBuilder.
 * Also processes {@link Before} and {@link After} annotations to order the methods.
 */
public final class EventReceiverMethodSupport {

    private static final Logger logger = LoggerFactory.getLogger(EventReceiverMethodSupport.class);

    private static final int FIXED_PARAM_COUNT = 2;

    private EventReceiverMethodSupport() {
    }

    /**
     * Scans for and registers all methods annotated with {@link ReceiveEvent} with the provided {@link EventProcessorBuilder}.
     *
     * @param eventReceiverObject The object to scan for methods
     * @param builder             The EventProcessorBuilder to register the methods with as event handlers.
     * @throws org.terasology.entitysystem.event.exception.InvalidEventReceiverObjectException if the eventReceiverObject is not a public class
     */
    public static void register(Object eventReceiverObject, EventProcessorBuilder builder) {
        Class<?> handlerClass = eventReceiverObject.getClass();
        if (!Modifier.isPublic(handlerClass.getModifiers())) {
            throw new EventSystemException("Cannot register handler " + handlerClass.getName() + ", must be public");
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

                registerEventHandler(new ByteCodeEventHandlerInfo(eventReceiverObject, method, componentParams), builder, handlerClass, globalBefore, globalAfter, method, types[0], requiredComponents);
            }
        }
    }

    private static void registerEventHandler(ByteCodeEventHandlerInfo byteCodeEventHandlerInfo, EventProcessorBuilder builder, Class<?> handlerClass, Set<Class<?>> globalBefore, Set<Class<?>> globalAfter, Method method, Class<?> type, Set<Class<? extends Component>> requiredComponents) {
        ByteCodeEventHandlerInfo handlerInfo = byteCodeEventHandlerInfo;
        builder.addHandler(handlerInfo, handlerClass, (Class<? extends Event>) type, requiredComponents);
        Set<Class<?>> beforeUnion = globalBefore;
        if (method.isAnnotationPresent(Before.class)) {
            beforeUnion = ImmutableSet.<Class<?>>builder().addAll(globalBefore).addAll(Arrays.asList(method.getAnnotation(Before.class).value())).build();
        }
        builder.orderBeforeAll(beforeUnion);

        Set<Class<?>> afterUnion = globalAfter;
        if (method.isAnnotationPresent(After.class)) {
            afterUnion = ImmutableSet.<Class<?>>builder().addAll(globalAfter).addAll(Arrays.asList(method.getAnnotation(After.class).value())).build();
        }
        builder.orderAfterAll(afterUnion);
    }

    private static List<Class<? extends Component>> gatherComponentParameters(Class<?>[] types) {
        List<Class<? extends Component>> componentParams = Lists.newArrayList();
        for (int i = FIXED_PARAM_COUNT; i < types.length; ++i) {
            componentParams.add((Class<? extends Component>) types[i]);
        }
        return componentParams;
    }

    private static boolean methodParametersValid(Class<?>[] types) {
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

    /**
     * Event handler using reflection
     */
    private static class ReflectedEventHandlerInfo implements EventHandler {
        private Object handler;
        private Method method;
        private ImmutableList<Class<? extends Component>> componentParams;

        public ReflectedEventHandlerInfo(Object handler,
                                         Method method,
                                         Collection<Class<? extends Component>> componentParams) {
            this.handler = handler;
            this.method = method;
            this.componentParams = ImmutableList.copyOf(componentParams);
        }

        @Override
        public EventResult onEvent(Event event, EntityRef entity) {
            try {
                Object[] params = new Object[FIXED_PARAM_COUNT + componentParams.size()];
                params[0] = event;
                params[1] = entity;
                for (int i = 0; i < componentParams.size(); ++i) {
                    params[i + FIXED_PARAM_COUNT] = entity.getComponent(componentParams.get(i));
                }
                return (EventResult) method.invoke(handler, params);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new EventSystemException("Error processing event", ex);
            }
        }

    }

    /**
     * Event handler using byte code generation.
     */
    private static class ByteCodeEventHandlerInfo implements EventHandler {
        private Object handler;
        private MethodAccess methodAccess;
        private int methodIndex;
        private ImmutableList<Class<? extends Component>> componentParams;

        public ByteCodeEventHandlerInfo(Object handler,
                                        Method method,
                                        Collection<Class<? extends Component>> componentParams) {
            this.handler = handler;
            this.methodAccess = MethodAccess.get(handler.getClass());
            methodIndex = methodAccess.getIndex(method.getName(), method.getParameterTypes());
            this.componentParams = ImmutableList.copyOf(componentParams);
        }

        @Override
        public EventResult onEvent(Event event, EntityRef entity) {
            try {
                Object[] params = new Object[FIXED_PARAM_COUNT + componentParams.size()];
                params[0] = event;
                params[1] = entity;
                for (int i = 0; i < componentParams.size(); ++i) {
                    params[i + FIXED_PARAM_COUNT] = entity.getComponent(componentParams.get(i)).orElseThrow(() -> new RuntimeException("Component unexpectedly missing"));
                }
                return (EventResult) methodAccess.invoke(handler, methodIndex, params);
            } catch (IllegalArgumentException ex) {
                throw new EventSystemException("Error processing event", ex);
            }
        }
    }

}
