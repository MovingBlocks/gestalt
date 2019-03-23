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

package org.terasology.entitysystem.lifecycle;

import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.entity.inmemory.EntityState;
import org.terasology.entitysystem.entity.inmemory.EntitySystemState;
import org.terasology.entitysystem.event.EventSystem;
import org.terasology.entitysystem.transaction.pipeline.TransactionContext;
import org.terasology.entitysystem.transaction.pipeline.TransactionInterceptor;
import org.terasology.util.collection.TypeKeyedMap;

/**
 * Lifecycle Event Handler sends events based on changes to entities during the transaction.
 */
public class LifecycleEventInterceptor implements TransactionInterceptor {
    private static final LifecycleEventFactories DEFAULT_EVENT_FACTORIES = new LifecycleEventFactories();

    private EntityManager entityManager;
    private EventSystem eventSystem;

    public LifecycleEventInterceptor(EntityManager entityManager, EventSystem eventSystem) {
        this.entityManager = entityManager;
        this.eventSystem = eventSystem;
    }

    @Override
    public void handle(TransactionContext context) {
        LifecycleEventFactories factories = context.getAttachment(LifecycleEventFactories.class).orElse(DEFAULT_EVENT_FACTORIES);
        context.getAttachment(EntitySystemState.class).ifPresent((entitySystemState) -> {
            for (EntityState entityState : entitySystemState.getEntityStates()) {
                TypeKeyedMap<Component> addedComponents = entityState.getAddedComponents();
                if (!addedComponents.isEmpty()) {
                    eventSystem.send(
                            factories.getAddedEventFactory().apply(
                                    entityState.getRevision(),
                                    addedComponents.values()),
                            entityManager.getEntity(entityState.getId()),
                            addedComponents.keySet());
                }

                TypeKeyedMap<Component> updatedComponents = entityState.getUpdatedComponents();
                if (!updatedComponents.isEmpty()) {
                    eventSystem.send(factories.getUpdatedEventFactory().create(
                            entityState.getRevision(),
                            updatedComponents.values()),
                            entityManager.getEntity(entityState.getId()),
                            updatedComponents.keySet());
                }

                TypeKeyedMap<Component> removedComponents = entityState.getRemovedComponents();
                if (!removedComponents.isEmpty()) {
                    eventSystem.send(factories.getRemovedFactoryEvent().apply(
                            entityState.getRevision(),
                            removedComponents.values()),
                            entityManager.getEntity(entityState.getId()),
                            entityState.getRemovedComponents().keySet());
                }
            }
        });
    }
}
