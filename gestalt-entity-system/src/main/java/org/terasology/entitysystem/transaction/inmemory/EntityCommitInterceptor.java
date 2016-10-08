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

package org.terasology.entitysystem.transaction.inmemory;

import gnu.trove.iterator.TLongIterator;
import gnu.trove.set.hash.TLongHashSet;
import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.transaction.TransactionEventListener;
import org.terasology.entitysystem.transaction.pipeline.TransactionContext;
import org.terasology.entitysystem.transaction.pipeline.TransactionInterceptor;
import org.terasology.entitysystem.transaction.pipeline.TransactionState;
import org.terasology.entitysystem.transaction.pipeline.TransactionalEntityState;
import org.terasology.entitysystem.transaction.references.CoreEntityRef;
import org.terasology.entitysystem.transaction.references.NewEntityRef;
import org.terasology.entitysystem.transaction.references.NullEntityRef;

import java.util.ConcurrentModificationException;
import java.util.Optional;
import java.util.Set;

/**
 *
 */
public class EntityCommitInterceptor implements TransactionInterceptor {

    private EntityManager entityManager;
    private EntityStore entityStore;

    public EntityCommitInterceptor(EntityStore entityStore, EntityManager entityManager) {
        this.entityStore = entityStore;
        this.entityManager = entityManager;
    }

    @Override
    public boolean handle(TransactionContext context) {

//        try (ClosableLock ignored = entityStore.lock(context.getState().getInvolvedEntityIds())) {
//            checkRevisions(context);
//            createNewEntities(context);
//
//            for (NewEntityRef newEntity : context.getState().getNewEntities()) {
//                Set<Class<? extends Component>> componentTypes = newEntity.getComponentTypes();
//                if (!componentTypes.isEmpty()) {
//                    long entityId = newEntity.getInnerEntityRef().get().getId();
//                    ClosableLock lock = entityStore.lock(new TLongHashSet(new long[]{entityId}));
//                    for (Class componentType : componentTypes) {
//                        Component comp = (Component) newEntity.getComponent(componentType).get();
//                        cleanUpEntityRefs(componentType, comp);
//                        entityStore.add(entityId, componentType, comp);
//                    }
//                    lock.close();
//                }
//                newEntity.activateInnerRef();
//            }
//
//            // Apply changes
//            for (CacheEntry comp : state.entityCache.values()) {
//                switch (comp.getAction()) {
//                    case ADD:
//                        cleanUpEntityRefs(comp.componentType, comp.component);
//                        if (!entityStore.add(comp.getEntityId(), comp.getComponentType(), comp.getComponent())) {
//                            throw new RuntimeException("Entity state does not match expected.");
//                        }
//                        break;
//                    case REMOVE:
//                        if (entityStore.remove(comp.getEntityId(), comp.getComponentType()) == null) {
//                            throw new RuntimeException("Entity state does not match expected.");
//                        }
//                        break;
//                    case UPDATE:
//                        cleanUpEntityRefs(comp.componentType, comp.component);
//                        if (!entityStore.update(comp.getEntityId(), comp.getComponentType(), comp.getComponent())) {
//                            throw new RuntimeException("Entity state does not match expected.");
//                        }
//                        break;
//                }
//            }
//
//            eventListeners.forEach(TransactionEventListener::onCommit);
//        }
        return true;
    }

    public void createNewEntities(TransactionContext context) {
        for (NewEntityRef newEntity : context.getState().getNewEntities()) {
            Set<Class<? extends Component>> componentTypes = newEntity.getComponentTypes();
            if (!componentTypes.isEmpty()) {
                long entityId = entityStore.createEntityId();
                newEntity.setInnerEntityRef(new CoreEntityRef(entityManager, entityId));
            } else {
                newEntity.setInnerEntityRef(NullEntityRef.get());
            }
        }
    }

    public void checkRevisions(TransactionContext context) {
        for (TransactionalEntityState entityState : context.getState().getEntityStates()) {
            if (entityState.getRevision() != entityStore.getEntityRevision(entityState.getId())) {
                throw new ConcurrentModificationException("Entity " + entityState.getId() + " modified outside of transaction");
            }
        }
    }

    private void wipeEntityRefs(TransactionState state) {
        for (NewEntityRef entityRef : state.getNewEntities()) {

        }

    }
}
