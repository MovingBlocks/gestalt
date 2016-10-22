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

package org.terasology.entitysystem.persistence.proto.typehandlers;

import com.google.common.collect.Lists;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.entity.inmemory.CoreEntityRef;
import org.terasology.entitysystem.entity.inmemory.NewEntityRef;
import org.terasology.entitysystem.core.NullEntityRef;
import org.terasology.entitysystem.persistence.proto.ProtoContext;
import org.terasology.entitysystem.persistence.proto.ProtoTypeHandler;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
        } else if (ref instanceof CoreEntityRef) {
            return ref.getId();
        } else if (ref instanceof NewEntityRef) {
            Optional<EntityRef> innerEntityRef = ((NewEntityRef) ref).getInnerEntityRef();
            if (innerEntityRef.isPresent() && innerEntityRef.get() instanceof CoreEntityRef) {
                return innerEntityRef.get().getId();
            }
        }
        return 0;
    }

    private EntityRef getEntityRef(long id) {
        if (id != 0) {
            return entityManager.getEntity(id);
        }
        return NullEntityRef.get();
    }

    @Override
    public EntityRef deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
        if (value.getLongCount() > 0) {
            return getEntityRef(value.getLong(0));
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
            refs.add(getEntityRef(value.getLong(i)));
        }
        return refs;
    }
}
