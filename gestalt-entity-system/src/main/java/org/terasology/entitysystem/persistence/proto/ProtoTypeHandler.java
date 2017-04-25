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

package org.terasology.entitysystem.persistence.proto;

import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A ProtoTypeHandler handles the conversion of a type to and froim a ProtoDatastore.Value. Default handling for collections of the values is provided, but can be overridden
 * if a more compact handling is desired.
 */
public interface ProtoTypeHandler<T> {

    /**
     * @param instance The object instance to serialize
     * @param type     The type of the object
     * @param context  The ProtoContext to use for serializing subvalues.
     * @return The serialized value
     */
    ProtoDatastore.Value.Builder serialize(T instance, Type type, ProtoContext context);

    /**
     * @param value   The serialized value to deserialize
     * @param type    The type to deserialize it to
     * @param context The ProtoContext to use for deserializing subvalues
     * @return The deserialized value
     */
    T deserialize(ProtoDatastore.Value value, Type type, ProtoContext context);

    /**
     * This method is implemented to serialize collections of the value. By default each is serialized individually, and then a value created that contains the collection
     * of serialized values. This behavior can be overridden to produce a more condensed serialization structure.
     *
     * @param instance The collection to serialize
     * @param type     The type of the object
     * @param context  The ProtoContext to use for serializing subvalues.
     * @return The serialized collection
     */
    default ProtoDatastore.Value.Builder serializeCollection(Collection<T> instance, Type type, ProtoContext context) {
        ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
        if (instance != null) {
            for (T item : instance) {
                builder.addValue(serialize(item, type, context));
            }
        }
        return builder;
    }

    /**
     * This method is implemented to deserialize collections of the value. It should be overridden when serializeCollection is overridden, to match its behavior.
     *
     * @param value   The serialized value to deserialize
     * @param type    The type to deserialize it to
     * @param context The ProtoContext to use for deserializing subvalues
     * @return A list of deserialized values.
     */
    default List<T> deserializeCollection(ProtoDatastore.Value value, Type type, ProtoContext context) {
        return value.getValueList().stream().map(item -> deserialize(item, type, context)).collect(Collectors.toList());
    }
}
