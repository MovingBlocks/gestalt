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

package org.terasology.entitysystem.persistence.proto.persistors;

import org.terasology.entitysystem.entity.EntityManager;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

/**
 * Interface for classes that serialize and deserialize entities.
 */
public interface EntityPersistor {

    /**
     * It is expected that the entity is part of an entity manager with an active transaction
     *
     * @param entity The entity to serialized
     * @return The protobuf serialized form of the entity
     */
    ProtoDatastore.EntityData.Builder serialize(EntityRef entity);

    /**
     * @param data          The protobuf serialized entity to deserialize
     * @param entityManager The entity manager to deserialize the entity into. A transaction is expected to be active.
     * @return The deserialized entity
     */
    EntityRef deserialize(ProtoDatastore.EntityData data, EntityManager entityManager);
}
