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

package org.terasology.entitysystem.index;

import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.entity.inmemory.EntityState;
import org.terasology.entitysystem.entity.inmemory.EntitySystemState;
import org.terasology.entitysystem.entity.inmemory.NewEntityState;
import org.terasology.entitysystem.transaction.TransactionManager;
import org.terasology.entitysystem.transaction.pipeline.TransactionContext;
import org.terasology.entitysystem.transaction.pipeline.TransactionInterceptor;
import org.terasology.entitysystem.transaction.pipeline.TransactionStage;
import org.terasology.util.collection.TypeKeyedMap;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import gnu.trove.TCollections;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

/**
 *
 */
public class GenericIndex implements Index {

    private final EntityManager entityManager;
    private final TLongSet entities;
    private final Predicate<Set<Class<? extends Component>>> relevantForUpdate;
    private final Predicate<TypeKeyedMap<Component>> includeInIndex;

    private ReentrantLock lock = new ReentrantLock();

    private TransactionInterceptor lockIndex = new TransactionInterceptor() {
        @Override
        public void handle(TransactionContext context) {
            Optional<EntitySystemState> entityState = context.getAttachment(EntitySystemState.class);
            if (entityState.isPresent()) {
                for (NewEntityState state : entityState.get().getNewEntities()) {
                    if (relevantForUpdate.test(state.getComponents().keySet())) {
                        lock.lock();
                        context.getOrAttach(LockInfo.class, LockInfo::new).addLocked(GenericIndex.this);
                        return;
                    }
                }
                for (EntityState state : entityState.get().getEntityStates()) {
                    if (relevantForUpdate.test(state.getInvolvedComponents())) {
                        lock.lock();
                        context.getOrAttach(LockInfo.class, LockInfo::new).addLocked(GenericIndex.this);
                        return;
                    }
                }
            }
            lock.lock();

            LockInfo lockInfo = context.getOrAttach(LockInfo.class, LockInfo::new);
            lockInfo.addLocked(this);
        }
    };
    private TransactionInterceptor unlockIndex = new TransactionInterceptor() {
        @Override
        public void handle(TransactionContext context) {
            Optional<LockInfo> existingLockInfo = context.getAttachment(LockInfo.class);
            if (existingLockInfo.isPresent()) {
                if (existingLockInfo.get().removeLocked(GenericIndex.this)) {
                    lock.unlock();
                }
            }
        }
    };
    private TransactionInterceptor updateIndex = new TransactionInterceptor() {
        @Override
        public void handle(TransactionContext context) {
            Optional<LockInfo> lockInfo = context.getAttachment(LockInfo.class);
            if (lockInfo.isPresent() && lockInfo.get().isLocked(GenericIndex.this)) {
                Optional<EntitySystemState> entityState = context.getAttachment(EntitySystemState.class);
                if (entityState.isPresent()) {
                    for (NewEntityState state : entityState.get().getNewEntities()) {
                        if (relevantForUpdate.test(state.getComponents().keySet()) && includeInIndex.test(state.getComponents())) {
                            entities.add(state.getId());
                        }
                    }
                    for (EntityState state : entityState.get().getEntityStates()) {
                        if (relevantForUpdate.test(state.getInvolvedComponents())) {
                            if (includeInIndex.test(state.getComponents())) {
                                entities.add(state.getId());
                            } else {
                                entities.remove(state.getId());
                            }
                        }
                    }
                }
            }
        }
    };

    public GenericIndex(TransactionManager transactionManager, EntityManager entityManager, Predicate<Set<Class<? extends Component>>> relevantForUpdate, Predicate<TypeKeyedMap<Component>> includeInIndex) {
        this.entityManager = entityManager;
        this.entities = TCollections.synchronizedSet(new TLongHashSet());
        this.relevantForUpdate = relevantForUpdate;
        this.includeInIndex = includeInIndex;

        transactionManager.getPipeline().registerInterceptor(TransactionStage.OBTAIN_LOCKS, lockIndex);
        transactionManager.getPipeline().registerInterceptor(TransactionStage.PROCESS_COMMIT, updateIndex);
        transactionManager.getPipeline().registerInterceptor(TransactionStage.RELEASE_LOCKS, unlockIndex);
    }

    @Override
    public boolean contains(EntityRef entity) {
        return entities.contains(entity.getId());
    }

    @Override
    public Iterator<EntityRef> iterator() {
        return entityManager.getEntities(entities).iterator();
    }
}
