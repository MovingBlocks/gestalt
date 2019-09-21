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

package org.terasology.entitysystem.component.store;


import net.jcip.annotations.ThreadSafe;

import org.terasology.entitysystem.component.ComponentIterator;
import org.terasology.entitysystem.component.management.ComponentType;
import org.terasology.entitysystem.component.Component;
import org.terasology.entitysystem.entity.EntityRef;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@ThreadSafe
public class ConcurrentComponentStore<T extends Component<T>> implements ComponentStore<T> {

    private final ComponentStore<T> store;
    private final ReadWriteLock locks = new ReentrantReadWriteLock();

    public ConcurrentComponentStore(ComponentStore<T> store) {
        this.store = store;
    }

    @Override
    public ComponentType<T> getType() {
        return store.getType();
    }

    @Override
    public boolean has(int entityId) {
        Lock lock = locks.readLock();
        lock.lock();
        try {
            return store.has(entityId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean get(int entityId, T into) {
        Lock lock = locks.readLock();
        lock.lock();
        try {
            return store.get(entityId, into);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean set(EntityRef entity, T component) {
        Lock lock = locks.writeLock();
        lock.lock();
        try {
            return store.set(entity, component);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T remove(EntityRef entity) {
        Lock lock = locks.writeLock();
        lock.lock();
        try {
            return store.remove(entity);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int iterationCost() {
        return store.iterationCost();
    }

    @Override
    public void extend(int capacity) {
        Lock lock = locks.writeLock();
        lock.lock();
        try {
            store.extend(capacity);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ComponentIterator<T> iterate() {
        return new ConcurrentComponentIterator(store.iterate());
    }

    private class ConcurrentComponentIterator implements ComponentIterator<T> {
        private final ComponentIterator<T> inner;

        private ConcurrentComponentIterator(ComponentIterator<T> iterator) {
            inner = iterator;
        }

        @Override
        public boolean next() {
            Lock lock = locks.readLock();
            lock.lock();
            try {
                return inner.next();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public int getEntityId() {
            return inner.getEntityId();
        }

        @Override
        public void getComponent(Component<T> component) {
            Lock lock = locks.readLock();
            lock.lock();
            try {
                inner.getComponent(component);
            } finally {
                lock.unlock();
            }
        }
    }
}
