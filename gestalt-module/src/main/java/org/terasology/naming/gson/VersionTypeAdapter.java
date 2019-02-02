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
package org.terasology.naming.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.terasology.naming.Version;
import org.terasology.naming.exception.VersionParseException;

import java.lang.reflect.Type;

/**
 * Gson type adapter for serializing and deserializing Versions
 *
 * @author Immortius
 */
public class VersionTypeAdapter implements JsonDeserializer<Version>, JsonSerializer<Version> {

    @Override
    public Version deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            return new Version(json.getAsString());
        } catch (VersionParseException e) {
            throw new JsonParseException("Invalid version '" + json.getAsString() + "'", e);
        }
    }

    @Override
    public JsonElement serialize(Version src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src.toString());
    }
}
