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

import android.support.annotation.RequiresApi;

import com.google.common.collect.ImmutableList;

import org.terasology.entitysystem.component.ComponentTypeGenerationException;
import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.event.exception.EventSystemException;
import org.terasology.entitysystem.event.impl.EventReceiverMethodSupport;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * An event handler that makes use of Java 7's MethodHandle class to provide high performance event method triggering
 */
@RequiresApi(26)
public class MethodHandleEventHandle implements EventHandler {

    private static final int FIXED_PARAM_COUNT = 3;

    private Object handler;
    private MethodHandle methodHandle;
    private ImmutableList<Class<? extends Component>> componentParams;

    public MethodHandleEventHandle(Object handler,
                                   Method method,
                                   Collection<Class<? extends Component>> componentParams) {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        this.handler = handler;
        try {
            methodHandle = lookup.findVirtual(handler.getClass(), method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()));
        } catch (NoSuchMethodException | IllegalAccessException e) {

        }
        this.componentParams = ImmutableList.copyOf(componentParams);
    }

    @Override
    public EventResult onEvent(Event event, EntityRef entity) {
        Object[] params = new Object[EventReceiverMethodSupport.FIXED_PARAM_COUNT + componentParams.size() + 1];
        params[0] = handler;
        params[1] = event;
        params[2] = entity;
        for (int i = 0; i < componentParams.size(); ++i) {
            params[i + FIXED_PARAM_COUNT] = entity.getComponent(componentParams.get(i)).orElseThrow(() -> new RuntimeException("Component unexpectedly missing"));
        }

        try {
            return (EventResult) methodHandle.invokeWithArguments(params);
        } catch (Throwable e) {
            throw new EventSystemException("Error processing event", e);
        }
    }
}
