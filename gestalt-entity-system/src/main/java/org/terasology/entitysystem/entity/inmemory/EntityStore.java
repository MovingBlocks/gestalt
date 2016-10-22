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

package org.terasology.entitysystem.entity.inmemory;

import gnu.trove.iterator.TLongIterator;
import gnu.trove.iterator.TLongObjectIterator;
import org.terasology.entitysystem.core.Component;

import java.util.Collection;
import java.util.Set;

/**
 *
 */
public interface EntityStore {

    long createEntityId();

    long getNextEntityId();

    int getEntityRevision(long entityId);

    boolean exists(long entityId);

    ClosableLock lock(Set<Long> entityIds);

    ClosableLock lockEntityCreation();

    <T extends Component> T get(long entityId, Class<T> componentClass);

    <T extends Component> boolean add(long entityId, T component);

    <T extends Component> boolean update(long entityId, T component);

    <T extends Component> Component remove(long entityId, Class<T> componentClass);

    void clear();

    int getComponentCount(Class<? extends Component> componentClass);

    Collection<Component> getComponents(long entityId);

    <T extends Component> TLongObjectIterator<T> componentIterator(Class<T> componentClass);

    TLongIterator entityIdIterator();

    int entityCount();

    boolean isAvailable(long entityId);

    EntityState getEntityState(long id);
}
