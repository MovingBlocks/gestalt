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

package org.terasology.entitysystem.transaction.pipeline;

import org.terasology.util.collection.TypeKeyedMap;

import java.util.Optional;
import java.util.function.Supplier;

/**
 *
 */
public class TransactionContext {

    private TypeKeyedMap<Object> attachments = new TypeKeyedMap<>();

    public <T> T getOrAttach(Class<T> type, Supplier<T> supplier) {
        T result = attachments.get(type);
        if (result == null) {
            result = supplier.get();
            attachments.put(type, result);
        }
        return result;
    }

    public <T> Optional<T> getAttachment(Class<T> type) {
        return Optional.ofNullable(attachments.get(type));
    }

    public <T> void attach(Class<T> type, T data) {
        attachments.put(type, data);
    }

}
