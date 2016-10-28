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

package org.terasology.entitysystem.lifecycle;

import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.event.Event;

import java.util.Collection;
import java.util.function.BiFunction;

public class LifecycleEventFactories {
    private BiFunction<Integer, Collection<Component>, Event> addedEventFactory = OnAdded::new;
    private BiFunction<Integer, Collection<Component>, Event> removedFactoryEvent = OnRemoved::new;
    private ChangedEventFactory updatedEventFactory = OnChanged::new;

    public BiFunction<Integer, Collection<Component>, Event> getAddedEventFactory() {
        return addedEventFactory;
    }

    public void setAddedEventFactory(BiFunction<Integer, Collection<Component>, Event> addedEventFactory) {
        this.addedEventFactory = addedEventFactory;
    }

    public ChangedEventFactory getUpdatedEventFactory() {
        return updatedEventFactory;
    }

    public void setUpdatedEventFactory(ChangedEventFactory updatedEventFactory) {
        this.updatedEventFactory = updatedEventFactory;
    }

    public BiFunction<Integer, Collection<Component>, Event> getRemovedFactoryEvent() {
        return removedFactoryEvent;
    }

    public void setRemovedFactoryEvent(BiFunction<Integer, Collection<Component>, Event> removedFactoryEvent) {
        this.removedFactoryEvent = removedFactoryEvent;
    }

    public interface ChangedEventFactory {
        Event create(int revision, Collection<Component> beforeComponents, Collection<Component> afterComponents);
    }
}
