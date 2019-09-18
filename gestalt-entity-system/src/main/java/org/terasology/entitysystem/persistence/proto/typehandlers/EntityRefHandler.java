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

import com.google.common.collect.Lists;

import org.terasology.entitysystem.entity.EntityManager;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.entity.NullEntityRef;
import org.terasology.entitysystem.persistence.proto.ProtoContext;
import org.terasology.entitysystem.persistence.proto.ProtoTypeHandler;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class EntityRefHandler implements ProtoTypeHandler<EntityRef> {

    private EntityManager entityManager;

    public EntityRefHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public ProtoDatastore.Value.Builder serialize(EntityRef instance, Type type, ProtoContext context) {
        ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
        builder.addLong(getId(instance));
        return builder;
    }

    private long getId(EntityRef ref) {
        if (ref == null) {
            return 0;
        } else {
            return ref.getId();
        }
    }

    private EntityRef getEntityRef(int id) {
        if (id != 0) {
            return entityManager.getEntity(id);
        }
        return NullEntityRef.get();
    }

    @Override
    public EntityRef deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
        if (value.getLongCount() > 0) {
            return getEntityRef(value.getInteger(0));
        }
        return NullEntityRef.get();
    }

    @Override
    public ProtoDatastore.Value.Builder serializeCollection(Collection<EntityRef> instance, Type type, ProtoContext context) {
        ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
        for (EntityRef ref : instance) {
            builder.addLong(getId(ref));
        }
        return builder;
    }

    @Override
    public List<EntityRef> deserializeCollection(ProtoDatastore.Value value, Type type, ProtoContext context) {
        List<EntityRef> refs = Lists.newArrayListWithCapacity(value.getLongCount());
        for (int i = 0; i < value.getLongCount(); ++i) {
            refs.add(getEntityRef(value.getInteger(i)));
        }
        return refs;
    }
}
