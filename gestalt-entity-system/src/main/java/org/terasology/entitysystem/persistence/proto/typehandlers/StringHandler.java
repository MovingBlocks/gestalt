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

package org.terasology.entitysystem.persistence.proto.typehandlers;

import org.terasology.entitysystem.persistence.proto.ProtoContext;
import org.terasology.entitysystem.persistence.proto.ProtoTypeHandler;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class StringHandler implements ProtoTypeHandler<String> {

    @Override
    public ProtoDatastore.Value.Builder serialize(String instance, Type type, ProtoContext context) {
        ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
        if (instance != null) {
            builder.addString(instance);
        }
        return builder;
    }

    @Override
    public String deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
        String result = null;
        if (value.getStringCount() > 0) {
            result = value.getString(0);
        }
        return result;
    }

    @Override
    public ProtoDatastore.Value.Builder serializeCollection(Collection<String> instance, Type type, ProtoContext context) {
        ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
        for (String item : instance) {
            if (item != null) {
                builder.addString(item);
            } else {
                builder.addString("");
            }
        }
        return builder;
    }

    @Override
    public List<String> deserializeCollection(ProtoDatastore.Value value, Type type, ProtoContext context) {
        return value.getStringList();
    }
}
