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

package org.terasology.entitysystem.persistence.proto.typehandlers.collections;

import com.google.common.collect.Sets;

import org.terasology.entitysystem.persistence.proto.ProtoContext;
import org.terasology.entitysystem.persistence.proto.ProtoTypeHandler;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;
import org.terasology.reflection.ReflectionUtil;

import java.lang.reflect.Type;
import java.util.Set;

/**
 *
 */
public class SetHandler implements ProtoTypeHandler<Set> {

    @Override
    public ProtoDatastore.Value.Builder serialize(Set instance, Type type, ProtoContext context) {
        return context.serializeCollection(instance, getContentType(type));
    }

    @Override
    public Set deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
        return Sets.newLinkedHashSet(context.deserializeCollection(value, getContentType(type)));
    }

    private Type getContentType(Type type) {
        Type contentType = ReflectionUtil.getTypeParameter(type, 0);
        if (contentType != null) {
            return contentType;
        } else {
            throw new IllegalArgumentException("Set has unknown content type.");
        }
    }
}
