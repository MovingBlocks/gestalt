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

package org.terasology.entitysystem.entity.inmemory;

import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.component.ComponentType;
import org.terasology.entitysystem.component.PropertyAccessor;
import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.core.NullEntityRef;
import org.terasology.entitysystem.transaction.pipeline.TransactionContext;
import org.terasology.entitysystem.transaction.pipeline.TransactionInterceptor;

import java.util.ConcurrentModificationException;
import java.util.Set;

/**
 *
 */
public class EntityCommitInterceptor implements TransactionInterceptor {

    private ReferenceAdaptor referenceAdaptor;
    private ComponentManager componentManager;
    private EntityStore entityStore;

    public EntityCommitInterceptor(EntityStore entityStore, ReferenceAdaptor referenceAdaptor, ComponentManager componentManager) {
        this.entityStore = entityStore;
        this.referenceAdaptor = referenceAdaptor;
        this.componentManager = componentManager;
    }

    @Override
    public void handle(TransactionContext context) {
        context.getAttachment(EntitySystemState.class).ifPresent((state) -> {
            try (ClosableLock ignored = entityStore.lock(state.getInvolvedEntityIds())) {
                checkRevisions(context);
                createNewEntities(context);
                applyEntityUpdates(context);
            }
        });
    }

    private EntitySystemState getState(TransactionContext context) {
        return context.getAttachment(EntitySystemState.class).orElseThrow(IllegalStateException::new);
    }

    private void applyEntityUpdates(TransactionContext context) {
        for (EntityState entityState : getState(context).getEntityStates()) {
            for (Class<? extends Component> componentType : entityState.getInvolvedComponents()) {
                switch (entityState.getUpdateAction(componentType)) {
                    case ADD: {
                        Component component = entityState.getComponent(componentType).get();
                        cleanUpEntityRefs(component);
                        if (!entityStore.add(entityState.getId(), component)) {
                            throw new RuntimeException("Entity state does not match expected.");
                        }
                        break;
                    }
                    case UPDATE: {
                        Component component = entityState.getComponent(componentType).get();
                        cleanUpEntityRefs(component);
                        if (!entityStore.update(entityState.getId(), component)) {
                            throw new RuntimeException("Entity state does not match expected.");
                        }
                        break;
                    }
                    case REMOVE: {
                        if (entityStore.remove(entityState.getId(), componentType) == null) {
                            throw new RuntimeException("Entity state does not match expected.");
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        }
    }

    private void createNewEntities(TransactionContext context) {
        try (ClosableLock ignored = entityStore.lockEntityCreation()) {
            generateNewEntityIds(context);
            applyNewEntityComponents(context);
        }
    }

    private void applyNewEntityComponents(TransactionContext context) {
        for (NewEntityRef newEntity : getState(context).getNewEntities()) {
            long id = newEntity.getInnerEntityRef().get().getId();
            for (Component component : newEntity.getComponents().values()) {
                cleanUpEntityRefs(component);
                entityStore.add(id, component);
            }
            newEntity.activateInnerRef();
        }
    }

    private void generateNewEntityIds(TransactionContext context) {
        for (NewEntityRef newEntity : getState(context).getNewEntities()) {
            Set<Class<? extends Component>> componentTypes = newEntity.getComponentTypes();
            if (!componentTypes.isEmpty()) {
                long entityId = entityStore.createEntityId();
                newEntity.setInnerEntityRef(new CoreEntityRef(referenceAdaptor, entityId));
            } else {
                newEntity.setInnerEntityRef(NullEntityRef.get());
            }
        }
    }

    private void checkRevisions(TransactionContext context) {
        for (EntityState entityState : getState(context).getEntityStates()) {
            if (entityState.getRevision() != entityStore.getEntityRevision(entityState.getId())) {
                throw new ConcurrentModificationException("Entity " + entityState.getId() + " modified outside of transaction");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void cleanUpEntityRefs(Component component) {
        ComponentType<?> type = componentManager.getType(component.getType());
        for (PropertyAccessor property : type.getPropertyInfo().getPropertiesOfType(EntityRef.class)) {
            Object o = property.get(component);
            if (o instanceof NewEntityRef) {
                NewEntityRef entityRef = (NewEntityRef) o;
                entityRef.getInnerEntityRef().ifPresent((x) -> property.set(component, x));
            }
        }
    }
}
