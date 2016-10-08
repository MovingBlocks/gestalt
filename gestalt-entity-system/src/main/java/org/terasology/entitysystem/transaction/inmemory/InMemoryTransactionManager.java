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

package org.terasology.entitysystem.transaction.inmemory;

import com.google.common.collect.Lists;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.transaction.EntityTransaction;
import org.terasology.entitysystem.transaction.TransactionEventListener;

import java.util.List;

/**
 *
 */
public class InMemoryTransactionManager {

    private final EntityManager entityManager;
    private final EntityStore entityStore;
    private final ComponentManager componentManager;
    private final List<TransactionEventListener> eventListeners = Lists.newCopyOnWriteArrayList();

    private ThreadLocal<InMemoryTransaction> transactions = new ThreadLocal<InMemoryTransaction>() {
        @Override
        protected InMemoryTransaction initialValue() {
            return new InMemoryTransaction(entityManager, entityStore, componentManager, eventListeners);
        }
    };

    public InMemoryTransactionManager(EntityManager entityManager, EntityStore entityStore, ComponentManager componentManager) {
        this.entityManager = entityManager;
        this.entityStore = entityStore;
        this.componentManager = componentManager;
    }

    public void registerTransactionListener(TransactionEventListener listener) {
        eventListeners.add(listener);
    }

    public void unregisterTransactionListener(TransactionEventListener listener) {
        eventListeners.remove(listener);
    }

    public void begin() {
        transactions.get().begin();
    }

    public void commit() {
        transactions.get().commit();
    }

    public void rollback() {
        transactions.get().rollback();
    }

    public EntityTransaction getCurrentTransaction() {
        return transactions.get();
    }
}
