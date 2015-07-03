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

package org.terasology.module;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.terasology.i18n.I18nMap;
import org.terasology.i18n.gson.I18nMapTypeAdapter;
import org.terasology.naming.Name;
import org.terasology.naming.Version;
import org.terasology.naming.gson.NameTypeAdapter;
import org.terasology.naming.gson.VersionTypeAdapter;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads/writes ModuleMetadata to and from a json format.
 * An example of this format is:
 * <pre>
 *     {
 *          "id": "Core",
 *          "version": "0.1.0",
 *          "displayName": {
 *              "en": "Short Name"
 *          },
 *          "description": {
 *              "en": "A longer description of the module"
 *          },
 *          "dependencies": [
 *              {
 *                  "id": "baseModule",
 *                  "minVersion": "1.0.0",
 *                  "maxVersion": "2.0.0"
 *              }
 *          ]
 *     }
 * </pre>
 * Extensions can be registered, as a mapping of identifier to Type, and optionally with a JsonSerializer/JsonDeserializer or TypeAdapter.
 *
 * @author Immortius
 */
public class ModuleMetadataJsonAdapter {

    private static final Type DEPENDENCY_LIST_TYPE = new TypeToken<List<DependencyInfo>>() {
    }.getType();
    private static final Type STRING_SET_TYPE = new TypeToken<Set<String>>() {
    }.getType();

    private final GsonBuilder builder;
    private volatile Gson cachedGson;
    private final Map<String, Type> extensionMap = Maps.newHashMap();

    public ModuleMetadataJsonAdapter() {
        this.builder = new GsonBuilder()
                .setPrettyPrinting()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                .registerTypeAdapter(Version.class, new VersionTypeAdapter())
                .registerTypeAdapter(Name.class, new NameTypeAdapter())
                .registerTypeAdapter(I18nMap.class, new I18nMapTypeAdapter())
                .registerTypeAdapter(ModuleMetadata.class, new ModuleMetadataTypeAdapter());
    }

    /**
     * Registers an extension for serialization. An entry in the json file with a matching identifier to the extension id will be serialized with the given type
     * and stored as an extension in the metadata.
     *
     * @param extensionId   The identifier for this extension.
     * @param extensionType The type of object this extension holds.
     */
    public void registerExtension(String extensionId, Type extensionType) {
        Preconditions.checkArgument(!ModuleMetadata.RESERVED_IDS.contains(extensionId), "Id '" + extensionId + "' is a reserved id and cannot be used for an extension");
        extensionMap.put(extensionId, extensionType);
    }

    /**
     * Registers an extension for serialization. An entry in the json file with a matching identifier to the extension id will be serialized with the given type
     * and stored as an extension in the resultant metadata. Additionally, the given gson-compatiable typeAdapter will be used to serialize the extensionType.
     *
     * @param extensionId   The identifier for this extension.
     * @param extensionType The type of object this extension holds.
     * @param typeAdapter   The Gson compatible type handler to use for the extension type.
     */
    public void registerExtension(String extensionId, Type extensionType, Object typeAdapter) {
        registerExtension(extensionId, extensionType);
        addTypeHandler(extensionType, typeAdapter);
    }

    /**
     * Adds a type handler to use when serializing the given type
     *
     * @param type        The type to handle
     * @param typeHandler The handler to handle type
     */
    public void addTypeHandler(Type type, Object typeHandler) {
        if (cachedGson != null) {
            cachedGson = null;
        }
        builder.registerTypeAdapter(type, typeHandler);
    }

    /**
     * @param reader A reader providing the json metadata
     * @return The ModuleMetadata represented by the JSON
     * @throws com.google.gson.JsonIOException     if there was a problem reading from the Reader
     * @throws com.google.gson.JsonSyntaxException if json is not valid
     */
    public ModuleMetadata read(Reader reader) {
        if (cachedGson == null) {
            cachedGson = builder.create();
        }
        return cachedGson.fromJson(reader, ModuleMetadata.class);
    }

