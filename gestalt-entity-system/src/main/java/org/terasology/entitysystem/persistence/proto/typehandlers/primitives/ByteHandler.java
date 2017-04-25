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

import com.google.common.collect.Lists;
import com.google.common.primitives.SignedBytes;
import com.google.protobuf.ByteString;
import org.terasology.entitysystem.persistence.proto.ProtoContext;
import org.terasology.entitysystem.persistence.proto.ProtoTypeHandler;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class ByteHandler implements ProtoTypeHandler<Byte> {

    @Override
    public ProtoDatastore.Value.Builder serialize(Byte instance, Type type, ProtoContext context) {
        ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
        if (instance != null) {
            builder.addInteger(instance);
        }
        return builder;
    }

    @Override
    public Byte deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
        if (value.getIntegerCount() > 0) {
            int byteValue = value.getInteger(0);
            if (byteValue >= Byte.MIN_VALUE && byteValue <= Byte.MAX_VALUE) {
                return SignedBytes.checkedCast(byteValue);
            }
        }
        return 0;
    }

    @Override
    public ProtoDatastore.Value.Builder serializeCollection(Collection<Byte> instance, Type type, ProtoContext context) {
        ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
        byte[] array = new byte[instance.size()];
        int index = 0;
        for (Byte b : instance) {
            array[index++] = b;
        }
        builder.setBytes(ByteString.copyFrom(array));
        return builder;
    }

    @Override
    public List<Byte> deserializeCollection(ProtoDatastore.Value value, Type type, ProtoContext context) {
        return Lists.newArrayList(value.getBytes().iterator());
    }
}
