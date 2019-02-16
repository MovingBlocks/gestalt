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

package org.terasology.entitysystem.persistence.proto;

import com.google.common.reflect.TypeToken;

/**
 * A ProtoTypeHandlerFactory is a factory that can be registered with a ProtoPersistence to produce ProtoTypeHandlers for types without explicit type handlers registered.
 */
public interface ProtoTypeHandlerFactory {

    /**
     * @param type The type a handle is being requested for
     * @param <T>
     * @return Either a ProtoTypeHandler, or null if this factory cannot produce a ProtoTypeHandler for the supplied type.
     */
    <T> ProtoTypeHandler<T> createTypeHandler(TypeToken<T> type);

}
