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

package org.terasology.entitysystem.prefab;

import com.google.common.collect.Queues;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.format.AbstractAssetFileFormat;
import org.terasology.assets.format.AssetDataFile;
import org.terasology.assets.management.AssetManager;
import org.terasology.assets.management.Context;
import org.terasology.assets.management.ContextManager;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.component.ComponentType;
import org.terasology.entitysystem.component.PropertyAccessor;
import org.terasology.entitysystem.component.module.ComponentTypeIndex;
import org.terasology.entitysystem.entity.Component;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.entity.references.NullEntityRef;
import org.terasology.module.ModuleEnvironment;
import org.terasology.util.collection.TypeKeyedMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An {@link org.terasology.assets.format.AssetFileFormat} for loading prefab assets from json files.
 */
public class PrefabJsonFormat extends AbstractAssetFileFormat<PrefabData> {

    public static final String DEFAULT_ROOT_ENTITY_NAME = "root";
    private static final Logger logger = LoggerFactory.getLogger(PrefabJsonFormat.class);

    private final ComponentTypeIndex componentIndex;
    private final ComponentManager componentManager;
    private final AssetManager assetManager;
    private final Gson gson;
    private final ThreadLocal<Deque<PrefabLoader>> loaderStack = new ThreadLocal<Deque<PrefabLoader>>() {
        @Override
        protected Deque<PrefabLoader> initialValue() {
            return Queues.newArrayDeque();
        }
    };

    public PrefabJsonFormat(ModuleEnvironment moduleEnvironment, ComponentManager componentManager, AssetManager assetManager, GsonBuilder gsonBuilder) {
        super("json", "prefab");
        this.componentIndex = new ComponentTypeIndex(moduleEnvironment);
        this.componentManager = componentManager;
        this.assetManager = assetManager;
        gsonBuilder.registerTypeAdapter(EntityRef.class, new EntityRefTypeHandler());
        this.gson = gsonBuilder.create();
    }

    @Override
    public PrefabData load(ResourceUrn urn, List<AssetDataFile> inputs) throws IOException {
        try (Context ignored = ContextManager.beginContext(urn.getModuleName())) {
            try (JsonReader reader = new JsonReader(new BufferedReader(new InputStreamReader(inputs.get(0).openStream())))) {
                reader.setLenient(true);
                JsonParser parser = new JsonParser();
                JsonElement assetRoot = parser.parse(reader);
                PrefabLoader loader = new PrefabLoader();
                loaderStack.get().push(loader);
                try {
                    return loader.load(assetRoot.getAsJsonObject(), urn);
                } finally {
                    loaderStack.get().pop();
                    if (loaderStack.get().isEmpty()) {
                        loaderStack.remove();
                    }
                }
            }
        }
    }

    private class EntityRefTypeHandler implements JsonDeserializer<EntityRef> {

        @Override
        public EntityRef deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String refString = json.getAsJsonPrimitive().getAsString();
            if (ResourceUrn.isValid(refString)) {
                return readPrefabRef(refString);
            } else {
                return readEntityRecipeRef(refString);
            }
        }

        private EntityRef readEntityRecipeRef(String refString) {
            PrefabLoader loader = loaderStack.get().peek();
            EntityRef ref = loader.prefabData.getRecipes().get(new ResourceUrn(loader.prefabUrn, refString));
            if (ref == null) {
                logger.error("Unable to resolve internal reference {}", refString);
                return NullEntityRef.get();
            } else {
                return ref;
            }
        }

