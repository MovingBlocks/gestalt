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

package org.terasology.i18n.gson;

import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.terasology.i18n.I18nMap;

import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Map;

/**
 * A gson type adapter for serializing and deserializing I18nMap.
 *
 * @author Immortius
 */
public class I18nMapTypeAdapter implements JsonDeserializer<I18nMap>, JsonSerializer<I18nMap> {

    @Override
    public I18nMap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonPrimitive()) {
            return new I18nMap(json.getAsString());
        } else if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            Map<Locale, String> values = Maps.newHashMapWithExpectedSize(obj.entrySet().size());
            for (Map.Entry<String, JsonElement> item : obj.entrySet()) {
                if (item.getValue().isJsonPrimitive()) {
                    values.put(Locale.forLanguageTag(item.getKey()), item.getValue().getAsString());
                } else {
                    throw new JsonParseException("Expected locale string pair, found " + item.getKey() + "'" + item.getValue() + "'");
                }
            }
            return new I18nMap(values);
        } else {
            throw new JsonParseException("Invalid I18nMap: '" + json + "'");
        }
    }

    @Override
    public JsonElement serialize(I18nMap src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        for (Map.Entry<Locale, String> item : src) {
            result.addProperty(item.getKey().toLanguageTag(), item.getValue());
        }
        return result;
    }
}
