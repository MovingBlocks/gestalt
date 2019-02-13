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

import org.terasology.entitysystem.persistence.proto.ProtoContext;
import org.terasology.entitysystem.persistence.proto.ProtoTypeHandler;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class CharHandler implements ProtoTypeHandler<Character> {

    @Override
    public ProtoDatastore.Value.Builder serialize(Character instance, Type type, ProtoContext context) {
        ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
        if (instance != null) {
            builder.addInteger(instance.charValue());
        }
        return builder;
    }

    @Override
    public Character deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
        if (value.getIntegerCount() > 0) {
            int intVal = value.getInteger(0);
            return (char) intVal;
        }
        return 0;
    }

    @Override
    public ProtoDatastore.Value.Builder serializeCollection(Collection<Character> instance, Type type, ProtoContext context) {
        ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
        char[] array = new char[instance.size()];
        int index = 0;
        for (Character c : instance) {
            array[index++] = c;
        }
        builder.addString(new String(array));
        return builder;
    }

    @Override
    public List<Character> deserializeCollection(ProtoDatastore.Value value, Type type, ProtoContext context) {
        if (value.getStringCount() > 0) {
            List<Character> result = Lists.newArrayListWithCapacity(value.getString(0).length());
            for (char c : value.getString(0).toCharArray()) {
                result.add(c);
            }
            return result;
        }
        return Lists.newArrayList();
    }
}
