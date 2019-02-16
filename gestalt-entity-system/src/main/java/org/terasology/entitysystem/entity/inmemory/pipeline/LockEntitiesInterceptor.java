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

package org.terasology.entitysystem.entity.inmemory.pipeline;

import org.terasology.entitysystem.entity.inmemory.EntityStore;
import org.terasology.entitysystem.entity.inmemory.EntitySystemState;
import org.terasology.entitysystem.transaction.pipeline.TransactionContext;
import org.terasology.entitysystem.transaction.pipeline.TransactionInterceptor;

/**
 * This transaction interceptor locks all the entities involved in a transaction.
 */
public class LockEntitiesInterceptor implements TransactionInterceptor {

    private EntityStore entityStore;

    public LockEntitiesInterceptor(EntityStore entityStore) {
        this.entityStore = entityStore;
    }

    @Override
    public void handle(TransactionContext context) {
        context.getAttachment(EntitySystemState.class).ifPresent((state) -> {
            state.setLock(entityStore.lock(state.getInvolvedEntityIds()));
        });
    }

}
