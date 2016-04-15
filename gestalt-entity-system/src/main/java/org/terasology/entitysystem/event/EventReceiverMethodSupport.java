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

import com.esotericsoftware.reflectasm.MethodAccess;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.terasology.entitysystem.Transaction;
import org.terasology.entitysystem.entity.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 *
 */
public final class EventReceiverMethodSupport {

//    private EventReceiverMethodSupport() {
//    }
//
//    public static void register(Object eventReceiverObject, EventProcessor eventProcessor) {
//
//    }
//
//    @Override
//    public void register(Object handler) {
//        Class handlerClass = handler.getClass();
//        if (!Modifier.isPublic(handlerClass.getModifiers())) {
//            throw new EventSystemException("Cannot register handler " + handler.getClass().getName() + ", must be public");
//        }
//
//        logger.debug("Registering event handler " + handlerClass.getName());
//        for (Method method : handlerClass.getMethods()) {
//            EventReceiver receiveEventAnnotation = method.getAnnotation(EventReceiver.class);
//            if (receiveEventAnnotation != null) {
//                registerEventReceiverMethod(handler, method, receiveEventAnnotation.components());
//            }
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    private void registerEventReceiverMethod(Object handler, Method method, Class<? extends Component>[] baseRequiredComponents) {
//        Set<Class<? extends Component>> requiredComponents = Sets.newLinkedHashSet();
//        requiredComponents.addAll(Arrays.asList(baseRequiredComponents));
//
//        method.setAccessible(true);
//        Class<?>[] types = method.getParameterTypes();
//
//        logger.debug("Found method: " + method.toString());
//        if (!Event.class.isAssignableFrom(types[0]) || !Long.TYPE.isAssignableFrom(types[1]) || !Transaction.class.isAssignableFrom(types[2])) {
//            logger.error("Invalid event handler method: {}::{}", handler.getClass().getSimpleName(), method.getName());
//            return;
//        }
//
//        List<Class<? extends Component>> componentParams = Lists.newArrayList();
//        for (int i = 3; i < types.length; ++i) {
//            if (!Component.class.isAssignableFrom(types[i])) {
//                logger.error("Invalid event handler method: {}::{} - {} is not a component class", handler.getClass().getSimpleName(), method.getName(), types[i]);
//                return;
//            }
//            requiredComponents.add((Class<? extends Component>) types[i]);
//            componentParams.add((Class<? extends Component>) types[i]);
//        }
//
//        ByteCodeEventHandlerInfo handlerInfo = new ByteCodeEventHandlerInfo(handler, method, requiredComponents, componentParams);
//        addEventHandler((Class<? extends Event>) types[0], handlerInfo);
//    }
//
//    @Override
//    public void unregister(Object handler) {
//        Class handlerClass = handler.getClass();
//        if (!Modifier.isPublic(handlerClass.getModifiers())) {
//            return;
//        }
//
//        for (Method method : handlerClass.getMethods()) {
//            EventReceiver receiveEventAnnotation = method.getAnnotation(EventReceiver.class);
//            if (receiveEventAnnotation != null) {
//                Class<?>[] types = method.getParameterTypes();
//
//                logger.debug("Found method: " + method.toString());
//                if (Event.class.isAssignableFrom(types[0])) {
//                    Class<? extends Event> eventClass = (Class<? extends Event>) types[0];
//                    removeHandlerFrom(handler, eventHandlers.get(eventClass));
//                    for (Class<? extends Event> childEvent : childEvents.get(eventClass)) {
//                        removeHandlerFrom(handler, eventHandlers.get(childEvent));
//                    }
//                }
//            }
//        }
//    }
//
//
//
//    private static class ReflectedEventHandlerInfo implements EventHandlerInfo {
//        private Object handler;
//        private Method method;
//        private ImmutableList<Class<? extends Component>> filterComponents;
//        private ImmutableList<Class<? extends Component>> componentParams;
//
//        public ReflectedEventHandlerInfo(Object handler,
//                                         Method method,
//                                         Collection<Class<? extends Component>> filterComponents,
//                                         Collection<Class<? extends Component>> componentParams) {
//            this.handler = handler;
//            this.method = method;
//            this.filterComponents = ImmutableList.copyOf(filterComponents);
//            this.componentParams = ImmutableList.copyOf(componentParams);
//        }
//
//        @Override
//        public ImmutableList<Class<? extends Component>> getFilterComponents() {
//            return filterComponents;
//        }
//
//        @Override
//        public EventResult invoke(long entityId, Event event, Transaction transaction) {
//            try {
//                Object[] params = new Object[3 + componentParams.size()];
//                params[0] = event;
//                params[1] = entityId;
//                params[2] = transaction;
//                for (int i = 0; i < componentParams.size(); ++i) {
//                    params[i + 3] = transaction.getComponent(entityId, componentParams.get(i));
//                }
//                return (EventResult) method.invoke(handler, params);
//            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
//                throw new EventSystemException("Error processing event", ex);
//            }
//        }
//
//        @Override
//        public Object getProvider() {
//            return handler;
//        }
//    }
//
//    private static class ByteCodeEventHandlerInfo implements EventHandlerInfo {
//        private Object handler;
//        private MethodAccess methodAccess;
//        private int methodIndex;
//        private ImmutableList<Class<? extends Component>> filterComponents;
//        private ImmutableList<Class<? extends Component>> componentParams;
//
//        public ByteCodeEventHandlerInfo(Object handler,
//                                        Method method,
//                                        Collection<Class<? extends Component>> filterComponents,
//                                        Collection<Class<? extends Component>> componentParams) {
//
//
//            this.handler = handler;
//            this.methodAccess = MethodAccess.get(handler.getClass());
//            methodIndex = methodAccess.getIndex(method.getName(), method.getParameterTypes());
//            this.filterComponents = ImmutableList.copyOf(filterComponents);
//            this.componentParams = ImmutableList.copyOf(componentParams);
//        }
//
//        @Override
//        public ImmutableList<Class<? extends Component>> getFilterComponents() {
//            return filterComponents;
//        }
//
//        public EventResult invoke(long entity, Event event, Transaction transaction) {
//            try {
//                Object[] params = new Object[3 + componentParams.size()];
//                params[0] = event;
//                params[1] = entity;
//                params[2] = transaction;
//                for (int i = 0; i < componentParams.size(); ++i) {
//                    params[i + 3] = transaction.getComponent(entity, componentParams.get(i)).orElseThrow(() -> new RuntimeException("Component unexpectedly missing"));
//                }
//                return (EventResult) methodAccess.invoke(handler, methodIndex, params);
//            } catch (IllegalArgumentException ex) {
//                throw new EventSystemException("Error processing event", ex);
//            }
//        }
//
//        @Override
//        public Object getProvider() {
//            return handler;
//        }
//    }
}
