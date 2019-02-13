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

import org.terasology.assets.ResourceUrn;
import org.terasology.entitysystem.persistence.proto.ProtoContext;
import org.terasology.entitysystem.persistence.proto.ProtoTypeHandler;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

import java.lang.reflect.Type;

/**
 *
 */
public class ResourceUrnHandler implements ProtoTypeHandler<ResourceUrn> {

    @Override
    public ProtoDatastore.Value.Builder serialize(ResourceUrn instance, Type type, ProtoContext context) {
        if (instance == null) {
            return context.serialize("", String.class);
        }
        return context.serialize(instance.toString(), String.class);
    }

    @Override
    public ResourceUrn deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
        String stringUrn = context.deserialize(value, String.class);
        if (stringUrn.isEmpty()) {
            return null;
        }
        return new ResourceUrn(stringUrn);
    }
}
