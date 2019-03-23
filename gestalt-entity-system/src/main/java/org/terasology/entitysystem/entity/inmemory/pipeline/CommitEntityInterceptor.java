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

package org.terasology.entitysystem.entity.inmemory.pipeline;

import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.component.ComponentType;
import org.terasology.entitysystem.component.PropertyAccessor;
import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.core.NullEntityRef;
import org.terasology.entitysystem.core.ProxyEntityRef;
import org.terasology.entitysystem.entity.inmemory.ClosableLock;
import org.terasology.entitysystem.entity.inmemory.EntityState;
import org.terasology.entitysystem.entity.inmemory.EntityStore;
import org.terasology.entitysystem.entity.inmemory.EntitySystemState;
import org.terasology.entitysystem.entity.inmemory.NewEntityState;
import org.terasology.entitysystem.transaction.pipeline.TransactionContext;
import org.terasology.entitysystem.transaction.pipeline.TransactionInterceptor;

/**
 * This TransactionInterceptor handles the committing of entity changes into an entity store.
 */
public class CommitEntityInterceptor implements TransactionInterceptor {

    private ComponentManager componentManager;
    private EntityManager entityManager;
    private EntityStore entityStore;

    /**
     * @param entityStore      The entity store to commit changes to
     * @param entityManager    The entity manager
     * @param componentManager The component manager
     */
    public CommitEntityInterceptor(EntityStore entityStore, EntityManager entityManager, ComponentManager componentManager) {
        this.entityStore = entityStore;
        this.entityManager = entityManager;
        this.componentManager = componentManager;
    }

    @Override
    public void handle(TransactionContext context) {
        context.getAttachment(EntitySystemState.class).ifPresent((state) -> {
            createNewEntities(context);
            applyEntityUpdates(context);
        });
    }

    private EntitySystemState getState(TransactionContext context) {
        return context.getAttachment(EntitySystemState.class).orElseThrow(IllegalStateException::new);
    }

    /**
     * Applies any changes to entities
     *
     * @param context
     */
    private void applyEntityUpdates(TransactionContext context) {
        for (EntityState entityState : getState(context).getEntityStates()) {
            for (Component component : entityState.getAddedComponents().values()) {
                cleanUpEntityRefs(component);
                if (!entityStore.add(entityState.getId(), component)) {
                    throw new RuntimeException("Entity state does not match expected.");
                }
            }
            for (Component component : entityState.getUpdatedComponents().values()) {
                cleanUpEntityRefs(component);
                if (!entityStore.update(entityState.getId(), component)) {
                    throw new RuntimeException("Entity state does not match expected.");
                }
            }
            for (Component component : entityState.getRemovedComponents().values()) {
                if (entityStore.remove(entityState.getId(), component.getClass()) == null) {
                    throw new RuntimeException("Entity state does not match expected.");
                }
            }
            entityState.setRevision(entityStore.getEntityRevision(entityState.getId()));
        }
    }

    /**
     * Create and save new entities.
     *
     * @param context
     */
    private void createNewEntities(TransactionContext context) {
        try (ClosableLock ignored = entityStore.lockEntityCreation()) {
            generateNewEntityIds(context);
            applyNewEntityComponents(context);
        }
    }

    private void applyNewEntityComponents(TransactionContext context) {
        for (NewEntityState newEntity : getState(context).getNewEntities()) {
            long id = newEntity.getId();
            for (Component component : newEntity.getComponents().values()) {
                cleanUpEntityRefs(component);
                entityStore.add(id, component);
            }
        }
    }

    private void generateNewEntityIds(TransactionContext context) {
        for (NewEntityState newEntity : getState(context).getNewEntities()) {
            if (!newEntity.getComponents().isEmpty()) {
                long entityId = entityStore.createEntityId();
                newEntity.setId(entityId);
                newEntity.setActualEntity(entityManager.getEntity(entityId));
            } else {
                newEntity.setActualEntity(NullEntityRef.get());
            }
        }
    }

    /**
     * Replace {@link ProxyEntityRef}s in a component with the actual EntityRef that is proxied.
     *
     * @param component
     */
    @SuppressWarnings("unchecked")
    private void cleanUpEntityRefs(Component component) {
        ComponentType<?> type = componentManager.getType(component.getClass());
        for (PropertyAccessor property : type.getPropertyInfo().getPropertiesOfType(EntityRef.class)) {
            Object o = property.get(component);
            if (o instanceof ProxyEntityRef) {
                property.set(component, ((ProxyEntityRef) o).getActualRef());
            }
        }
    }
}