    /**
     * @param reader A reader providing the json metadata
     * @return The ModuleMetadata represented by the JSON
     * @throws com.google.gson.JsonIOException     if there was a problem reading from the Reader
     * @throws com.google.gson.JsonSyntaxException if json is not valid
     */
    public ModuleMetadata read(JsonReader reader) {
        if (cachedGson == null) {
            cachedGson = builder.create();
        }
        return cachedGson.fromJson(reader, ModuleMetadata.class);
    }

    /**
     * @param writer A writer that receives the json metadata
     * @param data the module metadata that should be written
     * @throws com.google.gson.JsonIOException     if there was a problem writing to the Writer
     */
    public void write(ModuleMetadata data, Writer writer) {
        if (cachedGson == null) {
            cachedGson = builder.create();
        }
        cachedGson.toJson(data, writer);
    }

    /**
     * @param writer A writer that receives the json metadata
     * @param data the module metadata that should be written
     * @throws com.google.gson.JsonIOException     if there was a problem writing to the Writer
     */
    public void write(ModuleMetadata data, JsonWriter writer) {
        if (cachedGson == null) {
            cachedGson = builder.create();
        }
        cachedGson.toJson(data, ModuleMetadata.class, writer);
    }

    private class ModuleMetadataTypeAdapter implements JsonDeserializer<ModuleMetadata>, JsonSerializer<ModuleMetadata> {

        @SuppressWarnings("unchecked")
        @Override
        public ModuleMetadata deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject metadataObject = json.getAsJsonObject();
            ModuleMetadata metadata = new ModuleMetadata();
            for (Map.Entry<String, JsonElement> entry : metadataObject.entrySet()) {
                switch (entry.getKey()) {
                    case ModuleMetadata.ID:
                        metadata.setId(context.<Name>deserialize(entry.getValue(), Name.class));
                        break;
                    case ModuleMetadata.VERSION:
                        metadata.setVersion(context.<Version>deserialize(entry.getValue(), Version.class));
                        break;
                    case ModuleMetadata.DISPLAY_NAME:
                        metadata.setDisplayName(context.<I18nMap>deserialize(entry.getValue(), I18nMap.class));
                        break;
                    case ModuleMetadata.DESCRIPTION:
                        metadata.setDescription(context.<I18nMap>deserialize(entry.getValue(), I18nMap.class));
                        break;
                    case ModuleMetadata.DEPENDENCIES:
                        metadata.getDependencies().addAll((List<DependencyInfo>) context.deserialize(entry.getValue(), DEPENDENCY_LIST_TYPE));
                        break;
                    case ModuleMetadata.REQUIRED_PERMISSIONS:
                        metadata.getRequiredPermissions().addAll((Set<String>) context.deserialize(entry.getValue(), STRING_SET_TYPE));
                        break;
                    default:
                        Type extensionType = extensionMap.get(entry.getKey());
                        if (extensionType != null) {
                            Object extensionObject = context.deserialize(entry.getValue(), extensionType);
                            metadata.setExtension(entry.getKey(), extensionObject);
                        }
                        break;
                }
            }
            return metadata;
        }

        @Override
        public JsonElement serialize(ModuleMetadata src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.add(ModuleMetadata.ID, context.serialize(src.getId()));
            json.add(ModuleMetadata.VERSION, context.serialize(src.getVersion()));
            json.add(ModuleMetadata.DISPLAY_NAME, context.serialize(src.getDisplayName()));
            json.add(ModuleMetadata.DESCRIPTION, context.serialize(src.getDescription()));
            json.add(ModuleMetadata.DEPENDENCIES, context.serialize(src.getDependencies()));
            json.add(ModuleMetadata.REQUIRED_PERMISSIONS, context.serialize(src.getRequiredPermissions()));
            for (String extKey : extensionMap.keySet()) {
                Object extValue = src.getExtension(extKey, Object.class);
                if (extValue != null) {
                    json.add(extKey, context.serialize(extValue));
                }
            }
            return json;
        }
    }

}
