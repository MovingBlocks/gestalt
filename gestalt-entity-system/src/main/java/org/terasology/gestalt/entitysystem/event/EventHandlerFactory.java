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

import org.terasology.gestalt.entitysystem.component.Component;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Interface for generating event handlers. For convenience, it is nice if the EventHandler implementations constructor matches
 * this signature so that it's ::new can be used rather than needing to create an actual factory
 */
public interface EventHandlerFactory {

    /**
     * @param handler The object with the method to call for an event
     * @param method The method of the object to call
     * @param componentParams An ordered list of the component parameters the method requires.
     * @return An event handler that will call handler::method(event, entity, component1, component2, ...)
     */
    EventHandler create(Object handler,
                        Method method,
                        Collection<Class<? extends Component>> componentParams);
}
