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

package org.terasology.entitysystem.persistence.proto;

import com.google.common.collect.BiMap;

import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 * Serialization/deserialization context for persistence. Allows a type handler to
 * <ul>
 * <li>Request a type to be serialized/deserialized by its own handler</li>
 * <li>Request a collection of a type to be serialized/deserialized by its own handler</li>
 * <li>Convert a string value to an id, recording it in an id table if not present</li>
 * <li>Convert an id to a string value</li>
 * </ul>
 * Regarding the id tables, the context will maintain a number of id tables, identified by a string table type. When the id for a value which isn't already in the table
 * is requested, a new id will be generated. These tables should then be persisted separately. These tables are intended to allow the replacement of strings with numbers
 * to save space.
 */
public interface ProtoContext {

    /**
     * Deserializes a value.
     *
     * @param value The value to deserialize
     * @param type  The type to deserialize to
     * @param <T>   The type to deserialize to
     * @return The deserialized value
     */
    <T> T deserialize(ProtoDatastore.Value value, Class<T> type);

    /**
     * Deserializes a value.
     *
     * @param value The value to deserialize
     * @param type  The type to deserialize to
     * @param <T>   The type to deserialize to
     * @return The deserialized value
     */
    <T> T deserialize(ProtoDatastore.Value value, Type type);

    /**
     * Deserializes a collection of a value
     *
     * @param value The serialized collection to deserialize
     * @param type  The type to deserialize to
     * @param <T>   The type to deserialize to
     * @return The deserialized collection
     */
    <T> List<T> deserializeCollection(ProtoDatastore.Value value, Class<T> type);

    /**
     * Deserializes a collection of a value
     *
     * @param value The serialized collection to deserialize
     * @param type  The type to deserialize to
     * @param <T>   The type to deserialize to
     * @return The deserialized collection
     */
    <T> List<T> deserializeCollection(ProtoDatastore.Value value, Type type);

    /**
     * Serializes a value, using the class of the value.
     *
     * @param value The value to serialize
     * @return The serialized value
     */
    ProtoDatastore.Value.Builder serialize(Object value);

    /**
     * Serializes an object
     *
     * @param value The value to serialize
     * @param type  The type of the value
     * @return The serialized value
     */
    ProtoDatastore.Value.Builder serialize(Object value, Type type);

    /**
     * Serializes a collection of object.
     *
     * @param value The collection to serialize
     * @param type  The type of the objects in the collection
     * @return The serialized collection
     */
    ProtoDatastore.Value.Builder serializeCollection(Collection<Object> value, Type type);

    /**
     * Requests an id for the given string value, as part of lookup table
     *
     * @param tableId The id of the lookup table the string should be in
     * @param value   The value to get the id of
     * @return The id for value. generating a new id if required
     */
    int getIdFor(String tableId, String value);

    /**
     * @param tableId The id of the lookup table to get the string from
     * @param id      The id of the value to get
     * @return The string associated with the given id.
     * @throws org.terasology.entitysystem.persistence.proto.exception.PersistenceException If no string is associated with the given id
     */
    String getValueFor(String tableId, int id);

    /**
     * @param tableId The id of the lookup table to fetch
     * @return The lookup table
     */
    BiMap<Integer, String> getLookupTable(String tableId);

    /**
     * @return A list of the ids of all the lookup tables
     */
    Collection<String> getLookupTableIds();
}
