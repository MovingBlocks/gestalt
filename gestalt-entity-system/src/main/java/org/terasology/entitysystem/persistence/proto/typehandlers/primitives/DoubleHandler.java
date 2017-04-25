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

package org.terasology.entitysystem.persistence.proto.typehandlers.primitives;

import org.terasology.entitysystem.persistence.proto.ProtoContext;
import org.terasology.entitysystem.persistence.proto.ProtoTypeHandler;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class DoubleHandler implements ProtoTypeHandler<Double> {

    @Override
    public ProtoDatastore.Value.Builder serialize(Double instance, Type type, ProtoContext context) {
        ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
        if (instance != null) {
            builder.addDouble(instance);
        }
        return builder;
    }

    @Override
    public Double deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
        if (value.getDoubleCount() > 0) {
            return value.getDouble(0);
        }
        return 0.0;
    }

    @Override
    public ProtoDatastore.Value.Builder serializeCollection(Collection<Double> instance, Type type, ProtoContext context) {
        ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
        builder.addAllDouble(instance);
        return builder;
    }

    @Override
    public List<Double> deserializeCollection(ProtoDatastore.Value value, Type type, ProtoContext context) {
        return value.getDoubleList();
    }
}
