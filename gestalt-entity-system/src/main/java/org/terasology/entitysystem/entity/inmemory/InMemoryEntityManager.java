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

import com.google.common.base.Preconditions;
import gnu.trove.iterator.TLongIterator;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.entity.references.CoreEntityRef;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.entity.EntityTransaction;
import org.terasology.entitysystem.entity.TransactionEventListener;
import org.terasology.entitysystem.prefab.Prefab;
import org.terasology.util.Varargs;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;

/**
 *
 */
public class InMemoryEntityManager implements EntityManager {

    private final EntityStore entityStore;

    private final InMemoryTransactionManager transactionManager;

    public InMemoryEntityManager(ComponentManager library) {
        this(library, 1);
    }

    public InMemoryEntityManager(ComponentManager library, long nextEntityId) {
        entityStore = new ComponentTable(library, nextEntityId);
        transactionManager = new InMemoryTransactionManager(this, entityStore, library);
    }

    public long getEntityId() {
        return entityStore.getNextEntityId();
    }

    @Override
    public void registerTransactionListener(TransactionEventListener listener) {
        transactionManager.registerTransactionListener(listener);
    }

    @Override
    public void unregisterTransactionListener(TransactionEventListener listener) {
        transactionManager.unregisterTransactionListener(listener);
    }

    @Override
    public boolean isTransactionActive() {
        return false;
    }

    @Override
    public void beginTransaction() {
        transactionManager.begin();
    }

    @Override
    public void commit() throws ConcurrentModificationException {
        transactionManager.commit();
    }

    @Override
    public void rollback() {
        transactionManager.rollback();
    }

    @Override
    public EntityTransaction getRawTransaction() {
        return transactionManager.getCurrentTransaction();
    }

    @Override
    public EntityRef createEntity() {
        return transactionManager.getCurrentTransaction().createEntity();
    }

    @Override
    public EntityRef createEntity(Prefab prefab) {
        return transactionManager.getCurrentTransaction().createEntity(prefab);
    }

    @Override
    public Iterator<EntityRef> findEntitiesWithComponents(Class<? extends Component> first, Class<? extends Component>... additional) {
        return findEntitiesWithComponents(Varargs.combineToSet(first, additional));
    }

    @Override
    public Iterator<EntityRef> findEntitiesWithComponents(Set<Class<? extends Component>> componentTypes) {
        Preconditions.checkArgument(!componentTypes.isEmpty());
        return new EntityRefIterator(entityStore.findWithComponents(componentTypes), this);
    }

    @Override
    public Iterator<EntityRef> allEntities() {
        return new EntityRefIterator(entityStore.entityIdIterator(), this);
    }

    @Override
    public long getNextId() {
        return entityStore.getNextEntityId();
    }

    private static class EntityRefIterator implements Iterator<EntityRef> {

        private TLongIterator internal;
        private EntityManager entityManager;

        public EntityRefIterator(TLongIterator baseIterator, EntityManager entityManager) {
            this.internal = baseIterator;
            this.entityManager = entityManager;
        }

        @Override
        public boolean hasNext() {
            return internal.hasNext();
        }

        @Override
        public EntityRef next() {
            return new CoreEntityRef(entityManager, internal.next());
        }
    }
}
