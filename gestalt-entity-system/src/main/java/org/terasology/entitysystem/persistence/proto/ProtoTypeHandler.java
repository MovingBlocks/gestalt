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

import com.google.common.collect.Lists;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public interface ProtoTypeHandler<T> {

    ProtoDatastore.Value.Builder serialize(T instance, Type type, ProtoContext context);

    T deserialize(ProtoDatastore.Value value, Type type, ProtoContext context);

    default ProtoDatastore.Value.Builder serializeCollection(Collection<T> instance, Type type, ProtoContext context)  {
        ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
        if (instance != null) {
            for (T item : instance) {
                builder.addValue(serialize(item, type, context));
            }
        }
        return builder;
    }

    default List<T> deserializeCollection(ProtoDatastore.Value value, Type type, ProtoContext context) {
        return value.getValueList().stream().map(item -> deserialize(item, type, context)).collect(Collectors.toList());
    }
}
