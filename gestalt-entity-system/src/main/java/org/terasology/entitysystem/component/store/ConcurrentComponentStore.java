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

import org.terasology.entitysystem.component.Component;
import org.terasology.entitysystem.component.ComponentIterator;
import org.terasology.entitysystem.component.management.ComponentType;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ConcurrentComponentStore wraps another ComponentStore, making it thread safe - at least in so
 * far as making every method atomic. Note that this doesn't prevent iteration failure due to
 * concurrent modification - nor does it protect against lost update issues from other threads
 * making modifications between read and write operations.
 *
 * To provide thread safety, all interaction with the store must be through the wrapped store.
 *
 * Basic expected usage is to have a single thread that does writes, while other threads can still
 * read. Potentially this system could be extended with entity-level optimistic or pessimistic locking
 * to give true multithread read-write support, although performance overheads would have to be considered.
 *
 * @param <T> The type of component stored in this store.
 */
@ThreadSafe
public class ConcurrentComponentStore<T extends Component<T>> implements ComponentStore<T> {

    private final ComponentStore<T> store;
    private final ReadWriteLock locks = new ReentrantReadWriteLock();

    /**
     * @param store The store to wrap
     */
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
    public boolean set(int entityId, T component) {
        Lock lock = locks.writeLock();
        lock.lock();
        try {
            return store.set(entityId, component);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T remove(int entityId) {
        Lock lock = locks.writeLock();
        lock.lock();
        try {
            return store.remove(entityId);
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
