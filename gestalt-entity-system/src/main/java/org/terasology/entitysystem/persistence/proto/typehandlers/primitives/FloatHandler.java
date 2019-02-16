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
public class FloatHandler implements ProtoTypeHandler<Float> {

    @Override
    public ProtoDatastore.Value.Builder serialize(Float instance, Type type, ProtoContext context) {
        ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
        if (instance != null) {
            builder.addFloat(instance);
        }
        return builder;
    }

    @Override
    public Float deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
        if (value.getFloatCount() > 0) {
            return value.getFloat(0);
        }
        return 0f;
    }

    @Override
    public ProtoDatastore.Value.Builder serializeCollection(Collection<Float> instance, Type type, ProtoContext context) {
        ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
        builder.addAllFloat(instance);
        return builder;
    }

    @Override
    public List<Float> deserializeCollection(ProtoDatastore.Value value, Type type, ProtoContext context) {
        return value.getFloatList();
    }
}
