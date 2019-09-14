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

package org.terasology.entitysystem.event.impl;

import com.google.common.collect.ImmutableSet;

import org.terasology.entitysystem.component.Component;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.event.Event;

import java.util.Collections;
import java.util.Set;

/**
 *
 */
class PendingEventInfo {
    private final Event event;
    private final EntityRef entity;
    private final Set<Class<? extends Component>> triggeringComponents;

    public PendingEventInfo(Event event, EntityRef entity, Set<Class<? extends Component>> triggeringComponents) {
        this.event = event;
        this.entity = entity;
        this.triggeringComponents = ImmutableSet.copyOf(triggeringComponents);
    }

    public Event getEvent() {
        return event;
    }

    public EntityRef getEntity() {
        return entity;
    }

    public Set<Class<? extends Component>> getTriggeringComponents() {
        return Collections.unmodifiableSet(triggeringComponents);
    }
}
