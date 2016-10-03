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


import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.event.impl.EventProcessor;

/**
 * Interface for an event handler. An event handler receives notification when the event of interest has been sent against an entity that matches the conditions desired
 * when it was registered with an {@link EventProcessor}.
 *
 * @author Immortius
 */
public interface EventHandler<T extends Event> {

    /**
     * Call back for when the desired event has been sent against an entity matching the desired conditions the EventHandler was registered with.
     *
     * @param event       The event that was sent against the entity
     * @param entity      The entity that is receiving the event
     * @return Whether the event processing should continue or be halted.
     */
    EventResult onEvent(T event, EntityRef entity);
}
