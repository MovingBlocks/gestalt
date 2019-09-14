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

package org.terasology.entitysystem.entity.store;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.ResourceUrn;
import org.terasology.entitysystem.component.management.ComponentType;
import org.terasology.entitysystem.component.management.PropertyAccessor;
import org.terasology.entitysystem.component.Component;
import org.terasology.entitysystem.component.ComponentIterator;
import org.terasology.entitysystem.component.ComponentStore;
import org.terasology.entitysystem.entity.EntityIterator;
import org.terasology.entitysystem.entity.EntityManager;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.entity.NullEntityRef;
import org.terasology.entitysystem.prefab.EntityRecipe;
import org.terasology.entitysystem.prefab.GeneratedFromRecipeComponent;
import org.terasology.entitysystem.prefab.Prefab;
import org.terasology.entitysystem.prefab.PrefabRef;
import org.terasology.naming.Name;
import org.terasology.util.collection.TypeKeyedMap;
import org.terasology.util.collection.UniqueQueue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class CoreEntityManager implements EntityManager {

    private static final Logger logger = LoggerFactory.getLogger(CoreEntityManager.class);

    private static final double EXTENSION_RATE = 1.5;
    private static final int DEFAULT_CAPACITY = 1024;

    private final EntitySupplier entitySupplier;
    private final ReadWriteLock locks = new ReentrantReadWriteLock();
    private final ImmutableMap<Class<? extends Component>, ComponentStore<?>> componentStores;

    private final UniqueQueue<Integer> freedIdQueue = new UniqueQueue<>();

    private EntityRef[] entities;
    private int nextId = 0;

    public CoreEntityManager(Collection<ComponentStore<?>> componentStores) {
        this(componentStores, DEFAULT_CAPACITY);
    }

    public CoreEntityManager(Collection<ComponentStore<?>> componentStores, EntitySupplier entitySupplier) {
        this(componentStores, entitySupplier, DEFAULT_CAPACITY);
    }

    public CoreEntityManager(Collection<ComponentStore<?>> componentStores, int capacity) {
        this(componentStores, ManagedEntityRef::new, capacity);
    }

    public CoreEntityManager(Collection<ComponentStore<?>> componentStores, EntitySupplier entitySupplier, int capacity) {
        ImmutableMap.Builder<Class<? extends Component>, ComponentStore<?>> builder = ImmutableMap.builder();
        for (ComponentStore<?> store : componentStores) {
            builder.put(store.getType().getComponentClass(), store);
            store.extend(capacity);
        }
        this.componentStores = builder.build();
        this.entitySupplier = entitySupplier;
        this.entities = new ManagedEntityRef[capacity];
        Arrays.fill(this.entities, NullEntityRef.get());
    }

    @Override
    public EntityRef getEntity(int id) {
        Lock lock = locks.readLock();
        lock.lock();
        try {
            return entities[id];
        } finally {
            lock.unlock();
        }
    }

    @Override
    public EntityRef createEntity() {
        Lock lock = locks.readLock();
        lock.lock();
        try {
            int id;
            if (freedIdQueue.isEmpty()) {
                id = nextId++;
                if (id >= entities.length) {
                    extendStorage();
                }
            } else {
                id = freedIdQueue.remove();
            }
            EntityRef result = entitySupplier.create(this, id);
            entities[id] = result;
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public EntityRef createEntity(Collection<Component> components) {
        EntityRef entity = createEntity();
        entity.setComponents(components);
        return entity;
    }

    void freeEntityId(int id) {
        Lock lock = locks.writeLock();
        lock.lock();
        try {
            freedIdQueue.add(id);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        return nextId - freedIdQueue.size();
    }

    @Override
    public EntityIterator iterate(Component... components) {
        List<Component> componentsOrdered = Lists.newArrayList(components);
        componentsOrdered.sort(Comparator.comparing(x -> componentStores.get(x.getClass()).iterationCost()));
        List<ComponentStore> stores = componentsOrdered.stream().map(x -> componentStores.get(x.getClass())).collect(Collectors.toList());

        ComponentIterator drivingIterator = stores.get(0).iterate();

        return new ComponentsIterator(drivingIterator, componentsOrdered, stores);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component<T>> ComponentStore<T> getComponentStore(Class<T> componentType) {
        return (ComponentStore<T>) componentStores.get(componentType);
    }

    // TODO: Better home for prefab instantiation code?

    @Override
    public EntityRef createEntity(Prefab prefab) {
        Map<Name, EntityRef> entities = createEntities(prefab);
        return entities.get(prefab.getRootEntityUrn().getFragmentName());
    }

    @Override
    public Map<Name, EntityRef> createEntities(Prefab prefab) {
        Map<Name, EntityRef> result = createPrefabEntities(prefab);
        populatePrefabEntities(prefab, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private void populatePrefabEntities(Prefab prefab, Map<Name, EntityRef> result) {
        for (EntityRecipe entityRecipe : prefab.getEntityRecipes().values()) {
            List<Component> components = Lists.newArrayList();
            GeneratedFromRecipeComponent prefabComponent = new GeneratedFromRecipeComponent();
            prefabComponent.setEntityRecipe(entityRecipe.getIdentifier());
            components.add(prefabComponent);

            for (TypeKeyedMap.Entry<? extends Component> entry : entityRecipe.getComponents().entrySet()) {
                ComponentType componentType = componentStores.get(entry.getKey()).getType();
                Component component = componentType.createCopy(entry.getValue());
                processReferences(componentType, component, entityRecipe.getIdentifier(), result);
            }
            EntityRef entity = result.get(entityRecipe.getIdentifier().getFragmentName());
            entity.setComponents(components);
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
    private Map<Name, EntityRef> createPrefabEntities(Prefab prefab) {
        Map<Name, EntityRef> result = Maps.newLinkedHashMap();
        for (EntityRecipe entityRecipe : prefab.getEntityRecipes().values()) {
            result.put(entityRecipe.getIdentifier().getFragmentName(), createEntity());
        }
        return result;
    }

    @Override
    public Iterable<EntityRef> allEntities() {
        return Collections.unmodifiableList(Arrays.asList(entities));
    }

    /**
     * Extends the internal storage of the entity manager and all the component stores, if they are exhausted
     */
    private void extendStorage() {
        Lock lock = locks.writeLock();
        lock.lock();
        try {
            if (entities.length <= nextId) {
                int newSize = Math.min((int) (entities.length * EXTENSION_RATE), entities.length + 1);
                EntityRef[] newEntities = new EntityRef[newSize];
                System.arraycopy(entities, 0, newEntities, 0, entities.length);
                Arrays.fill(newEntities, entities.length, newEntities.length, NullEntityRef.get());
                entities = newEntities;

                for (ComponentStore<?> store : componentStores.values()) {
                    store.extend(entities.length);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterable<ComponentStore<?>> allComponentStores() {
        return componentStores.values();
    }

    public interface EntitySupplier {
        EntityRef create(CoreEntityManager entityManager, int id);
    }

    private class ComponentsIterator implements EntityIterator {
        private ComponentIterator drivingIterator;
        private List<Component> components;
        private List<ComponentStore> componentStores;

        private ComponentsIterator(ComponentIterator drivingIterator, List<Component> components, List<ComponentStore> componentStores) {
            this.drivingIterator = drivingIterator;
            this.components = components;
            this.componentStores = componentStores;
        }

        @SuppressWarnings("unchecked")
        public boolean next() {
            while (drivingIterator.next()) {
                int entityId = drivingIterator.getEntityId();
                boolean found = true;
                for (int i = 1; i < componentStores.size(); ++i) {
                    if (!componentStores.get(i).get(entityId, components.get(i))) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    drivingIterator.getComponent(components.get(0));
                    return true;
                }
            }
            return false;
        }

        @Override
        public EntityRef getEntity() {
            return CoreEntityManager.this.getEntity(drivingIterator.getEntityId());
        }
    }
}
