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

package org.terasology.entitysystem.persistence.proto.persistors;

import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.component.ComponentType;
import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.persistence.proto.ComponentManifest;
import org.terasology.entitysystem.persistence.proto.ComponentMetadata;
import org.terasology.entitysystem.persistence.proto.EntityRecipeManifest;
import org.terasology.entitysystem.persistence.proto.EntityRecipeMetadata;
import org.terasology.entitysystem.persistence.proto.ProtoPersistence;
import org.terasology.entitysystem.persistence.proto.exception.PersistenceException;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;
import org.terasology.entitysystem.prefab.GeneratedFromEntityRecipeComponent;
import org.terasology.util.collection.TypeKeyedMap;

import java.util.Optional;

/**
 * Serializes and Deserializes entities. If an entity was generated from an EntityRecipe, then it is serialized and deserialized as a delta on that recipe. This allows
 * changes to the recipe to be reflected in existing entities when they are next loaded.
 */
public class PrefabAwareEntityPersistor implements EntityPersistor {

    private final SimpleEntityPersistor defaultPersistor;
    private final ComponentPersistor componentPersistor;
    private final ComponentManifest componentManifest;
    private final ComponentManager componentManager;
    private final EntityRecipeManifest recipeManifest;

    public PrefabAwareEntityPersistor(ComponentManager componentManager, ProtoPersistence context, ComponentManifest componentManifest, EntityRecipeManifest recipeManifest) {
        this.componentManager = componentManager;
        this.componentManifest = componentManifest;
        this.recipeManifest = recipeManifest;
        this.componentPersistor = new ComponentPersistor(context, componentManifest);
        this.defaultPersistor = new SimpleEntityPersistor(context, componentManifest);
    }

    @Override
    public ProtoDatastore.EntityData.Builder serialize(EntityRef entity) {
        Optional<GeneratedFromEntityRecipeComponent> metadata = entity.getComponent(GeneratedFromEntityRecipeComponent.class);
        if (metadata.isPresent()) {
            GeneratedFromEntityRecipeComponent prefabInfo = metadata.get();
            Optional<EntityRecipeMetadata> entityRecipeMetadata = recipeManifest.getEntityRecipeMetadata(prefabInfo.getEntityRecipe());
            if (entityRecipeMetadata.isPresent()) {
                ProtoDatastore.EntityData.Builder builder = serializeEntityDelta(entityRecipeMetadata.get().getComponents(), entity);
                builder.setRecipe(entityRecipeMetadata.get().getId());
                return builder;
            } else {
                return defaultPersistor.serialize(entity);
            }

        } else {
            return defaultPersistor.serialize(entity);
        }
    }

    private ProtoDatastore.EntityData.Builder serializeEntityDelta(TypeKeyedMap<Component> baseComponents, EntityRef entity) {
        ProtoDatastore.EntityData.Builder builder = ProtoDatastore.EntityData.newBuilder();
        builder.setId(entity.getId());
        for (TypeKeyedMap.Entry<? extends Component> component : entity.getComponents().entrySet()) {
            if (GeneratedFromEntityRecipeComponent.class.isAssignableFrom(component.getKey())) {
                continue;
            }
            Component recipeComponent = baseComponents.get(component.getKey());
            if (recipeComponent != null) {
                if (!recipeComponent.equals(component.getValue())) {
                    builder.addComponent(componentPersistor.serializeDelta(recipeComponent, component.getValue()));
                }
            } else {
                builder.addComponent(componentPersistor.serialize(component.getValue()));
            }
            for (Class<? extends Component> componentType : baseComponents.keySet()) {
                if (!entity.getComponent(componentType).isPresent()) {
                    builder.addRemovedComponentIndex(componentManifest.getComponentMetadata(componentType).getId());
                }
            }
        }
        return builder;
    }

    @Override
    public EntityRef deserialize(ProtoDatastore.EntityData data, EntityManager entityManager) {
        if (data.hasRecipe()) {
            EntityRecipeMetadata recipe = recipeManifest.getEntityRecipeMetadata(data.getRecipe()).orElseThrow(() -> new PersistenceException("Missing entity recipe with id '" + data.getRecipe() + "'"));
            return deserializeWithEntityDelta(recipe, data, entityManager);
        } else {
            return defaultPersistor.deserialize(data, entityManager);
        }

    }

    private EntityRef deserializeWithEntityDelta(EntityRecipeMetadata base, ProtoDatastore.EntityData data, EntityManager entityManager) {
        EntityRef entity = entityManager.getEntity(data.getId());
        copyBaseComponents(base, entity);
        applyComponentChanges(data, entity);
        dropRemovedComponents(data, entity);

        GeneratedFromEntityRecipeComponent sourceEntityRecipe = entity.addComponent(GeneratedFromEntityRecipeComponent.class);
        sourceEntityRecipe.setEntityRecipe(base.getUrn());

        return entity;
    }

    private void dropRemovedComponents(ProtoDatastore.EntityData data, EntityRef entity) {
        for (int componentIndex : data.getRemovedComponentIndexList()) {
            ComponentMetadata<?> componentMetadata = componentManifest.getComponentMetadata(componentIndex).orElseThrow(() -> new PersistenceException("Missing component metadata with id '" + componentIndex));
            componentMetadata.getComponentType().ifPresent((x) -> entity.removeComponent(x.getComponentClass()));
        }
    }

    private void applyComponentChanges(ProtoDatastore.EntityData data, EntityRef entity) {
        for (ProtoDatastore.ComponentData componentData : data.getComponentList()) {
            ComponentMetadata<?> componentMetadata = componentManifest.getComponentMetadata(componentData.getTypeIndex()).orElseThrow(() -> new PersistenceException("Missing component metadata with id '" + componentData.getTypeIndex()));
            if (componentMetadata.getComponentType().isPresent()) {
                ComponentType type = componentMetadata.getComponentType().get();
                Component c = (Component) entity.getComponent(type.getComponentClass()).orElseGet(() -> entity.addComponent(type.getComponentClass()));
                componentPersistor.deserializeOnto(componentData, c);
            }
        }
    }

    private void copyBaseComponents(EntityRecipeMetadata base, EntityRef entity) {
        for (TypeKeyedMap.Entry<? extends Component> entry : base.getComponents().entrySet()) {
            componentManager.copy(entry.getValue(), entity.addComponent(entry.getKey()));
        }
    }


}
