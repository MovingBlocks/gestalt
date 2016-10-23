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
import com.google.common.collect.Maps;
import gnu.trove.iterator.TLongIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.ResourceUrn;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.component.ComponentType;
import org.terasology.entitysystem.component.PropertyAccessor;
import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.core.NullEntityRef;
import org.terasology.entitysystem.prefab.EntityRecipe;
import org.terasology.entitysystem.prefab.GeneratedFromEntityRecipeComponent;
import org.terasology.entitysystem.prefab.Prefab;
import org.terasology.entitysystem.prefab.PrefabRef;
import org.terasology.entitysystem.transaction.TransactionManager;
import org.terasology.entitysystem.transaction.exception.ComponentAlreadyExistsException;
import org.terasology.entitysystem.transaction.exception.ComponentDoesNotExistException;
import org.terasology.entitysystem.transaction.pipeline.TransactionStage;
import org.terasology.naming.Name;
import org.terasology.util.collection.TypeKeyedMap;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 *
 */
public class InMemoryEntityManager implements EntityManager, ReferenceAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryEntityManager.class);

    private final EntityStore entityStore;
    private final ComponentManager componentManager;
    private final TransactionManager transactionManager;

    public InMemoryEntityManager(ComponentManager library, TransactionManager transactionManager) {
        this(library, transactionManager, 1);
    }

    public InMemoryEntityManager(ComponentManager library, TransactionManager transactionManager, long nextEntityId) {
        Preconditions.checkNotNull(library);
        Preconditions.checkNotNull(transactionManager);
        this.componentManager = library;
        this.entityStore = new ComponentTable(library, nextEntityId);
        this.transactionManager = transactionManager;
        transactionManager.getPipeline().registerInterceptor(TransactionStage.PRE_TRANSACTION, context -> context.attach(EntitySystemState.class, new EntitySystemState()));
        transactionManager.getPipeline().registerInterceptor(TransactionStage.OBTAIN_LOCKS, new LockEntitiesInterceptor(entityStore));
        transactionManager.getPipeline().registerInterceptor(TransactionStage.VERIFY_COMMIT, new VerifyCommitInterceptor(entityStore));
        transactionManager.getPipeline().registerInterceptor(TransactionStage.PROCESS_COMMIT, new CommitEntityInterceptor(entityStore, this, componentManager));
        transactionManager.getPipeline().registerInterceptor(TransactionStage.RELEASE_LOCKS, new UnlockEntitiesInterceptor());
        transactionManager.getPipeline().registerInterceptor(TransactionStage.POST_ROLLBACK, new WipeNewEntityInterceptor());
    }

    @Override
    public EntityRef createEntity() {
        EntitySystemState state = getState();
        NewEntityRef newEntityRef = new NewEntityRef(componentManager);
        state.getNewEntities().add(newEntityRef);
        return newEntityRef;
    }

    @Override
    public EntityRef getEntity(long id) {
        return new CoreEntityRef(this, id);
    }

    // TODO: New home for prefab instantiation code?
    @Override
    public EntityRef createEntity(Prefab prefab) {
        Map<Name, EntityRef> entities = createEntities(prefab);
        return entities.get(prefab.getRootEntityUrn().getFragmentName());
    }

    @Override
    public Iterator<EntityRef> allEntities() {
        return new EntityRefIterator(entityStore.entityIdIterator(), this);
    }

    @Override
    public long getNextId() {
        return entityStore.getNextEntityId();
    }

    @Override
    public boolean exists(long id) {
        EntityState entityState = getEntityState(id);
        return entityState.getRevision() != 0;
    }

    @Override
    public <T extends Component> Optional<T> getComponent(long entityId, Class<T> componentType) {
        EntityState entityState = getEntityState(entityId);
        return entityState.getComponent(componentType);
    }

    @Override
    public TypeKeyedMap<Component> getEntityComposition(long entityId) {
        EntityState entityState = getEntityState(entityId);
        return entityState.getComponents();
    }

    @Override
    public <T extends Component> T addComponent(long entityId, Class<T> componentType) {
        EntityState entityState = getEntityState(entityId);
        if (entityState.getComponent(componentType).isPresent()) {
            throw new ComponentAlreadyExistsException("Entity " + entityId + " already has a component of type " + componentType.getSimpleName());
        }
        T newComp = componentManager.create(componentType);
        entityState.addComponent(newComp);
        return newComp;
    }

    @Override
    public <T extends Component> void removeComponent(long entityId, Class<T> componentType) {
        EntityState entityState = getEntityState(entityId);
        if (!entityState.getComponent(componentType).isPresent()) {
            throw new ComponentDoesNotExistException("Entity " + entityId + " does not have a component of type " + componentType.getSimpleName());
        }
        entityState.removeComponent(componentType);
    }

    @Override
    public Map<Name, EntityRef> createEntities(Prefab prefab) {
        Map<Name, EntityRef> result = createEntityStubs(prefab);
        populatePrefabEntities(prefab, result);
        return result;
    }

    private void populatePrefabEntities(Prefab prefab, Map<Name, EntityRef> result) {
        for (EntityRecipe entityRecipe : prefab.getEntityRecipes().values()) {
            EntityRef entity = result.get(entityRecipe.getIdentifier().getFragmentName());
            GeneratedFromEntityRecipeComponent entityMetadata = entity.addComponent(GeneratedFromEntityRecipeComponent.class);
            entityMetadata.setEntityRecipe(entityRecipe.getIdentifier());

            for (TypeKeyedMap.Entry<? extends Component> entry : entityRecipe.getComponents().entrySet()) {
                Component component = entity.addComponent(entry.getKey());
                componentManager.copy(entry.getValue(), component);
                processReferences(componentManager.getType(entry.getKey()), component, entityRecipe.getIdentifier(), result);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processReferences(ComponentType<?> componentType, Component component, ResourceUrn entityRecipeUrn, Map<Name, EntityRef> entityMap) {
        for (PropertyAccessor property : componentType.getPropertyInfo().getPropertiesOfType(EntityRef.class)) {
            EntityRef existing = (EntityRef) property.get(component);
            EntityRef newRef;
            if (existing instanceof EntityRecipe) {
                newRef = entityMap.get(((EntityRecipe) existing).getIdentifier().getFragmentName());
                if (newRef == null) {
                    logger.error("{} references external or unknown entity prefab {}", entityRecipeUrn, existing);
                    newRef = NullEntityRef.get();
                }
            } else if (existing instanceof PrefabRef) {
                newRef = createEntity(((PrefabRef) existing).getPrefab());
            } else {
                logger.error("{} contains unsupported entity ref {}", entityRecipeUrn, existing);
                newRef = NullEntityRef.get();
            }
            property.set(component, newRef);
        }
    }

    /**
     * Create all the entities described by a prefab.
     */
    private Map<Name, EntityRef> createEntityStubs(Prefab prefab) {
        Map<Name, EntityRef> result = Maps.newLinkedHashMap();
        for (EntityRecipe entityRecipe : prefab.getEntityRecipes().values()) {
            result.put(entityRecipe.getIdentifier().getFragmentName(), createEntity());
        }
        return result;
    }

    private EntitySystemState getState() {
        Preconditions.checkState(transactionManager.isActive(), "No active transaction");
        return transactionManager.getContext().getAttachment(EntitySystemState.class).orElseThrow(IllegalStateException::new);
    }

    private EntityState getEntityState(long id) {
        EntitySystemState state = getState();
        Optional<EntityState> entityState = state.getStateFor(id);
        if (entityState.isPresent()) {
            return entityState.get();
        }
        EntityState newState = entityStore.getEntityState(id);
        state.addState(newState);
        return newState;
    }

    private static class EntityRefIterator implements Iterator<EntityRef> {

        private TLongIterator internal;
        private ReferenceAdaptor detailsProvider;

        EntityRefIterator(TLongIterator baseIterator, ReferenceAdaptor detailsProvider) {
            this.internal = baseIterator;
            this.detailsProvider = detailsProvider;
        }

        @Override
        public boolean hasNext() {
            return internal.hasNext();
        }

        @Override
        public EntityRef next() {
            return new CoreEntityRef(detailsProvider, internal.next());
        }
    }
}