        private EntityRef readPrefabRef(String refString) {
            Optional<Prefab> refPrefab = assetManager.getAsset(refString, Prefab.class);
            if (refPrefab.isPresent()) {
                return new PrefabRef(refPrefab.get());
            } else {
                return NullEntityRef.get();
            }
        }
    }

    private class PrefabLoader {
        public static final String INHERIT = "inherit";
        public static final String ENTITIES = "entities";
        public static final String ENTITY = "entity";
        public static final String ROOT = "root";
        private PrefabData prefabData;
        private ResourceUrn prefabUrn;

        public PrefabData load(JsonObject prefabDataObject, ResourceUrn prefabUrn) throws IOException {
            this.prefabUrn = prefabUrn;
            this.prefabData = new PrefabData();

            if (prefabDataObject.has(INHERIT)) {
                inheritPrefab(prefabDataObject.getAsJsonPrimitive(INHERIT).getAsString());
            }

            if (prefabDataObject.has(ENTITIES)) {
                loadEntitiesFromJson(prefabUrn, prefabDataObject.getAsJsonObject(ENTITIES));
            } else if (prefabDataObject.has(ENTITY)) {
                loadEntityFromJson(prefabUrn, prefabDataObject.getAsJsonObject(ENTITY));
            } else {
                throw new IOException("Prefab file missing entity definition");
            }

            if (prefabDataObject.has(ROOT)) {
                prefabData.setRootEntityId(new ResourceUrn(prefabUrn, prefabDataObject.getAsJsonPrimitive(ROOT).getAsString()));
            } else if (prefabData.getRecipes().containsKey(new ResourceUrn(prefabUrn, DEFAULT_ROOT_ENTITY_NAME))) {
                prefabData.setRootEntityId(new ResourceUrn(prefabUrn, DEFAULT_ROOT_ENTITY_NAME));
            }
            return prefabData;
        }

        private void loadEntitiesFromJson(ResourceUrn prefabUrn, JsonObject entities) throws IOException {
            for (Map.Entry<String, JsonElement> entry : entities.entrySet()) {
                addEntityRecipeIfMissing(new ResourceUrn(prefabUrn, entry.getKey()));
            }
            for (Map.Entry<String, JsonElement> entry : entities.entrySet()) {
                ResourceUrn entityUrn = new ResourceUrn(prefabUrn, entry.getKey());
                loadEntityRecipe(prefabData.getRecipes().get(entityUrn), entry.getValue().getAsJsonObject());
            }
        }

        private void loadEntityFromJson(ResourceUrn prefabUrn, JsonObject entity) throws IOException {
            ResourceUrn entityUrn = new ResourceUrn(prefabUrn, DEFAULT_ROOT_ENTITY_NAME);
            addEntityRecipeIfMissing(entityUrn);
            loadEntityRecipe(prefabData.getRecipes().get(entityUrn), entity);
        }

        private void addEntityRecipeIfMissing(ResourceUrn entityUrn) {
            if (!prefabData.getRecipes().containsKey(entityUrn)) {
                prefabData.addEntityPrefab(new EntityRecipe(entityUrn));
            }
        }

        private void inheritPrefab(String parentPrefabUrn) throws IOException {
            Prefab parentPrefab = assetManager.getAsset(parentPrefabUrn, Prefab.class).orElseThrow(() -> new IOException("Unable to resolve parent prefab " + parentPrefabUrn + " for prefab " + prefabUrn));
            for (EntityRecipe recipe : parentPrefab.getEntityRecipes().values()) {
                EntityRecipe copy = new EntityRecipe(new ResourceUrn(prefabUrn, recipe.getIdentifier().getFragmentName()));
                recipe.getComponents().forEach(new TypeKeyedMap.EntryConsumer<Component>() {
                    @Override
                    public <U extends Component> void accept(Class<U> type, U value) {
                        copy.add(type, componentManager.copy(value));
                    }
                });
                prefabData.addEntityPrefab(copy);
            }
        }

        private void loadEntityRecipe(EntityRecipe entityRecipe, JsonObject entityPrefabData) throws IOException {
            for (Map.Entry<String, JsonElement> componentData : entityPrefabData.entrySet()) {
                Class<? extends Component> componentClass = componentIndex.find(componentData.getKey()).orElseThrow(() -> new IOException("Unable to resolve component '" + componentData.getKey() + "'"));
                loadComponent(entityRecipe, componentClass, componentData.getValue().getAsJsonObject());
            }
        }

        private <T extends Component> void loadComponent(EntityRecipe entityRecipe, Class<T> componentClass, JsonObject value) {
            ComponentType<T> type = componentManager.getType(componentClass);
            T component = entityRecipe.getComponent(componentClass).orElseGet(() -> {
                T newComp = type.create();
                entityRecipe.add(componentClass, newComp);
                return newComp;
            });

            type.getPropertyInfo().getProperties().entrySet().stream().filter(properties -> value.has(properties.getKey())).forEach(properties -> {
                PropertyAccessor<T, ?> propertyAccessor = properties.getValue();
                readProperty(properties.getKey(), value, component, propertyAccessor);
            });
        }

        private <T extends Component, U> void readProperty(String name, JsonObject value, T component, PropertyAccessor<T, U> propertyAccessor) {
            propertyAccessor.set(component, gson.fromJson(value.get(name), propertyAccessor.getPropertyType()));
        }

    }

    /**
     * A builder used to aid construction of {@link PrefabJsonFormat}
     */
    public static class Builder {
        private final ModuleEnvironment moduleEnvironment;
        private final ComponentManager componentManager;
        private AssetManager assetManager;
        private GsonBuilder gsonBuilder;

        /**
         * @param moduleEnvironment The module environment, used to discover available component types
         * @param componentManager The manager for components, used to construct and work with components
         * @param assetManager The asset manager, used to look up prefabs.
         */
        public Builder(ModuleEnvironment moduleEnvironment, ComponentManager componentManager, AssetManager assetManager) {
            this.moduleEnvironment = moduleEnvironment;
            this.componentManager = componentManager;
            this.assetManager = assetManager;

            gsonBuilder = new GsonBuilder()
                    .setLenient()
                    .enableComplexMapKeySerialization()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        }

        /**
         * Register a GSON type adapter. All the rules for {@link GsonBuilder}.registerTypeAdapter apply.
         * @param type
         * @param typeAdapter
         * @return The builder.
         */
        public Builder registerTypeAdapter(Type type, Object typeAdapter) {
            gsonBuilder.registerTypeAdapter(type, typeAdapter);
            return this;
        }

        /**
         * Register a GSON type hierarchy adapter. All the rules for {@link GsonBuilder}.registerTypeHierarchyAdapter apply.
         * @param type
         * @param typeAdapter
         * @return The builder
         */
        public Builder registerTypeHierarchyAdapter(Class<?> type, Object typeAdapter) {
            gsonBuilder.registerTypeHierarchyAdapter(type, typeAdapter);
            return this;
        }

        /**
         * Register a GSON type adapter factory. All the rules for {@link GsonBuilder}.registerTypeAdapterFactory apply.
         * @param factory
         * @return The builder
         */
        public Builder registerTypeAdapterFactory(TypeAdapterFactory factory) {
            gsonBuilder.registerTypeAdapterFactory(factory);
            return this;
        }

        /**
         * @return The new PrefabJsonFormat.
         */
        public PrefabJsonFormat create() {
            return new PrefabJsonFormat(moduleEnvironment, componentManager, assetManager, gsonBuilder);
        }
    }
}
