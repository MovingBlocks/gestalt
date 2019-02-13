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

package org.terasology.entitysystem.persistence.proto.typehandlers.collections;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.terasology.entitysystem.persistence.proto.ProtoContext;
import org.terasology.entitysystem.persistence.proto.ProtoTypeHandler;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;
import org.terasology.reflection.ReflectionUtil;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class MapHandler implements ProtoTypeHandler<Map> {

    @Override
    public ProtoDatastore.Value.Builder serialize(Map instance, Type type, ProtoContext context) {
        Type keyType = getKeyType(type);
        Type contentType = getValueType(type);
        List keys = Lists.newArrayList();
        List values = Lists.newArrayList();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) instance).entrySet()) {
            keys.add(entry.getKey());
            values.add(entry.getValue());
        }
        ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
        builder.addValue(context.serializeCollection(keys, keyType));
        builder.addValue(context.serializeCollection(values, contentType));
        return builder;
    }

    @Override
    public Map deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
        Type keyType = getKeyType(type);
        Type contentType = getValueType(type);
        if (value.getValueCount() >= 2) {
            ProtoDatastore.Value keyValues = value.getValue(0);
            ProtoDatastore.Value contentValues = value.getValue(1);
            List keys = context.deserializeCollection(keyValues, keyType);
            List values = context.deserializeCollection(contentValues, contentType);
            int size = Math.min(keys.size(), values.size());
            Map map = new LinkedHashMap(size);
            for (int i = 0; i < size; ++i) {
                map.put(keys.get(i), values.get(i));
            }
            return map;
        }
        return Maps.newLinkedHashMap();
    }

    private Type getValueType(Type type) {
        Type contentType = ReflectionUtil.getTypeParameter(type, 1);
        if (contentType != null) {
            return contentType;
        } else {
            throw new IllegalArgumentException("Map has unknown value type.");
        }
    }

    private Type getKeyType(Type type) {
        Type contentType = ReflectionUtil.getTypeParameter(type, 0);
        if (contentType != null) {
            return contentType;
        } else {
            throw new IllegalArgumentException("Map has unknown key type.");
        }
    }
}
