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

import org.terasology.entitysystem.core.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark up methods that can be registered to receive events through the EventSystem
 * <p>
 * These methods should have the form
 * <code>public EventResult handlerMethod(EventType event, EntityRef ref, Transaction transaction, [, SomeComponent component...])</code>
 * <p>
 * That is the method's parameters are the event, the entityId, and optionally components to be provided to the method. If components are listed in the method, then they
 * are required on the entity and do not need to be listed in the annotation's components list.
 *
 * @author Immortius
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface ReceiveEvent {

    /**
     * What components that the entity must have for this method to be invoked
     */
    Class<? extends Component>[] components() default {};
}
