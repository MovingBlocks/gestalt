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

import com.google.common.collect.Sets;

import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.entity.inmemory.EntityState;
import org.terasology.entitysystem.entity.inmemory.EntitySystemState;
import org.terasology.entitysystem.event.EventSystem;
import org.terasology.entitysystem.transaction.pipeline.TransactionContext;
import org.terasology.entitysystem.transaction.pipeline.TransactionInterceptor;

import java.util.Set;
import java.util.stream.Collectors;

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
                Set<Class<? extends Component>> addedComponentTypes = Sets.newLinkedHashSet();
                Set<Class<? extends Component>> updatedComponentTypes = Sets.newLinkedHashSet();
                Set<Class<? extends Component>> removedComponentTypes = Sets.newLinkedHashSet();

                for (Class<? extends Component> componentType : entityState.getInvolvedComponents()) {
                    switch (entityState.getUpdateAction(componentType)) {
                        case ADD: {
                            addedComponentTypes.add(componentType);
                        }
                        break;
                        case UPDATE: {
                            updatedComponentTypes.add(componentType);
                        }
                        break;
                        case REMOVE: {
                            removedComponentTypes.add(componentType);
                        }
                        break;
                        default:
                    }

                    if (!addedComponentTypes.isEmpty()) {
                        eventSystem.send(
                                factories.getAddedEventFactory().apply(
                                        entityState.getRevision(),
                                        addedComponentTypes.stream().map((t) -> entityState.getComponent(t).get()).collect(Collectors.toList())),
                                entityManager.getEntity(entityState.getId()),
                                addedComponentTypes);
                    }
                    if (!updatedComponentTypes.isEmpty()) {
                        eventSystem.send(factories.getUpdatedEventFactory().create(
                                entityState.getRevision(),
                                updatedComponentTypes.stream().map((t) -> entityState.getOriginalComponent(t).get()).collect(Collectors.toList()),
                                updatedComponentTypes.stream().map((t) -> entityState.getComponent(t).get()).collect(Collectors.toList())),
                                entityManager.getEntity(entityState.getId()),
                                updatedComponentTypes);
                    }
                    if (!removedComponentTypes.isEmpty()) {
                        eventSystem.send(factories.getRemovedFactoryEvent().apply(
                                entityState.getRevision(),
                                removedComponentTypes.stream().map((t) -> entityState.getOriginalComponent(t).get()).collect(Collectors.toList())),
                                entityManager.getEntity(entityState.getId()),
                                removedComponentTypes);
                    }
                }
            }
        });
    }
}
