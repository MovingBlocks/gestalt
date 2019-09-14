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

import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.entity.NullEntityRef;
import org.terasology.entitysystem.persistence.proto.ProtoContext;
import org.terasology.entitysystem.persistence.proto.ProtoTypeHandler;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

import java.lang.reflect.Type;

/**
 * When serializing/deserializing prefabs, ignore the entity refs. The prefabs are only stored in case of deserializing entities later without the prefabs present, to provide
 * the base the entity should deserialize on. EntityRefs will always be serialized though, since they are always different from the prefab's values.
 */
public class PrefabEntityRefHandler implements ProtoTypeHandler<EntityRef> {
    @Override
    public ProtoDatastore.Value.Builder serialize(EntityRef instance, Type type, ProtoContext context) {
        return ProtoDatastore.Value.newBuilder();
    }

    @Override
    public EntityRef deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
        return NullEntityRef.get();
    }
}
