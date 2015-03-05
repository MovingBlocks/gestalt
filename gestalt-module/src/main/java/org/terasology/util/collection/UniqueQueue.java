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

package org.terasology.util.collection;

import com.google.common.collect.Iterators;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import org.terasology.module.sandbox.API;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

/**
 * A queue that ensures its contents are unique - an attempt to add an element to the queue that is already in the queue will be ignored.
 *
 * @author Immortius
 */
@API
public class UniqueQueue<T> implements Queue<T> {

    private final Set<T> containedSet;
    private final Queue<T> internalQueue;

    /**
     * Creates a new, empty queue
     */
    public UniqueQueue() {
        this(Sets.<T>newHashSet(), Queues.<T>newArrayDeque());
    }

    private UniqueQueue(Set<T> internalSet, Queue<T> internalQueue) {
        this.containedSet = internalSet;
        this.internalQueue = internalQueue;
    }

    /**
     * @param <T> The type that can be contained in the queue
     * @return A new, empty queue
     */
    public static <T> UniqueQueue<T> create() {
        return new UniqueQueue<>();
    }

    /**
     * @param size The expected size of the queue
     * @param <T>  The type that can be contained in the queue
     * @return A new, empty queue.
     */
    public static <T> UniqueQueue<T> createWithExpectedSize(int size) {
        return new UniqueQueue<>(Sets.<T>newHashSetWithExpectedSize(size), Queues.<T>newArrayDeque());
    }

    @Override
    public int size() {
        return containedSet.size();
    }

    @Override
    public boolean isEmpty() {
        return containedSet.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return containedSet.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return Iterators.unmodifiableIterator(internalQueue.iterator());
    }

    @Override
    public Object[] toArray() {
        return internalQueue.toArray();
    }

    @Override
    public <U> U[] toArray(U[] a) {
        return internalQueue.toArray(a);
    }

    @Override
    public boolean add(T t) {
        return containedSet.add(t) && internalQueue.add(t);
    }

    @Override
    public boolean remove(Object o) {
        return containedSet.remove(o) && internalQueue.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return containedSet.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean changed = false;
        for (T item : c) {
            if (containedSet.add(item)) {
                internalQueue.add(item);
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return containedSet.removeAll(c) && internalQueue.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return containedSet.retainAll(c) && internalQueue.retainAll(c);
    }

    @Override
    public void clear() {
        containedSet.clear();
        internalQueue.clear();
    }

    @Override
    public boolean offer(T t) {
        return containedSet.add(t) && internalQueue.offer(t);
    }

    @Override
    public T remove() {
        T result = internalQueue.remove();
        containedSet.remove(result);
        return result;
    }

    @Override
    public T poll() {
        T result = internalQueue.poll();
        containedSet.remove(result);
        return result;
    }

    @Override
    public T element() {
        return internalQueue.element();
    }

    @Override
    public T peek() {
        return internalQueue.peek();
    }
}
